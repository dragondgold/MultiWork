package com.protocolanalyzer.andres;

import com.multiwork.andres.R;
import com.protocolanalyzer.api.Protocol;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class LogicAnalizerPrefsFragment extends PreferenceFragment{

	private static final boolean DEBUG = true;

    // Preferencias del canal
    private static ListPreference        protocolList;
    private static ListPreference        clockList;
    private static CheckBoxPreference    simpleTriggerPreference;
    private static EditTextPreference    baudEditText;
    private static CheckBoxPreference    nineDataBits;
    private static ListPreference        parityList;
    private static CheckBoxPreference    checkBoxStopBit;

    // Id de la lista para cada canal
    final static private int[] idChannels = {
            com.multiwork.andres.R.array.channelNames1,
            com.multiwork.andres.R.array.channelNames2,
            com.multiwork.andres.R.array.channelNames3,
            com.multiwork.andres.R.array.channelNames4,
            com.multiwork.andres.R.array.channelNames5,
            com.multiwork.andres.R.array.channelNames6,
            com.multiwork.andres.R.array.channelNames7,
            com.multiwork.andres.R.array.channelNames8,
    };

    // Id de los valores de la lista para cada canal
    final static private int[] idChannelsValues = {
            com.multiwork.andres.R.array.clockValues1,
            com.multiwork.andres.R.array.clockValues2,
            com.multiwork.andres.R.array.clockValues3,
            com.multiwork.andres.R.array.clockValues4,
            com.multiwork.andres.R.array.clockValues5,
            com.multiwork.andres.R.array.clockValues6,
            com.multiwork.andres.R.array.clockValues7,
            com.multiwork.andres.R.array.clockValues8,
    };

    final static private CharSequence[] protocolNames = new CharSequence[Protocol.ProtocolType.values().length];
    final static private CharSequence[] protocolValues = new CharSequence[Protocol.ProtocolType.values().length];

    private static PreferenceScreen mPreferenceScreen;
    private static SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private static SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Creo los nombres e índices de los protocolos basados en la enumeracion
        final int vNumber = Protocol.ProtocolType.values().length;
        for(int n = 0; n < vNumber; ++n){
            protocolNames[n] = Protocol.ProtocolType.values()[n].toString();
            protocolValues[n] = Integer.toString(Protocol.ProtocolType.values()[n].ordinal());
        }

        if(DEBUG){
            if(idChannels.length != idChannelsValues.length)
                throw new IllegalStateException("idChannels y idChannelsValues deben tener el mismo tamaño");
        }
        if(DEBUG) Log.i("PreferenceFragment", "onCreate() -> LogicAnalizerPrefsFragment");

    	String mString = getArguments().getString("name");
    	if(mString != null){
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    		if(mString.contains("Channel")){
    			mPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());

                // Leo el numero del canal (los canales empiezan enumerados en 0)
    			int n = Integer.decode(""+mString.charAt(7));
    			if(DEBUG) Log.i("PreferenceFragment", "n: " + n);

    			PreferenceCategory mPreferenceCategory = new PreferenceCategory(getActivity());
                mPreferenceCategory.setTitle(getString(R.string.AnalyzerProtocolCategory) + " " + (n+1));

                // Protocolo
                protocolList = new ListPreference(getActivity());
                protocolList.setDefaultValue(Integer.toString(Protocol.ProtocolType.UART.ordinal()));
                protocolList.setEntries(protocolNames);
                protocolList.setEntryValues(protocolValues);
                protocolList.setKey("protocol" + (n+1));
                protocolList.setTitle(getString(R.string.AnalyzerProtocolTitle) + " " + (n + 1));
                protocolList.setDialogTitle(getString(R.string.AnalyzerProtocolTitle) + " " + (n + 1));

                // Clock
                clockList = new ListPreference(getActivity());
                clockList.setDefaultValue("-1");
                clockList.setEntries(idChannels[n]);
                clockList.setEntryValues(idChannelsValues[n]);
                clockList.setKey("CLK" + (n + 1));
                clockList.setSummary(R.string.AnalyzerCLKSummary);
                clockList.setTitle(R.string.AnalyzerCLKTitle);
                clockList.setDialogTitle(R.string.AnalyzerCLKTitle);

                // Simple Trigger
                simpleTriggerPreference = new CheckBoxPreference(getActivity());
                simpleTriggerPreference.setDefaultValue(false);
                simpleTriggerPreference.setTitle(getString(R.string.AnalyzerSimpleTriggerTitle));
                simpleTriggerPreference.setKey("simpleTrigger" + (n + 1));
                simpleTriggerPreference.setSummary(R.string.AnalyzerSimpleTriggerChannelSummary);

                /*********************** UART ***********************/
                    // Baudios
                    baudEditText = new EditTextPreference(getActivity());
                    baudEditText.setDefaultValue("9600");
                    baudEditText.setTitle(R.string.AnalyzerBaudTitle);
                    baudEditText.setKey("BaudRate" + (n + 1));
                    baudEditText.setSummary(R.string.AnalyzerBaudSummary);
                    baudEditText.setDialogTitle(R.string.AnalyzerBaudSummary);

                    // 9 bits de dato
                    nineDataBits = new CheckBoxPreference(getActivity());
                    nineDataBits.setDefaultValue(false);
                    nineDataBits.setTitle(R.string.AnalyzerNineDataTitle);
                    nineDataBits.setKey("nineData" + (n + 1));
                    nineDataBits.setSummary(R.string.AnalyzerNineDataSummary);

                    // Paridad
                    parityList = new ListPreference(getActivity());
                    parityList.setDefaultValue("-1");
                    parityList.setEntries(R.array.parityNames);
                    parityList.setEntryValues(R.array.parityValues);
                    parityList.setKey("Parity" + (n + 1));
                    parityList.setSummary(R.string.AnalyzerParitySummary);
                    parityList.setTitle(R.string.AnalyzerParityTitle);
                    parityList.setDialogTitle(R.string.AnalyzerParityTitle);

                    // Doble bit de Stop
                    checkBoxStopBit = new CheckBoxPreference(getActivity());
                    checkBoxStopBit.setDefaultValue(false);
                    checkBoxStopBit.setTitle(R.string.AnalyzerStopBitTitle);
                    checkBoxStopBit.setKey("dualStop" + (n + 1));
                    checkBoxStopBit.setSummary(R.string.AnalyzerStopBitSummary);

                mPreferenceScreen.addPreference(mPreferenceCategory);
                hideSelectedPreferences(mPrefs.getString("protocol" + (n+1), ""+ Protocol.ProtocolType.UART.ordinal()));
                setProtocolSummaries("protocol" + (n+1));
                setPreferenceScreen(mPreferenceScreen);
    		}
    		else if(mString.equals("General")){
    			if(DEBUG) Log.i("PreferenceFragment", "General Pref.");
    			addPreferencesFromResource(R.xml.logicgeneral);
    		}

            preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    // Si cambió el protocolo oculto/muestro los items correspondientes
                    if(key.contains("protocol")){
                        hideSelectedPreferences(mPrefs.getString(key, ""+ Protocol.ProtocolType.UART.ordinal()));
                        setProtocolSummaries(key);
                    }
                }
            };
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
	public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
		if(DEBUG) Log.i("PreferenceFragment", "setPreferenceScreen() -> LogicAnalizerPrefsFragment");
		super.setPreferenceScreen(preferenceScreen);
	}

    /**
     * Oculta o muestra las preferencias de acuerdo a al protocolo seleccionado en la lista.
     * Por ejemplo si seleccionamos UART se oculta la configuración de I2C y viceversa.
     * @param protocol protocolo seleccionado
     */
    private void hideSelectedPreferences (String protocol){
        int protocolValue = Integer.valueOf(protocol);
        if(protocolValue == Protocol.ProtocolType.I2C.ordinal()){
            Log.i("Preferences", "I2C Adjust");
            mPreferenceScreen.addPreference(protocolList);
            mPreferenceScreen.addPreference(clockList);
            mPreferenceScreen.addPreference(simpleTriggerPreference);
            mPreferenceScreen.removePreference(baudEditText);
            mPreferenceScreen.removePreference(nineDataBits);
            mPreferenceScreen.removePreference(parityList);
            mPreferenceScreen.removePreference(checkBoxStopBit);
        }else if(protocolValue == Protocol.ProtocolType.UART.ordinal()){
            Log.i("Preferences", "UART Adjust");
            mPreferenceScreen.addPreference(protocolList);
            mPreferenceScreen.removePreference(clockList);
            mPreferenceScreen.addPreference(simpleTriggerPreference);
            mPreferenceScreen.addPreference(baudEditText);
            mPreferenceScreen.addPreference(nineDataBits);
            mPreferenceScreen.addPreference(parityList);
            mPreferenceScreen.addPreference(checkBoxStopBit);
        }else if(protocolValue == Protocol.ProtocolType.CLOCK.ordinal()){
            Log.i("Preferences", "Clock Adjust");
            mPreferenceScreen.addPreference(protocolList);
            mPreferenceScreen.removePreference(clockList);
            mPreferenceScreen.addPreference(simpleTriggerPreference);
            mPreferenceScreen.removePreference(baudEditText);
            mPreferenceScreen.removePreference(nineDataBits);
            mPreferenceScreen.removePreference(parityList);
            mPreferenceScreen.removePreference(checkBoxStopBit);
        }else if(protocolValue == Protocol.ProtocolType.NONE.ordinal()){
            Log.i("Preferences", "NA Adjust");
            mPreferenceScreen.addPreference(protocolList);
            mPreferenceScreen.removePreference(clockList);
            mPreferenceScreen.addPreference(simpleTriggerPreference);
            mPreferenceScreen.removePreference(baudEditText);
            mPreferenceScreen.removePreference(nineDataBits);
            mPreferenceScreen.removePreference(parityList);
            mPreferenceScreen.removePreference(checkBoxStopBit);
        }
    }

    /**
     * Configura el sumario de la preferencia indicando el protocolo seleccionado
     * @param key key del protocolo a configurar
     */
    private void setProtocolSummaries (String key){
        final int value = Integer.valueOf(mPrefs.getString(key, ""+Protocol.ProtocolType.UART.ordinal()));

        protocolList.setSummary(getString(R.string.AnalyzerProtocolSummary) + " " +
                                                Protocol.ProtocolType.values()[value].toString());
    }
}