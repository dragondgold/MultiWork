package com.multiwork.andres;

import java.io.InputStream;
import java.io.OutputStream;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bluetoothutils.andres.BluetoothHelper;
import com.bluetoothutils.andres.DeviceListActivity;
import com.bluetoothutils.andres.OnBluetoothConnected;
import com.protocolanalyzer.andres.LogicAnalizerActivity;
import com.protocolanalyzer.andres.PruebaParser;
import com.roboticarm.andres.BrazoRobot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
   
	private static final boolean DEBUG = true;
	private static final Class<?>[] className = {LCView.class, FrecView.class,
		LogicAnalizerActivity.class, BrazoRobot.class, PruebaParser.class};
	private static String[] MenuNames = new String[className.length];

	private static ApplicationContext myApp;
	private static SharedPreferences mPrefs;
	private static String bluetoothName = "linvor";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("MainMenu", "onCreate() -> MainMenu");
        
        myApp = (ApplicationContext)getApplication();
        // Nombres de los Menu
        MenuNames[0] = getString(R.string.LCMeterMenu);
        MenuNames[1] = getString(R.string.FrecMenu);
        MenuNames[2] = getString(R.string.LogicAnalyzerMenu);
        MenuNames[3] = getString(R.string.BrazoMenu);
        MenuNames[4] = "Prueba parseador de bits";
        
        // Menu
        setListAdapter(new ArrayAdapter<String>(MainMenu.this, android.R.layout.simple_list_item_1, MenuNames));
        
        // Obtengo el nombre del dispositivo bluetooth al cual conectarme
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        bluetoothName = mPrefs.getString("btName", "linvor");
        
        // Solo creo el diálogo si ya no lo cree antes
        if(myApp.mBluetoothHelper == null){
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
					myApp.mBluetoothHelper = new BluetoothHelper(ctx, bluetoothName, true, (OnBluetoothConnected)ctx);
				}
			});
			
			mDialog.setNegativeButton(getString(R.string.No), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(DEBUG) Log.i("MainMenu", "Offline mode disabled");
					// Online
					myApp.mBluetoothHelper = new BluetoothHelper(ctx, bluetoothName, false, (OnBluetoothConnected)ctx);
					myApp.mBluetoothHelper.connect(false);
				}
			});
			
			mDialog.setCancelable(false);
			mDialog.show();
        }
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode != RESULT_CANCELED){
			if(requestCode == BluetoothHelper.BLUETOOTH_CONNECTION){
				Bundle extras = data.getExtras();
				String btAddress = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				String btName = extras.getString(DeviceListActivity.EXTRA_DEVICE_NAME);
				
				myApp.mBluetoothHelper.connectWithAddress(btAddress);
				
				// Guardo el nuevo nombre del bluetooth
				mPrefs.edit().putString("btName", btName).apply();
			}
		}else{
			if(requestCode == BluetoothHelper.BLUETOOTH_CONNECTION){
				// TODO: no se conecto
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
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
		
		Intent mIntent = new Intent(MainMenu.this, className[position]);
		startActivity(mIntent);
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
			myApp.mBluetoothHelper.disconnect();
			myApp.mBluetoothHelper = null;
			finish();
			break;
		case R.id.settingsMain:
 			startActivity(new Intent(this, MainPrefs.class));
		}
		
		return true;
	}
	
	@Override
	public void onBluetoothConnected(InputStream mInputStream, OutputStream mOutputStream) {
		myApp.mInputStream = mInputStream;
		myApp.mOutputStream = mOutputStream;
	}
	
}