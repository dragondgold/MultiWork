package com.protocolanalyzer.andres;

import java.util.List;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.multiwork.andres.R;
import com.protocolanalyzer.api.LogicHelper;
import com.utils.andres.ConflictChecker;
import com.utils.andres.Dependency;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

public class LogicAnalizerPrefs extends SherlockPreferenceActivity {

	private static final boolean DEBUG = true;
	private static OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener;
	private static SharedPreferences mPrefs;
	private static Context mContext;
	
	// Id de la lista para cada canal
    final static public int[] idChannels = {com.multiwork.andres.R.array.channelNames1,
    		com.multiwork.andres.R.array.channelNames2,
    		com.multiwork.andres.R.array.channelNames3,
    		com.multiwork.andres.R.array.channelNames4,
    		com.multiwork.andres.R.array.channelNames5,
    		com.multiwork.andres.R.array.channelNames6,
    		com.multiwork.andres.R.array.channelNames7,
    		com.multiwork.andres.R.array.channelNames8,
    };
    
    // Id de los valores de la lista para cada canal
    final static public int[] idChannelsValues = {com.multiwork.andres.R.array.clockValues1,
    		com.multiwork.andres.R.array.clockValues2,
    		com.multiwork.andres.R.array.clockValues3,
    		com.multiwork.andres.R.array.clockValues4,
    		com.multiwork.andres.R.array.clockValues5,
    		com.multiwork.andres.R.array.clockValues6,
    		com.multiwork.andres.R.array.clockValues7,
    		com.multiwork.andres.R.array.clockValues8,
    };
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("PreferenceActivity", "onCreate() -> LogicAnalizerPrefs");
        
        getSupportActionBar().setTitle(getString(R.string.AnalyzerPrefsActionTitle));
        
