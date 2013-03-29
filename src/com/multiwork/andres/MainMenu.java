package com.multiwork.andres;

import java.io.InputStream;
import java.io.OutputStream;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bluetoothutils.andres.BluetoothHelper;
import com.bluetoothutils.andres.OnBluetoothConnected;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
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
public class MainMenu extends SherlockListActivity implements OnBluetoothConnected{
   
	public static BluetoothHelper mBluetoothHelper = null;
	public static final String bluetoothName = "linvor";
	public static boolean offlineMode = false;
	
	public static InputStream mInputStream = null;
	public static OutputStream mOutputStream = null;
	
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
    
        final Context ctx = this;
    	
    	// Pregunto si deseo entrar en modo offline primero
		final AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
		mDialog.setTitle(getString(R.string.BTOfflineTitle));
		mDialog.setMessage(getString(R.string.BTOfflineSummary));
		
		mDialog.setPositiveButton(getString(R.string.Yes), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(DEBUG) Log.i("MainMenu", "Offline mode enabled");
				// Offline
				mBluetoothHelper = new BluetoothHelper(ctx, bluetoothName, true);
			}
		});
		
		mDialog.setNegativeButton(getString(R.string.No), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(DEBUG) Log.i("MainMenu", "Offline mode disabled");
				// Online
				mBluetoothHelper = new BluetoothHelper(ctx, bluetoothName, false);
				mBluetoothHelper.connect(true);
				mBluetoothHelper.setOnBluetoothConnected((OnBluetoothConnected)ctx);
			}
		});
		
		mDialog.setCancelable(false);
		mDialog.show();
    }
    
    @Override
	protected void onResume() {
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
			finish();
			break;
		case R.id.settingsMain:
 			startActivity(new Intent(this, MainPrefs.class));
		}
		
		return true;
	}
	
	@Override
	public void onBluetoothConnected(InputStream mInputStream, OutputStream mOutputStream) {
		MainMenu.mInputStream = mInputStream;
		MainMenu.mOutputStream = mOutputStream;
	}
	
}