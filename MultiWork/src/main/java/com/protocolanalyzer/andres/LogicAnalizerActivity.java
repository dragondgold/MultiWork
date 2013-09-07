package com.protocolanalyzer.andres;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bluetoothutils.andres.BluetoothHelper;
import com.bluetoothutils.andres.OnNewBluetoothDataReceived;
import com.multiwork.andres.ApplicationContext;

import com.multiwork.andres.MenuListAdapter;
import com.multiwork.andres.R;
import com.protocolanalyzer.api.Clock;
import com.protocolanalyzer.api.EmptyProtocol;
import com.protocolanalyzer.api.I2CProtocol;
import com.protocolanalyzer.api.LogicBitSet;
import com.protocolanalyzer.api.LogicHelper;
import com.protocolanalyzer.api.Protocol;
import com.protocolanalyzer.api.UARTProtocol;
import com.protocolanalyzer.api.Protocol.ProtocolType;
import com.protocolanalyzer.api.utils.ByteArrayBuffer;

public class LogicAnalizerActivity extends SherlockFragmentActivity implements OnActionBarClickListener, OnNewBluetoothDataReceived, ListView.OnItemClickListener{

	private static final boolean DEBUG = true;
	private static final byte startByte = 'S';
	private static final byte logicAnalyzerMode = 'L';
	private static final int initialBufferSize = 1000;
	private static final int PREFERENCES_CODE = 1;
	public static final int maxBufferSize = 16000;
	
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
	private static final int timeOutToast = 3;
	
	private static final int timeOutLimit = 67;
	
	public static final int I2C = 1;
	public static final int UART = 2;
	public static final int Clock = 3;
	public static final int NA = -1;
	
	/** Interface donde paso los datos decodificados a los Fragments, los mismo deben implementar el Listener */
	private static OnDataDecodedListener mChartDataDecodedListener;
	private static OnDataDecodedListener mListDataDecodedListener;
	private static OnDataClearedListener mOnDataClearedListener;
	
	/** Fragment que contiene al gráfico */
	private static LogicAnalizerChartFragment mFragmentChart;
	/** Fragment que con la lista de datos en formato raw */
	private static LogicAnalizerListFragment mFragmentList;
	
    /** Numero de canales de entrada */
    public static final int channelsNumber = 8;
    /** Indica si recibo datos del Service o no (Play o Pause) */
    private static boolean isPlaying = false; 
    
    private static BluetoothHelper mBluetoothHelper;
	
	/** Buffers de recepción donde se guarda los bytes recibidos */
    private static byte[] tempBuffer;	
    private static ByteArrayBuffer mByteArrayBuffer = new ByteArrayBuffer(initialBufferSize);
    /** Canales */
	private static Protocol[] channel = new Protocol[channelsNumber];
	
	private static boolean isStarting = true;
	private static ProgressDialog mDialog;
	private static SharedPreferences getPrefs;
	private static int maxSamplesNumber;
	private static int timeOutCounter;
	
	private static Context mActivityContext;

    // DrawerLayout
    private static DrawerLayout mDrawerLayout;
    private static ActionBarDrawerToggle mDrawerToggle;
    private static ListView mDrawerList;
    private static String[] mStringDrawerList = new String[LogicAnalizerActivity.channelsNumber];
    private static final int[] mIconList = { R.drawable.settings_light, R.drawable.settings_light, R.drawable.settings_light,
            R.drawable.settings_light, R.drawable.settings_light, R.drawable.settings_light, R.drawable.settings_light,
            R.drawable.settings_light};
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if(DEBUG) Log.i("mFragmentActivity","onCreate() LogicAnalizerActivity");

        setContentView(R.layout.logic_fragments);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configuramos el DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerList = (ListView) findViewById(R.id.drawerList);

