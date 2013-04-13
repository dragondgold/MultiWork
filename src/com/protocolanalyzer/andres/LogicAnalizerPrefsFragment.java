package com.protocolanalyzer.andres;

import android.os.Bundle;
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
                mPreferenceCategory.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle));
    			
    			ListPreference mListPreference = new ListPreference(getActivity());
        		mListPreference.setDefaultValue("0");
        		mListPreference.setEntries(com.multiwork.andres.R.array.protocolList);
        		mListPreference.setEntryValues(com.multiwork.andres.R.array.protocolValues);
        		mListPreference.setKey("protocol" + n);
        		mListPreference.setSummary(com.multiwork.andres.R.string.AnalyzerProtocolSummary);
        		mListPreference.setTitle(getString(com.multiwork.andres.R.string.AnalyzerProtocolTitle) + " " + n);
        	
        		ListPreference mListPreference2 = new ListPreference(getActivity());
        		mListPreference2.setDefaultValue("1");
        		mListPreference2.setEntries(com.multiwork.andres.R.array.channelNames);
        		mListPreference2.setEntryValues(com.multiwork.andres.R.array.protocolValues);
        		mListPreference2.setKey("SCL" + n);
        		mListPreference2.setSummary(com.multiwork.andres.R.string.AnalyzerSCLSummary);
        		mListPreference2.setTitle(com.multiwork.andres.R.string.AnalyzerSCLTitle);
        		
        		EditTextPreference mEditTextPreference = new EditTextPreference(getActivity());
        		mEditTextPreference.setDefaultValue("9600");
        		mEditTextPreference.setTitle(com.multiwork.andres.R.string.AnalyzerBaudTitle);
        		mEditTextPreference.setKey("BaudRate" + n);
        		mEditTextPreference.setSummary(com.multiwork.andres.R.string.AnalyzerBaudSummary);
                
                mPreferenceScreen.addPreference(mPreferenceCategory);
        		mPreferenceScreen.addPreference(mListPreference);
        		mPreferenceScreen.addPreference(mListPreference2);
        		mPreferenceScreen.addPreference(mEditTextPreference);
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
