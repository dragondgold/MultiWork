package com.protocolanalyzer.andres;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

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

public class LogicAnalyzerActivity extends SherlockFragmentActivity implements OnActionBarClickListener, OnNewBluetoothDataReceived, ListView.OnItemClickListener{

	private static final boolean DEBUG = true;
	private static final byte startByte = 'S';
	private static final byte logicAnalyzerMode = 'L';
    private static final byte retryByte = 'R';
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

    // Tiempo en mS para que se aborte la espera de datos. Para calcular el tiempo en mS
    // es timeOutLimit*30. En este caso 67*30=2010mS
	private static final int timeOutLimit = 67;
	
	/** Interface donde paso los datos decodificados a los Fragments, los mismo deben implementar el Listener */
	private static OnDataDecodedListener mChartDataDecodedListener;
	private static OnDataDecodedListener mListDataDecodedListener;
	private static OnDataClearedListener mOnDataClearedListener;
	
	/** Fragment que contiene al gráfico */
	private static LogicAnalyzerChartFragment mFragmentChart;
	/** Fragment que con la lista de datos en formato raw */
	private static LogicAnalyzerListFragment mFragmentList;
	
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
	private static SharedPreferences mPrefs;
	private static int maxSamplesNumber;
	private static int timeOutCounter;
	
	private static Context mActivityContext;
    public static com.protocolanalyzer.api.utils.Configuration channelsConfig = new com.protocolanalyzer.api.utils.Configuration();

    // DrawerLayout
    private static DrawerLayout mDrawerLayout;
    private static ActionBarDrawerToggle mDrawerToggle;
    private static ListView mDrawerList;
    private static String[] mStringDrawerList = new String[LogicAnalyzerActivity.channelsNumber];
    private static final int[] mIconList = { R.drawable.settings_dark, R.drawable.settings_dark, R.drawable.settings_dark,
            R.drawable.settings_dark, R.drawable.settings_dark, R.drawable.settings_dark, R.drawable.settings_dark,
            R.drawable.settings_dark};

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if(DEBUG) Log.i("mFragmentActivity","onCreate() LogicAnalyzerActivity");

        setContentView(R.layout.logic_fragments);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configuramos el DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerList = (ListView) findViewById(R.id.drawerList);

