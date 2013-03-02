package com.protocolanalyzer.andres;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

public class LogicAnalizerPrefsFragment extends PreferenceFragment{

	private static final boolean DEBUG = true;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(DEBUG) Log.i("PreferenceFragment", "onCreate() -> LogicAnalizerPrefsFragment");
        int res = getActivity().getResources().getIdentifier(getArguments().getString("logicprefsheaders"), "xml", getActivity().getPackageName());
        addPreferencesFromResource(res);
        
    }
	
}
