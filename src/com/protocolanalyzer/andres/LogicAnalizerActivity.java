package com.protocolanalyzer.andres;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.util.ByteArrayBuffer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.WindowManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bluetoothutils.andres.BluetoothHelper;
import com.bluetoothutils.andres.OnNewBluetoothDataReceived;
import com.multiwork.andres.MainMenu;
import com.multiwork.andres.R;

import com.protocolanalyzer.api.andres.LogicData;
import com.protocolanalyzer.api.andres.LogicDataSet;
import com.protocolanalyzer.api.andres.LogicHelper;
import com.protocolanalyzer.api.andres.LogicData.Protocol;

public class LogicAnalizerActivity extends SherlockFragmentActivity implements OnActionBarClickListener, OnNewBluetoothDataReceived{

	private static final boolean DEBUG = true;
	private static final byte startByte = 'S';
	private static final byte logicAnalyzerMode = 'L';
	private static final int initialBufferSize = 1000;
	private static final int PREFERENCES_CODE = 1;
	
	private static final int F40MHz = 'A';
	private static final int F20MHz = 'S';
	private static final int F10MHz = 'D';
	private static final int F4MHz = 'F';
	private static final int F400KHz = 'G';
	private static final int F2KHz = 'H';
	private static final int F10Hz = 'J';
	
	private static final int updateDialogTitle = 0;
	private static final int dispatchInterfaces = 1;
	private static final int dismissDialog = 2;
	
	/** Interface donde paso los datos decodificados a los Fragments, los mismo deben implementar el Listener */
	private static OnDataDecodedListener mChartDataDecodedListener;
	private static OnDataDecodedListener mListDataDecodedListener;
	
	/** Fragment que contiene al grafico */
	private static Fragment mFragmentChart;
	/** Fragment que con la lista de datos en formato raw */
	private static Fragment mFragmentList;
	
    /** Numero de canales de entrada */
    public static final int channelsNumber = 4;
    /** Indica si recibo datos del Service o no (Play o Pause) */
    private static boolean isPlaying = false; 
    /** Tiempo del grafico */
    private static double time = 0;
    
    private static BluetoothHelper mBluetoothHelper;
	
	/** Buffers de recepcion donde se guarda los bytes recibidos */
    private static byte[] ReceptionBuffer;	
    private static ByteArrayBuffer mByteArrayBuffer = new ByteArrayBuffer(initialBufferSize);
    /** Dato decodificado desde LogicHelper para ser mostrado en el grafico, contiene las posiciones para mostar
      * el tipo de protocolo, etc
      * @see LogicData.java */
	private static LogicData[] mData = new LogicData[channelsNumber];
	private static LogicDataSet mDataSet = new LogicDataSet();
	
	private static boolean isStarting = true;
	private static ProgressDialog mDialog;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if(DEBUG) Log.i("mFragmentActivity","onCreate() LogicAnalizerActivity");
		
		ReceptionBuffer = new byte[1];
		
		setContentView(R.layout.logic_fragments);
		mFragmentList = getSupportFragmentManager().findFragmentById(R.id.logicFragment);
		
		// Obtengo el OnDataDecodedListener de los Fragments
		try { mListDataDecodedListener = (OnDataDecodedListener) mFragmentList; }
		catch (ClassCastException e) { throw new ClassCastException(mFragmentList.toString() + " must implement OnDataDecodedListener"); }
		
