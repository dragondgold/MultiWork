package com.bluetoothutils.andres;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.widget.Toast;

import com.multiwork.andres.R;

public class BluetoothHelper {

	private static final boolean DEBUG = true; 
	public static final int BLUETOOTH_CONNECTION = 1;
	
	private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private final Activity mActivity;
	private final Context ctx;
	private String bluetoothName;
	
	private OnNewBluetoothDataReceived mOnNewBluetoothDataReceived = null;
	private OnBluetoothConnected mOnBluetoothConnected = null;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothSocket mBluetoothSocket;
	
	private OutputStream mBluetoothOut;
	private InputStream mBluetoothIn;
	private Thread mBTThread;
	private ProgressDialog mDialog;
	
	private boolean noException = false;
	private boolean keepRunning = false;
	private boolean offlineMode = false;
	private boolean connectionDialog = false;
	
	/**
	 * Constructor
	 * @param ctx contexto de la Activity
	 * @param bluetoothName nombre del bluetooth al cual conectarse
	 * @param offlineMode indica si se encuentra en modo offline. Esto permite que se usen todas los metodos
	 * de la clase y no se envíe nada y/o reciba nada.
	 * @param mInterface la interface a ser ejecutada cuando se realize la conexión con el dispositivo bluetooth
	 */
	public BluetoothHelper (final Context ctx, String bluetoothName, boolean offlineMode, OnBluetoothConnected mInterface) {
		mActivity = ((Activity)ctx);
		this.ctx = ctx;
		this.bluetoothName = bluetoothName;
		this.offlineMode = offlineMode;
		setOnBluetoothConnected(mInterface);
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
			mBTThread = new Thread(mBTRunnable);
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
	 * Define si se muestra un diálogo de espera mientras se está conectando al Bluetooth
	 * @param state
	 */
	public void setConnectionDialog (boolean state){
		connectionDialog = state;
	}
	
	/**
	 * Establece un OnBluetoothConnected para ser llamado cuando se conecta al dispositivo deseado.
	 * @param mInterface
	 */
	private void setOnBluetoothConnected (OnBluetoothConnected mInterface){
		mOnBluetoothConnected = mInterface;
	}
	
	public void removeOnBluetoothConnected (){
		mOnBluetoothConnected = null;
	}
	
	public boolean isOfflineMode (){
		return offlineMode;
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
	
	public void disconnect() {
		if(mBluetoothIn != null){
			try {mBluetoothIn.close();} catch (Exception e) { e.printStackTrace(); }
			mBluetoothIn = null;
		}
		if(mBluetoothOut != null){
			try {mBluetoothOut.close();} catch (Exception e) { e.printStackTrace(); }
			mBluetoothOut = null;
		}
		if(mBluetoothSocket != null){
            try {mBluetoothSocket.close();} catch (Exception e) { e.printStackTrace(); }
            mBluetoothSocket = null;
		}
	}
	
	public boolean isConnected(){
		if(mBluetoothSocket != null) return mBluetoothSocket.isConnected();
		else return false;
	}
	
	public void scanForDevices() {
		mActivity.startActivityForResult(new Intent(ctx, DeviceListActivity.class), BLUETOOTH_CONNECTION);
	}
	
	/**
	 * Se conecta con el dispositivo dado por la dirección address. Este método solo debe llamarse luego de haberse
	 * ejecutado connect(), se lo utiliza en el onActivityResult().
	 * @param address
	 */
	public void connectWithAddress (String address){
		mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
		bluetoothName = mBluetoothDevice.getName(); 
		establishConnection();
	}
	
	/**
	 * Se conecta al dispositivo Bluetooth cuyo nombre se definió en el constructor, si el dispositivo no
	 * esta en los emparejados se le dara al usuario una lista de los dispositivos bluetooth disponibles por lo
	 * que se debe implementar onActivityResult() con el requestCode = BLUETOOTH_CONNECTION donde se dará
	 * el nombre y MAC del dispositivo bluetooth que se seleccionó.
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
					if(finishOnFail){
						disconnect();
						mActivity.finish();	// Cierro porque no existe un módulo Bluetooth
					}
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
						boolean inPaired = false;
						
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
									inPaired = true;
						        	break;
						        }
						    }
						}
						// Si no estan en los emparejados busco nuevos dispositivos
						if(!inPaired) scanForDevices();
					}
				});
				
				mDialog.setNegativeButton(ctx.getString(R.string.No), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(DEBUG) Log.i("BluetoothHelper", "Exit");
						if(finishOnFail){
							disconnect();
							mActivity.finish();
						}
					}
				});
				mDialog.show();
			}
			// Bluetooth ya encendido, me conecto
			else{
				boolean inPaired = false;
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
							inPaired = true;
				        	break;
				        }
				    }
				}
				// Si no estan en los emparejados busco nuevos dispositivos
				if(!inPaired) scanForDevices();
			}
		}
	}
	
	// Establezco una conexión con el dispositivo que ya definí anteriormente
	private void establishConnection () {
		if(DEBUG) Log.i("BluetoothHelper", "Connecting...");
				
		// Establesco una conexión con el dispositivo bluetooth asignado en mBluetoothDevice
        try { mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID); }
        catch (IOException e1) { e1.printStackTrace(); }
        
        if(connectionDialog){
	         mDialog = ProgressDialog.show(mActivity, mActivity.getString(R.string.PleaseWait),
	        		mActivity.getString(R.string.BTConnecting), true);
	        mDialog.setCancelable(false);
        }
        	
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
    						if(connectionDialog) mDialog.dismiss();
    					}
    		    	});
    		        Log.i("BluetoothHelper", "Conectado a " + mBluetoothDevice.getName());
    		        if(mOnBluetoothConnected != null) mOnBluetoothConnected.onBluetoothConnected(mBluetoothIn, mBluetoothOut);
    		        if(mOnNewBluetoothDataReceived != null && !keepRunning) mBTThread.start();
    		    }
    		    // Error
    		    else{
    		    	mActivity.runOnUiThread(new Runnable() {
    					public void run() { 
    						Toast.makeText(mActivity, "Error de conexion", Toast.LENGTH_SHORT).show(); 
    						if(connectionDialog) mDialog.dismiss();
    					}
    		    	});
    		    	Log.i("BluetoothHelper", "Error Connecting");
    		    	disconnect();
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
	
	private final Runnable mBTRunnable = new Runnable() {
		
		@Override
		public void run() {
			if(DEBUG) Log.i("BTThread", "Thread Running");
			try {
				int prevData = mBluetoothIn.available();
	    		while(keepRunning){
	    				// Si hay algún dato disponible ejecuto la interface para avisar a la Activity
						if(mBluetoothIn.available() != prevData){
							prevData = mBluetoothIn.available();
							if(mOnNewBluetoothDataReceived.onNewBluetoothDataReceivedListener(
									mBluetoothIn, mBluetoothOut) == false){
								// Si es falso, ya no ejecuto el listener
								removeOnNewBluetoothDataReceived();
							}
						}
					}
	    			try { Thread.sleep(20); }
					catch (InterruptedException e) { e.printStackTrace(); }
	    		}
			catch (IOException e) { e.printStackTrace(); }
    		if(DEBUG) Log.i("BTThread", "Thread Stop");
		}
	};

}
