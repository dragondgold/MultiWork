package com.protocolanalyzer.api.andres;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/**
 * Esta clase contiene los LogicData de cada uno de los canales para que al decodificar se tengan todos los canales desde donde
 * obtener el clock y otras lineas necesarias dependientes de cada protocolo
 * @author andres
 */
public class LogicDataSet {

	private static final boolean DEBUG = true;
	private List<LogicData> mLogicData = new ArrayList<LogicData>();
	
	/**
	 * Agrega un canal al DataSet
	 * @param data, LogicData a agregar
	 */
	public void addLogicData (LogicData data) {
		mLogicData.add(data);
	}
	
	/**
	 * Obtiene un LogicData en el index especificado
	 * @param index, posicion del LogicData
	 * @return LogicData en el index seleccionado
	 */
	public LogicData getLogicDataAt(int index) {
		return mLogicData.get(index);
	}
	
	/**
	 * Obtiene el número de canal que actúa como clock
	 * @param n, numero del canal del cual obtener su canal de clock
	 * @return numero del canal de clock
	 */
	private int getClockChannelNumber (int n) {
		return mLogicData.get(n).getClockSource();
	}
	
	/**
	 * Pasa el ReceptionBuffer a los buffer de cada canal
	 * @author Andres
	 * @param data es un array de bytes con los bytes que se reciben del analizador logico, siendo el bit 0 el estado
	 * del canal 0 hasta el bit 4 el estado del canal 4
	 */
	 public void BufferToChannel (final byte[] data) {
		
		if(DEBUG) Log.i("LogicHelper-BufferToChannel", "Lenght data array: " + data.length);
		
		// Borro los bits anteriores porq ya no me hacen falta
		for(int n=0; n < mLogicData.size(); ++n) mLogicData.get(n).clearDataBits();
		
		for(int n=0; n < data.length; ++n){						// Voy a traves de los bytes recibidos
			for(int bit=0; bit < mLogicData.size(); ++bit){		// Voy a traves de los 4 bits de cada byte
				if(LogicHelper.bitTest(data[n],bit)){			// Si es 1
					mLogicData.get(bit).setBit(n);				// bit es el numero del canal y el bit a poner a 1 o 0
				}
				else{											// Si es 0
					mLogicData.get(bit).clearBit(n);
				}
			}
		}
	}
	
	/**
	 * Decodifica un canal con el protocolo correspondiente del canal
	 * @param channelNumber, n�mero del canal a analizar
	 */
	public void decode(int channelNumber, double startTime) {
		switch(mLogicData.get(channelNumber).getProtocol()) {
			case I2C:
				mLogicData.get(channelNumber).setStartTime(startTime);
				mLogicData.get(channelNumber).clearDecodedData();
				I2CDecoder.i2cProtocolDecode(mLogicData.get(channelNumber), mLogicData.get(getClockChannelNumber(channelNumber)));
				break;
			case UART:
				mLogicData.get(channelNumber).setStartTime(startTime);
				mLogicData.get(channelNumber).clearDecodedData();
				UARTDecoder.UARTDecode(mLogicData.get(channelNumber));
			case SPI:
				break;
			case CLOCK:
			case NONE:
				break;
		}
	}
	
}