        // Si no estoy en al menos Android Honeycomb no uso fragments
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
        	// Agrego todas las preferencias de cada canal
        	PreferenceScreen mPreferenceScreen = getPreferenceManager().createPreferenceScreen(this);
        	for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
        		PreferenceCategory mPreferenceCategory = new PreferenceCategory(this);
                mPreferenceCategory.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolCategory) + " " + (n+1));
    			
                // Protocolo
                ListPreference mListPreference = new ListPreference(this);
        		mListPreference.setDefaultValue("2");
        		mListPreference.setEntries(com.multiwork.andres.R.array.protocolList);
        		mListPreference.setEntryValues(com.multiwork.andres.R.array.protocolValues);
        		mListPreference.setKey("protocol" + (n+1));
        		mListPreference.setSummary(com.multiwork.andres.R.string.AnalyzerProtocolSummary);
        		mListPreference.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle) + " " + (n+1));
        		mListPreference.setDialogTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle) + " " + (n+1));
        		
        		// Clock
        		ListPreference mListPreference2 = new ListPreference(this);
        		mListPreference2.setDefaultValue("-1");
        		mListPreference2.setEntries(idChannels[n]);
        		mListPreference2.setEntryValues(idChannelsValues[n]);
        		mListPreference2.setKey("CLK" + (n+1));
        		mListPreference2.setSummary(com.multiwork.andres.R.string.AnalyzerCLKSummary);
        		mListPreference2.setTitle(com.multiwork.andres.R.string.AnalyzerCLKTitle);
        		mListPreference2.setDialogTitle(com.multiwork.andres.R.string.AnalyzerCLKTitle);
        		
        		// Baudios
        		EditTextPreference mEditTextPreference = new EditTextPreference(this);
        		mEditTextPreference.setDefaultValue("9600");
        		mEditTextPreference.setTitle(com.multiwork.andres.R.string.AnalyzerBaudTitle);
        		mEditTextPreference.setKey("BaudRate" + (n+1));
        		mEditTextPreference.setSummary(com.multiwork.andres.R.string.AnalyzerBaudSummary);
        		mEditTextPreference.setDialogTitle(com.multiwork.andres.R.string.AnalyzerBaudSummary);
                
        		// Simple Trigger
        		CheckBoxPreference mBoxPreference = new CheckBoxPreference(this);
        		mBoxPreference.setDefaultValue(false);
        		mBoxPreference.setTitle(getString(R.string.AnalyzerSimpleTriggerTitle));
        		mBoxPreference.setKey("simpleTrigger" + (n+1));
        		mBoxPreference.setSummary(getString(R.string.AnalyzerSimpleTriggerChannelSummary));
        		
                mPreferenceScreen.addPreference(mPreferenceCategory);
        		mPreferenceScreen.addPreference(mListPreference);
        		mPreferenceScreen.addPreference(mListPreference2);
        		mPreferenceScreen.addPreference(mEditTextPreference);
        		mPreferenceScreen.addPreference(mBoxPreference);
        		setPreferenceScreen(mPreferenceScreen);
        	}
        	this.addPreferencesFromResource(R.xml.logicgeneral);
        }
        // Resultado que enviara cuando esta Activity termine y sea llamada con startActivityForResult();
        this.setResult(RESULT_OK);
        mContext = this;
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        testIntegrity("sampleRate", Long.decode(mPrefs.getString("sampleRate", "4000000")) );
        
        final ConflictChecker mChecker = new ConflictChecker(mPrefs);
		for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
			Dependency mDependency1 = new Dependency("protocol" + (n+1), LogicAnalizerActivity.I2C, -1);
			mDependency1.setInvalidationValue(-1);
			mDependency1.addSecondaryReferencedDependency("CLK" + (n+1), "protocol*", LogicAnalizerActivity.Clock);
			
			mChecker.addDependency(mDependency1);
		}
		if(mChecker.detectConflicts()){
			Toast.makeText(this, getString(R.string.AnalyzerDependencies), Toast.LENGTH_SHORT).show();
		}
        
        // Detección del cambio en las preferencias
        mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				// Veo si debo alertar al usuario por el cambio de Baudios o SampleRate
				if(key.contains("BaudRate") || key.equals("sampleRate")){
					Log.i("Preferences", "Changed " + key + " to: " + sharedPreferences.getString(key, "0"));
					testIntegrity( key, Long.decode(sharedPreferences.getString(key, "0")) ); 
				}
				// Si cambié algun trigger reconstruyo la máscara de trigger
				else if(key.contains("simpleTrigger")){
					boolean state;
					byte mask = 0;
					// Coloca cada bit del mask a 1 o 0 dependiendo si tiene activado o no el trigger
					for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
						state = sharedPreferences.getBoolean("simpleTrigger" + (n+1), false);
						mask = LogicHelper.bitSet(mask, state, n);
					}
					// Guardo la nueva mascara
					sharedPreferences.edit().putInt("simpleTriggerMask", mask).apply();
					if(DEBUG) Log.i("PreferenceActivity", "Mask: " + Integer.toBinaryString(mask));	
				}
				if(key.contains("protocol")){
					if(DEBUG) Log.i("PreferenceActivity", "Protocol changed to " + mPrefs.getString(key, ""+LogicAnalizerActivity.UART));	
					switch (Integer.valueOf(mPrefs.getString(key, ""+LogicAnalizerActivity.UART))) {
					
					case LogicAnalizerActivity.I2C:
						int index = Integer.valueOf(mPrefs.getString("CLK" + key.charAt(8), "-1"));
						mPrefs.edit().putString("protocol" + index, ""+LogicAnalizerActivity.Clock).apply();
						break;

					default:
						break;
					}
				}
				if(mChecker.detectConflicts()){
					Toast.makeText(mContext, getString(R.string.AnalyzerDependencies), Toast.LENGTH_SHORT).show();
				}
			}
		};
	}

    @Override
	protected void onPause() {
		super.onPause();
		mPrefs.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
	}
    



	@Override
	protected void onResume() {
		super.onResume();
		mPrefs.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
	}
	



	@SuppressLint("NewApi")
	@Override
    public void onBuildHeaders(List<Header> target) {  
    	if(DEBUG) Log.i("PreferenceActivity", "onBuildHeaders() -> LogicAnalizerPrefs");
    	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
    		
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
		
		if(DEBUG)Log.i("Preferences", "sampleRate: " + sampleRate);
		if(DEBUG)Log.i("Preferences", "newValue: " + newValue);
		
		// Si cambio el SampleRate verifico que sea posible para cada baudio seleccionado
		if(changedPreference.contains("sampleRate")) {
			// Baudios
			final long baudRate[] = new long[LogicAnalizerActivity.channelsNumber];
			boolean state = false;
			
			// Compruebo para cada baudio si es posible un correcto muestreo
			for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n) {
				baudRate[n] = Long.decode(getPrefs.getString("BaudRate" + (n+1), "9600"));
				if(DEBUG)Log.i("Preferences", "baudRate[" + n + "]: " + baudRate[n]);
				if( ((1.0d/baudRate[n]) /  (1.0d/newValue)) < 3.0d) state = true;
			}
			if(state) dialog(0);
			// Si todo esta bien compruebo si el sample rate no es demasiado alto y no van a entrar
			// todos los bits en el buffer
			else{
				final long bufferSize = LogicAnalizerActivity.maxBufferSize;
				state = false;
				for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n) {
					baudRate[n] = Long.decode(getPrefs.getString("BaudRate" + (n+1), "9600"));
					if(DEBUG)Log.i("Preferences", "baudRate[" + n + "]: " + baudRate[n]);
					if((Math.ceil((1.0d/baudRate[n]) / sampleTime)*10) > bufferSize) state = true; 
				}
				if(state) dialog(1);
			}
		}
		// Si cambio un baudio verifico que sea posible con el SampleRate actual
		else if(changedPreference.contains("BaudRate")) {
			final long bufferSize = LogicAnalizerActivity.maxBufferSize;
			if( ((1.0d/newValue) / sampleTime) < 3.0d ) dialog(0);
			if((Math.ceil((1.0d/newValue) / sampleTime)*10) > bufferSize) dialog(1);
		}
    }
    
    /**
	 * Crea un dialogo advirtiendo al usuario que la configuración del SampleRate y Baudios no es posible
	 * osea que no se alcanzaría a muestrear debidamente
 	 * @author Andres Torti
 	 * @see http://developer.android.com/guide/topics/ui/menus.html
 	 */
	private void dialog(int dialogType) {
		if(DEBUG)Log.i("Preferences", "ALERT DIALOG");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		// Velocidad de muestreo muy baja
		if(dialogType == 0){
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
		// Velocidad de muestreo muy alta
		else if(dialogType == 1){
			alert.setTitle(getString(R.string.AnalyzerDialogSampleTitle));
			alert.setMessage(getString(R.string.AnalyzerDialogSampleAlert2));	
			alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			alert.show();
		}
	}
}