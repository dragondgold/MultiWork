package com.protocolanalyzer.andres;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.multiwork.andres.R;
import com.protocolanalyzer.api.andres.LogicData;
import com.protocolanalyzer.api.andres.LogicData.Protocol;

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

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.listLogic).setIcon(R.drawable.chart2);
		super.onPrepareOptionsMenu(menu);
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
		
		v = inflater.inflate(R.layout.logic_rawdata, container, false);
		
		int tvRawDataLogic[] = {R.id.tvRawDataLogic1, R.id.tvRawDataLogic2, R.id.tvRawDataLogic3, R.id.tvRawDataLogic4};
		int tvRawDataChannelTitle[] = { R.id.tvRawDataChannelTitle1, R.id.tvRawDataChannelTitle2, R.id.tvRawDataChannelTitle3,
				R.id.tvRawDataChannelTitle4};
		
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
	}

	@Override
	public double onDataDecodedListener(LogicData[] mLogicData, int samplesCount, boolean isConfig) {
		if(DEBUG) Log.i("mFragmentList","onDataDecodedListener() - " + mLogicData.length + " channels");
		for(int n=0; n < mRawData.length; ++n) mRawData[n].setText("");
		for(int n=0; n < mRawData.length; ++n){
			if(mLogicData[n].getProtocol() != Protocol.CLOCK){
				mRawDataTitle[n].setText( Html.fromHtml("<u>" + getString(R.string.AnalyzerChannel) + " " + (n+1) 
						+ " - " + mLogicData[n].getProtocol().toString() + "</u>") );
				
				for(int i=0; i < mLogicData[n].getStringCount(); ++i){
					// Con código HTML se puede aplicar propiedades de texto a ciertas partes unicamente
					// http://stackoverflow.com/questions/1529068/is-it-possible-to-have-multiple-styles-inside-a-textview
					mRawData[n].append( Html.fromHtml("<b><font color=#ff0000>" +
							mLogicData[n].getString(i) + "</font></b>"  
							+ "\t --> " + String.format("%.2f", (mLogicData[n].getPositionAt(i)[0]*1000000))
							+ "uS<br/>") );
				}
			}
		}
		return 0;
	}
	
}
