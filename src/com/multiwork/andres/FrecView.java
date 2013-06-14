package com.multiwork.andres;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class FrecView extends SherlockActivity {

	private static final boolean DEBUG = true;
	private static final int Hz = 0;
	private static final int KHz = 1;
	private static final int MHz = 2;
	
	/** Indica si esta en pausa */
	private static boolean isPlaying = false;
	
	/** Display de la frecuencia */
	private static TextView tvFrecDisplay, tvPeriodo, tvFrecDisplay2;
	/** Frecuencia m�xima y m�nima */
	private static TextView tvMax, tvMin;
	/** Escala de frecuencia */
	private static TextView tvHz, tvKHz, tvMHz;
	/** Background de la frecuencia */
	private static TextView tvFrecBackground, tvFrecBackground2;
	/** TRUE --> frecuencia 1 seleccionada  FALSE --> frecuencia 2 seleccionada */
	private static boolean frecSelected = true;
	
	/** ActionBar */
	private static ActionBar actionBar;
	/** Bundle para recibir los datos provenientes del Service */
	private static Bundle data;
	/** Rango de la frecuencia (Hz, KHz, MHz)*/
	private static int rango1 = Hz, rango2 = Hz;
    /** BroadcastReceiver del Service para obtener los datos */
    private static MyReceiver mServiceReceiver;
    /** Intent del Service desde donde se reciben los datos */
    private static Intent serviceIntent;
    
    private static Frecuencia frec1 = new Frecuencia();
    private static Frecuencia frec2 = new Frecuencia();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frecview);
		if(DEBUG) Log.i("FrecView", "onCreate() -> FrecView");
		
		// findViewById()
		tvFrecDisplay = (TextView) findViewById(R.id.tvFrec);
		tvFrecDisplay2 = (TextView) findViewById(R.id.tvFrec2);
		tvPeriodo = (TextView) findViewById(R.id.tvPeriodo);
		tvMax = (TextView) findViewById(R.id.tvMaxFrec);
		tvMin = (TextView) findViewById(R.id.tvMinFrec);
		tvHz = (TextView) findViewById(R.id.tvHz);
		tvKHz = (TextView) findViewById(R.id.tvKHz);
		tvMHz = (TextView) findViewById(R.id.tvMHz);
		tvFrecBackground = (TextView) findViewById(R.id.tvBackgroundFrec);
		tvFrecBackground2 = (TextView) findViewById(R.id.tvBackgroundFrec2);
        
        actionBar = getSupportActionBar();				// Obtengo el ActionBar
        actionBar.setDisplayHomeAsUpEnabled(true);		// El icono de la aplicacion funciona como boton HOME
        
        // Fuente para el texto
	    Typeface font = Typeface.createFromAsset(getAssets(), "lcdlike.otf"); 
	    tvFrecDisplay.setTypeface(font);
	    tvFrecDisplay2.setTypeface(font);
	    tvFrecBackground.setTypeface(font);
	    tvFrecBackground2.setTypeface(font);
	    
	    // Colores
        tvFrecBackground.setTextColor(Color.argb(60, 255, 255, 255));	// Background blanco pero con transparencia
        tvFrecBackground2.setTextColor(Color.argb(60, 255, 255, 255));	// Background blanco pero con transparencia
	    tvHz.setTextColor(Color.RED);					// Color rojo (seleccionado)
	    tvKHz.setTextColor(Color.WHITE);				// Color blanco (no seleccionado)
	    tvMHz.setTextColor(Color.WHITE);				// Color blanco (no seleccionado)
	    
	    tvFrecDisplay.setTextColor(Color.RED);			// Primera frecuencia seleccionada
	    tvFrecDisplay.setText("0");
	    tvFrecDisplay2.setText("0");
	    
        // OnClickListener()
        tvHz.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				tvHz.setTextColor(Color.RED);			// Color rojo (seleccionado)
			    tvKHz.setTextColor(Color.WHITE);		// Color blanco (no seleccionado)
			    tvMHz.setTextColor(Color.WHITE);		// Color blanco (no seleccionado)
			    if(frecSelected) frec1.setRange(Frecuencia.Hz);
			    else frec2.setRange(Frecuencia.Hz);
			}
        });
        tvKHz.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				tvHz.setTextColor(Color.WHITE);			// Color blanco (no seleccionado)
			    tvKHz.setTextColor(Color.RED);			// Color rojo (seleccionado)
			    tvMHz.setTextColor(Color.WHITE);		// Color blanco (no seleccionado)
			    if(frecSelected) frec1.setRange(Frecuencia.KHz);
			    else frec2.setRange(Frecuencia.KHz);
			}
        });
        tvMHz.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				tvHz.setTextColor(Color.WHITE);			// Color blanco (no seleccionado)
			    tvKHz.setTextColor(Color.WHITE);		// Color blanco (no seleccionado)
			    tvMHz.setTextColor(Color.RED);			// Color rojo (seleccionado)
			    if(frecSelected) frec1.setRange(Frecuencia.MHz);
			    else frec2.setRange(Frecuencia.MHz);
			}
        });
	    
        tvFrecBackground.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tvFrecDisplay.setTextColor(Color.RED);		// Selecciono la frecuencia 1
				tvFrecDisplay2.setTextColor(Color.WHITE);
				frecSelected = true;
				update();
			}
        });
        tvFrecBackground2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tvFrecDisplay2.setTextColor(Color.RED);		// Selecciono la frecuencia 2
				tvFrecDisplay.setTextColor(Color.WHITE);
				frecSelected = false;
				update();
			}
        });

        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        /**
         * Mantiene a la pantalla encendida en esta Activity unicamente
         * @see http://developer.android.com/reference/android/os/PowerManager.html
         * @see http://stackoverflow.com/questions/2131948/force-screen-on
         */
        if(getPrefs.getBoolean("keepScreenAwake", false)) {
        	if(DEBUG) Log.i("FrecView","Screen Awake");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        // Simulamos un click para que se actualizen los datos del rango seleccionado cuando la Activity regresa
        tvFrecBackground.performClick();
        tvFrecBackground2.performClick();
        
	}
	
	/**
	 * Actualiza los TextView Hz, KHz, y MHz al realizarse un cambio en la selecci�n de la frecuencia
	 */
	private static void update() {
		if(frecSelected) {
			switch(rango1) {
				case Hz:		// Hz
					tvHz.performClick();
					break;
				case KHz:		// KHz
					tvKHz.performClick();
					break;
				case MHz:		// MHz
					tvMHz.performClick();
					break;
			}
		}
		else {
			switch(rango2) {
				case Hz:		// Hz
					tvHz.performClick();
					break;
				case KHz:		// KHz
					tvKHz.performClick();
					break;
				case MHz:		// MHz
					tvMHz.performClick();
					break;
			}
		}
	}
	
	@Override
	protected void onResume() {
		if(DEBUG) Log.i("onResume()","Resume FrecView");
		this.invalidateOptionsMenu();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if(DEBUG) Log.i("onPause()","Pause FrecView");
		if(isPlaying) {		// Detengo el Service si esta funcionando
			unregisterReceiver(mServiceReceiver);
			isPlaying = false;
		}
		super.onPause();
	}

	// Creo el ActionBar con los iconos
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	if(DEBUG) Log.i("FrecView", "onCreateOptionsMenu() -> FrecView");
		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.actionbarfrec, menu);
		return true;
	}
    
	/**
	 * @author Andres Torti
	 * Actualiza los iconos del ActionBar cuando se llama a this.invalidateOptionsMenu();
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(isPlaying) menu.findItem(R.id.PlayPauseFrec).setIcon(R.drawable.pause);
		else menu.findItem(R.id.PlayPauseFrec).setIcon(R.drawable.play);
		return super.onPrepareOptionsMenu(menu);
	}
    
    /**
     * @author Andres Torti
     * Al presionar los iconos del ActionBar
     * @see http://www.cafeaulait.org/course/week2/43.html
     */
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		if(DEBUG) Log.i("FrecView", "onOptionsItemSelected() -> FrecView - Item: " + item.getItemId());
 		
 		switch(item.getItemId()){
 		case android.R.id.home:
 			Intent intent = new Intent(this, MainMenu.class);
 			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // Si la Activity ya esta abierta ir a ella no abrir otra nueva
 			startActivity(intent);
 			break;
 		case R.id.settingsFrec:
 			break;
 		case R.id.PlayPauseFrec:
 			if(isPlaying) {
 				unregisterReceiver(mServiceReceiver);		// Detengo el BroadcastReceiver del Service
 				isPlaying = false;
 	 			this.invalidateOptionsMenu();
 			}
 			else {
 				// Inicio el Servicio
 				//serviceIntent = new Intent(FrecView.this, MultiService.class);
 				startService(serviceIntent);
 				// Registro el broadcast del Service para obtener los datos
 		 		mServiceReceiver = new MyReceiver();
 		 		IntentFilter intentFilter = new IntentFilter();
 		 		//intentFilter.addAction(MultiService.mAction);
 		 		registerReceiver(mServiceReceiver, intentFilter);
 		 		isPlaying = true;
 	 			this.invalidateOptionsMenu();
 			}
 			break;
 		case R.id.restartFrec:
 			frec1.restart();
 			Toast.makeText(FrecView.this, getString(R.string.FrecReinicio), Toast.LENGTH_SHORT).show();
 			break;
 		}
 		return true;
 	}
 	
 	/**
	 * Receiver del Service, aqui se obtienen los datos que envia el Service
	 */
	private class MyReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			data = arg1.getBundleExtra("FrecMode");
			
			frec1.setFrequency(data.getLong("frec"));
			frec2.setFrequency(data.getLong("frec2"));
			
			// M�ximo
			if(frecSelected) tvMax.setText("Max: " + frec1.getMaxFrec() + " Hz");
			else tvMax.setText("Max: " + frec2.getMaxFrec() + " Hz");
			// Minimo
			if(frecSelected) tvMin.setText("Min: " + frec1.getMinFrec() + " Hz");
			else tvMin.setText("Min: " + frec2.getMinFrec() + " Hz");
			
			// Muestro las frecuencias
			tvFrecDisplay.setText(frec1.getStringFrequencyRanged());
			tvFrecDisplay2.setText(frec2.getStringFrequencyRanged());
			
			// Muestra el per�odo
			if(frecSelected) tvPeriodo.setText(frec1.getPeriodString());
			else tvPeriodo.setText(frec2.getPeriodString());

		}
	}

}
