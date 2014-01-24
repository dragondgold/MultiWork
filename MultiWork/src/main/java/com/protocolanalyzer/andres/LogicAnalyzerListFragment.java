package com.protocolanalyzer.andres;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.multiwork.andres.R;
import com.protocolanalyzer.api.I2CProtocol;
import com.protocolanalyzer.api.Protocol;
import com.protocolanalyzer.api.TimePosition;
import com.protocolanalyzer.api.Protocol.ProtocolType;
import com.protocolanalyzer.api.UARTProtocol;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class LogicAnalyzerListFragment extends SherlockFragment implements OnDataDecodedListener, AdapterView.OnItemClickListener{

	private static final boolean DEBUG = true;
	
	private static SherlockFragmentActivity mActivity;
	private static ActionBar mActionBar;
    private static TextView propertiesTextView;
	private static View v;
    private static int itemSelected = 0;
	
	private static Protocol[] mProtocols;
    private static ArrayList<String> dataList = new ArrayList<String>();
    private static ExpandableListAdapter mAdapter;

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
		
		mActionBar = mActivity.getSupportActionBar();	// Obtengo el ActionBar
		mActionBar.setDisplayHomeAsUpEnabled(true);		// El icono de la aplicación funciona como boton HOME
		mActionBar.setTitle(getString(R.string.AnalyzerName)) ;		// Nombre
        this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if(DEBUG) Log.i("mFragmentList","onCreateView()");
		v = inflater.inflate(R.layout.logic_rawdata, container, false);

        propertiesTextView = (TextView) v.findViewById(R.id.tvChannelProperties);
        propertiesTextView.setTypeface(Typeface.MONOSPACE);

        ExpandableListView mExpandable = (ExpandableListView) v.findViewById(R.id.rawDataList);
        mAdapter = new AnalyzerExpandableListView(getActivity(), dataList);
        mExpandable.setAdapter(mAdapter);

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

            // Si el protocolo no es Clock, porque si lo es, no debo mostrar datos
            if(data[itemSelected].getProtocol() != ProtocolType.CLOCK){
                // Titulo del canal en el ActionBar
                mActionBar.setTitle(getString(R.string.AnalyzerChannel) + " " + (itemSelected+1)
                        + " - " + data[itemSelected].getProtocol().toString() );

                propertiesTextView.setText("");

                for(int i=0; i < stringData.size(); ++i){
                    // http://stackoverflow.com/questions/3282940/set-color-of-textview-span-in-android
                    // http://stackoverflow.com/questions/12793593/how-to-align-string-on-console-output
                    String text = String.format("%-7s → %.3f μS", stringData.get(i).getString(),
                                                                  stringData.get(i).startTime()*1E6);

                    /*
                    Spannable spannedText = new SpannableString(text);
                    spannedText.setSpan(new ForegroundColorSpan(Color.RED), 0, text.indexOf(' '), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannedText.setSpan(new StyleSpan(Typeface.BOLD), 0, text.indexOf(' '), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    */

                    // TODO: ver de agregar Spanned Text
                    dataList.add(text);
                }
                ((BaseExpandableListAdapter)mAdapter).notifyDataSetChanged();

                /*
                // Propiedades de cada protocolo
                ProtocolType mProtocol = data[itemSelected].getProtocol();
                String text = "";
                if(mProtocol == ProtocolType.I2C){
                    String frecText;
                    float frecTolerance = frequencyScaling( (int)((I2CProtocol)data[itemSelected]).getClockSource().getFrequencyTolerance() )[0];
                    float[] frec = frequencyScaling(((I2CProtocol)data[itemSelected]).getClockSource().getCalculatedFrequency());

                    if(frec[1] == 0) frecText = " MHz";
                    else if(frec[1] == 1) frecText = " KHz";
                    else frecText = " Hz";

                    text = String.format("%-40s", getString(R.string.AnalyzerRawFrec) + ": " +
                                         String.format("%.1f", frec[0]) + " ± " +
                                         String.format("%.1f", frecTolerance) + frecText);
                }
                else if(mProtocol == ProtocolType.UART){
                    text = String.format("%-40s\n%-40s\n%-40s\n%-40s",
                            getString(R.string.AnalyzerBaudTitle) + ": " +
                                    ((UARTProtocol)data[itemSelected]).getBaudRate(),
                            getString(R.string.AnalyzerRawDataBits) + ": " +
                                    (((UARTProtocol)data[itemSelected]).is9BitsMode() ? "9" : "8"),
                            getString(R.string.AnalyzerRawStopBits) + ": " +
                                    (((UARTProtocol)data[itemSelected]).isTwoStopBits() ? "2" : "1"),
                            getString(R.string.AnalyzerRawParity) + ": " +
                                    ((UARTProtocol)data[itemSelected]).getParity().toString()
                            );
                }
                propertiesTextView.setText(text);*/
			}else{
                mActionBar.setTitle(getString(R.string.AnalyzerChannel) + " " + (itemSelected+1)
                        + " - " + data[itemSelected].getProtocol().toString() );
                propertiesTextView.setText("");
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

    /**
     * Escala automáticamente la frecuencia pasada a MHz, KHz o Hz
     * @param frec frecuencia en Hz a escalar
     * @return frecuencia escalada a MHz, KHz, o Hz en el index 0. A qué frecuencia se escalo en el
     * index 1 siendo 0 (MHZ), 1 (KHz) o 2 (Hz)
     */
    private static float[] frequencyScaling (int frec){
        // Si es mayor o igual a 1MHz escalo a MHz
        if(frec >= 1E6){
            return new float[] { (float)(frec/1E6), 0 };
        // Si es mayor o igual a 1KHz pero menor a 1 MHz escalo a KHz
        }else if(frec >= 1E3){
            return new float[] { (float)(frec/1E3), 1 };
        // Sino la frecuencia en Hz
        }else{
            return new float[] { frec, 2 };
        }

    }
}
