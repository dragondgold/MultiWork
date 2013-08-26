package com.multiwork.andres;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainPrefs extends PreferenceActivity{

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.appgeneralprefs);
	}
	
}
