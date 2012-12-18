package com.multiwork.andres;

import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

public class LogicAnalizerActivity extends SherlockFragmentActivity {

	private static final boolean DEBUG = true;
	ActionBar actionBar;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if(DEBUG) Log.i("mFragment","onCreate() LogicAnalizerActivity");
		
		actionBar = getSupportActionBar();
		this.setContentView(R.layout.fragment);
	}
	
	@Override
	protected void onResume() {
		if(DEBUG) Log.i("mFragment","onResume() LogicAnalizerActivity");
		super.onResume();
	}
	
	/**
	 * Crea el ActionBar desde el XML actionbarlogic que define los iconos en el mismo
	 */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	if(DEBUG) Log.i("onCreateOptionsMenu()", "onCreateOptionsMenu() -> LogicAnalizerView");
    	MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.actionbarlogic, menu);
		return true;
	}
}
