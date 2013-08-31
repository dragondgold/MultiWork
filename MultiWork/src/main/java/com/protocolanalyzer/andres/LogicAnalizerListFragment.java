package com.protocolanalyzer.andres;

import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.multiwork.andres.R;
import com.protocolanalyzer.api.Protocol;
import com.protocolanalyzer.api.TimePosition;
import com.protocolanalyzer.api.Protocol.ProtocolType;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

@SuppressLint("ValidFragment")
public class LogicAnalizerListFragment extends SherlockFragment implements OnDataDecodedListener{

	private static final boolean DEBUG = true;
	
	private static SherlockFragmentActivity mActivity;
	private static ActionBar mActionBar;
	private static TextView mRawData[] = new TextView[LogicAnalizerActivity.channelsNumber];
	private static TextView mRawDataTitle[] = new TextView[LogicAnalizerActivity.channelsNumber];
	private static View v;
	
	private static Protocol[] mProtocols;
	
	private static final int tvRawDataLogic[] = {R.id.tvRawDataLogic1, R.id.tvRawDataLogic2,
													R.id.tvRawDataLogic3, R.id.tvRawDataLogic4,
													R.id.tvRawDataLogic5, R.id.tvRawDataLogic6,
													R.id.tvRawDataLogic7, R.id.tvRawDataLogic8,};
	
	private static final int tvRawDataChannelTitle[] = { R.id.tvRawDataChannelTitle1, R.id.tvRawDataChannelTitle2,
																R.id.tvRawDataChannelTitle3, R.id.tvRawDataChannelTitle4,
																R.id.tvRawDataChannelTitle5, R.id.tvRawDataChannelTitle6,
																R.id.tvRawDataChannelTitle7, R.id.tvRawDataChannelTitle8,};

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.actionbar_logiclist, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(DEBUG) Log.i("mFragmentList","onCreate()");
		
		// Obtengo la Activity que contiene el Fragment
		mActivity = getSherlockActivity();
		
		mActionBar = mActivity.getSupportActionBar();				// Obtengo el ActionBar
		mActionBar.setDisplayHomeAsUpEnabled(true);					// El icono de la aplicacion funciona como boton HOME
		mActionBar.setTitle(getString(R.string.AnalyzerName)) ;		// Nombre
        this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if(DEBUG) Log.i("mFragmentList","onCreateView()");
		v = inflater.inflate(R.layout.logic_rawdata, container, false);
		
		for(int n = 0; n < tvRawDataLogic.length; ++n){
			mRawData[n] = (TextView) v.findViewById(tvRawDataLogic[n]);
			mRawData[n].setMovementMethod(new ScrollingMovementMethod());
		
			mRawDataTitle[n] = (TextView) v.findViewById(tvRawDataChannelTitle[n]);
			mRawDataTitle[n].setText( Html.fromHtml("<u>" + getString(R.string.AnalyzerChannel) + " " + (n+1)) );
		}
		
		/** Hace que todos los toques en la pantalla sean para esta Fragment y no el que esta detras */
		v.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(DEBUG) Log.i("mFragmentList","Resume");
		if(mProtocols != null) onDataDecodedListener(mProtocols, false);
	}

	@Override
	public void onDataDecodedListener(Protocol[] data, boolean isConfig) {
		if(!isConfig){
			mProtocols = data;
			if(DEBUG) Log.i("mFragmentList","onDataDecodedListener() - " + data.length + " channels");
			for(int n=0; n < mRawData.length; ++n) mRawData[n].setText("");
			
			for(int n=0; n < mRawData.length; ++n){
				List<TimePosition> stringData = data[n].getDecodedData();
				if(DEBUG) Log.i("mFragmentList","onDataDecodedListener() Channel " + n + " -> " + stringData.size());
				
				if(data[n].getProtocol() != ProtocolType.CLOCK){
					mRawDataTitle[n].setText( Html.fromHtml("<u>" + getString(R.string.AnalyzerChannel) + " " + (n+1) 
							+ " - " + data[n].getProtocol().toString() + "</u>") );
					
					
					for(int i=0; i < stringData.size(); ++i){
						// Con código HTML se puede aplicar propiedades de texto a ciertas partes unicamente
						// http://stackoverflow.com/questions/1529068/is-it-possible-to-have-multiple-styles-inside-a-textview
						mRawData[n].append( Html.fromHtml("<b><font color=#ff0000>" +
								stringData.get(i).getString() + "</font></b>"  
								+ "\t --> " + String.format("%.3f", (stringData.get(i).startTime()*1E6))
								+ "uS<br/>") );
					}
				}
			}
		}
	}
	
}
