package com.multiwork.andres;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.WindowManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.protocolanalyzer.andres.LogicData;
import com.protocolanalyzer.andres.LogicDataSet;
import com.protocolanalyzer.andres.LogicData.Protocol;

public class LogicAnalizerActivity extends SherlockFragmentActivity implements OnActionBarClickListener {

	private static final boolean DEBUG = true;
	
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
    
    /** BroadcastReceiver del Service para obtener los datos */
    private static MyReceiver mServiceReceiver;	
    /** Intent del Service desde donde se reciben los datos */
    private static Intent serviceIntent;
	
	/** Buffers de recepcion donde se guarda los bytes recibidos desde el USBMultiService */
    private static byte[] ReceptionBuffer;	
    /** Dato decodificado desde LogicHelper para ser mostrado en el grafico, contiene las posiciones para mostar
      * el tipo de protocolo, etc
      * @see LogicData.java */
	private static LogicData[] mData = new LogicData[channelsNumber];
	private static LogicDataSet mDataSet = new LogicDataSet();
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if(DEBUG) Log.i("mFragmentActivity","onCreate() LogicAnalizerActivity");
		
		setContentView(R.layout.logic_fragments);
		mFragmentChart = getSupportFragmentManager().findFragmentById(R.id.chartFragment);
		
		// Obtengo el OnDataDecodedListener de los Fragments
		try { mChartDataDecodedListener = (OnDataDecodedListener) mFragmentChart; }
		catch (ClassCastException e) { throw new ClassCastException(mFragmentChart.toString() + " must implement OnDataDecodedListener"); }
		
		// Creo los LogicData y los agrego al DataSet
		for(int n=0; n < channelsNumber; ++n){
			mData[n] = new LogicData();
			mDataSet.addLogicData(mData[n]);
		}

		setPreferences();
	}
	
	/**
	 * Si estoy tomando datos y salgo de la Activity elimino el CallBack para no recibir mas datos desde el Service.
	 */
	@Override
	protected void onPause() {
		if(DEBUG) Log.i("mFragmentActivity","onPause() - " + this.toString());
		if(isPlaying) {		// Detengo el Service si esta funcionando
			isPlaying = false;
			unregisterReceiver(mServiceReceiver);
		}
		super.onPause();
	}
	
	/**
	 * En onResume() se llama a invalidateOptionsMenu(); para dibujar el boton Play/Pause en el modo correcto y no
	 * en el que aparece en el layout XML del ActionBar por defecto.
	 */
	@Override
	protected void onResume() {
		if(DEBUG) Log.i("mFragmentActivity","onResume() - " + this.toString());
		super.onResume();
		invalidateOptionsMenu();  // Actualizo el ActionBar
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
		// Boton Play/Pause
		case R.id.PlayPauseLogic:
			if(isPlaying) {
				unregisterReceiver(mServiceReceiver);	// Detengo el BroadcastReceiver del Service
				isPlaying = false;
				invalidateOptionsMenu();
			}
			else {
				// Inicio el Servicio si no esta activo
				if(!MultiService.isRunning){
					serviceIntent = new Intent(this, MultiService.class);
					startService(serviceIntent);
				}
				// Registro el broadcast del Service para obtener los datos
		 		mServiceReceiver = new MyReceiver();
		 		IntentFilter intentFilter = new IntentFilter();
		 		intentFilter.addAction(MultiService.mAction);
		 		registerReceiver(mServiceReceiver, intentFilter);
		 		isPlaying = true;
		 		invalidateOptionsMenu();
			}
			break;
		case R.id.restartLogic:
			for(int n=0; n < channelsNumber; ++n){
				mData[n].freeDataMemory();
				mData[n].clearDecodedData();
			}
			break;
		case R.id.settingsLogic:
			setPreferences();
			break;
		case R.id.listLogic:
			mFragmentList = getSupportFragmentManager().findFragmentByTag("ListLogic");
			try { mListDataDecodedListener = (OnDataDecodedListener) mFragmentList; }
			catch (ClassCastException e) { throw new ClassCastException(mFragmentList.toString() + " must implement OnDataDecodedListener"); }
			break;
		}
	}
	
	/**
	 * Crea el ActionBar desde el XML actionbarlogic.xml que define los iconos en el mismo
	 */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.actionbarlogic, menu);
		return true;
	}
	
	/**
	 * Actualiza los iconos del ActionBar cuando se llama a invalidateOptionsMenu(); porque si no se llama a esta
	 * opcion en cada onResume() cuando la Activity vuelve el ActionBar se crea con el layout del XML y el boton
	 * Play/Pause debe mostrarse de acuerdo al estado actual.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(isPlaying) {
			menu.findItem(R.id.PlayPauseLogic).setIcon(R.drawable.pause);
		}
		else {
			menu.findItem(R.id.PlayPauseLogic).setIcon(R.drawable.play);
		}
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
        	if(DEBUG) Log.i("LogicAnalizerView","Screen Awake");
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
 	}
 	
 	/**
	 * Receiver del Service, aqui se obtienen los datos que envia el Service MultiService. Se obtienen los datos en un
	 * array de bytes, se los pasa a su canal correspondiente, se los decodifica y se los pasa a los Fragments para que
	 * muestren los datos en la forma de cada uno.
	 */
	private class MyReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if(DEBUG) Log.i("mFragmentActivity", "onReceive() - Lenght: " + arg1.getByteArrayExtra("LogicData").length);
			// Decodifico los datos
			ReceptionBuffer = arg1.getByteArrayExtra("LogicData");
			// Paso el buffer a cada canal
			mDataSet.BufferToChannel(ReceptionBuffer);
			
			// Decodifico cada canal con su correspondiente fuente de clock
			for(int n = 0; n < channelsNumber; ++n) {
				mDataSet.decode(n, time);
			}
    	    // Paso los datos decodificados a los Fragment
			time = mChartDataDecodedListener.onDataDecodedListener(mData, ReceptionBuffer.length);
			if(mFragmentList != null) mListDataDecodedListener.onDataDecodedListener(mData, ReceptionBuffer.length);
		}
	}
 	
}
