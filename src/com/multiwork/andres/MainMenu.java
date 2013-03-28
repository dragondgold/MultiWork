package com.multiwork.andres;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * @author Andres Torti
 * @version 1.0
 * Services: http://developer.android.com/guide/components/services.html
 */
public class MainMenu extends SherlockListActivity{
   
	private static final boolean DEBUG = true;
	private static final String[] ClassName = {"com.multiwork.andres.LCView", "com.multiwork.andres.FrecView",
		"com.protocolanalyzer.andres.LogicAnalizerActivity", "com.roboticarm.andres.BrazoRobot",
		"com.protocolanalyzer.andres.PruebaParser"};
	private static String[] MenuNames = new String[ClassName.length];
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("MainMenu", "onCreate() -> MainMenu");
        
        // Nombres de los Menu
        MenuNames[0] = getString(R.string.LCMeterMenu);
        MenuNames[1] = getString(R.string.FrecMenu);
        MenuNames[2] = getString(R.string.LogicAnalyzerMenu);
        MenuNames[3] = getString(R.string.BrazoMenu);
        MenuNames[4] = "Prueba parseador de bits";
        
        // Menu
        setListAdapter(new ArrayAdapter<String>(MainMenu.this, android.R.layout.simple_list_item_1, MenuNames));
    }
    
    @Override
	protected void onResume() {
    	// Creo y me uno al Service
    	Intent mServiceIntent = new Intent();
    	mServiceIntent.setClassName("com.multiwork.andres.MultiService", "com.multiwork.andres.MainMenu");
    	bindService(mServiceIntent, null, Context.BIND_AUTO_CREATE);
    	startService(mServiceIntent);
    	
		super.onResume();
	}

	// Click en algun elemento de la lista
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if(DEBUG) Log.i("MainMenu", "onListItemClick() - Position: " + position);
		
		try{
			Class<?> myClass = Class.forName(ClassName[position]);
			Intent mIntent = new Intent(MainMenu.this, myClass);
			startActivity(mIntent);
		}catch (ClassNotFoundException e){
			e.printStackTrace();
		}
			
	}
    
    // Creo el ActionBar con los iconos
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	if(DEBUG) Log.i("MainMenu", "onCreateOptionsMenu() -> MainMenu");
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.actionbarmain, menu);
		return true;
	}

    // Al presionar los iconos del ActionBar
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(DEBUG) Log.i("MainMenu", "onOptionsItemSelected() -> MainMenu - Item: " + item.getTitle());
		
		switch(item.getItemId()){
		case R.id.exitMain:
			// Detengo el service y salgo de la aplicacion
			if(MultiService.isRunning) stopService(new Intent(MainMenu.this,MultiService.class));
			this.finish();
			break;
		case R.id.settingsMain:
 			startActivity(new Intent(this, MainPrefs.class));
		}
		
		return true;
	}
	
}