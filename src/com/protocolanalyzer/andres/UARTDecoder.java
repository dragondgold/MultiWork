package com.protocolanalyzer.andres;

import android.util.Log;


public class UARTDecoder {

	private static final boolean DEBUG = true;
	
	/**
	 * Decodifica el BitSet (grupo de bits) que le es pasado segun el protocolo UART
	 * @param dataSource con el canal
	 * @return LogicData que contiene Strings con los datos decodificados siendo:
	 * 			"S" -> bit de Start
	 * 			[numero] -> valor de los 8 bits
	 * 			"P" -> bit de Stop
	 * Ademas contiene las posiciones del array donde se decodifican los datos para luego ser mostrados en graficos y
	 * el tipo de protocolo
	 * @see http://stackoverflow.com/questions/2978569/android-java-append-string-int (para StringBuilder)
	 * @see http://www.dosideas.com/noticias/java/339-string-vs-stringbuffer-vs-stringbuilder.html (datos sobre StringBuilder y StringBuffer)
	 */
	public static void UARTDecode (LogicData dataSource) {
		
		LogicBit data = dataSource.getBits();
		
		if(DEBUG) Log.i("UARTDecode", "LogicHelper -> UARTDecode()");
		if(DEBUG) Log.i("UARTDecode", "dataSource size: " + dataSource.getSize() );
		if(DEBUG) Log.i("UARTDecode", "data size: " + data.length());
		if(DEBUG) Log.i("UARTDecode", "data: " + data.toString());
		
		int n = 0;			// Index
		int tempIndex;		// Index para guardado temporal
		final int dataBits;
		final double sampleTime = 1.0d/LogicData.getSampleRate();			// Cuanto tiempo demora cada muestreo
		final int samplesPerBit = (int)Math.ceil((1.0d/dataSource.getBaudRate()) / sampleTime);
		final int halfBit = (int)Math.ceil(samplesPerBit/2.0);				// Tiempo hasta la mitad del bit
		
		if(dataSource.is9BitsTransmission()) dataBits = 9;
		else dataBits = 8;
		
		if(DEBUG) Log.i("UARTDecode", "samplesPerBit: " + samplesPerBit);
		if(DEBUG) Log.i("UARTDecode", "halfBit: " + halfBit);
		
		// Comprueba que halla al menos 3 samples por cada bit para asegura un buen muestreo
		if( ((1.0d/dataSource.getBaudRate()) / sampleTime) < 3.0d) return;
		
		// Si llege al final del array de datos salgo del bucle (el (samplesPerBit*10 es porque necesito
		// al menos 10 bits para la trama del UART, si hay menos esta incompleta)
		while(n <= (data.length()-(samplesPerBit*10))){
			n = data.nextFallingEdge(n); if(n == -1) break;		// Busco un flanco de bajada (Start)
			if(DEBUG) Log.i("", "-------------------------------");
			if(DEBUG) Log.i("", "n Falling Edge: " + n);
			// Voy a la mitad del bit de Start para verificar si es 0
			n += halfBit;
			if(DEBUG) Log.i("", "n Start: " + n);
			if(data.get(n) == false){ 							// Si el siguiente bit es 0 entonces es el bit de Start
				tempIndex = n - halfBit;						// Lugar de inicio del bit de Start
				n += samplesPerBit;								// Sumo un bit para estar a la mitad del otro
				if(DEBUG) Log.i("UARTDecode", "n de inicio de byte: " + n);
				int dataByte = 0;
				// Empiezo leyendo desde el LSB y lo voy colocando en el byte
				for(int bit = 0; bit < dataBits; ++bit){				// Voy tomando los bits y armo el byte del dato
					dataByte = LogicHelper.bitSet(dataByte, data.get(n), (dataBits-1) - bit);
					n += samplesPerBit;
				}
				if(DEBUG) Log.i("UARTDecode", "dataByte: " + Integer.toBinaryString(dataByte) + " - dataByte: " + dataByte);
				n += samplesPerBit;
				// Si el siguiente bit es de Stop entonces escribo en el String los datos
				// Sino es simplemente un error y no escribo nada
				if(data.get(n) == true){	
					if(DEBUG) Log.i("UARTDecode", "n stopBit: " + n);
					dataSource.addStringS("S", 		tempIndex*sampleTime);		// Bit de Start
					dataSource.addStringS(""+dataByte, 	(tempIndex*sampleTime)+(samplesPerBit*sampleTime));	// Posicion de inicio al bit siguiente al de Start
					dataSource.addStringS("P", 		((n-halfBit)*sampleTime));	// Bit de Stop
				}
				n -= halfBit;
			}
			if(DEBUG) Log.i("UARTDecode", "n before while: " + n);
			if(DEBUG) Log.i("UARTDecode", "Condition: " + (data.length()-(samplesPerBit*10.0)) );
		}
		if(DEBUG) Log.i("UARTDecode", "Exit while()");
		if(DEBUG) Log.i("UARTDecode", "String size: " + dataSource.getStringCount());
	}
	
}
