package com.bluetoothutils.andres;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.widget.Toast;

import com.multiwork.andres.R;

public class BluetoothHelper {

	private static final boolean DEBUG = true; 
	
	private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private final Activity mActivity;
	private final Context ctx;
	private final String bluetoothName;
	
	private OnNewBluetoothDataReceived mOnNewBluetoothDataReceived = null;
	private OnBluetoothConnected mOnBluetoothConnected = null;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothSocket mBluetoothSocket;
	
	private OutputStream mBluetoothOut;
	private InputStream mBluetoothIn;
	
	private boolean noException = false;
	private boolean keepRunning = false;
	private boolean offlineMode = false;
	
	/**
	 * Constructor
	 * @param ctx contexto de la Activity
	 * @param bluetoothName nombre del bluetooth al cual conectarse
	 */
	public BluetoothHelper (final Context ctx, final String bluetoothName, boolean offlineMode) {
		mActivity = ((Activity)ctx);
		this.ctx = ctx;
		this.bluetoothName = bluetoothName;
		this.offlineMode = offlineMode;
	}
	
	/**
	 * Establece un OnNewBluetoothDataReceived para ser llamado cuando halla nuevos datos.
	 * Debe implementarse OnNewBluetoothDataReceived en la Activity.
	 * @param mInterface
	 */
	public void setOnNewBluetoothDataReceived (OnNewBluetoothDataReceived mInterface){
		mOnNewBluetoothDataReceived = mInterface;	// Interface
		if(noException){							// Arranco el Thread
			keepRunning = true;
			mBTThread.start();
		}
	}
	
	/**
	 * Elimina el OnNewBluetoothDataReceived 
	 */
	public void removeOnNewBluetoothDataReceived (){
		mOnNewBluetoothDataReceived = null;
		keepRunning = false;
	}
	
	/**
	 * Establece un OnBluetoothConnected para ser llamado cuando se conecta al dispositivo deseado.
	 * @param mInterface
	 */
	public void setOnBluetoothConnected (OnBluetoothConnected mInterface){
		mOnBluetoothConnected = mInterface;
	}
	
	public void removeOnBluetoothConnected (){
		mOnBluetoothConnected = null;
	}
	
	/**
	 * Envía un byte por Bluetooth
	 * @param data
	 */
	public void write (int data){
		if(mBluetoothOut != null && !offlineMode){
			try { mBluetoothOut.write(data); }
			catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/**
	 * Se conecta al dispositivo Bluetooth cuyo nombre se definió en el constructor.
	 * @param finishOnFail si es true finaliza la Activity si hay un error de conexión
	 */
	public void connect (final boolean finishOnFail){
		if(DEBUG) Log.i("BluetoothHelper", "connect()...");
		// Compruebo que el dispositivo tenga Bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Si no hay Bluetooth en el dispositivo muestro un dialogo alertando al usuario y salgo de la Activity
		    AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
		    dialog.setTitle(ctx.getString(R.string.NoBTAlertTitle));
		    dialog.setMessage(ctx.getString(R.string.NoBTAlertText));
		    dialog.setPositiveButton(ctx.getString(R.string.Ok), new OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(DEBUG) Log.i("BluetoothHelper", "No bluetooth on device");
					if(finishOnFail) mActivity.finish();	// Cierro porque no existe un módulo Bluetooth
				}
		    });
		}
		// Si el dispositivo tiene Bluetooth me conecto
		else{
			// Compruebo que el Bluetooth esté activado, sino pido al usuario que lo active
			if (!mBluetoothAdapter.isEnabled()) {
				final AlertDialog.Builder mDialog = new AlertDialog.Builder(ctx);
				mDialog.setTitle(ctx.getString(R.string.BTRequestTitle));
				mDialog.setMessage(ctx.getString(R.string.BTRequestSummary));
				
				mDialog.setPositiveButton(ctx.getString(R.string.Yes), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(DEBUG) Log.i("BluetoothHelper", "Turning on Bluetooth...");
						mBluetoothAdapter.enable();		// Enciendo el Bluetooth
						
						// Espero a que encienda el Bluetooth
						while(!mBluetoothAdapter.isEnabled());
						
						// Compruebo si el dispositivo no esta en los dispositivos emparejados (paired)
						Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
						if (pairedDevices.size() > 0) {
						    // Loop a travez de los dispositivos emparejados (paired)
						    for (BluetoothDevice device : pairedDevices) {
						        if(DEBUG) Log.i("BluetoothHelper", "Name: " + device.getName() + " -- Address:  " + device.getAddress());
						        // Si el dispositivo coincide con el que busco lo asigno
						        if(device.getName().equals(bluetoothName)){
						        	mBluetoothDevice = device;
									// Establezco una conexión Bluetooth para enviar datos
									establishConnection();
						        	break;
						        }
						    }
						}
						// Sino salgo, debe estar en los dispositivos emparejados
						else{
							if(DEBUG) Log.i("BluetoothHelper", "Finish Activity not in paired devices");
							mBluetoothAdapter.disable();
							if(finishOnFail) mActivity.finish();
						}
					}
				});
				
