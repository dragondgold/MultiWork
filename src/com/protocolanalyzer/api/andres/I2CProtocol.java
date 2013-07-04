package com.protocolanalyzer.api.andres;

import android.util.Log;

public class I2CProtocol extends Protocol{

	private static final boolean DEBUG = false;
	private Clock clockSource = null;
	
	public I2CProtocol(long freq) {
		super(freq);
	}
	
	/**
	 * Decodifica el LogicBitSet (grupo de bits) que contiene el canal segun protocolo I2C
	 * @author Andres Torti
	 * @param startTime, offset del tiempo que se desea agregar
	 * Los Strings que se escriben de acuerdo a los datos que se decodifican son:
	 * 			"S" 	-> condicion de Start
	 * 			"Sr" 	-> condicion de Start repetida
	 * 			"P" 	-> condicion de parada
	 * 			"\R" 	-> modo lectura
	 * 			"\W" 	-> modo escritura
	 * 			"ACK" 	-> bit de ACK
	 * 			"NAK" 	-> bit de NO NACK
	 * 			[numero] -> valor de los 8 bits transmitidos
	 * 			"A([numero])" -> valor de la direccion
	 * 			"E" 	-> ERROR
	 * @see http://www.i2c-bus.org/repeated-start-condition/
	 * @see Imagenes I2CSignal en la carpeta Extras del proyecto
	 */
	@Override
	public void decode(double startTime) {
		
		if(DEBUG) Log.i("I2CDecode", "I2C Protocol decode");
		
		if(clockSource == null) 
			throw new ClockNotDefinedException("Se debe definir una fuente de clock para el protocolo I2C");
		
		final LogicBitSet clock = clockSource.getChannelBitsData();
		
		if(DEBUG) Log.i("I2CDecode", "Data lenght: " 	+ 	logicData.length());
		if(DEBUG) Log.i("I2CDecode", "Clock lenght: " 	+ 	clock.length());
		if(DEBUG) Log.i("I2CDecode", "BitSet data: " 	+ 	logicData.toString());
		if(DEBUG) Log.i("I2CDecode", "BitSet clock: " 	+ 	clock.toString());
		
		// Definiciones de las diferentes condiciones de la máquina de estados
		final int startCondition = 0;
		final int readByte = 1;
		final int readAddress = 2;
		final int stopCondition = 3;
		
		// Condicion incial de la máquina de estados
		int I2Cstate = startCondition;
		
		/** Tiempo que demora un muestreo */
		final double sampleTime = 1.0d/sampleFrec;		
		/** Periodo del Clock SCL */
		final int clockDuration;	
		int index = 0;					// Index para pasar a travez de los muestreos
		int[] data;						// Datos leidos del I2C
		boolean rwBit = false;			// Estado del bit RW
		boolean ackBit = false;		// Estado del bit ACK/NACK
		
		if(clock.nextRisingEdge(0) == -1 || clock.nextRisingEdge(clock.nextRisingEdge(0)) == -1) return;
		
		// Resto entre dos flancos de subida (que me asegure antes que hubiera) para calcular el tiempo
		clockDuration = clock.nextRisingEdge(clock.nextRisingEdge(0)) - clock.nextRisingEdge(0);
		// Comprueba que halla al menos 3 samples por cada bit para asegura un buen muestreo
		if( ((double)clockDuration / sampleTime) < 3) return;
		
		// El bucle se repita mientras halla datos
		while(index < logicData.length()) {
			
			// Máquina de estados
			switch(I2Cstate) {
			
			// Condicion de START
			case startCondition:					
				// Si existe una condición de Start leo, sino salgo de la función
				if(nextStartCondition(index) != -1){
					if(DEBUG) Log.i("I2CDecode", "Start Condition - index: " + index);
					addString("S", (index*sampleTime), nextStartCondition(index)*sampleTime, startTime);
					I2Cstate = readAddress;
				}
				else return;
				break;
				
			// Condicion de STOP
			case stopCondition:
				data = getStopCondition(index, clockDuration);
				addString("P", data[0]*sampleTime, data[1]*sampleTime, startTime);
				index = data[1];
				I2Cstate = startCondition;
				break;
				
			// Leo dirección
			case readAddress:
				// Leo los 7 bits de la dirección
				data = readBits(index, 7);
				if(data == null) return;
				try {
					if(DEBUG) Log.i("I2CDecode", "I2CAddress: " + Integer.toBinaryString(data[2] & 0xFF) + " -> " + data[2]);
					// Direccion
					addString("A("+data[2]+")", data[0]*sampleTime,data[1]*sampleTime, startTime);	
		
					// Obtengo el bit RW (bit 8)
					index = clock.nextSetBitToTest(data[1]);
					rwBit = logicData.get(index);
						
					if(rwBit) addString("\\R", data[1]*sampleTime, index*sampleTime, startTime);	
					else addString("\\W", data[1]*sampleTime, index*sampleTime, startTime);
						
					// Obtengo el bit de ACK
					index = clock.nextSetBitToTest(index);	
					ackBit = logicData.get(index);	
						
					if(!ackBit) addString("ACK", index*sampleTime, (index+clockDuration)*sampleTime, startTime);
					else addString("NAK", index*sampleTime, (index+clockDuration)*sampleTime, startTime);
					
					// Si existe una condicion de STOP despues de esto no tengo que leer nada
					if (existsStopCondition(index, clockDuration)){
						I2Cstate = stopCondition;
						break;
					}
					
					// Compruebo el estado del ACK, si hay ACK leo el byte sino es un error porque tengo NACK
					// y no hay condicion de STOP
					if(!ackBit) I2Cstate = readByte;
					else {
						I2Cstate = startCondition;
						addString("E", index*sampleTime, (index+clockDuration)*sampleTime, startTime);
					}
				} catch (IndexOutOfBoundsException e) {
					if(DEBUG) Log.i("I2CDecode", "IndexOutOfBoundsException - Returning");
					return;
				}
				break;
			
			// Leo un byte de dato
			case readByte:		
				if(DEBUG) Log.i("I2CDecode", "Read Byte - index: " + index);
				I2Cstate = startCondition;	// Si por algun error tengo que salir va a ir a buscar un Start de nuevo
				
				data = readBits(index, 8);
				if(data == null) return;
				try{
					if(DEBUG) Log.i("I2CDecode", "I2CData: " + Integer.toBinaryString(data[2] & 0xFF) + " -> " + data[2]);
					
					// Byte leido
					addString(""+data[2], data[0]*sampleTime, data[1]*sampleTime, startTime);
					
					// Obtengo el bit de ACK
					index = clock.nextSetBitToTest(data[1]);
					ackBit = logicData.get(index);
					
					if(!ackBit) addString("ACK", data[1]*sampleTime, index*sampleTime, startTime);
					else addString("NAK", data[1]*sampleTime, index*sampleTime, startTime);
					
					// Si existe una condicion de STOP despues de esto no tengo que leer nada
					if (existsStopCondition(index, clockDuration)){
						I2Cstate = stopCondition;
						break;
					}
					
					// Compruebo el estado del ACK, si hay ACK leo otro byte sino es un error porque tengo NACK
					// y no hay condicion de STOP
					if(!ackBit) I2Cstate = readByte;
					else {
						I2Cstate = startCondition;
						addString("E", index*sampleTime, (index+clockDuration)*sampleTime, startTime);
					}
				} catch (IndexOutOfBoundsException e) {
					if(DEBUG) Log.i("I2CDecode", "IndexOutOfBoundsException - Returning");
					return;
				}
				break;
			}
		}
	}
	
