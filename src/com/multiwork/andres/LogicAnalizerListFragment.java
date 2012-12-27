package com.multiwork.andres;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.protocolanalyzer.andres.LogicData;
import com.protocolanalyzer.andres.LogicData.Protocol;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LogicAnalizerListFragment extends SherlockFragment implements OnDataDecodedListener{

	private static final boolean DEBUG = true;
	
	private static SherlockFragmentActivity mActivity;
	private static ActionBar mActionBar;
	private static TextView mRawData[] = new TextView[LogicAnalizerActivity.channelsNumber];
	private static OnActionBarClickListener mActionBarListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(DEBUG) Log.i("mFragment2","onCreate()");
		
		// Obtengo la Activity que contiene el Fragment
		mActivity = getSherlockActivity();
		
		mActionBar = mActivity.getSupportActionBar();				// Obtengo el ActionBar
		mActionBar.setDisplayHomeAsUpEnabled(true);					// El icono de la aplicacion funciona como boton HOME
		mActionBar.setTitle(getString(R.string.AnalyzerName)) ;		// Nombre
        this.setHasOptionsMenu(true);
        
        // Obtengo el OnActionBarClickListener de la Activity
     	try { mActionBarListener = (OnActionBarClickListener) mActivity; }
     	catch (ClassCastException e) { throw new ClassCastException(mActivity.toString() + " must implement OnActionBarClickListener"); }
        
        mActionBarListener.onActionBarClickListener(R.id.PlayPauseLogic);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.logic_rawdata, container, false);
		
		mRawData[0] = (TextView) v.findViewById(R.id.tvRawDataLogic1); 
		mRawData[0].setMovementMethod(new ScrollingMovementMethod());
		mRawData[1] = (TextView) v.findViewById(R.id.tvRawDataLogic2); 
		mRawData[1].setMovementMethod(new ScrollingMovementMethod());
		mRawData[2] = (TextView) v.findViewById(R.id.tvRawDataLogic3); 
		mRawData[2].setMovementMethod(new ScrollingMovementMethod());
		mRawData[3] = (TextView) v.findViewById(R.id.tvRawDataLogic4); 
		mRawData[3].setMovementMethod(new ScrollingMovementMethod());
		
		mRawData[1].setPadding(40, 0, 0, 0);
		mRawData[2].setPadding(40, 0, 0, 0);
		mRawData[3].setPadding(40, 0, 0, 0);
		
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
		if(DEBUG) Log.i("mFragment2","Resume");
	}

	@Override
	public double onDataDecodedListener(LogicData[] mLogicData, int samplesCount) {
		if(DEBUG) Log.i("mFragment2","onDataDecodedListener() - " + mLogicData.length + " channels");
		
		for(int n=0; n < mRawData.length; ++n) mRawData[n].setText("");
		
		for(int n=0; n < mRawData.length; ++n){
			if(mLogicData[n].getProtocol() != Protocol.CLOCK){
				mRawData[n].append("Canal " + n + " - " + mLogicData[n].getProtocol().toString() + "\n");
				for(int i=0; i < mLogicData[n].getStringCount(); ++i){
					mRawData[n].append(mLogicData[n].getString(i) + "\t --> " + 
							String.format("%.2f", (mLogicData[n].getPositionAt(i)[0]*1000000)) + "uS\n");
				}
				mRawData[n].append("\n");
			}
		}

		return 0;
	}
	
}
