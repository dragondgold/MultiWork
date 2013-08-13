package com.protocolanalyzer.api.andres;

import org.apache.http.util.ByteArrayBuffer;

import android.util.Log;

public class LogicHelper {
    
	private static final String TAG = "LogicHelper";
	private static final boolean DEBUG = true;
	
	/**
	 * Testea un bit dentro de un byte
	 * @author Andres Torti
	 * @param a byte para testear
	 * @param bit numero de bit a testear 0-7
	 * @see http://en.wikipedia.org/wiki/Mask_(computing) (Mask bits)
	 */
	public static boolean bitTest (byte a, int bit) {
		return (a & (1 << bit)) != 0;
	}
	
	/**
	 * Setea un bit dentro de un byte al estado dado
	 * @param a byte el cual modificar
	 * @param state true o false
	 * @param bit numero del bit a modificar (0 a 7)
	 * @return byte modificado
	 * @see http://en.wikipedia.org/wiki/Mask_(computing) (Mask bits)
	 */
	public static byte bitSet (byte a, boolean state, int bit){
		if(bit < 0) return a;
		if(state == true) return (byte)(a | (1 << bit));
		else return (byte)(a & ~(1 << bit));
	}
	
	/**
	 * Setea un bit dentro de un byte al estado dado
	 * @param a byte el cual modificar
	 * @param state true o false
	 * @param bit numero del bit a modificar (0 a 7)
	 * @return byte modificado
	 * @see http://en.wikipedia.org/wiki/Mask_(computing) (Mask bits)
	 */
	public static int bitSet (int a, boolean state, int bit){
		if(bit < 0) return a;
		if(state == true) return (a | (1 << bit));
		else return (a & ~(1 << bit));
	}
	
	/**
	 * Parseador de bits, se la pasa un String con los 1 y 0 que se desean y los coloca en un LogicBitSet
	 * con el numero de muestreos por bit que se deseen
	 * @param data String con los 1 y 0, los demas caracteres son ignorados
	 * @param samplesPerBit numero de bits por cada 1 y 0
	 * @param times indica la cantidad de veces que se repite el String pasado para crear cadenas mas largas
	 * @return LogicBitSet con los bits de acuerdo al String y los samplesPerBit
	 */
	public static LogicBitSet bitParser (final String data, final int samplesPerBit, final int times){
		LogicBitSet bitSet = new LogicBitSet();
		
		if(DEBUG) Log.i("BitParse", "samplesPerBit: " + samplesPerBit);
		if(DEBUG) Log.i("BitParse", "String: " + data);
		if(DEBUG) Log.i("BitParse", "String Length: " + data.length());
		
		int bitPosition = 0;
		
		for(int l = 0; l < times; ++l){
			for(int n = 0; n < data.length(); ++n){
				if(data.charAt(n) == '1'){
					for(int t = 0; t < samplesPerBit; ++t){
						bitSet.set(bitPosition++);
					}
				}
				else if(data.charAt(n) == '0'){
					for(int t = 0; t < samplesPerBit; ++t){
						bitSet.clear(bitPosition++);
					}
				}
			}
		}
		if(DEBUG) Log.i("BitParse", "BitSet length: " + bitSet.length());
		return bitSet;
	}
	
	/**
	 * Convierte dos bytes a un int
	 * @param LSB LSB byte
	 * @param MSB MSB byte
	 * @return int con los 16 bits
	 */
	public static int byteToInt (final byte LSB, final byte MSB){
		int temp = 0;
	    temp = temp | (MSB & 0xFF);		// Coloco el MSB
	    temp <<= 8;    					// Desplazo el byte
	    temp = temp | (LSB & 0xFF);  	// Coloco el LSB
	    return temp;
	}
	
	/**
	 * Decodifica el algoritmo de compresión Run Lenght
	 * @param data ByteArrayBuffer con los bytes comprimidos
	 * @return array de byte[] con los datos descomprimidos
	 */
	public static byte[] runLenghtDecode (final ByteArrayBuffer data){
		
		int lenght = data.length();
		int repeat;
		ByteArrayBuffer returnData = new ByteArrayBuffer(data.length());
		
		for(int n = 0; n < lenght; n += 3){
			repeat = LogicHelper.byteToInt((byte)data.byteAt(n), (byte)data.byteAt(n+1));
			for(int k = 0; k < repeat; ++k){
				returnData.append(data.byteAt(n+2));
			}
		}
		
		return returnData.toByteArray();
	}
	
	/**
	 * Pasa un buffer de bytes a los buffers individuales de cada canal
	 * @author Andres
	 * @param data es un array de bytes con los bytes que se reciben del analizador logico, siendo el bit 0 el estado
	 * del canal 0 hasta el bit 8 el estado del canal 8
	 */
	 public static void bufferToChannel (final byte[] data, Protocol[] list) {
		
		if(DEBUG) Log.i(TAG, "Lenght data array: " + data.length);
		
		// Borro los bits anteriores porq ya no me hacen falta
		for(int n=0; n < list.length; ++n) list[n].getChannelBitsData().clear();
		
		for(int n=0; n < data.length; ++n){						// Voy a traves de los bytes recibidos
			for(int bit=0; bit < list.length; ++bit){			// Voy a traves de cada canal (cada bit del byte)
				if(LogicHelper.bitTest(data[n], bit)){			// Si es 1
					list[bit].getChannelBitsData().set(n);		// bit es el numero del canal y el bit a poner a 1 o 0
				}
				else{											// Si es 0
					list[bit].getChannelBitsData().clear(n);
				}
			}
		}
	}
	 
	/**
	 * Agrega un buffer de bytes a los buffers individuales de cada canal
	 * @author Andres
	 * @param data es un array de bytes con los bytes que se reciben del analizador logico, siendo el bit 0 el estado
	 * del canal 0 hasta el bit 8 el estado del canal 8
	 */
	 public static void addBufferToChannel (final byte[] data, Protocol[] list) {
		
		if(DEBUG) Log.i(TAG, "Lenght data array: " + data.length);
		if(DEBUG) Log.i(TAG, "Lenght BitSet: " + list[0].getChannelBitsData().length());
		int initialLenght = list[0].getBitsNumber()+1;
		
		// Agrego los bits a los ganales
		for(int n=initialLenght, k=0; n < (initialLenght+data.length); ++n, ++k){		// Voy a traves de los bytes recibidos
			for(int bit=0; bit < list.length; ++bit){			// Voy a traves de cada canal (cada bit del byte)
				if(LogicHelper.bitTest(data[n], bit)){			// Si es 1
					list[bit].getChannelBitsData().set(n);		// bit es el numero del canal y el bit a poner a 1 o 0
				}
				else{											// Si es 0
					list[bit].getChannelBitsData().clear(n);
				}
			}
		}
	}
	
}
