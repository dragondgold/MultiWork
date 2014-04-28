package com.protocolanalyzer.andres;

import java.util.List;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.multiwork.andres.R;
import com.protocolanalyzer.api.LogicHelper;
import com.protocolanalyzer.api.Protocol;
import com.utils.andres.ConflictChecker;
import com.utils.andres.Dependency;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class LogicAnalizerPrefs extends SherlockPreferenceActivity {

	private static final boolean DEBUG = true;
	private static OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener;
	private static SharedPreferences mPrefs;
	private static Context mContext;

    private static ConflictChecker mChecker;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return LogicAnalizerPrefsFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("PreferenceActivity", "onCreate() -> LogicAnalizerPrefs");
        
        getSupportActionBar().setTitle(getString(R.string.AnalyzerPrefsActionTitle));
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Resultado que enviara cuando esta Activity termine y sea llamada con startActivityForResult();
        this.setResult(RESULT_OK);
        mContext = this;

        testIntegrity("sampleRate", Long.decode(mPrefs.getString("sampleRate", "4000000")) );

        // Creación del comprobador de conflictos
        mChecker = new ConflictChecker(mPrefs);
		for(int n = 0; n < LogicAnalyzerActivity.channelsNumber; ++n){
			Dependency mDependency = new Dependency("protocol" + (n+1), Protocol.ProtocolType.I2C.ordinal(),
                                                                        Protocol.ProtocolType.NONE.ordinal());

			mDependency.setInvalidationValue(Protocol.ProtocolType.NONE.ordinal());
			mDependency.addSecondaryReferencedDependency("CLK" + (n+1), "protocol*", Protocol.ProtocolType.CLOCK.ordinal());

			mChecker.addDependency(mDependency);
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

				// Si cambié algún trigger reconstruyo la máscara de trigger
				else if(key.contains("simpleTrigger")){
					boolean state;
					byte mask = 0;
					// Coloca cada bit del mask a 1 o 0 dependiendo si tiene activado o no el trigger
					for(int n = 0; n < LogicAnalyzerActivity.channelsNumber; ++n){
						state = sharedPreferences.getBoolean("simpleTrigger" + (n+1), false);
						mask = LogicHelper.bitSet(mask, state, n);
					}
					// Guardo la nueva máscara
					sharedPreferences.edit().putInt("simpleTriggerMask", mask).apply();
					if(DEBUG) Log.i("PreferenceActivity", "Mask: " + Integer.toBinaryString(mask));	
				}

                // Detecto conflictos y configuro los canales de Clock que corresponden
                else if(key.contains("CLK")){
                    // Configuro el canal que se seleccionó como clock como tal
                    String index = mPrefs.getString(key, null);
                    if(!index.equals("-1")) mPrefs.edit().putString("protocol" + index, ""+ Protocol.ProtocolType.CLOCK.ordinal()).apply();

				    if(mChecker.detectConflicts()){
					    Toast.makeText(mContext, getString(R.string.AnalyzerDependencies), Toast.LENGTH_SHORT).show();
                    }
                }
			}
		};
	}

    @Override
	protected void onPause() {
        if(DEBUG) Log.i("PreferenceActivity", "onPause() -> LogicAnalizerPrefs");
        // Corrijo conflictos antes de salir
        if(mChecker.detectConflicts())
            Toast.makeText(this, getString(R.string.AnalyzerDependencies), Toast.LENGTH_SHORT).show();

		mPrefs.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
        // Actualizo los Headers mostrando la informacion cambiada en las preferencias
        invalidateHeaders();
		mPrefs.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
	}

	@Override
    public void onBuildHeaders(List<Header> target) {  
    	if(DEBUG) Log.i("PreferenceActivity", "onBuildHeaders() -> LogicAnalizerPrefs");
    	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {

    		Bundle mBundle = new Bundle();
    		mBundle.putString("name", "General");
    		target.add(createHeader(0, getString(R.string.GeneralTitle), "", "", "", R.drawable.settings_dark,
    				"com.protocolanalyzer.andres.LogicAnalizerPrefsFragment", mBundle));	

            // Construyo un 'Header' para cada canal que se enviará al fragment con el número de canal en el Bundle
            // para identificarlo y mostrar la preferencia correspondiente
    		for(int n = 0; n < LogicAnalyzerActivity.channelsNumber; ++n){
                int v = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("protocol" + (n + 1),
                        "" + Protocol.ProtocolType.UART.ordinal()));

                String protocol = Protocol.ProtocolType.values()[v].toString();

    			mBundle = new Bundle();
    			mBundle.putString("name", "Channel" + n);
    			target.add(createHeader(0, getString(R.string.AnalyzerChannel) + " " + (n+1), protocol,
    					"", "", R.drawable.settings_dark, "com.protocolanalyzer.andres.LogicAnalizerPrefsFragment", mBundle));
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
			final long baudRate[] = new long[LogicAnalyzerActivity.channelsNumber];
			boolean state = false;
			
			// Compruebo para cada baudio si es posible un correcto muestreo
			for(int n=0; n < LogicAnalyzerActivity.channelsNumber; ++n) {
				baudRate[n] = Long.decode(getPrefs.getString("BaudRate" + (n+1), "9600"));
				if(DEBUG)Log.i("Preferences", "baudRate[" + n + "]: " + baudRate[n]);
				if( ((1.0d/baudRate[n]) /  (1.0d/newValue)) < 3.0d) state = true;
			}
			if(state) dialog(0);
			// Si todo esta bien compruebo si el sample rate no es demasiado alto y no van a entrar
			// todos los bits en el buffer
			else{
				final long bufferSize = LogicAnalyzerActivity.maxBufferSize;
				state = false;
				for(int n=0; n < LogicAnalyzerActivity.channelsNumber; ++n) {
					baudRate[n] = Long.decode(getPrefs.getString("BaudRate" + (n+1), "9600"));
					if(DEBUG)Log.i("Preferences", "baudRate[" + n + "]: " + baudRate[n]);
					if((Math.ceil((1.0d/baudRate[n]) / sampleTime)*10) > bufferSize) state = true; 
				}
				if(state) dialog(1);
			}
		}
		// Si cambio un baudio verifico que sea posible con el SampleRate actual
		else if(changedPreference.contains("BaudRate")) {
			final long bufferSize = LogicAnalyzerActivity.maxBufferSize;
			if( ((1.0d/newValue) / sampleTime) < 3.0d ) dialog(0);
			if((Math.ceil((1.0d/newValue) / sampleTime)*10) > bufferSize) dialog(1);
		}
    }
    
    /**
	 * Crea un dialogo advirtiendo al usuario que la configuración del SampleRate y Baudios no es posible
	 * osea que no se alcanzaría a muestrear debidamente
 	 * See http://developer.android.com/guide/topics/ui/menus.html
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