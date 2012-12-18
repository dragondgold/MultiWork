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
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class LCView extends SherlockActivity{
	
	static final boolean DEBUG = true;
	static final boolean Capacitor = false;
	static final boolean Inductor = true;
	static final double Cx = 0.000000001d;		// Capacitor de referencia 100nF
	static final double Lx = 0.000045d;			// Bobina de referencia 470uH	
	static final double micro = 0.000001d;
	static final double nano = 0.000000001d;
	static final double pico = 0.000000000001d;
	
	/** Define si esta o no en pausa */
	static boolean isPlaying = false;
	static TextView tvFrecDisplay, tvLcDisplay, tvLCBackground, tvFrecBackground, tvMax, tvMin, tvnF, tvuF, tvpF;
	/** Datos recibidos del Service */
	static Bundle data;
	/** ActionBar */
	static ActionBar actionBar;
	/** Modo del LC Meter (Inductancia o capacitor) */
	static boolean LCMode = Capacitor;
    /** BroadcastReceiver del Service para obtener los datos */
    private static MyReceiver mServiceReceiver;
    /** Intent del Service desde donde se reciben los datos */
    private static Intent serviceIntent;
    /** Unidad usada para la conversion a micro, nano y pico */
    private static double Escala = micro;
    /** Indica si es la primera vez que se toma la medida para poner los maximos y minimos */
    private static boolean firstTime = true;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lcview);
		
		if(DEBUG) Log.i("LCView", "onCreate() -> LCView");
        
        //ActionBar
        actionBar = getSupportActionBar();				// Obtengo el ActionBar
        actionBar.setDisplayHomeAsUpEnabled(true);		// El icono de la aplicacion funciona como boton HOME
		
		//FindViewById
		tvFrecDisplay = (TextView) findViewById(R.id.tvFrecLC);
		tvFrecBackground = (TextView) findViewById(R.id.tvFrecBackgroundLC);
		tvLcDisplay = (TextView) findViewById(R.id.tvLC);
		tvLCBackground = (TextView) findViewById(R.id.tvBackgroundLC);
		tvuF = (TextView) findViewById(R.id.tvuF);
		tvnF = (TextView) findViewById(R.id.tvnF);
		tvpF = (TextView) findViewById(R.id.tvpF);
		
		tvMax = (TextView) findViewById(R.id.tvMaxLC);
		tvMin = (TextView) findViewById(R.id.tvMinLC);
		
		//Fuente para el texto
	    Typeface font = Typeface.createFromAsset(getAssets(), "lcdlike.otf"); 
	    tvFrecDisplay.setTypeface(font);
	    tvFrecBackground.setTypeface(font);
	    tvLcDisplay.setTypeface(font);
	    tvLCBackground.setTypeface(font);
	    
	    tvFrecBackground.setTextColor(Color.argb(60, 255, 255, 255));	// Background blanco pero con transparencia
	    tvLCBackground.setTextColor(Color.argb(60, 255, 255, 255));		// Background blanco pero con transparencia
	    tvuF.setTextColor(Color.RED);
	 
	    tvuF.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				tvuF.setTextColor(Color.RED);		// Selecciono uF con rojo
				tvnF.setTextColor(Color.WHITE);		// nF se queda en blanco (no seleccionado)
				tvpF.setTextColor(Color.WHITE);		// pF se queda en blanco (no seleccionado)
				// Convierto el texto mostrado a la nueva unidad
				tvLcDisplay.setText("" + String.format("%.2f", (Double.parseDouble(tvLcDisplay.getText().toString())*Escala)/micro) );
				
				tvMax.setText("" + String.format("%.2f", (Double.parseDouble(tvMax.getText().toString().substring(0, 
						tvMax.getText().toString().indexOf(" ")))*Escala)/micro) + " u");
				
				tvMin.setText("" + String.format("%.2f", (Double.parseDouble(tvMin.getText().toString().substring(0, 
						tvMin.getText().toString().indexOf(" ")))*Escala)/micro) + " u");
				
				if(LCMode == Capacitor) { tvMin.append("F"); tvMax.append("F"); }
				else { tvMin.append("H"); tvMax.append("H"); }
				Escala = micro;
			}
	    	
	    });
	    
	    tvnF.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				tvnF.setTextColor(Color.RED);		// Selecciono nF con rojo
				tvuF.setTextColor(Color.WHITE);		// uF se queda en blanco (no seleccionado)
				tvpF.setTextColor(Color.WHITE);		// pF se queda en blanco (no seleccionado)
				// Convierto el texto mostrado a la nueva unidad
				tvLcDisplay.setText("" + String.format("%.2f", (Double.parseDouble(tvLcDisplay.getText().toString())*Escala)/nano) );
				
				tvMax.setText("" + String.format("%.2f", (Double.parseDouble(tvMax.getText().toString().substring(0, 
						tvMax.getText().toString().indexOf(" ")))*Escala)/nano)  + " n");
				
				tvMin.setText("" + String.format("%.2f", (Double.parseDouble(tvMin.getText().toString().substring(0, 
						tvMin.getText().toString().indexOf(" ")))*Escala)/nano) + " n");
				
				if(LCMode == Capacitor) { tvMin.append("F"); tvMax.append("F"); }
				else { tvMin.append("H"); tvMax.append("H"); }
				Escala = nano;
			}
	    	
	    });
	    
	    tvpF.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				tvpF.setTextColor(Color.RED);		// Selecciono pF con rojo
				tvnF.setTextColor(Color.WHITE);		// nF se queda en blanco (no seleccionado)
				tvuF.setTextColor(Color.WHITE);		// uF se queda en blanco (no seleccionado)
				// Convierto el texto mostrado a la nueva unidad
				tvLcDisplay.setText("" + String.format("%.2f", (Double.parseDouble(tvLcDisplay.getText().toString())*Escala)/pico) );
				
				tvMax.setText("" + String.format("%.2f", (Double.parseDouble(tvMax.getText().toString().substring(0, 
						tvMax.getText().toString().indexOf(" ")))*Escala)/pico) + " p");
				
				tvMin.setText("" + String.format("%.2f", (Double.parseDouble(tvMin.getText().toString().substring(0, 
						tvMin.getText().toString().indexOf(" ")))*Escala)/pico) + " p");
				
				if(LCMode == Capacitor) { tvMin.append("F"); tvMax.append("F"); }
				else { tvMin.append("H"); tvMax.append("H"); }
				Escala = pico;
			}
	    	
	    });
	    
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        /**
         * Mantiene a la pantalla encendida en esta Activity unicamente
         * @see http://stackoverflow.com/questions/2131948/force-screen-on
         */
        if(getPrefs.getBoolean("keepScreenAwake", false)) {
        	if(DEBUG) Log.i("LCView","Screen Awake");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
	    
	}
	
	// onResume() se llama al resumir la Activity y tambien al crearse la Activity
	@Override
	protected void onResume() {
		if(DEBUG) Log.i("onResume()","Resume LCView");
		this.invalidateOptionsMenu();
		super.onResume();
	}

	// onPause() se llama cuando se pausa la Activity y tambien antes de su destruccion
	@Override
	protected void onPause() {
		if(DEBUG) Log.i("onPause()","Pause LCView");
		if(isPlaying) {		// Detengo el Service si esta funcionando
			unregisterReceiver(mServiceReceiver);
			isPlaying = false;
		}
		super.onPause();
	}

    // Creo el ActionBar con los iconos
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	if(DEBUG) Log.i("LCView", "onCreateOptionsMenu() -> MainMenu");
		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.actionbarlc, menu);
		return true;
	}
    
	/**
	 * @author Andres Torti
	 * Actualiza los iconos del ActionBar cuando se llama a this.invalidateOptionsMenu();
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		if(isPlaying) menu.findItem(R.id.PlayPauseLc).setIcon(R.drawable.pause);
		else menu.findItem(R.id.PlayPauseLc).setIcon(R.drawable.play);
		
		if(LCMode == Capacitor) {
			menu.findItem(R.id.LCMode).setIcon(R.drawable.capacitor);
			tvuF.setText("uF");		// Coloco las tres unidades para el capacitor
			tvnF.setText("nF");
			tvpF.setText("pF");
		}
		else {
			menu.findItem(R.id.LCMode).setIcon(R.drawable.bobina);
			tvuF.setText("nH");		// Coloco las dos unidades para la bobina
			tvnF.setText("mH");
			tvpF.setText("pH");
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
    
    /**
     * @author Andres Torti
     * Al presionar los iconos del ActionBar
     * @see http://www.cafeaulait.org/course/week2/43.html
     */
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		if(DEBUG) Log.i("FrecView", "onOptionsItemSelected() -> FrecView - Item: " + item.getTitle());
 		
 		switch(item.getItemId()){
 		case android.R.id.home:
 			Intent intent = new Intent(this, MainMenu.class);
 			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // Si la Activity ya esta abierta ir a ella no abrir otra nueva
 			startActivity(intent);
 			break;
 		case R.id.settingsLc:
 			break;
 		case R.id.PlayPauseLc:
 			if(isPlaying) {
 				unregisterReceiver(mServiceReceiver);		// Detengo el BroadcastReceiver del Service
 				isPlaying = false;
 	 			this.invalidateOptionsMenu();
 			}
 			else {
 				// Inicio el Servicio
 				serviceIntent = new Intent(LCView.this, MultiService.class);
 				startService(serviceIntent);
 				// Registro el broadcast del Service para obtener los datos
 		 		mServiceReceiver = new MyReceiver();
 		 		IntentFilter intentFilter = new IntentFilter();
 		 		intentFilter.addAction(MultiService.mAction);
 		 		registerReceiver(mServiceReceiver, intentFilter);
 		 		isPlaying = true;
 	 			this.invalidateOptionsMenu();
 			}
 			break;
 		case R.id.LCMode:
 			LCMode = (LCMode == Capacitor) ? Inductor : Capacitor;
 			MultiService.setLCMode(LCMode);
 			firstTime = true;
 			this.invalidateOptionsMenu();
 			break;
 		case R.id.restartLC:
 			firstTime = true;
 			Toast.makeText(LCView.this, "Reiniciado", Toast.LENGTH_SHORT).show();
 		}
 		return true;
 	}
 	
 	/**
 	 * Calcula el valor de la inductancia o el capacitor dependiendo de LCMode
 	 * Para el calculo del capacitor:
 	 *
 	 *             1
 	 * F = -----------------
 	 *	   2*pi*sqrt(C+Cx*L)
 	 *
 	 *      |   1    |^2    1
 	 * Cx = |--------|   * --- - C
 	 *      | 2*pi*f |      L
 	 *      
 	 * @param F, frecuencia en [Hz]
 	 * @param mode, calculo de inductancia o capacitor, true -> Inductancia, false -> Capacitor
 	 * @return Valor del capacitor o inductancia en faradios y henrios respectivamente
 	 * @see http://www.micros-designs.com.ar/medidor-lc/
 	 */
 	static double calculateLC (long F, boolean mode) {
 		if(DEBUG) Log.i("LCView", "Frecuencia: " + F);
 		if(DEBUG) Log.i("LCView", "Escala: " + Escala);
 		double resultado;
 		
 		if(mode == Capacitor) {
 			resultado =  ((Math.pow(1.0d/(2*Math.PI*(double)F), 2)*(1/Lx))-Cx);
 			if(DEBUG) Log.i("LCView", "Resultado Capacitor: " + resultado);
 			if(resultado < 0) resultado = Double.POSITIVE_INFINITY;
 			return resultado;
 		}
 		else {
 			resultado = ((Math.pow(1.0d/(2*Math.PI*(double)F), 2)*(1/Cx))-Lx);
 			if(DEBUG) Log.i("LCView", "Resultado Inductancia: " + resultado);
 			if(resultado < 0) resultado = Double.POSITIVE_INFINITY;
 			return resultado;
 		}
 	}
 	
 	/**
 	 * Recibe los datos del Service
 	 */
 	private class MyReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if(DEBUG) Log.i("LCView", "BroadcastReceiver");
			data = arg1.getBundleExtra("LCMode");
			
			final double LCValue = calculateLC(data.getLong("frecLC"), LCMode);
			
			if(firstTime) { 
				tvMax.setText("" + String.format("%.2f", LCValue) );
				tvMin.setText("" + String.format("%.2f", LCValue) );
				
				// Coloco la unidad dependiendo de la escala
				if(Escala == micro) tvMax.append(" u");
				if(Escala == nano) tvMax.append(" n");
				if(Escala == pico) tvMax.append(" p");
				
				if(Escala == micro) tvMin.append(" u");
				if(Escala == nano) tvMin.append(" n");
				if(Escala == pico) tvMin.append(" p");
				
				if(LCMode == Capacitor) { tvMin.append("F"); tvMax.append("F"); }
				else { tvMin.append("H"); tvMax.append("H"); }
				
				firstTime = false;
			}
				
			// Obtengo el dato de la frecuencia y el LC y lo muestro
			if(isPlaying)	// Si no estoy en pause muestro los datos
			{
				// Si la Inductancia/Capacitor es distinta de infinita determino el máximo y mínimo
				if(!tvLcDisplay.getText().toString().contains("Infinity")) {
					// Coloco el minimo y el maximo de los valores de LC leidos
					// Máximo
					if(LCValue > Double.parseDouble(tvMax.getText().toString().substring(0, 
							tvMax.getText().toString().indexOf(" ")))*Escala ){
						
						tvMax.setText("" + String.format("%.2f", LCValue/Escala) );
						// Coloco la unidad dependiendo de la escala
						if(Escala == micro) tvMax.append(" u");
						if(Escala == nano) tvMax.append(" n");
						if(Escala == pico) tvMax.append(" p");
						
						if(LCMode == Capacitor) { tvMax.append("F"); }
						else { tvMax.append("H"); }
					}
					// Minimo
					if(LCValue < Double.parseDouble(tvMin.getText().toString().substring(0, 
							tvMin.getText().toString().indexOf(" ")))*Escala && Double.parseDouble(String.format("%.2f", LCValue/Escala)) != 0) {
						
						tvMin.setText("" + String.format("%.2f", LCValue/Escala) );
						// Coloco la unidad dependiendo de la escala
						if(Escala == micro) tvMin.append(" u");
						if(Escala == nano) tvMin.append(" n");
						if(Escala == pico) tvMin.append(" p");
						
						if(LCMode == Capacitor) { tvMin.append("F"); }
						else { tvMin.append("H"); }
					}
				}
				tvFrecDisplay.setText("" + (data.getLong("frecLC")) );				// Frecuencia en Hz
				tvLcDisplay.setText("" + String.format("%.2f", LCValue/Escala) );	// Valor LC con dos decimales
				
				if(DEBUG) Log.i("LCView", "TextView: " + tvLcDisplay.getText().toString());
			}
		}
 	}
 
}
