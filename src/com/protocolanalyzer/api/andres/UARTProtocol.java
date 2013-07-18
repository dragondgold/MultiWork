package com.protocolanalyzer.api.andres;

import android.util.Log;

public class UARTProtocol extends Protocol{

	private static final boolean DEBUG = false;
	
	private int baudRate = 9600;
	private boolean is9Bits = false;
	
	public UARTProtocol(long freq) {
		super(freq);
	}

	/**
	 * Decodifica el LogicBitSet (grupo de bits) que contiene el canal segun protocolo UART
	 * @param startTime, offset del tiempo que se desea agregar
	 * @return LogicData que contiene Strings con los datos decodificados siendo:
	 * 			"\S" -> bit de Start
	 * 			[numero] -> valor de los 8 bits
	 * 			"\P" -> bit de Stop
	 * @see http://stackoverflow.com/questions/2978569/android-java-append-string-int (para StringBuilder)
	 * @see http://www.dosideas.com/noticias/java/339-string-vs-stringbuffer-vs-stringbuilder.html (datos sobre StringBuilder y StringBuffer)
	 */
	@Override
	public void decode(double startTime) {
			
		if(DEBUG) Log.i("UARTDecode", "UART Protocol decode");
		if(DEBUG) Log.i("UARTDecode", "Source lenght: " + logicData.length());
		if(DEBUG) Log.i("UARTDecode", "Dart: " 			+ logicData.toString());
		
		int n = 0;			// Index
		int tempIndex;		// Index para guardado temporal
		final int dataBits;
		final double sampleTime = 1.0d/sampleFrec;					// Cuanto tiempo demora cada muestreo
		final int samplesPerBit = (int)Math.ceil((1.0d/baudRate) / sampleTime);
		final int halfBit = (int)Math.ceil(samplesPerBit/2.0);		// Tiempo hasta la mitad del bit
		
		if(is9Bits) dataBits = 9;
		else dataBits = 8;
		
		if(DEBUG) Log.i("UARTDecode", "samplesPerBit: " + samplesPerBit);
		if(DEBUG) Log.i("UARTDecode", "halfBit: " + halfBit);
		
		// Comprueba que halla al menos 3 samples por cada bit para asegura un buen muestreo
		if( ((1.0d/baudRate) / sampleTime) < 3.0d) return;
		
		// Si llege al final del array de datos salgo del bucle (el (samplesPerBit*10 es porque necesito
		// al menos 10 bits para la trama del UART, si hay menos esta incompleta)
		while(n <= (logicData.length()-(samplesPerBit*10))){
			n = logicData.nextFallingEdge(n); if(n == -1) break;		// Busco un flanco de bajada (Start)
			if(DEBUG) Log.i("UARTDecode", "-------------------------------");
			if(DEBUG) Log.i("UARTDecode", "n Falling Edge: " + n);
			// Voy a la mitad del bit de Start para verificar si es 0
			n += halfBit;
			if(DEBUG) Log.i("UARTDecode", "n Start: " + n);
			if(logicData.get(n) == false){ 							// Si el siguiente bit es 0 entonces es el bit de Start
				tempIndex = n - halfBit;						// Lugar de inicio del bit de Start
				n += samplesPerBit;								// Sumo un bit para estar a la mitad del otro
				if(DEBUG) Log.i("UARTDecode", "n de inicio de byte: " + n);
				int dataByte = 0;
				// Empiezo leyendo desde el LSB y lo voy colocando en el byte
				for(int bit = 0; bit < dataBits; ++bit){		// Voy tomando los bits y armo el byte del dato
					dataByte = LogicHelper.bitSet(dataByte, logicData.get(n), (dataBits-1) - bit);
					n += samplesPerBit;
				}
				if(DEBUG) Log.i("UARTDecode", "dataByte: " + Integer.toBinaryString(dataByte) + " - dataByte: " + dataByte);
				n += samplesPerBit;
				// Si el siguiente bit es de Stop entonces escribo en el String los datos
				// Sino es simplemente un error y no escribo nada
				if(logicData.get(n) == true){	
					if(DEBUG) Log.i("UARTDecode", "n stopBit: " + n);
					// Bit de Start
					addString("\\S", tempIndex*sampleTime, (tempIndex+samplesPerBit)*sampleTime, startTime);		
					// Posicion de inicio al bit siguiente al de Start (dato)
					addString(""+dataByte, (tempIndex+samplesPerBit)*sampleTime,
							(tempIndex+samplesPerBit+(samplesPerBit*dataBits)*sampleTime), startTime);
					// Bit de Stop
					addString("\\P", ((n-halfBit)*sampleTime), ((n-halfBit)+samplesPerBit)*sampleTime, startTime);	
				}
				n -= halfBit;
			}
			if(DEBUG) Log.i("UARTDecode", "n before while: " + n);
			if(DEBUG) Log.i("UARTDecode", "Condition: " + (logicData.length()-(samplesPerBit*10.0)) );
		}
		if(DEBUG) Log.i("UARTDecode", "Exit while()");
		if(DEBUG) Log.i("UARTDecode", "String size: " + mDecodedData.size());
	}

	@Override
	public ProtocolType getProtocol() {
		return ProtocolType.UART;
	}
	
	/**
	 * Define la velocidad en baudios del UART
	 * @param baud
	 */
	public void setBaudRate (int baud){
		baudRate = baud;
	}
	
	/**
	 * Obtiene la velocidad en baudios del UART. Por defecto 9600 baudios.
	 * @return
	 */
	public int getBaudRate() {
		return baudRate;
	}
	
	/**
	 * Define si se transmite con el modo de 9 bits o no
	 * @param state
	 */
	public void set9BitsMode (boolean state){
		is9Bits = state;
	}
	
	public boolean is9BitsMode (){
		return is9Bits;
	}

}
