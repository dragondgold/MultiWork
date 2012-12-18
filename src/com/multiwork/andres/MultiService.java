package com.multiwork.andres;

import java.util.BitSet;
import java.util.Random;

import com.protocolanalyzer.andres.LogicHelper;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class MultiService extends Service{
	
	public static final String mAction = "MY_ACTION";
	public static boolean isRunning = false;
	
	/**
	 * @author Andres Torti
	 * Thread en el cual se ejecuta el Service, debe crearse un nuevo Thread porque Service se ejecuta sobre el Thread
	 * principal realizando la ejecucion de la aplicacion
	 */
	public class MyThread extends Thread{
		@Override
		public void run() {
			while(true){
				try {
					Intent intent = new Intent();
					intent.setAction(mAction);
					intent.putExtra("LogicData", getLogicAnalizerData());
					intent.putExtra("FrecMode", getDataFrec());
					intent.putExtra("LCMode", getDataLC());
					sendBroadcast(intent);
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MyThread myThread = new MyThread();
		myThread.start();
		isRunning = true;
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		isRunning = false;
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * Se llama a este metodo para obtener 64 bytes con los datos leidos del analizador logico, siendo cada byte un muestreo
	 * y el muestreo de cada canal se toma desde el LSB al MSB:
	 * 		bit 0 = Canal 0
	 * 		bit 1 = Canal 1
	 *		bit 2 = Canal 2
	 *		bit 3 = Canal 3 
	 * El tama�o del array devuelto es variable de acuerdo a la cantidad de datos enviados desde el PIC
	 * @return Array de bytes con los 64 bytes de los muestreos
	 */
	public static byte[] getLogicAnalizerData (){
		
		Random crazy = new Random();
		byte[] data = new byte[50];
		
		for(int n=0; n<data.length; ++n){
			data[n] = (byte)crazy.nextInt();
		}
		
		return data;
	}
	
	// Seteo el modo del LC Meter en modo Inductancia o Capacitor
	public static void setLCMode(boolean mode) {
		
	}

	// Obtengo los datos del LC Meter
	public static Bundle getDataLC() {
		Random crazy = new Random();
		
		Bundle data = new Bundle();
		data.putBoolean("modo", true); 			// Si modo==true -> L si modo==false -> C
		data.putLong("frecLC", Math.abs(crazy.nextInt(700000)));		// Frecuencia del oscilador LC
		return data;
	}

	// Obtengo los datos del frecuencimetro
	public static Bundle getDataFrec() {
		Random crazy = new Random();
		
		Bundle data = new Bundle();
		data.putLong("frec", Math.abs(crazy.nextInt(20000)));	// Frecuencia en Hz
		data.putLong("frec2", Math.abs(crazy.nextInt(20000)));	// Frecuencia en Hz
		return data;
	}

}