        // ListView del DrawerLayout
        final ArrayList<String> stringList = new ArrayList<String>();
        for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
            stringList.add(getString(R.string.AnalyzerDrawerChannel) + " " + (n+1));
        }
        mStringDrawerList = stringList.toArray(new String[stringList.size()]);
        mDrawerList.setAdapter(new MenuListAdapter(this, mStringDrawerList, mStringDrawerList, mIconList));
        mDrawerList.setOnItemClickListener(this);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  // Activity
                mDrawerLayout,         // DrawerLayout
                R.drawable.ic_drawer,  // Icono del Navigation Drawer que reemplaza al 'Up' del ActionBar
                R.string.AnalyzerDrawerOpen,
                R.string.AnalyzerDrawerClosed
        ) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Agrego el ListFragment
		mFragmentList = new LogicAnalizerListFragment();
		getSupportFragmentManager().beginTransaction().add(R.id.logicFragment, mFragmentList).commit();
		
		// Obtengo el OnDataDecodedListener de los Fragments
		try { mListDataDecodedListener = mFragmentList; }
		catch (ClassCastException e) { throw new ClassCastException(mFragmentList.toString() + " must implement OnDataDecodedListener"); }
		
		// Array de tamaño 0 para evitar NullPointerException
		tempBuffer = new byte[0];
		getPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if(arg0 == null || !arg0.getBoolean("setStuff")){
			setPreferences();
		}
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("setStuff", true);
		super.onSaveInstanceState(outState);
	}

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // Si estoy tomando datos y salgo de la Activity elimino el CallBack para no recibir mas datos desde el Service.
	@Override
	protected void onPause() {
		super.onPause();
		if(DEBUG) Log.i("mFragmentActivity","onPause()");
		mBluetoothHelper.removeOnNewBluetoothDataReceived();
		mBluetoothHelper.write(0);	// Indico al PIC que salí de la Activity
	}
	
	/**
	 * En onResume() se llama a supportInvalidateOptionsMenu(); para dibujar el botón Play/Pause en el modo correcto y no
	 * en el que aparece en el layout XML del ActionBar por defecto.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if(DEBUG) Log.i("mFragmentActivity","onResume()");
		
		ApplicationContext myApp = (ApplicationContext)getApplication();
		
		isStarting = true;
		isPlaying = false;
		
		// Solo si estoy en modo online procedo a obtener la conexión
		mBluetoothHelper = myApp.mBluetoothHelper;
		mBluetoothHelper.setOnNewBluetoothDataReceived(this);
		// Indico que entré en el analizador lógico
		mBluetoothHelper.write(logicAnalyzerMode);
		timeOutCounter = 0;
		mActivityContext = this;
		
		this.supportInvalidateOptionsMenu();  // Actualizo el ActionBar
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
	 		case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(mDrawerList))
                    mDrawerLayout.closeDrawer(mDrawerList);
                else
                    mDrawerLayout.openDrawer(mDrawerList);
	 			break;
	 		// Botón Play/Pause
			case R.id.PlayPauseLogic:
				if(!mBluetoothHelper.isOfflineMode()){
					// Digo al PIC que comience el muestreo
					startSample();
				}else{
					// Reinicio cada canal
					for(int n = 0; n < channelsNumber; ++n) {
						channel[n].reset();
					}
					// Demo de señal
					for(int n = 0; n < channelsNumber; ++n){
                        if(DEBUG) Log.i("mFragmentActivity","Demo: " + n);
						if(channel[n].getProtocol() != ProtocolType.CLOCK){
							LogicBitSet data, clk;
							data = LogicHelper.bitParser("100 11010010011100101 0 11010011110000111 0 11010011110000111 1 0011", 5, 2);
							channel[n].setChannelBitsData(data);
							
							if(channel[n].getProtocol() == ProtocolType.I2C){
								clk = LogicHelper.bitParser("110 01010101010101010 1 01010101010101010 1 01010101010101010 1 0111", 5, 2);
								
								Clock clockChannel = ((I2CProtocol)channel[n]).getClockSource();
								clockChannel.setChannelBitsData(clk);
							}
						}
					}
					// Decodifico cada canal
					for(int n = 0; n < channelsNumber; ++n) {
						channel[n].decode(0);
					}
					
					updateUIThread.sendEmptyMessage(dispatchInterfaces);
				}
				break;
	 		case R.id.listLogic:
	 			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	 			// Reemplazo este Fragment con el gráfico, addToBackStack() hace que al presionar la tecla
	 			// de atrás se vuelva a este Fragment y no se destruya el mismo
	 			if(!isFragmentActive("ChartLogic")){
	 				if(DEBUG) Log.i("mFragmentActivity", "Chart Fragment Created");
	 				mFragmentChart = new LogicAnalizerChartFragment(channel);
	 				
		 			transaction.replace(R.id.logicFragment, mFragmentChart, "ChartLogic");
		 			transaction.addToBackStack(null);
		 			transaction.commit();
		 			getSupportFragmentManager().executePendingTransactions();
		 			
		 			// Agrego el OnDataDecodedListener cuando se agrega el nuevo Fragment
					try { mChartDataDecodedListener = mFragmentChart; }
					catch (ClassCastException e) { throw new ClassCastException(mFragmentChart.toString() + " must implement OnDataDecodedListener"); }
					// Agrego el OnDataCleared
					try { mOnDataClearedListener = mFragmentChart; }
					catch (ClassCastException e) { throw new ClassCastException(mFragmentChart.toString() + " must implement OnDataClearedListener"); }
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
     * Envía la configuración por Bluetooth para que el Hardware inicie el muestreo
     */
	private void startSample() {
		mBluetoothHelper.write(1);
		// Envío la frecuencia de muestreo que quiero
		if(channel[0].getSampleFrequency() == 40000000) mBluetoothHelper.write(F40MHz);
		else if(channel[0].getSampleFrequency() == 20000000) mBluetoothHelper.write(F20MHz);
		else if(channel[0].getSampleFrequency() == 10000000) mBluetoothHelper.write(F10MHz);
		else if(channel[0].getSampleFrequency() == 4000000) mBluetoothHelper.write(F4MHz);
		else if(channel[0].getSampleFrequency() == 400000) mBluetoothHelper.write(F400KHz);
		else if(channel[0].getSampleFrequency() == 2000) mBluetoothHelper.write(F2KHz);
		else if(channel[0].getSampleFrequency() == 10) mBluetoothHelper.write(F10Hz);
		// Si usa trigger o no
		mBluetoothHelper.write(getPrefs.getBoolean("simpleTriggerGeneral", false) ? 'S' : 'N');
		// Mask
		mBluetoothHelper.write(getPrefs.getInt("simpleTriggerMask", 0));
		isPlaying = true;
		supportInvalidateOptionsMenu();
		
		if(DEBUG) Log.i("mFragmentActivity", "Data sent, waiting for data");
		// Muestro un diálogo de progreso indeterminado mientras se procesan los datos
		mDialog = ProgressDialog.show(this, getString(R.string.AnalyzerDialogReceiving),
				getString(R.string.PleaseWait), true);
		mDialog.setCancelable(false);
	}
	
	/**
	 * Detecta si el Fragment identificado con el fragmentTag esta activo o no
	 * @param fragmentTag nombre del Fragment
	 * @return true si esta activo, false de otro modo
	 */
	private boolean isFragmentActive (String fragmentTag){
		return !(getSupportFragmentManager().findFragmentByTag(fragmentTag) == null ||
				!getSupportFragmentManager().findFragmentByTag(fragmentTag).isVisible());
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Cambio en las preferencias
		if(requestCode == PREFERENCES_CODE){
			if(resultCode == RESULT_OK) {
				if(DEBUG) Log.i("mFragmentActivity", "Preferences Setted");
				// Aviso a los fragment que cambiaron las preferencias
				mListDataDecodedListener.onDataDecodedListener(channel, true);
				if(isFragmentActive("ChartFragment")) mChartDataDecodedListener.onDataDecodedListener(channel, true);
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
		if(DEBUG) Log.i("mFragmentActivity","onActionBarClickListener()");
		switch(buttonID){
			case R.id.restartLogic:
				for(int n=0; n < channelsNumber; ++n){
					channel[n].getChannelBitsData().clear();
				}
				break;
			case R.id.settingsLogic:
				setPreferences();
				break;
		}
	}
	
	/**
	 * Actualiza los iconos del ActionBar cuando se llama a supportInvalidateOptionsMenu(); porque si no se llama a esta
	 * opción en cada onResume() cuando la Activity vuelve el ActionBar se crea con el layout del XML y el boton
	 * Play/Pause debe mostrarse de acuerdo al estado actual.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(isPlaying) menu.findItem(R.id.PlayPauseLogic).setIcon(R.drawable.pause);
		else menu.findItem(R.id.PlayPauseLogic).setIcon(R.drawable.play);
		
		return true;
	}
	
	/**
	 * Define los parámetros de los canales de acuerdo como tipo de protocolo, velocidad de muestreo,
	 * velocidad en Baudios para el UART y si la pantalla debe permanecer o no encendida.
	 */
 	private void setPreferences() {
 		// Defino la velocidad de muestreo que es comun a todos los canales (por defecto 4MHz)
 		long sampleFrec = Long.valueOf(getPrefs.getString("sampleRate", "4000000"));
 		
        for(int n=0; n < channelsNumber; ++n){
        	if(DEBUG) Log.i("mFragmentActivity", "Channel " + (n+1) + ": " + getPrefs.getString("protocol" + (n+1), ""+UART));
        	// Seteo el protocolo para cada canal y configuraciones generales
        	switch(Integer.valueOf(getPrefs.getString("protocol" + (n+1), ""+UART))){
	        	case I2C:		// I2C
	        		channel[n] = new I2CProtocol(sampleFrec);
	        		break;
	        		
	        	case UART:		// UART
	        		channel[n] = new UARTProtocol(sampleFrec);
	        		
	        		// Configuraciones
		        	((UARTProtocol)channel[n]).setBaudRate(Integer.decode(getPrefs.getString("BaudRate" + (n+1), "9600")));
                    ((UARTProtocol)channel[n]).set9BitsMode(getPrefs.getBoolean("nineData" + (n+1), false));
                    ((UARTProtocol)channel[n]).setTwoStopBits(getPrefs.getBoolean("dualStop" + (n+1), false));

                    String parity = getPrefs.getString("Parity" + (n+1), "-1");
                    if(parity.equals("-1")) ((UARTProtocol)channel[n]).setParity(UARTProtocol.Parity.NoParity);
                    else if(parity.equals("1")) ((UARTProtocol)channel[n]).setParity(UARTProtocol.Parity.Even);
                    else if(parity.equals("2")) ((UARTProtocol)channel[n]).setParity(UARTProtocol.Parity.Odd);

	        		break;
	        		
	        	case Clock:		// CLOCK
	        		channel[n] = new Clock(sampleFrec);
	        		break;
	        		
	        	case NA:		// NONE
	        		channel[n] = new EmptyProtocol(sampleFrec);
	        		break;
	        		
	        	default:
	        		channel[n] = new EmptyProtocol(sampleFrec);
	        		break;
        	}
        }
        // Configuro las fuentes de clock
        for(int n = 0; n < channelsNumber; ++n) {
        	if(DEBUG) Log.i("mFragmentActivity", "n: " + n);
        	if(channel[n].getProtocol() == ProtocolType.I2C){
        		int clockIndex = Integer.valueOf(getPrefs.getString("CLK" + (n+1), NA+""));
        		if(DEBUG) Log.i("mFragmentActivity", "Clock Index: " + clockIndex);
        		if(clockIndex != -1)
        		    ((I2CProtocol)channel[n]).setClockSource((Clock)channel[clockIndex-1]);
                else
                    Log.e("mFragmentActivity", "Canal " + (n+1) + " de tipo I2C sin fuente de clock");
        	}
		}
        
        maxSamplesNumber = Integer.valueOf(getPrefs.getString("maxSamples","3"))*maxBufferSize;

        /**
         * Mantiene a la pantalla encendida en esta Activity únicamente
         * @see http://developer.android.com/reference/android/os/PowerManager.html
         * @see http://stackoverflow.com/questions/2131948/force-screen-on
         */
        if(getPrefs.getBoolean("keepScreenAwake", false)) {
        	if(DEBUG) Log.i("LogicAnalizerActivity","Screen Awake");
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
 	}
 	
 	// Handler para actualizar el UI Thread
 	final Handler updateUIThread = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
				case updateDialogTitle:
					mDialog.setTitle(getString(R.string.AnalyzerDialogLoading));
					break;

				case dispatchInterfaces:
					mListDataDecodedListener.onDataDecodedListener(channel, false);
					if(isFragmentActive("ChartLogic")) mChartDataDecodedListener.onDataDecodedListener(channel, false);
					break;
				
				case dismissDialog:
					mDialog.dismiss();
					supportInvalidateOptionsMenu();
					break;
					
				case timeOutToast:
					Toast.makeText(mActivityContext, getString(R.string.AnalyzerConnectionTimeOut), Toast.LENGTH_LONG).show();
					break;
			}
			return false;
		}
	});
 	
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
				if(isStarting)
					if(DEBUG) Log.i("LogicAnalizerBT", "Nothing detected");
			} catch (IOException e) { e.printStackTrace(); }
		}
		// Sino recibo los datos
		else if(isPlaying){
			if(DEBUG) Log.i("LogicAnalizerBT", "Data receive");
			try {
			int[] data = new int[3];
			
			while(mBTIn.available() > 0){
				if(mBTIn.read() == startByte && mBTIn.read() == logicAnalyzerMode){
					if(DEBUG) Log.i("LogicAnalizerBT", "Receiving data...");
					boolean keepGoing = true;
					mByteArrayBuffer.clear();
					
					while(keepGoing){
						timeOutCounter = 0;
						while(!(mBTIn.available() > 0)){
							if(timeOutCounter >= timeOutLimit){
								updateUIThread.sendEmptyMessage(dismissDialog);
								updateUIThread.sendEmptyMessage(timeOutToast);
								return true;
							}
							
							if(DEBUG) Log.i("LogicAnalizerBT", "Waiting more data");
							try { Thread.sleep(30); }
							catch (InterruptedException e) { e.printStackTrace(); }
							
							++timeOutCounter;
						}
						if(DEBUG) Log.i("LogicAnalizerBT", "Finish Waiting data");
						
						for(int n = 0; n < data.length; ++n){
							data[n] = mBTIn.read();
							if(DEBUG) Log.i("LogicAnalizerBT", "Data [HEX] " + n + ": " + Integer.toHexString(data[n]));
							if(n == 1 && data[0] == 0xFF && data[1] == 0xFF){
								keepGoing = false;
								if(DEBUG) Log.i("LogicAnalizerBT", "Finished receiving data");
								break;
							}
						}
						if(keepGoing){
							mByteArrayBuffer.append(data[0]);
							mByteArrayBuffer.append(data[1]);
							mByteArrayBuffer.append(data[2]);
						}
					}
					
					if(DEBUG) Log.i("LogicAnalizerBT", "Received data lenght: " + mByteArrayBuffer.length());
					updateUIThread.sendEmptyMessage(updateDialogTitle);
					
					// Paso el array de bytes decodificados con el algoritmo Run Lenght
					tempBuffer = LogicHelper.runLenghtDecode(mByteArrayBuffer);
					if(DEBUG) Log.i("LogicAnalizerBT", "Received data full lenght: " + tempBuffer.length);
					if(DEBUG) Log.i("LogicAnalizerBT", "Channels lenght: " + channel[0].getBitsNumber() + " samples");
					
					// Compruebo q ningun canal se pase de las muestras, si es así los reinicio
					if(channel[0].getBitsNumber() > maxSamplesNumber){
						if(DEBUG) Log.i("LogicAnalizerBT", "Reset Channels: " + channel[0].getBitsNumber() + " samples");
						if(isFragmentActive("ChartLogic")) mOnDataClearedListener.onDataCleared();
						for(Protocol mProtocol : channel){
							mProtocol.reset();
						}
					}
					LogicHelper.addBufferToChannel(tempBuffer, channel);
					
					// Decodifico cada canal
					for(int n = 0; n < channelsNumber; ++n) {
						channel[n].decode(0);
					}
					
					if(DEBUG) Log.i("LogicAnalizerBT", "Dispatching interfaces");
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

    /**
     * Click de los items en el DrawerLayout
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // Envío el item seleccionado al fragment y cierro el DrawerLayout
        mFragmentList.onItemClick(adapterView, view, i, l);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
}
