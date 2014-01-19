package com.frecuencimeter.andres;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bluetoothutils.andres.BluetoothHelper;
import com.bluetoothutils.andres.OnNewBluetoothDataReceived;
import com.multiwork.andres.ApplicationContext;
import com.multiwork.andres.MainMenu;
import com.multiwork.andres.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

public class FrecView extends SherlockActivity implements OnNewBluetoothDataReceived{

	private static final boolean DEBUG = true;
	private static final int Hz     = 0;
	private static final int KHz    = 1;
	private static final int MHz    = 2;
	
	/** Indica si esta en pausa */
	private static boolean isPlaying = false;
	
	/** Display de la frecuencia */
	private static TextView tvFrecDisplay, tvFrecDisplay2, tvExtraData, tvExtraData2;

	/** Background de la frecuencia */
	private static TextView tvFrecBackground, tvFrecBackground2;
	
	/** ActionBar */
	private static ActionBar actionBar;
    
    private static Frecuencia frec1 = new Frecuencia();
    private static Frecuencia frec2 = new Frecuencia();
	private static BluetoothHelper mBluetoothHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frecview);
		if(DEBUG) Log.i("FrecView", "onCreate() -> FrecView");

		tvFrecDisplay = (TextView) findViewById(R.id.tvFrec);
		tvFrecDisplay2 = (TextView) findViewById(R.id.tvFrec2);

		tvFrecBackground = (TextView) findViewById(R.id.tvBackgroundFrec);
		tvFrecBackground2 = (TextView) findViewById(R.id.tvBackgroundFrec2);
        
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        // Fuente para el texto
	    Typeface font = Typeface.createFromAsset(getAssets(), "lcdlike.otf"); 
	    tvFrecDisplay.setTypeface(font);
	    tvFrecDisplay2.setTypeface(font);
	    tvFrecBackground.setTypeface(font);
	    tvFrecBackground2.setTypeface(font);
	    
	    // Color de los TextView de fondo
        tvFrecBackground.setTextColor(Color.LTGRAY);
        tvFrecBackground2.setTextColor(Color.LTGRAY);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        /**
         * Mantiene a la pantalla encendida en esta Activity únicamente
         * @see http://developer.android.com/reference/android/os/PowerManager.html
         * @see http://stackoverflow.com/questions/2131948/force-screen-on
         */
        if(prefs.getBoolean("keepScreenAwake", false)) {
        	if(DEBUG) Log.i("FrecView","Screen Awake");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
	}
	
	@Override
	protected void onResume() {
        super.onResume();
		if(DEBUG) Log.i("onResume()", "Resume FrecView");
		this.invalidateOptionsMenu();

        mBluetoothHelper = ((ApplicationContext)getApplication()).mBluetoothHelper;
	}
	
	@Override
	protected void onPause() {
		if(DEBUG) Log.i("onPause()", "Pause FrecView");
        mBluetoothHelper.removeOnNewBluetoothDataReceived();
		super.onPause();
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.actionbarfrec, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(isPlaying) menu.findItem(R.id.PlayPauseFrec).setIcon(R.drawable.pause);
		else menu.findItem(R.id.PlayPauseFrec).setIcon(R.drawable.play);
		return super.onPrepareOptionsMenu(menu);
	}

 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		if(DEBUG) Log.i("FrecView", "onOptionsItemSelected() -> FrecView - Item: " + item.getItemId());
 		
 		switch(item.getItemId()){
            case android.R.id.home:
                Intent intent = new Intent(this, MainMenu.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // Si la Activity ya esta abierta ir a ella no abrir otra nueva
                startActivity(intent);
                break;
            case R.id.settingsFrec:

                break;
            case R.id.PlayPauseFrec:
                isPlaying = !isPlaying;
                if(isPlaying){
                    mBluetoothHelper.setOnNewBluetoothDataReceived(this);
                }else{
                    mBluetoothHelper.removeOnNewBluetoothDataReceived();
                }
                break;
 		}
 		return true;
 	}

    @Override
    public boolean onNewBluetoothDataReceivedListener(InputStream mBTIn, OutputStream mBTOut) {
        return true;
    }
}