	/**
	 * Retorna el index donde se produce el flanco de bajada de SDA (cuando ya es 0) en caso de que
	 * exista condicion de START de otro modo retorna -1
	 * @param index index donde empezar la busqueda de la condicion de start
	 * @return
	 */
	private int nextStartCondition (int index){
		if (index < 0) return -1;
		// Busco primero que SDA y SCL esten en alto
		for(int n = index; n < logicData.length(); ++n){
			if(logicData.get(n) && clockSource.getChannelBitsData().get(n)){
				index = n;
				break;
			}
		}
		
		int fallIndex = logicData.nextFallingEdge(index);
			
		// Condicion START: Flanco de bajada en SDA mientras SCL esta en alto
		while(fallIndex != -1 && clockSource.getChannelBitsData().get(fallIndex) == false){
			fallIndex = logicData.nextFallingEdge(fallIndex);
		}
			
		return fallIndex;
	}
	
	/**
	 * Lee nBits de la linea SCL empezando por el MSB
	 * @param index
	 * @param nBits
	 * @return array int[] siendo [0] = index de inicio; [1] = index final; [2] = dato leido
	 */
	private int[] readBits (int index, int nBits){
		int[] i2cData = new int[3];
		// Index de inicio
		if(clockSource.getChannelBitsData().nextSetBitToTest(index) != -1) 
			i2cData[0] = clockSource.getChannelBitsData().nextSetBitToTest(index);
		
		// Leo la cantidad de bits especificados empezando por el MSB
		for(int bit = nBits; bit > 0; --bit){			
			index = clockSource.getChannelBitsData().nextSetBitToTest(index);
			if(index == -1) return null;
			i2cData[2] = LogicHelper.bitSet(i2cData[2], logicData.get(index), bit-1);	// Compruebo SDA en la mitad del bit SCL
			// El (bit-1) es porque la dirección es de 7 bits y un 0 más a la izquierda modificaría el valor:
			// 11010100 != 1101010
		}
		// Index final
		i2cData[1] = index;
		return i2cData;
	}
	
	/**
	 * Determina si hay a continuación una condición de STOP
	 * @param index index donde empezar
	 * @param clockDuration duración del período de clock en cantidad de muestras
	 * @return true si existe condicion de STOP, false de otro modo
	 */
	private boolean existsStopCondition (int index, int clockDuration){
		int dataRisingEdge = -1;
		
		int clockRisingEdge = clockSource.getChannelBitsData().nextRisingEdge(index);
		if(clockRisingEdge != -1) dataRisingEdge = logicData.nextRisingEdge(clockRisingEdge);
		
		if(dataRisingEdge != -1 && clockSource.getChannelBitsData().get(dataRisingEdge+(clockDuration/2)+1)
				&& logicData.get(dataRisingEdge+(clockDuration/2)+1)) return true;
		else return false;
	}
	
	private int[] getStopCondition (int index, int clockDuration) {
		int dataRisingEdge = -1;
		
		int clockRisingEdge = clockSource.getChannelBitsData().nextRisingEdge(index);
		if(clockRisingEdge != -1) dataRisingEdge = logicData.nextRisingEdge(clockRisingEdge);
		
		if(dataRisingEdge != -1 && clockSource.getChannelBitsData().get(dataRisingEdge+(clockDuration/2)+1)
				&& logicData.get(dataRisingEdge+(clockDuration/2)+1)) 
			return new int[] {clockRisingEdge, dataRisingEdge+(clockDuration/2)+1};
		
		else return null;
	}
	
	@Override
	public ProtocolType getProtocol() {
		return ProtocolType.I2C;
	}
	
	/**
	 * Fuente de clock SCL
	 * @param channel
	 */
	public void setClockSource (Clock channel){
		clockSource = channel;
	}
	
	public Clock getClockSource() {
		return clockSource;
	}

}
