package com.multiwork.andres;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class LogicAnalizerListView extends SherlockActivity{

	private static TextView data;
	
    /** BroadcastReceiver del Service para obtener los datos */
    private static MyReceiver mServiceReceiver;	
    /** Indica si el grafico esta activo o no (Play o Pause) */
    private static boolean isPlaying = false; 
    /** Intent del Service desde donde se reciben los datos */
    private static Intent serviceIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		data = new TextView(this);
		// Permite que el TextView se pueda mover (Scrollable)
		data.setMovementMethod(new ScrollingMovementMethod());
		
		setContentView(data);
		
		/*
		if(!MultiService.isRunning()){
			// Inicio el Servicio si no esta ya iniciado
			if(!MultiService.isRunning()){
	 			serviceIntent = new Intent(LogicAnalizerListView.this, MultiService.class);
	 			startService(serviceIntent);
			}
			// Registro el broadcast del Service para obtener los datos
		 	mServiceReceiver = new MyReceiver();
		 	IntentFilter intentFilter = new IntentFilter();
		 	intentFilter.addAction(MultiService.mAction);
		 	registerReceiver(mServiceReceiver, intentFilter);
		}*/
	}
	
	/**
	 * Receiver del Service, aqui se obtienen los datos que envia el Service
	 */
	private class MyReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			/*
			if(DEBUG) Log.i("ServiceReceiver", "onReceive() - Lenght: " + arg1.getByteArrayExtra("LogicData").length);
			// Decodifico los datos
			ReceptionBuffer = arg1.getByteArrayExtra("LogicData");
			// Paso el buffer a cada canal
			mDataSet.BufferToChannel(ReceptionBuffer);
			
			// Decodifico cada canal con su correspondiente fuente de clock
			for(int n = 0; n < channelsNumber; ++n) {
				mDataSet.decode(n, time);
			}
    	    // Actualizo el grafico
			mUpdaterHandler.post(mUpdaterTask);*/
		}
	}

}
