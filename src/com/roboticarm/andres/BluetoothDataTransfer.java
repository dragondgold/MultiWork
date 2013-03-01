package com.roboticarm.andres;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.http.util.ByteArrayBuffer;

import android.util.Log;

public class BluetoothDataTransfer {
	
	private final static boolean DEBUG = true;
	/** Datos de transmicion */
	private static final byte startByte = 'S';
	private static final byte ACK = 0x06;
	
	private final OutputStream mOutputStream;
	private final InputStream mInputStream;
	
	/** TimeOut de la espera del byte de ACK en nS (1 segundo por defecto) */
	private long timeOut = 1000000000;
	
	private boolean isBTSending = false;
	private boolean isNewDataToSend = false;
	
	private byte[] dataToSend;
	private byte dataTypeToSend;
	
	//private Buffer mFifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(8));

	/**
	 * Constructor
	 * @param outS OutputStream usado para enviar los datos
	 * @param inS InputStream usado para recibir los datos
	 */
	BluetoothDataTransfer(final OutputStream outS, final InputStream inS) {
		mOutputStream = outS;
		mInputStream = inS;
	}
	
	/**
	 * Inicia el Thread que enviará los datos por Bluetooth, debe llamarse luego de haberse creado el objeto
	 */
	public void start (){
		mBTThread.start();
	}
	
	/**
	 * Configura el timeout de la espera de un ACK en mS (por defecto 1 segundo)
	 * @param time tiempo en mS
	 */
	public void setTimeout (final int time){
		timeOut = time*1000000;
	}
	
	/**
	 * Obtiene el timeout de la espera de un ACK en mS
	 * @return tiempo en mS
	 */
	public int getTimeOut (){
		return (int)(timeOut/1000000);
	}
	
	/**
	 * 
	 * @param data bytes a enviar
	 * @param dataType tipo de dato a enviar
	 */
	public void btSendDataWithSync (final byte[] data, final byte dataType){
		dataToSend = data;				// Datos
		dataTypeToSend = dataType;		// Tipo de dato
		isNewDataToSend = true;			// Nuevos datos para enviar
	}
	
    // Thread que ejecuta al Runnable
	private final Thread mBTThread = new Thread(){
    	@Override
        public void run() {
    		if(DEBUG) Log.i("BTThread", "Thread Running");
    		while(true){
	    		if(!isBTSending && isNewDataToSend){
	    			isNewDataToSend = false;
	    			mBTSendRunnable.run();
	    		}
	    		try { Thread.sleep(10); }
	    		catch (InterruptedException e) { e.printStackTrace(); }
    		}
        }
    };
    
	// Runnable que envía los datos por Bluetooth
	private final Runnable mBTSendRunnable = new Runnable() {
        public void run() {
        	isBTSending = true;
        	
        	if(DEBUG) Log.i("BTSendBrazo", "Sending Start...");
			// Envío el Byte de Start
			try { mOutputStream.write(startByte); }
			catch (IOException e2) { e2.printStackTrace(); }
			
			if(DEBUG) Log.i("BTSendBrazo", "Waiting ACK...");
			// Espero por un ACK
			try {
				int data = -1;
				long startTime = System.nanoTime();
				do{
					if(mInputStream.available() > 0) data = mInputStream.read();
					// TimeOut
					if((System.nanoTime() - startTime) >= timeOut){
						if(DEBUG) Log.i("BTSendBrazo", "TimeOut");
						isBTSending = false;
						return;
					}
				}while(data != ACK); 
			}
			catch (IOException e2) { e2.printStackTrace(); }
			
			if(DEBUG) Log.i("BTSendBrazo", "Sending data type...");
			// Envío el tipo de dato que voy a enviar
			try { mOutputStream.write(dataTypeToSend); }
			catch (IOException e2) { e2.printStackTrace(); }
			
			// Envío los datos
			btSendByteWithSync(dataToSend);
			isBTSending = false;
        }
	};

	/**
	 * Envía una cadena de bytes y espera en caso de que ser pida una espera antes de seguir enviando
	 * @param dataToSend
	 */
	private synchronized void btSendByteWithSync (final byte[] dataToSend){
		
		long st = System.nanoTime();
		for(int n = 0; n < dataToSend.length; ++n){
			
			if(DEBUG) Log.i("BTSendBrazo", "Waiting ACK 2...");
			
			// Espero por un ACK
			try {
				int data = -1;
				long startTime = System.nanoTime();
				do{
					if(mInputStream.available() > 0) data = mInputStream.read();
					// 1 segundo de time out
					if((System.nanoTime() - startTime) >= timeOut){
						if(DEBUG) Log.i("BTSendBrazo", "TimeOut 2");
						return;
					}
				}while(data != ACK); 
			}
			catch (IOException e2) { e2.printStackTrace(); }
			
			try { mOutputStream.write(dataToSend[n]); }
			catch (IOException e) { e.printStackTrace(); }
		}
		long sp = System.nanoTime();
		if(DEBUG) Log.i("BTSendBrazo", "Time to transfer " + ((sp-st)/1000000) + "mS");
	}
}