		// Creo los LogicData y los agrego al DataSet
		for(int n=0; n < channelsNumber; ++n){
			mData[n] = new LogicData();
			mDataSet.addLogicData(mData[n]);
		}
	}
	
	// Si estoy tomando datos y salgo de la Activity elimino el CallBack para no recibir mas datos desde el Service.
	@Override
	protected void onPause() {
		if(DEBUG) Log.i("mFragmentActivity","onPause()");
		mBluetoothHelper.write(0);	// Indico al PIC que salí de la Activity
		super.onPause();
	}
	
	/**
	 * En onResume() se llama a supportInvalidateOptionsMenu(); para dibujar el boton Play/Pause en el modo correcto y no
	 * en el que aparece en el layout XML del ActionBar por defecto.
	 */
	@Override
	protected void onResume() {
		if(DEBUG) Log.i("mFragmentActivity","onResume()");
		
		isStarting = true;
		isPlaying = false;
		
		// Solo si estoy en modo online procedo a obtener la conexion
		// Obtengo la conexión Bluetooth
		mBluetoothHelper = MainMenu.mBluetoothHelper;
		mBluetoothHelper.setOnNewBluetoothDataReceived(this);
		// Indico que entré en el analizador lógico
		mBluetoothHelper.write(logicAnalyzerMode);
		setPreferences();
		
		this.supportInvalidateOptionsMenu();  // Actualizo el ActionBar
		super.onResume();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
	 		case android.R.id.home:
	 			Intent intent = new Intent(this, MainMenu.class);
	 			// Si la aplicacion ya esta abierta ir a ella no abrir otra nueva
	 			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	 			startActivity(intent);
	 			break;
	 		// Boton Play/Pause
			case R.id.PlayPauseLogic:
				// Digo al PIC que comienze el muestreo
				mBluetoothHelper.write(1);
				// Envío la frecuencia de muestreo que quiero
				if(LogicData.getSampleRate() == 40000000) mBluetoothHelper.write(F40MHz);
				else if(LogicData.getSampleRate() == 20000000) mBluetoothHelper.write(F20MHz);
				else if(LogicData.getSampleRate() == 10000000) mBluetoothHelper.write(F10MHz);
				else if(LogicData.getSampleRate() == 4000000) mBluetoothHelper.write(F4MHz);
				else if(LogicData.getSampleRate() == 400000) mBluetoothHelper.write(F400KHz);
				else if(LogicData.getSampleRate() == 2000) mBluetoothHelper.write(F2KHz);
				else if(LogicData.getSampleRate() == 10) mBluetoothHelper.write(F10Hz);
				isPlaying = true;
				supportInvalidateOptionsMenu();
				
				// Muestro un diálogo de progreso indeterminado mientras se procesan los datos
				mDialog = ProgressDialog.show(this, getString(R.string.AnalyzerDialogReceiving),
						getString(R.string.PleaseWait), true);
				mDialog.setCancelable(false);
				break;
	 		case R.id.listLogic:
	 			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	 			// Reemplazo este Fragment con el gráfico, addToBackStack() hace que al presionar la tecla
	 			// de atras se vuelva a este Fragment y no se destruya el mismo
	 			if(getSupportFragmentManager().findFragmentByTag("ChartLogic") == null
	 					|| !getSupportFragmentManager().findFragmentByTag("ChartLogic").isVisible()){
	 				if(DEBUG) Log.i("mFragmentActivity", "Chart Fragment Launched");
		 			transaction.replace(R.id.logicFragment, new LogicAnalizerChartFragment(mData), "ChartLogic");
		 			transaction.addToBackStack(null);
		 			transaction.commit();
		 			getSupportFragmentManager().executePendingTransactions();
		 			
		 			// Agrego el OnDataDecodedListener cuando se agrega el nuevo Fragment
					mFragmentChart = getSupportFragmentManager().findFragmentByTag("ChartLogic");
					try { mChartDataDecodedListener = (OnDataDecodedListener) mFragmentChart; }
					catch (ClassCastException e) { throw new ClassCastException(mFragmentChart.toString() + " must implement OnDataDecodedListener"); }
	 			}
	 			else{
	 				if(DEBUG) Log.i("mFragmentActivity","Chart Fragment Removed");
					getSupportFragmentManager().popBackStackImmediate();
	 			}
	 			break;
	 		case R.id.settingsLogic:
	 			startActivityForResult(new Intent(this, LogicAnalizerPrefs.class), PREFERENCES_CODE);
	 			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Detecta si el Fragment identificado con el fragmentTag esta activo o no
	 * @param fragmentTag nombre del Fragment
	 * @return true si esta activo, false de otro modo
	 */
	private boolean isFragmentActive (String fragmentTag){
		if(getSupportFragmentManager().findFragmentByTag(fragmentTag) == null ||
				!getSupportFragmentManager().findFragmentByTag(fragmentTag).isVisible()){
			return false;
		}
		else return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Cambio en las preferencias
		if(requestCode == PREFERENCES_CODE){
			if(resultCode == RESULT_OK) {
				if(DEBUG) Log.i("mFragmentActivity", "Preferences Setted");
				// Aviso a la Activity que cambiaron las preferencias
				mListDataDecodedListener.onDataDecodedListener(mData, ReceptionBuffer.length, true);
				if(isFragmentActive("ChartFragment")) mChartDataDecodedListener.onDataDecodedListener(mData, ReceptionBuffer.length, true);
				setPreferences();
			}
		}
	}

	/**
	 * Listener cuando se presiona los botones del ActionBar de alguno de los Fragment. La Activity implementa
	 * la interface creada por mi onActionBarClickListener(); y los Fragments utilizan la misma para que la Activity
	 * realiza las operaciones necesarias.
	 */
	@Override
	public void onActionBarClickListener(int buttonID) {
		if(DEBUG) Log.i("mFragmentActivity","onActionBarClickListener() - " + this.toString());
		switch(buttonID){
			case R.id.restartLogic:
				for(int n=0; n < channelsNumber; ++n){
					mData[n].freeDataMemory();
					mData[n].clearDecodedData();
				}
				break;
			case R.id.settingsLogic:
				setPreferences();
				break;
		}
	}
	
	/**
	 * Actualiza los iconos del ActionBar cuando se llama a supportInvalidateOptionsMenu(); porque si no se llama a esta
	 * opcion en cada onResume() cuando la Activity vuelve el ActionBar se crea con el layout del XML y el boton
	 * Play/Pause debe mostrarse de acuerdo al estado actual.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(isPlaying) menu.findItem(R.id.PlayPauseLogic).setIcon(R.drawable.pause);
		else menu.findItem(R.id.PlayPauseLogic).setIcon(R.drawable.play);
		
		return true;
	}
	
	/**
	 * Define los parametros de los canales de acuerdo como tipo de protocolo, velocidad de muestreo,
	 * velocidad en Baudios para el UART y si la pantalla debe permanecer o no encendida.
	 */
 	private void setPreferences() {
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        for(int n=0; n < channelsNumber; ++n){
        	// Seteo el protocolo para cada canal
        	switch(Byte.decode(getPrefs.getString("protocol" + (n+1), "0"))){
	        	case 0:		// I2C
	        		mData[n].setProtocol(Protocol.I2C);
	        		break;
	        	case 1:		// UART
	        		mData[n].setProtocol(Protocol.UART);
	        		break;
	        	case 2:		// CLOCK
	        		mData[n].setProtocol(Protocol.CLOCK);
	        		break;
	        	case 3:		// NONE
	        		mData[n].setProtocol(Protocol.NONE);
	        		break;
        	}
        	// Seteo el canal que hace de fuente de clock
        	mData[n].setClockSource(Byte.decode(getPrefs.getString("SCL" + (n+1), "0")));
        	// Defino la velocidad en baudios de cada canal en caso de usarse UART
        	mData[n].setBaudRate(Long.decode(getPrefs.getString("BaudRate" + (n+1), "9600")));
        }
    	// Defino la velocidad de muestreo que es comun a todos los canales (por defecto 4MHz)
    	LogicData.setSampleRate(Long.decode(getPrefs.getString("sampleRate", "4000000")));

        /**
         * Mantiene a la pantalla encendida en esta Activity unicamente
         * @see http://developer.android.com/reference/android/os/PowerManager.html
         * @see http://stackoverflow.com/questions/2131948/force-screen-on
         */
        if(getPrefs.getBoolean("keepScreenAwake", false)) {
        	if(DEBUG) Log.i("LogicAnalizerActivity","Screen Awake");
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
 	}
 	
 	// Handler para actualizar el UI Thread
 	final Handler updateUIThread = new Handler(){
 		@Override 
 	    public void handleMessage(Message msg) { 
 			switch (msg.what) {
 				case updateDialogTitle:
 					mDialog.setTitle(getString(R.string.AnalyzerDialogLoading));
 					break;

 				case dispatchInterfaces:
 					time = mListDataDecodedListener.onDataDecodedListener(mData, ReceptionBuffer.length, false);
					if(isFragmentActive("ChartFragment")) mChartDataDecodedListener.onDataDecodedListener(mData, ReceptionBuffer.length, false);
 					break;
 				
 				case dismissDialog:
 					mDialog.dismiss();
 					supportInvalidateOptionsMenu();
 					break;
			}
 	    } 
 	};
 	
	@Override
	public boolean onNewBluetoothDataReceivedListener(InputStream mBTIn, OutputStream mBTOut) {
		if(DEBUG) Log.i("LogicAnalizerBT", "onNewBluetoothDataReceivedListener()");
		// La primera vez veo que lo que halla recibido coincida con el modo en el que estoy
		if(isStarting){
			if(DEBUG) Log.i("LogicAnalizerBT", "Starting");
			try {
				while(mBTIn.available() > 0){
					if(mBTIn.read() == logicAnalyzerMode){
						isStarting = false;
						break;
					}
				}
				// Si no tengo el modo que corresponde notifico con un Toast
				if(isStarting){
					if(DEBUG) Log.i("LogicAnalizerBT", "Nothing detected");
				}
			} catch (IOException e) { e.printStackTrace(); }
		}
		else if(isPlaying){
			if(DEBUG) Log.i("LogicAnalizerBT", "Data receive");
			try {
			int[] data = new int[3];
			while(mBTIn.available() > 0){
				if(mBTIn.read() == startByte && mBTIn.read() == logicAnalyzerMode){
					if(DEBUG) Log.i("LogicAnalizerBT", "Receiving data...");
					boolean keepGoing = true;
					
					while(mBTIn.available() > 0 && keepGoing){
						for(int n = 0; n < data.length; ++n){
							data[n] = mBTIn.read();
							if(DEBUG) Log.i("LogicAnalizerBT", "Data [HEX] " + n + ": " + Integer.toHexString(data[n]));
							if(n == 1 && data[0] == 0xFF && data[1] == 0xFF){
								keepGoing = false;
								break;
							}
						}
						if(keepGoing){
							mByteArrayBuffer.append(data[0]);
							mByteArrayBuffer.append(data[1]);
							mByteArrayBuffer.append(data[2]);
						}
					}
					
					if(DEBUG) Log.i("LogicAnalizerBT", "Byte buffer lenght: " + mByteArrayBuffer.length());
					updateUIThread.sendEmptyMessage(updateDialogTitle);
					
					// Paso el array de bytes decodificados con el algoritmo Run Lenght
					mDataSet.BufferToChannel(LogicHelper.runLenghtDecode(mByteArrayBuffer));
					
					// Decodifico cada canal con su correspondiente fuente de clock
					for(int n = 0; n < channelsNumber; ++n) {
						mDataSet.decode(n, time);
					}
					
					// Paso los datos decodificados a los Fragment en el Thread de la UI
					updateUIThread.sendEmptyMessage(dispatchInterfaces);
				}
			}
			} catch (IOException e) { e.printStackTrace(); }
			isPlaying = false;
			updateUIThread.sendEmptyMessage(dismissDialog);
		}
		return true;
	}
}
