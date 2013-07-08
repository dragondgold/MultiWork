package com.protocolanalyzer.andres;

import com.multiwork.andres.R;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

public class LogicAnalizerPrefsFragment extends PreferenceFragment{

	private static final boolean DEBUG = true;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("PreferenceFragment", "onCreate() -> LogicAnalizerPrefsFragment");
        
    	String mString = getArguments().getString("name");
    	if(mString != null){
    		if(mString.contains("Channel")){
    			PreferenceScreen mPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
    			
    			int n = Integer.decode(""+mString.charAt(7));
    			if(DEBUG) Log.i("PreferenceFragment", "n: " + n);
    			
    			PreferenceCategory mPreferenceCategory = new PreferenceCategory(getActivity());
                mPreferenceCategory.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolCategory) + " " + n);
    			
                // Protocol
    			ListPreference mListPreference = new ListPreference(getActivity());
        		mListPreference.setDefaultValue("2");
        		mListPreference.setEntries(com.multiwork.andres.R.array.protocolList);
        		mListPreference.setEntryValues(com.multiwork.andres.R.array.protocolValues);
        		mListPreference.setKey("protocol" + n);
        		mListPreference.setSummary(com.multiwork.andres.R.string.AnalyzerProtocolSummary);
        		mListPreference.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle) + " " + n);
        		mListPreference.setDialogTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle) + " " + n);
        		
        		// Clock
        		ListPreference mListPreference2 = new ListPreference(getActivity());
        		mListPreference2.setDefaultValue("-1");
        		mListPreference2.setEntries(LogicAnalizerPrefs.idChannels[n-1]);
        		mListPreference2.setEntryValues(LogicAnalizerPrefs.idChannelsValues[n-1]);
        		mListPreference2.setKey("CLK" + n);
        		mListPreference2.setSummary(com.multiwork.andres.R.string.AnalyzerCLKSummary);
        		mListPreference2.setTitle(com.multiwork.andres.R.string.AnalyzerCLKTitle);
        		mListPreference2.setDialogTitle(com.multiwork.andres.R.string.AnalyzerCLKTitle);
        		
        		// Baudios
        		EditTextPreference mEditTextPreference = new EditTextPreference(getActivity());
        		mEditTextPreference.setDefaultValue("9600");
        		mEditTextPreference.setTitle(com.multiwork.andres.R.string.AnalyzerBaudTitle);
        		mEditTextPreference.setKey("BaudRate" + n);
        		mEditTextPreference.setSummary(com.multiwork.andres.R.string.AnalyzerBaudSummary);
        		mEditTextPreference.setDialogTitle(com.multiwork.andres.R.string.AnalyzerBaudSummary);
        		
        		// Simple Trigger
        		CheckBoxPreference mBoxPreference = new CheckBoxPreference(getActivity());
        		mBoxPreference.setDefaultValue(false);
        		mBoxPreference.setTitle(getString(R.string.AnalyzerSimpleTriggerTitle));
        		mBoxPreference.setKey("simpleTrigger" + n);
        		mBoxPreference.setSummary(getString(R.string.AnalyzerSimpleTriggerChannelSummary));
                
                mPreferenceScreen.addPreference(mPreferenceCategory);
        		mPreferenceScreen.addPreference(mListPreference);
        		mPreferenceScreen.addPreference(mListPreference2);
        		mPreferenceScreen.addPreference(mEditTextPreference);
        		mPreferenceScreen.addPreference(mBoxPreference);
        		setPreferenceScreen(mPreferenceScreen);
    		}
    		else if(mString.equals("General")){
    			if(DEBUG) Log.i("PreferenceFragment", "General Pref.");
    			addPreferencesFromResource(com.multiwork.andres.R.xml.logicgeneral);
    		}
        }
    }

	@Override
	public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
		if(DEBUG) Log.i("PreferenceFragment", "setPreferenceScreen() -> LogicAnalizerPrefsFragment");
		super.setPreferenceScreen(preferenceScreen);
	}
}
