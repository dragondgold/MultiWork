package com.multiwork.andres;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/** Documentacion de ActionBar en: http://developer.android.com/guide/topics/ui/actionbar.html#Style
 * 	Toast.makeText(this, "MenuItem " + item.getTitle() + " selected.", Toast.LENGTH_SHORT).show();
 * 
 * @author Andres Torti
 * @version 1.0
 * 
 */
public class MainMenu extends SherlockListActivity{
   
	private static final boolean DEBUG = true;
	private static final String[] ClassName = {"com.multiwork.andres.LCView", "com.multiwork.andres.FrecView",
		"com.multiwork.andres.LogicAnalizerView", "com.roboticarm.andres.BrazoRobot",
		"com.multiwork.andres.PruebaParser"};
	private static String[] MenuNames = new String[ClassName.length];
	private static ActionBar actionBar;
	
	//private final String ACTION_USB_PERMISSION = "com.multitools.andres.USB_PERMISSION";
	//UsbDevice device;
	
	/*
	//Pide permisos al usuario para comunicacion con el dispositivo USB
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
		    if (ACTION_USB_PERMISSION.equals(action)) {
		    	synchronized (this) {
		    		UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
		            	if(device != null){
		            		//call method to set up device communication
			            }
			        } 
			        else {
			        	Log.i(TAG, "Permission denied for device " + device);
			        }
			    }
			}
		}
	};*/
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.i("MainMenu", "onCreate() -> MainMenu");
        
        // ActionBar
        actionBar = getSupportActionBar();						// Obtengo el ActionBar
        
        // Nombres de los Menu
        MenuNames[0] = getString(R.string.LCMeterMenu);
        MenuNames[1] = getString(R.string.FrecMenu);
        MenuNames[2] = getString(R.string.LogicAnalyzerMenu);
        MenuNames[3] = getString(R.string.BrazoMenu);
        MenuNames[4] = "Prueba parseador de bits";
        
        // Menu
        setListAdapter(new ArrayAdapter<String>(MainMenu.this, android.R.layout.simple_list_item_1, MenuNames));
        
		/*
		// USB
        if(DEBUG) Log.i(TAG, "Setting UsbManager -> MainMenu");
        UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if(DEBUG) Log.i(TAG, "mUsbManager: " + mUsbManager);
        PendingIntent mPermissionIntent;
        
        if(DEBUG) Log.i(TAG, "Setting PermissionIntent -> MainMenu");
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0); Log.i(TAG, "mPermissionIntent: " +  mPermissionIntent);
        if(DEBUG) Log.i(TAG, "Setting IntentFilter -> MainMenu");
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION); Log.i(TAG, "IntentFilter: " +  filter);
        if(DEBUG) Log.i(TAG, "Setting registerReceiver -> MainMenu");
        registerReceiver(mUsbReceiver, filter);
        if(DEBUG) Log.i(TAG, "Setting requestPermission -> MainMenu");
        
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Log.i(TAG, "Device List: " + deviceList);
        //mUsbManager.requestPermission(device, mPermissionIntent);
        */
    }
    
    // Click en algun elemento de la lista
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if(DEBUG) Log.i("MainMenu", "onListItemClick() - Position: " + position);
		
		try{
			Class<?> myClass = Class.forName(ClassName[position]);
			Intent myIntent = new Intent(MainMenu.this, myClass);
			startActivity(myIntent);
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