package com.protocolanalyzer.andres;

import java.util.List;

import com.multiwork.andres.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class LogicAnalizerPrefs extends PreferenceActivity {

	private static final boolean DEBUG = true;
	private static OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("PreferenceActivity", "onCreate() -> LogicAnalizerPrefs");
        // Si no estoy en Android GingerBread no uso fragments
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
        	// Agrego todas las preferencias de cada canal
        	PreferenceScreen mPreferenceScreen = getPreferenceManager().createPreferenceScreen(this);
        	for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
        		PreferenceCategory mPreferenceCategory = new PreferenceCategory(this);
                mPreferenceCategory.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolCategory) + " " + (n+1));
    			
                ListPreference mListPreference = new ListPreference(this);
        		mListPreference.setDefaultValue("0");
        		mListPreference.setEntries(com.multiwork.andres.R.array.protocolList);
        		mListPreference.setEntryValues(com.multiwork.andres.R.array.protocolValues);
        		mListPreference.setKey("protocol" + n);
        		mListPreference.setSummary(com.multiwork.andres.R.string.AnalyzerProtocolSummary);
        		mListPreference.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle) + " " + (n+1));
        		mListPreference.setDialogTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle) + " " + (n+1));
        		
        		ListPreference mListPreference2 = new ListPreference(this);
        		mListPreference2.setDefaultValue("1");
        		mListPreference2.setEntries(com.multiwork.andres.R.array.channelNames);
        		mListPreference2.setEntryValues(com.multiwork.andres.R.array.protocolValues);
        		mListPreference2.setKey("SCL" + n);
        		mListPreference2.setSummary(com.multiwork.andres.R.string.AnalyzerSCLSummary);
        		mListPreference2.setTitle(com.multiwork.andres.R.string.AnalyzerSCLTitle);
        		mListPreference2.setDialogTitle(com.multiwork.andres.R.string.AnalyzerSCLTitle);
        		
        		EditTextPreference mEditTextPreference = new EditTextPreference(this);
        		mEditTextPreference.setDefaultValue("9600");
        		mEditTextPreference.setTitle(com.multiwork.andres.R.string.AnalyzerBaudTitle);
        		mEditTextPreference.setKey("BaudRate" + n);
        		mEditTextPreference.setSummary(com.multiwork.andres.R.string.AnalyzerBaudSummary);
        		mEditTextPreference.setDialogTitle(com.multiwork.andres.R.string.AnalyzerBaudSummary);
                
                mPreferenceScreen.addPreference(mPreferenceCategory);
        		mPreferenceScreen.addPreference(mListPreference);
        		mPreferenceScreen.addPreference(mListPreference2);
        		mPreferenceScreen.addPreference(mEditTextPreference);
        		setPreferenceScreen(mPreferenceScreen);
        	}
        	this.addPreferencesFromResource(R.xml.logicgeneral);
        }
        // Resultado que enviara cuando esta Activity termine y sea llamada con startActivityForResult();
        this.setResult(RESULT_OK);
        
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        testIntegrity("sampleRate", Long.decode(mPrefs.getString("sampleRate", "4000000")) );
        
        // Detección del cambio en las preferencias
        mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if(key.contains("BaudRate") || key.equals("sampleRate")){
					Log.i("Preferences", "Changed " + key + " to: " + sharedPreferences.getString(key, "0"));
					testIntegrity( key, Long.decode(sharedPreferences.getString(key, "0")) ); 
				}
			}
		};
		mPrefs.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
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
     * Comprueba que el sampleRate y los baudios del UART den para que sea posible un muestreo.
     * Debe haber por lo menos 3 muestreos por cada bit.
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
	 * Crea una ventana advirtiendo al usuario que la configuración del SampleRate y Baudios no es posible
	 * osea que no se alcanzaría a muestrear debidamente
 	 * @author Andres Torti
 	 * @see http://developer.android.com/guide/topics/ui/menus.html
 	 */
	private void dialog() {
		Log.i("Preferences", "ALERT DIALOG");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.AnalyzerDialogSampleTitle));
		alert.setMessage(getString(R.string.AnalyzerDialogSampleAlert));	
		alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alert.show();
	}
}