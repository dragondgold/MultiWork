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
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.widget.Toast;

public class BluetoothHelper {

	private static final boolean DEBUG = true;
	
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

    // Strings
    private String BTRequestTitle = "Petición del sistema";
    private String BTRequestSummary = "¿Desea encender el Bluetooth?";
    private String PleaseWaitString = "Por favor, espere";
    private String ConnectingString = "Conectando…";
    private String ScanString = "Escanear";
    private String ScanningString = "Escaneando";
    private String SelectDeviceString = "Seleccione un dispositivo";
    private String NoDeviceString = "No hay dispositivos";
	
	private boolean noException = false;
	private boolean keepRunning = false;
	private boolean offlineMode = false;
	private boolean connectionDialog = false;

    // Intervalo de espera en milisegundos entre cada muestreo para ver si hay nuevos datos desde el bluetooth
    private int pollInterval = 20;
	
	/**
	 * Constructor
	 * @param ctx contexto de la Activity
	 * @param bluetoothName nombre del bluetooth al cual conectarse
	 * @param offlineMode indica si se encuentra en modo offline. Esto permite que se usen todas los metodos
	 * de la clase y no se envíe nada y/o reciba nada.
	 * @param mInterface la interface a ser ejecutada cuando se realize la conexión con el dispositivo bluetooth
	 */
	public BluetoothHelper (final Context ctx, String bluetoothName, boolean offlineMode, OnBluetoothConnected mInterface) {
        try {
            mActivity = ((Activity) ctx);
        }catch (Exception e){
            throw new ClassCastException("El contexto debe pertenecer a una Activity");
        }
		this.ctx = ctx;
		this.bluetoothName = bluetoothName;
		this.offlineMode = offlineMode;
		setOnBluetoothConnected(mInterface);

        // Compruebo que el dispositivo tenga Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            if (DEBUG) Log.i("BluetoothHelper", "No bluetooth on device");
        }
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

    public BluetoothHelper setBTRequestTitleString(String title){
        BTRequestTitle = title;
        return this;
    }

    public BluetoothHelper setBTRequestTitleString(int title){
        setBTRequestTitleString(mActivity.getString(title));
        return this;
    }

    public BluetoothHelper setBTRequestSummaryString(String summary){
        BTRequestSummary = summary;
        return this;
    }

    public BluetoothHelper setBTRequestSummaryString(int summary){
        setBTRequestSummaryString(mActivity.getString(summary));
        return this;
    }

    public BluetoothHelper setPleaseWaitString(String string){
        PleaseWaitString = string;
        return this;
    }

    public BluetoothHelper setPleaseWaitString(int string){
        setPleaseWaitString(mActivity.getString(string));
        return this;
    }

    public BluetoothHelper setConnectingString(String string){
        ConnectingString = string;
        return this;
    }

    public BluetoothHelper setConnectingString(int string){
        setConnectingString(mActivity.getString(string));
        return this;
    }

    public BluetoothHelper setScanString(String scanString) {
        ScanString = scanString;
        return this;
    }

    public BluetoothHelper setScanString(int scanString) {
        setScanString(mActivity.getString(scanString));
        return this;
    }

    public BluetoothHelper setScanningString(String scanningString) {
        ScanningString = scanningString;
        return this;
    }

    public BluetoothHelper setScanningString(int scanningString) {
        setScanningString(mActivity.getString(scanningString));
        return this;
    }

    public BluetoothHelper setSelectDeviceString(String selectDeviceString) {
        SelectDeviceString = selectDeviceString;
        return this;
    }

    public BluetoothHelper setSelectDeviceString(int selectDeviceString) {
        setSelectDeviceString(mActivity.getString(selectDeviceString));
        return this;
    }

    public BluetoothHelper setNoDeviceString(String noDeviceString) {
        NoDeviceString = noDeviceString;
        return this;
    }

    public BluetoothHelper setNoDeviceString(int noDeviceString) {
        setNoDeviceString(mActivity.getString(noDeviceString));
        return this;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

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
        if(DEBUG) Log.i("BluetoothHelper", "scanForDevices()");
        new DeviceScanner(new OnDeviceSelected() {
            @Override
            public void onDeviceSelected(String address, String name) {
                if(address != null) connectWithAddress(address);
                else offlineMode = true;
            }
        }, mActivity, ScanString, ScanningString, SelectDeviceString, NoDeviceString);
	}
	
	/**
	 * Se conecta con el dispositivo dado por la dirección MAC
	 * @param address
	 */
	private void connectWithAddress (String address){
		mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
		bluetoothName = mBluetoothDevice.getName(); 
		establishConnection();
	}

    public void turnOnBluetooth(){
        if(!mBluetoothAdapter.isEnabled()){
            final AlertDialog.Builder mDialog = new AlertDialog.Builder(ctx);
            mDialog.setTitle(BTRequestTitle);
            mDialog.setMessage(BTRequestSummary);

            mDialog.setPositiveButton(ctx.getString(android.R.string.yes), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (DEBUG) Log.i("BluetoothHelper", "Turning on Bluetooth...");
                    mBluetoothAdapter.enable();        // Enciendo el Bluetooth

                    // Espero a que encienda el Bluetooth
                    while(!mBluetoothAdapter.isEnabled());
                }
            });
            mDialog.show();
        }
    }

    public void turnOffBluetooth(){
        mBluetoothAdapter.disable();
        disconnect();
    }
	
	/**
	 * Se conecta al dispositivo Bluetooth cuyo nombre se definió en el constructor
	 */
	public void connect (){
		if(DEBUG) Log.i("BluetoothHelper", "connect()...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(DEBUG) Log.i("BluetoothHelper", "Waiting for Bluetooth to turn on...");
                while(!mBluetoothAdapter.isEnabled());

                if(DEBUG) Log.i("BluetoothHelper", "Looking into paired devices...");
                boolean inPaired = false;
                // Compruebo si el dispositivo no esta en los dispositivos emparejados (paired devices)
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    // Loop a travez de los dispositivos emparejados (paired devices)
                    for (BluetoothDevice device : pairedDevices) {
                        if(DEBUG) Log.i("BluetoothHelper", "Name: " + device.getName() + " -- Address:  " + device.getAddress());
                        // Si el dispositivo coincide con el que busco lo asigno
                        if(device.getName().equals(bluetoothName)){
                            mBluetoothDevice = device;
                            // Establezco la conexión
                            establishConnection();
                            inPaired = true;
                            break;
                        }
                    }
                }
                // Si no está en los emparejados busco nuevos dispositivos
                if(!inPaired){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanForDevices();
                        }
                    });
                }
            }
        }).start();
	}
	
	// Establezco una conexión con el dispositivo que ya definí anteriormente
	private void establishConnection () {
		if(DEBUG) Log.i("BluetoothHelper", "Connecting...");
				
		// Establezco una conexión con el dispositivo bluetooth asignado en mBluetoothDevice
        try { mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID); }
        catch (IOException e1) { e1.printStackTrace(); }
        
        if(connectionDialog){
	        mDialog = ProgressDialog.show(mActivity, PleaseWaitString, ConnectingString, true);
	        mDialog.setCancelable(false);
        }

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
	    			try { Thread.sleep(pollInterval); }
					catch (InterruptedException e) { e.printStackTrace(); }
	    		}
			catch (IOException e) { e.printStackTrace(); }
    		if(DEBUG) Log.i("BTThread", "Thread Stop");
		}
	};

}
