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
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

@SuppressLint("ValidFragment")
public class LogicAnalizerListFragment extends SherlockFragment implements OnDataDecodedListener, AdapterView.OnItemClickListener{

	private static final boolean DEBUG = true;
	
	private static SherlockFragmentActivity mActivity;
	private static ActionBar mActionBar;
    private static TextView mTextView;
	private static View v;
    private static int itemSelected = 0;
	
	private static Protocol[] mProtocols;

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
		mActionBar.setDisplayHomeAsUpEnabled(true);					// El icono de la aplicación funciona como boton HOME
		mActionBar.setTitle(getString(R.string.AnalyzerName)) ;		// Nombre
        this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if(DEBUG) Log.i("mFragmentList","onCreateView()");
		v = inflater.inflate(R.layout.logic_rawdata, container, false);

        mTextView = (TextView) v.findViewById(R.id.tvRawDataChannel);
        mTextView.setMovementMethod(new ScrollingMovementMethod());     // Permite scroll del TextView
        mActionBar.setTitle(getString(R.string.AnalyzerChannel) + " " + (itemSelected+1));
		
		/** Hace que todos los toques en la pantalla sean para esta Fragment y no el que esta detrás */
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

            List<TimePosition> stringData = data[itemSelected].getDecodedData();
            if(DEBUG) Log.i("mFragmentList","onDataDecodedListener() Channel " + itemSelected + " -> " + stringData.size());

            if(data[itemSelected].getProtocol() != ProtocolType.CLOCK){
                // Titulo del canal en el ActionBar
                mActionBar.setTitle(getString(R.string.AnalyzerChannel) + " " + (itemSelected+1)
                        + " - " + data[itemSelected].getProtocol().toString() );

                if(stringData.size() == 0)
                    mTextView.setText(getString(R.string.AnalyzerNoData));

                mTextView.setText("");
                for(int i=0; i < stringData.size(); ++i){
                    // http://stackoverflow.com/questions/3282940/set-color-of-textview-span-in-android
                    // http://stackoverflow.com/questions/12793593/how-to-align-string-on-console-output
                    String text = String.format("%-10s --> %.3f uS\n", stringData.get(i).getString(),
                                                                       stringData.get(i).startTime()*1E6);
                    Spannable spannedText = new SpannableString(text);
                    spannedText.setSpan(new ForegroundColorSpan(Color.RED), 0, text.indexOf(' '), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannedText.setSpan(new StyleSpan(Typeface.BOLD), 0, text.indexOf(' '), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    mTextView.append(spannedText);
                }
			}else{
                mActionBar.setTitle(getString(R.string.AnalyzerChannel) + " " + (itemSelected+1)
                        + " - " + data[itemSelected].getProtocol().toString() );
                mTextView.setText(getString(R.string.AnalyzerNoData));
            }
		}
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(DEBUG) Log.i("mFragmentList","Item: " + i + " clicked");
        itemSelected = i;
        if(mProtocols != null) onDataDecodedListener(mProtocols, false);
        else mActionBar.setTitle(getString(R.string.AnalyzerChannel) + " " + (itemSelected+1));
    }
}
