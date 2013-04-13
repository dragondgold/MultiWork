package com.protocolanalyzer.andres;

import java.util.List;

import com.multiwork.andres.MainMenu;
import com.multiwork.andres.R;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity.Header;
import android.util.Log;

public class LogicAnalizerPrefs extends PreferenceActivity {

	private static final boolean DEBUG = true;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("PreferenceActivity", "onCreate() -> LogicAnalizerPrefs");
        // Si no estoy en Android GingerBread no uso fragments
        if(android.os.Build.VERSION.SDK_INT < 12) {
        	//this.addPreferencesFromResource(R.xml.c1analizerprefs);
        	//this.addPreferencesFromResource(R.xml.c2analizerprefs);
        	//this.addPreferencesFromResource(R.xml.c3analizerprefs);
        	//this.addPreferencesFromResource(R.xml.c4analizerprefs);
        	this.addPreferencesFromResource(R.xml.logicgeneral);
        }
        // Resultado que enviara cuando esta Activity termine y sea llamada con startActivityForResult();
        this.setResult(RESULT_OK);
        
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        testIntegrity("sampleRate", Long.decode(getPrefs.getString("sampleRate", "4000000")) );
        
        // Android menor a GingerBread (sin fragments)
        if(android.os.Build.VERSION.SDK_INT < 12) {
	        // Cambio en preferencias
	        this.findPreference("sampleRate").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Log.i("Preferences", "Change: " + newValue.toString());
					testIntegrity("sampleRate", Long.decode(newValue.toString()));
					return true;
				}
			});
	        
	        // BaudRate1
	        this.findPreference("BaudRate1").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Log.i("Preferences", "Change: " + newValue.toString());
					testIntegrity("BaudRate1", Long.decode(newValue.toString()));
					return true;
				}
			});
	        
	        // BaudRate2
	        this.findPreference("BaudRate2").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Log.i("Preferences", "Change: " + newValue.toString());
					testIntegrity("BaudRate2", Long.decode(newValue.toString()));
					return true;
				}
			});
	        
	        // BaudRate3
	        this.findPreference("BaudRate3").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Log.i("Preferences", "Change: " + newValue.toString());
					testIntegrity("BaudRate3", Long.decode(newValue.toString()));
					return true;
				}
			});
	        
	        // BaudRate4
	        this.findPreference("BaudRate4").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Log.i("Preferences", "Change: " + newValue.toString());
					testIntegrity("BaudRate4", Long.decode(newValue.toString()));
					return true;
				}
			});
        }
    }

    @SuppressLint("NewApi")
	@Override
    public void onBuildHeaders(List<Header> target) {  
    	if(DEBUG) Log.i("PreferenceActivity", "onBuildHeaders() -> LogicAnalizerPrefs");
    	if(android.os.Build.VERSION.SDK_INT >= 12) {
    		
    		Bundle mBundle = new Bundle();
    		mBundle.putString("name", "General");
    		target.add(createHeader(0, getString(R.string.GeneralTitle), "", "", "", R.drawable.settings,
    				"com.protocolanalyzer.andres.LogicAnalizerPrefsFragment", mBundle));	
    		
    		for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
    			mBundle = new Bundle();
    			mBundle.putString("name", "Channel" + (n+1));
    			target.add(createHeader(0, getString(R.string.AnalyzerChannel) + " " + (n+1), getString(R.string.AnalyzerHeaderSummary),
    					"", "", R.drawable.settings, "com.protocolanalyzer.andres.LogicAnalizerPrefsFragment", mBundle));	
    		}
    	}
    }
    
    /**
     * Crea un nuevo header con los datos especificados
     * @param mID ID del Header, 0 si no se usa
     * @param mTitle Título del Header
     * @param mSummary Sumario del Header
     * @param mBreadCrumbTitle BreadCrumbTitle del Header
     * @param mShortBreadCrumbTitle Header del Fragment
     * @param mIcon ID donde se encuentra el icono del Header
     * @param mFragment Nombre del Fragment a llamar
     * @param mExtrasBundle Extras
     * @return Nuevo Header
     */
    @SuppressLint("NewApi")
	private Header createHeader(long mID, String mTitle, String mSummary, String mBreadCrumbTitle,
    		String mShortBreadCrumbTitle, int mIcon, String mFragment, Bundle mExtrasBundle) {
    	Header mHeader = new Header();
    	
    	if(mID != 0) mHeader.id = mID;
    	else mHeader.id = HEADER_ID_UNDEFINED;
    	
    	mHeader.title = mTitle;
    	mHeader.summary = mSummary;
    	mHeader.breadCrumbTitle = mBreadCrumbTitle;
    	mHeader.breadCrumbShortTitle = mShortBreadCrumbTitle;
    	
    	mHeader.iconRes = mIcon;
    	mHeader.fragment = mFragment;
    	mHeader.fragmentArguments = mExtrasBundle;	
    	
    	return mHeader;
	}
    
    /**
     * Comprueba que el sampleRate y los baudios del UART den para que sea posible un muestreo
     * Debe haber por lo menos 3 muestreos por cada bit
     * @param changedPreference, nombre de la preferencia cambiada
     * @param newValue, nuevo valor de la preferencia que se cambio
     */
    private void testIntegrity(String changedPreference, long newValue) {
		SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		// SampleRate
		final long sampleRate = Long.decode(getPrefs.getString("sampleRate", "4000000"));
		final double sampleTime = 1.0d/sampleRate;
		
		Log.i("Preferences", "sampleRate: " + sampleRate);
		Log.i("Preferences", "newValue: " + newValue);
		
		// Si cambio el SampleRate verifico que sea posible para cada baudio seleccionado
		if(changedPreference.contains("sampleRate")) {
			// Baudios
			final long baudRate[] = new long[LogicAnalizerActivity.channelsNumber];
			boolean state = false;
			
			// Compruebo para cada baudio si es posible un correcto muestreo
			for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n) {
				baudRate[n] = Long.decode(getPrefs.getString("BaudRate" + (n+1), "9600"));
				Log.i("Preferences", "baudRate[" + n + "]: " + baudRate[n]);
				if( ((1.0d/baudRate[n]) /  (1.0d/newValue)) < 3.0d) state = true;
			}
			if(state) dialog();
		}
		// Si cambio un baudio verifico que sea posible con el SampleRate actual
		else if(changedPreference.contains("BaudRate")) {
			if( ((1.0d/newValue) / sampleTime) < 3.0d ) dialog();
		}
    }
    
    /**
	 * Crea una ventana advirtiendo al usuario que la configuraci�n del SampleRate y Baudios no es posible
	 * osea que no se alcanzaría a muestrear debidamente
 	 * @author Andres Torti
 	 * @see http://developer.android.com/guide/topics/ui/menus.html
 	 */
	private void dialog() {
		Log.i("Preferences", "ALERT DIALOG");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.AnalyzerDialogSampleTitle));

		alert.setMessage(getString(R.string.AnalyzerDialogSampleAlert));		
		
		alert.show();
	}
}