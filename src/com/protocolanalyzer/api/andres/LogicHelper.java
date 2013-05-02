package com.protocolanalyzer.api.andres;

import java.util.BitSet;

import org.apache.http.util.ByteArrayBuffer;

import android.util.Log;

public class LogicHelper {
    
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
	 * Parseador de bits, se la pasa un String con los 1 y 0 que se desean y los coloca en un BitSet con el numero
	 * de muestreos por bit que se deseen
	 * @param data String con los 1 y 0, los demas caracteres son ignorados
	 * @param samplesPerBit numero de bits por cada 1 y 0
	 * @param times indica la cantidad de veces que se repite el String pasado para crear cadenas mas largas
	 * @return BitSet con los bits de acuerdo al String y los samplesPerBit
	 */
	public static BitSet bitParser (final String data, final int samplesPerBit, final int times){
		BitSet bitSet = new BitSet();
		
		Log.i("BitParse", "samplesPerBit: " + samplesPerBit);
		Log.i("BitParse", "String: " + data);
		Log.i("BitParse", "String Length: " + data.length());
		
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
		Log.i("BitParse", "BitSet length: " + bitSet.length());
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
	
}