        // ListView del DrawerLayout
        final ArrayList<String> stringList = new ArrayList<String>();
        for(int n = 0; n < LogicAnalyzerActivity.channelsNumber; ++n){
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
		mFragmentList = new LogicAnalyzerListFragment();
		getSupportFragmentManager().beginTransaction().add(R.id.logicFragment, mFragmentList).commit();
		
		// Obtengo el OnDataDecodedListener de los Fragments
		try { mListDataDecodedListener = mFragmentList; }
		catch (ClassCastException e) { throw new ClassCastException(mFragmentList.toString() + " must implement OnDataDecodedListener"); }
		
		// Array de tamaño 0 para evitar NullPointerException
		tempBuffer = new byte[0];
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		setPreferences();
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

	@Override
	protected void onPause() {
		if(DEBUG) Log.i("mFragmentActivity","onPause()");

		mBluetoothHelper.removeOnNewBluetoothDataReceived();
		mBluetoothHelper.write(0);	// Indico al PIC que salí de la Activity

        // Guardo los datos de cada canal para luego recuperarlos en onResume()
        if(DEBUG) Log.i("mFragmentActivity", "Saving Activity State");
        // Guardo el conjunto de bits de cada canal
        for(int c = 0; c < channel.length; ++c){
            Protocol mProtocol = channel[c];
            boolean[] list = new boolean[mProtocol.getChannelBitsData().length()];

            for(int n = 0; n < mProtocol.getChannelBitsData().length(); ++n){
                list[n] = mProtocol.getChannelBitsData().get(n);
            }
            getIntent().putExtra(String.valueOf(c), list);
        }

        super.onPause();
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

        // Recupero el estado de la Activity que guarde en onPause(). Cuando se llama a la
        //  Activity de las preferencias al volver todos los canales son creados de nuevo
        //  de acuerdo a las preferencias, es necesario entonces recuperar sus valores y
        //  volver a decodificarlos.
        if(getIntent().getExtras() == null){
            if(DEBUG) Log.i("mFragmentActivity","NULL Extras");
        }
        else{
            if(DEBUG) Log.i("mFragmentActivity", "Restoring Previous Activity State");

            for(int n = 0; n < channel.length; ++n){
                // Obtengo cada array de boolean que contiene los datos del canal, los coloco en un
                //  LogicBitSet y los agrego al canal correspondiente
                Protocol mProtocol = channel[n];
                LogicBitSet mBitSet = new LogicBitSet();
                boolean[] list = getIntent().getExtras().getBooleanArray(String.valueOf(n));

                for(int i = 0; i < list.length; ++i){
                    mBitSet.set(i, list[i]);
                }
                mProtocol.setChannelBitsData(mBitSet);
            }

            // Decodifico cada canal
            for(int n = 0; n < channelsNumber; ++n) {
                channel[n].decode(0);
            }

            if(DEBUG) Log.i("LogicAnalyzerBT", "Enviando datos decodificados a los Fragments");
            // Paso los datos decodificados a los Fragment en el Thread de la UI
            updateUIThread.sendEmptyMessage(dispatchInterfaces);
        }

		isStarting = true;
		isPlaying = false;

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
                // Solo abro el NavigationDrawer si estoy en el fragment de la lista
                if(!isFragmentActive("ChartLogic")){
                    if (mDrawerLayout.isDrawerOpen(mDrawerList))
                        mDrawerLayout.closeDrawer(mDrawerList);
                    else
                        mDrawerLayout.openDrawer(mDrawerList);
                }
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
	 				mFragmentChart = new LogicAnalyzerChartFragment(channel);
	 				
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
            case R.id.restartLogic:
                if(DEBUG) Log.i("mFragmentActivity","Action Restarted");
                for(int n=0; n < channelsNumber; ++n){
                    channel[n].reset();
                }
                updateUIThread.sendEmptyMessage(dispatchInterfaces);
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
		mBluetoothHelper.write(mPrefs.getBoolean("simpleTriggerGeneral", false) ? 'S' : 'N');
		// Mask
		mBluetoothHelper.write(mPrefs.getInt("simpleTriggerMask", 0));
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
					channel[n].reset();
				}
                updateUIThread.sendEmptyMessage(dispatchInterfaces);
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

    private String getPreferenceType(String key){
        int n = 0;
        while(n <= 2){
            try {
                switch (n){
                    case 0:
                        mPrefs.getString(key, null);
                        return "String";
                    case 1:
                        mPrefs.getInt(key, 0);
                        return "Integer";
                    case 2:
                        mPrefs.getBoolean(key, false);
                        return "Boolean";
                }
            } catch (ClassCastException e){ ++n; }
        }
        return "Error";
    }

	/**
	 * Define los parámetros de los canales de acuerdo como tipo de protocolo, velocidad de muestreo,
	 * velocidad en Baudios para el UART y si la pantalla debe permanecer o no encendida.
	 */
 	private void setPreferences() {
 		// Defino la velocidad de muestreo que es común a todos los canales (por defecto 4MHz)
 		long sampleFrec = Long.valueOf(mPrefs.getString("sampleRate", "4000000"));

        // Mapeo los valores del SharedPreference en las configuraciones de los canales.
        // channelsConfig contiene las configuraciones para todos los tipos de canales en el mismo objeto.
        Map<String, ?> prefsMap = mPrefs.getAll();
        for(Map.Entry<String, ?> entry : prefsMap.entrySet()){
            String prefType = getPreferenceType(entry.getKey());
            String key = entry.getKey();

            // Salteo el key si no esta definido, se usara el valor por defecto de la clase
            if(!mPrefs.contains(entry.getKey())) continue;

            if(key.contains("BaudRate") || key.contains("Parity")){
                channelsConfig.setProperty(key, Integer.valueOf(mPrefs.getString(key, "")));
            }else if(key.contains("nineData") || key.contains("dualStop")){
                channelsConfig.setProperty(key, mPrefs.getBoolean(key, false));
            }
        }
 		
        for(int n=0; n < channelsNumber; ++n){
        	// Configuro el protocolo para cada canal y configuraciones generales
            final int value = Integer.valueOf(mPrefs.getString("protocol" + (n + 1), "" + ProtocolType.UART.ordinal()));

            // I2C
            if(value == ProtocolType.I2C.ordinal()){
	            channel[n] = new I2CProtocol(sampleFrec, channelsConfig, n);
            // UART
            }else if(value == ProtocolType.UART.ordinal()){
        		channel[n] = new UARTProtocol(sampleFrec, channelsConfig, n);
            // Clock
            }else if(value == ProtocolType.CLOCK.ordinal()){
                channel[n] = new Clock(sampleFrec, channelsConfig, n);
            // None
            }else if(value == ProtocolType.NONE.ordinal()){
                channel[n] = new EmptyProtocol(sampleFrec, channelsConfig, n);
            // Default
            }else{
	        	channel[n] = new EmptyProtocol(sampleFrec, channelsConfig, n);
        	}
        }
        // Configuro las fuentes de clock
        for(int n = 0; n < channelsNumber; ++n) {
        	if(DEBUG) Log.i("mFragmentActivity", "n: " + n);
        	if(channel[n].getProtocol() == ProtocolType.I2C){
        		int clockIndex = Integer.valueOf(mPrefs.getString("CLK" + (n + 1), "" + ProtocolType.NONE.ordinal()));
        		if(DEBUG) Log.i("mFragmentActivity", "Clock Index: " + clockIndex);
        		if(clockIndex != -1)
        		    ((I2CProtocol)channel[n]).setClockSource((Clock)channel[clockIndex-1]);
                else
                    Log.e("mFragmentActivity", "Canal " + (n+1) + " de tipo I2C sin fuente de clock");
        	}
		}
        
        maxSamplesNumber = Integer.valueOf(mPrefs.getString("maxSamples","3"))*maxBufferSize;

        /**
         * Mantiene a la pantalla encendida en esta Activity únicamente
         * @see http://developer.android.com/reference/android/os/PowerManager.html
         * @see http://stackoverflow.com/questions/2131948/force-screen-on
         */
        if(mPrefs.getBoolean("keepScreenAwake", false)) {
        	if(DEBUG) Log.i("LogicAnalyzerActivity","Screen Awake");
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
					if(DEBUG) Log.e("LogicAnalizerBT", "Logic Analyzer Mode ERROR");
			} catch (IOException e) { e.printStackTrace(); }
		}
		// Sino recibo los datos
		else if(isPlaying){
			if(DEBUG) Log.i("LogicAnalizerBT", "Data receive");
			try {
			int[] data = new int[3];
            boolean retry = false;
			
			while(mBTIn.available() > 0 || retry){
                // Los primeros bytes deben ser de Start y el Modo
				if(mBTIn.read() == startByte && mBTIn.read() == logicAnalyzerMode){
					if(DEBUG) Log.i("LogicAnalizerBT", "Receiving data...");
					boolean keepGoing = true;
					mByteArrayBuffer.clear();
					
					while(keepGoing){
						timeOutCounter = 0;
                        // Espero y cuento el tiempo transcurrido aproximado para cancelar la espera
                        // en caso de que se exceda
						while(!(mBTIn.available() > 0)){
                            // Notifico al usuario que cancelo la comunicación
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

                        // Recibo los bytes y compruebo si recibo dos 0xFF seguidos que indican la terminación
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
                    // Leo los dos bytes del CRC16
					int CRC16L = mBTIn.read();
                    int CRC16H = mBTIn.read();
                    int CRC16Checksum = LogicHelper.byteToInt((byte)CRC16L, (byte)CRC16H);

					if(DEBUG) Log.i("LogicAnalizerBT", "Received data lenght: " + mByteArrayBuffer.length());
					updateUIThread.sendEmptyMessage(updateDialogTitle);
					
					// Paso el array de bytes decodificados con el algoritmo Run Lenght
					tempBuffer = LogicHelper.runLenghtDecode(mByteArrayBuffer);
					if(DEBUG) Log.i("LogicAnalizerBT", "Received data full lenght: " + tempBuffer.length);
					if(DEBUG) Log.i("LogicAnalizerBT", "Channels lenght: " + channel[0].getBitsNumber() + " samples");

                    // Compruebo que el CRC recibido coincida con los datos
                    int calculatedCRC16 = CRC16.calculateCRC(tempBuffer);
                    if(calculatedCRC16 == CRC16Checksum){
                        if(DEBUG) Log.i("LogicAnalizerBT", "Checksum CRC16 coinciden: " + calculatedCRC16);
                        retry = false;
                    }
                    // Pido que se reenvíen los datos si el checksum no coincide
                    else{
                        if(DEBUG) Log.i("LogicAnalizerBT", "Checksum CRC16 NO coinciden. Recibido: " + calculatedCRC16
                        + " - Calculado: " + calculatedCRC16);
                        mBTOut.write(retryByte);
                        retry = true;
                    }

                    if(!retry){
                        // Compruebo q ningún canal se pase de las muestras, si es así los reinicio
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
