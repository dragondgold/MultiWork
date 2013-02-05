package com.protocolanalyzer.andres;

import android.util.Log;

public class I2CDecoder {

	private static final boolean DEBUG = false;
	
	/**
	 * Decodifica el BitSet (grupo de bits) que le es pasado segun el protocolo I2C
	 * @author Andres Torti
	 * @param dataSource es un LogicData con los bit de los datos (SDA) y sus caracteristicas
	 * @param clockSource es un LogicData con los bits del clock del I2C (SCL) y sus caracteristicas
	 * Los Strings que se escriben de acuerdo a los datos que se decodifican son:
	 * 			"S" -> condicion de Start
	 * 			"Sr" -> condicion de Start repetida
	 * 			"P" -> condicion de parada
	 * 			"\R" -> modo lectura
	 * 			"\W" -> modo escritura
	 * 			"ACK" -> bit de ACK
	 * 			"NAK" -> bit de NO NACK
	 * 			[numero] -> valor de los 8 bits transmitidos
	 * 			"A([numero])" -> valor de la direccion
	 * 			"E" -> ERROR
	 * @see http://www.i2c-bus.org/repeated-start-condition/
	 * @see Imagenes I2CSignal en la carpeta Extras del proyecto
	 */
	public static void I2CDecode(LogicData dataSource, final LogicData clockSource) {

		final LogicBit data = dataSource.getBits();
		final LogicBit clock = clockSource.getBits();
		
		if(DEBUG) Log.i("I2CDecode", "----------------------------------------");
		if(DEBUG) Log.i("I2CDecode", "Length data: " + data.length());
		if(DEBUG) Log.i("I2CDecode", "Length clock: " + clock.length());
		if(DEBUG) Log.i("I2CDecode", "BitSet data: " + data.toString());
		if(DEBUG) Log.i("I2CDecode", "BitSet clock: " + clock.toString());
		
		// Definiciones de las diferentes condiciones de la máquina de estados
		final int startCondition = 0;
		final int readByte = 1;
		final int readAddress = 2;
		
		// Condicion incial de la máquina de estados
		int I2Cstate = startCondition;
		
		/** Tiempo que demora un muestreo */
		final double sampleTime = 1.0d/LogicData.getSampleRate();		
		final double clockFrequency;
		/** Es el index máximo al cual se puede llegar sin que falten datos para completar la trama */
		final int maxIndex;
		/** Periodo del Clock SCL */
		final int clockDuration;	
		int index = 0;					// Index para pasar a travez de los muestreos
		int tempIndex = 0;				// Index temporal
		int i2cData = 0;				// Dato del I2C
		boolean rwBit = false;			// Estado del bit RW
		boolean ackBit = false;		// Estado del bit ACK/NACK
		
		// Tiene que haber al menos 9 pulsos de clock para testear con clock.nextSetBitToTest(); para que no le falten bits
		// en medio de la comprobacion de una trama (como minimo una trama de I2C requiere 9 pulsos de clock)
		int i = 0, c = 0, last9items[] = new int[9];
		boolean isMoreThan9 = false;
		while(true){
			i = clock.nextSetBitToTest(i);
			// Siempre guardo las ultimas 9 muestras q tomé
			if(i == -1) break;
			else {
				last9items[c++] = i;
				if(c > 8){ c = 0; isMoreThan9 = true; } 
			}
		}
		
		// Resto entre dos flancos de subida (que me asegure antes que hubiera) para calcular el tiempo y la frecuencia
		clockFrequency = (int)(1/((clock.nextRisingEdge(clock.nextRisingEdge(0)) - clock.nextRisingEdge(0)) * sampleTime));
		clockDuration = clock.nextRisingEdge(clock.nextRisingEdge(0)) - clock.nextRisingEdge(0);
		// Comprueba que halla al menos 3 samples por cada bit para asegura un buen muestreo
		if( ((double)clockDuration / sampleTime) < 3.0d) return;
		
		if(isMoreThan9) maxIndex = last9items[c];	// Si hay al menos 9 pulsos de clock decodifico los datos ( 7 bits de direccion + 1 RW + 1 ACK) o (8 bits de dato + 1 ACK)
		else return;					// Sino salgo
		
		while(index < maxIndex) {		// El bucle se repita mientras halla datos suficientes (9 pulsos de clock por lo menos)
			
			// Máquina de estados
			switch(I2Cstate) {
			
			// Condicion de START, leo la direccion y el bit RW
			case startCondition:								// Condicion de START
				index = data.nextFallingEdge(index);			// Busco un flanco de bajada en SDA
				if(index == -1) break;							// Si no hay ninguno salgo
				if(clock.get(index)) {							// Si el flanco de bajada fue mientras SCL estaba en alto (Start)
					if(DEBUG) Log.i("I2CDecode", "Start Condition - index: " + index);
					dataSource.addStringS("S", (index*sampleTime), clock.nextSetBitToTest(index)*sampleTime);
					I2Cstate = readAddress;
				}
				break;
				
			case readAddress:
				// En este lugar empiezan los bits de la dirección
				tempIndex = clock.nextSetBitToTest(index);
				i2cData = 0;
				// Obtengo los 7 bits de direccion, primero se envia el MSB hasta el LSB
				for(int bit = 7; bit > 0; --bit){			
					index = clock.nextSetBitToTest(index);
					if(index == -1) break;
					i2cData = LogicHelper.bitSet(i2cData, data.get(index), bit-1);	// Compruebo SDA en la mitad del bit SCL
					// El (bit-1) es porque la dirección es de 7 bits y un 0 más a la izquierda modificaría el valor:
					// 11010100 != 1101010
				}
				if(DEBUG) Log.i("I2CDecode", "I2CAddress: " + Integer.toBinaryString(i2cData & 0xFF) + " -> " + i2cData);
				// Direccion
				dataSource.addStringS(""+i2cData, tempIndex*sampleTime, clock.nextSetBitToTest(index)*sampleTime);	
	
				// Obtengo el bit RW
				index = clock.nextSetBitToTest(index);
				rwBit = data.get(index);
					
				if(rwBit) dataSource.addStringS("\\R", (index*sampleTime), clock.nextSetBitToTest(index)*sampleTime);	
				else dataSource.addStringS("\\W", (index*sampleTime), clock.nextSetBitToTest(index)*sampleTime);
					
				// Obtengo el bit de ACK
				index = clock.nextSetBitToTest(index);	
				ackBit = data.get(index);	
					
				if(!ackBit) dataSource.addStringS("ACK", (index*sampleTime), (index+clockDuration)*sampleTime);
				else dataSource.addStringS("NAK", (index*sampleTime), (index+clockDuration)*sampleTime);
				
				// Compruebo el estado del ACK, si hay ACK leo el byte sino condicion de Start de nuevo
				if(!ackBit) I2Cstate = readByte;
				else I2Cstate = startCondition;
				break;
			
			// Leo un byte de dato
			case readByte:		
				if(DEBUG) Log.i("I2CDecode", "Read Byte - index: " + index);
				I2Cstate = startCondition;	// Si por algun error tengo que salir va a ir a buscar un Start de nuevo
				// Le sumo medio periodo de clock sino coincide con el tiempo del ACK que viene de readAddress
				tempIndex = index + (clockDuration/2);
				i2cData = 0;
				// Empiezo a recibir los 8 bits desde el MSB
				for(int bit = 7; bit >= 0; --bit){			
					index = clock.nextSetBitToTest(index);
					if(index == -1) break;
					i2cData = LogicHelper.bitSet(i2cData, data.get(index), bit);		
				}
				if(DEBUG) Log.i("I2CDecode", "I2CData: " + Integer.toBinaryString(i2cData & 0xFF) + " -> " + i2cData);
				
				// Byte leido
				dataSource.addStringS(""+i2cData, (tempIndex*sampleTime), clock.nextSetBitToTest(index)*sampleTime);
				
				// Obtengo el bit de ACK
				index = clock.nextSetBitToTest(index);
				ackBit = data.get(index);
				
				if(!ackBit) dataSource.addStringS("ACK", (index*sampleTime), clock.nextSetBitToTest(index)*sampleTime);
				else dataSource.addStringS("NAK", (index*sampleTime), clock.nextSetBitToTest(index)*sampleTime);
				
				// Condicion de STOP (Verifico primero que existan los flancos)
				if( (clock.nextRisingEdge(index) != -1) && (data.nextRisingEdge(index) != -1) ){ 
					if((data.get(clock.nextRisingEdge(index)) == false) && (clock.get(data.nextRisingEdge(index)) == true)) {
						if(DEBUG) Log.i("I2CDecode", "STOP - index: " + index);
						index = clock.nextRisingEdge(index);
						// Condicion de parada
						dataSource.addStringS("P", (index*sampleTime), (index+clockDuration)*sampleTime);
					}
				}
				// Start Repetido
				else if((data.nextFallingEdge(index) != -1) && clock.get(data.nextFallingEdge(index)) == true){	
					if(DEBUG) Log.i("I2CDecode", "Start Repetido - index: " + index);
				}
				// Vuelvo a condicion de Start pero indico error ya que no hubo parada ni start repetido
				else {
					dataSource.addStringS("E", (index*sampleTime), (index+clockDuration)*sampleTime);
				}
				break;
			}
		}
	}
	
}