				mDialog.setNegativeButton(ctx.getString(R.string.No), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(DEBUG) Log.i("BluetoothHelper", "Exit");
						if(finishOnFail) mActivity.finish();
					}
				});
				mDialog.show();
			}
			// Bluetooth ya encendido, me conecto
			else{
				// Compruebo si el dispositivo no esta en los dispositivos emparejados (paired)
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
				if (pairedDevices.size() > 0) {
				    // Loop a travez de los dispositivos emparejados (paired)
				    for (BluetoothDevice device : pairedDevices) {
				        if(DEBUG) Log.i("BluetoothHelper", "Name: " + device.getName() + " -- Address:  " + device.getAddress());
				        // Si el dispositivo coincide con el que busco lo asigno
				        if(device.getName().equals(bluetoothName)){
				        	mBluetoothDevice = device;
							// Establezco una conexión Bluetooth para enviar datos
							establishConnection();
				        	break;
				        }
				    }
				}
				// Sino salgo, debe estar en los dispositivos emparejados
				else{
					if(DEBUG) Log.i("BluetoothHelper", "Finish Activity not in paired devices");
					mBluetoothAdapter.disable();
					if(finishOnFail) mActivity.finish();
				}
			}
		}
	}
	
	// Establezco una conexión con el dispositivo que ya definí anteriormente
	private void establishConnection () {
		if(DEBUG) Log.i("BluetoothHelper", "Connecting...");
		
		// Establesco una conexión con el dispositivo bluetooth asignado en mBluetoothDevice
        try { mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID); }
        catch (IOException e1) { e1.printStackTrace(); }
        	
        // Runnable donde se encuentra el código a ejecutar por un Handler o Thread
        final Runnable mRunnable = new Runnable() {
            public void run() {
            	noException = true;
    			// Desactivo el descubrimiento de dispositivos porque hace lenta la conexion
    			if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
    	        
    	        // Me conecto al dispositivo, esto se bloqueará hasta que se conecte por eso debe hacerse
    	        // en un Thread diferente
    	        try { mBluetoothSocket.connect(); } 
    	        catch (IOException e) { 
    	        	try { mBluetoothSocket.close(); }
    	        	catch (IOException e1) { e1.printStackTrace(); }
    	        	Log.i("BluetoothHelper", "Connection Exception");
    	        	noException = false;
    	        }
    	        
    	        // Obtengo el OutputStream para enviar datos al Bluetooth
    	        try { mBluetoothOut = mBluetoothSocket.getOutputStream(); }
    	        catch (IOException e) { e.printStackTrace(); }
    	        
    	        // Obtengo el InputStream para recibir datos desde el Bluetooth
    			try { mBluetoothIn = mBluetoothSocket.getInputStream(); }
    			catch (IOException e) { e.printStackTrace(); }
    			
    		    // Conectado
    		    if(noException){
    		    	mActivity.runOnUiThread(new Runnable() {
    					public void run() { 
    						Toast.makeText(mActivity, "Conectado a " + mBluetoothDevice.getName(), Toast.LENGTH_SHORT).show(); 
    					}
    		    	});
    		        Log.i("BluetoothHelper", "Conectado a " + mBluetoothDevice.getName());
    		        if(mOnBluetoothConnected != null) mOnBluetoothConnected.onBluetoothConnected(mBluetoothIn, mBluetoothOut);
    		        if(mOnNewBluetoothDataReceived != null && keepRunning == false) mBTThread.start();
    		    }
    		    // Error
    		    else{
    		    	mActivity.runOnUiThread(new Runnable() {
    					public void run() { 
    						Toast.makeText(mActivity, "Error de conexion", Toast.LENGTH_SHORT).show(); 
    					}
    		    	});
    		    	Log.i("BluetoothHelper", "Error");
    		    	mActivity.finish();
    		    }
    	    }
        };
        // Thread que ejecuta al Runnable
        final Thread mThread = new Thread(){
        	@Override
            public void run() {
               mRunnable.run();
            }
        };
        mThread.start();
    }
	
	// Thread que ejecuta al Runnable
	private final Thread mBTThread = new Thread(){
    	@Override
        public void run() {
    		if(DEBUG) Log.i("BTThread", "Thread Running");
    		while(keepRunning){
    			try {
    				// Si hay algún dato disponible ejecuto la interface para avisar a la Activity
					if(mBluetoothIn.available() > 0){
						if(mOnNewBluetoothDataReceived.onNewBluetoothDataReceivedListener(
								mBluetoothIn, mBluetoothOut) == false){
							// Si es falso, ya no ejecuto el listener
							removeOnNewBluetoothDataReceived();
						}
					}
				}
    			catch (IOException e) { e.printStackTrace(); }
    			try { Thread.sleep(20); }
				catch (InterruptedException e) { e.printStackTrace(); }
    		}
        }
    };

}
