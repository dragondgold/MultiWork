package com.protocolanalyzer.andres;

import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.ActionMode;

import com.multiwork.andres.R;
import com.protocolanalyzer.api.andres.LogicBitSet;
import com.protocolanalyzer.api.andres.Protocol;
import com.protocolanalyzer.api.andres.TimePosition;

@SuppressLint("ValidFragment")
public class LogicAnalizerChartFragment extends SherlockFragment implements OnDataDecodedListener{
	
	/** Debugging */
	private static final boolean DEBUG = true;
	
	/** Valores del eje Y que son tomados como inicio ('0' lógico) para las Series de los canales de entrada */
	private static final float yChannel[] = {0, 4, 8, 12};
    /** Cuanto se incrementa en el eje Y para hacer un '1' logico */
	private static final float bitScale = 1;		
    /** Valor del eje X maximo inicial */
    private static final double xMax = 10;				
    /** Colores de linea para cada canal */
    private static final int lineColor[] = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
    
    /** Vibrador del dispositivo */
    private static Vibrator mVibrator;
    
	/** ActionBar */
	private static ActionBar mActionBar;		
	/** Handler para la actualizacion del grafico en el UI Thread */
    private static Handler mUpdaterHandler = new Handler();	
    /** Tiempo que va transcurriendo (eje x del grafico) */
    private static double time = 0;		
    /** Cuantos segundos representa un cuadrito (una unidad) en el grafico */
    private static double timeScale; 
    
    /** Numero maximo de muestras en las series (osea en el grafico) */
    private static int maxSamples = 5;
    private static int currentSamples = 0;
    
    /** Serie que muestra los '1' y '0' de cada canal */
    private static XYSeries[] mSerie;
    /** Renderer para cada Serie, indica color, tamaño, etc */
    private static XYSeriesRenderer[] mRenderer;
    /** Dataset para agrupar las Series */
    private static XYMultipleSeriesDataset mSerieDataset;
    /** Dataser para agrupar los Renderer */
    private static XYMultipleSeriesRenderer mRenderDataset;
	
	private static GraphicalView mChartView;
	private static SherlockFragmentActivity mActivity;
	private static OnActionBarClickListener mActionBarListener;
    
	/** Coordenadas de inicio cuando se toco por primera vez el touchscreen */
	private static float x = 0, y = 0;
	/** Indica si se esta deslizando el dedo en vez de mantenerlo apretado */
	private static boolean isMoving = false;
	/** Indica si se esta sosteniendo el dedo sobre la pantalla (long-press) */
	private static boolean fingerStillDown = false;
	
	/** Dato decodificado desde LogicHelper para ser mostrado en el grafico, contiene las posiciones para mostar
     * el tipo de protocolo, etc
     * @see LogicData.java */
	private static Protocol[] decodedData;
	
	private static int samplesNumber = 0;
	
	// Constructor
	public LogicAnalizerChartFragment(Protocol[] data, int samplesCount) {
		if(DEBUG) Log.i("mFragmentChart","LogicAnalizerChartFragment() Constructor");
		decodedData = data;
		samplesNumber = samplesCount;
	}
	
	public LogicAnalizerChartFragment() {
		decodedData = null;
	}
	
	@Override
	public double onDataDecodedListener(Protocol[] data, int samplesCount, boolean isConfig) {
		if(DEBUG) Log.i("mFragmentChart","onDataDecoded() - isConfig: " + isConfig);
		if(DEBUG) Log.i("mFragmentChart","Data: " + data.toString());
		// Si se cambiaron las configuraciones las actualizo
		if(isConfig) setChartPreferences();
		else{
			decodedData = data;
			samplesNumber = samplesCount;
			
			if(samplesNumber > 0) mUpdaterHandler.post(mUpdaterTask);;
		}
		return 0;
	}
	
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Log.i("mFragmentChart", "onCreate()");
		
        mActionBar = mActivity.getSupportActionBar();				// Obtengo el ActionBar
        mActionBar.setDisplayHomeAsUpEnabled(true);					// El icono de la aplicacion funciona como boton HOME
        mActionBar.setTitle(getString(R.string.AnalyzerName)) ;		// Nombre
        this.setHasOptionsMenu(true);								
        
        setChartPreferences();
     
        // Obtengo el OnActionBarClickListener de la Activity que creo este Fragment
     	try { mActionBarListener = (OnActionBarClickListener) mActivity; }
     	catch (ClassCastException e) { throw new ClassCastException(mActivity.toString() + " must implement OnActionBarClickListener"); }
        
     	// Vibrador
        mVibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        
        final Runnable longClickRun = new Runnable() {
			@Override
			public void run() {
				if(fingerStillDown && !isMoving) {
					if(DEBUG) Log.i("Runnable longClickRun()", "LONG CLICK");
					mVibrator.vibrate(80);	// Vibro e inicio el ActionMode
					mActivity.startActionMode(new ActionModeEnable());
				}
			}       	
        };
        
        mChartView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {		
				// Si me movi al menos 20 unidades en cualquier direccion ya se toma como scroll NO long-press
				if(Math.abs(event.getX() - x) > 20 || Math.abs(event.getY() - y) > 20) {
					isMoving = true;
				}
				// Obtengo las coordenadas iniciales para tomar el movimiento
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					x = event.getX(); y = event.getY();
					fingerStillDown = true; isMoving = false;
					mChartView.postDelayed(longClickRun, 1000);		// En 1000mS se iniciara el Long-Press
				}
				// Si levanto el dedo ya no cuenta para el long-press
				else if(event.getAction() == MotionEvent.ACTION_UP){
					mChartView.removeCallbacks(longClickRun);		// Elimino el postDelayed()
					fingerStillDown = false;
					isMoving = false; x = y = 0;
				}
				
				// Sleep por 50mS para que no este continuamente testeando y ahorre recursos (no hace falta gran velocidad)
				try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
				// return false; da lugar a que se analizen otros eventos de touch (como cuando deslizamos el grafico). Si fuera
				// true el grafico no se desplazaría porque este se activa primero y evita al otro
				return false;
			}
        });   
        if(decodedData != null) mUpdaterHandler.post(mUpdaterTask);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.actionbar_logicchart, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i("mFragmentChart", "onCreateView()");
		
		// Obtengo la Activity que contiene el Fragment
		mActivity = getSherlockActivity();
		
		mSerieDataset = new XYMultipleSeriesDataset();
		mRenderDataset = new XYMultipleSeriesRenderer();
		mSerie = new XYSeries[LogicAnalizerActivity.channelsNumber];
		mRenderer = new XYSeriesRenderer[LogicAnalizerActivity.channelsNumber];
		
		for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n) {
	    	// Crea las Serie que es una linea en el grafico (cada una de las entradas)
	    	mSerie[n] = new XYSeries(getString(R.string.AnalyzerName) + n);
	    	
	    	mRenderer[n] = new XYSeriesRenderer();			// Creo el renderer de la Serie
	    	mRenderDataset.addSeriesRenderer(mRenderer[n]);	// Agrego el renderer al Dataset
	    	mSerieDataset.addSeries(mSerie[n]);				// Agrego la seria al Dataset
	    	
	    	mRenderer[n].setColor(lineColor[n]);			// Color de la Serie
	    	mRenderer[n].setFillPoints(true);
	    	mRenderer[n].setPointStyle(PointStyle.CIRCLE);
	    	mRenderer[n].setLineWidth(2f);
	    	
	    	mRenderer[n].getTextPaint().setTextSize(30);	// Tamaño del texto
	    	mRenderer[n].getTextPaint().setColor(Color.WHITE);
	    	mRenderer[n].getTextPaint().setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
	    	mRenderer[n].getTextPaint().setTextAlign(Align.CENTER);
	    	
	    	mRenderer[n].getRectPaint().setColor(Color.WHITE);
	    	mRenderer[n].getRectPaint().setStrokeWidth(2f);
	    	mRenderer[n].getRectPaint().setStyle(Style.STROKE);
	    }

        // Configuraciones generales
        mRenderDataset.setYTitle(getString(R.string.AnalyzerYTitle));
        mRenderDataset.setAntialiasing(true);
        mRenderDataset.setYAxisMax(yChannel[yChannel.length-1]+4);
        mRenderDataset.setXAxisMin(0);
        mRenderDataset.setXAxisMax(xMax);
        mRenderDataset.setPanEnabled(true);
        mRenderDataset.setShowGrid(true);
        mRenderDataset.setPointSize(4f);
        mRenderDataset.setExternalZoomEnabled(true);
        mRenderDataset.setPanEnabled(true, false);
        mRenderDataset.setZoomEnabled(true, false);
        mRenderDataset.setPanLimits(new double[] {0d , Double.MAX_VALUE, -1d, yChannel[yChannel.length-1]+4});
        
        mChartView = ChartFactory.getLineChartView(mActivity, mSerieDataset, mRenderDataset);
        setChartPreferences();
		
		// Renderizo el layout
		View view = inflater.inflate(R.layout.logicanalizer, container, false);
		FrameLayout f = (FrameLayout)view.findViewById(R.id.mChart);
		f.addView(mChartView);
		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if(DEBUG) Log.i("mFragmentChart","onResume()");
		
		/*
		// Elimino primero el View porque si ya esta agregado genera una excepcion
		((FrameLayout) mActivity.findViewById(R.id.mChart)).removeViewInLayout(mChartView);
		// Agrego un View al layout que se renderizo en onCreateView. No puedo hacerlo antes porque dentro de 
		// onCreateView() el layout no se renderizo y por lo tanto es null.
		((FrameLayout) mActivity.findViewById(R.id.mChart)).addView(mChartView);*/
		
	}
    
	// Activa el ActionMode del ActionBar
	private final class ActionModeEnable implements ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if(DEBUG) Log.i("ActionMode", "Create");
			MenuInflater inflater = mActivity.getSupportMenuInflater();
			inflater.inflate(R.menu.actionmodelogic, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if(DEBUG) Log.i("ActionMode", "Prepare");
			return false;
		}

		// Al presionar iconos en el ActionMode
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if(DEBUG) Log.i("ActionMode", "Item clicked: " + item.getItemId() + " - " + item.getTitle());
			switch(item.getItemId()) {
			case R.id.restartLogic:
				mActionBarListener.onActionBarClickListener(R.id.restartLogic);
				restart();
 				break;
	 		case R.id.saveLogic:
	 			createDialog();
	 			break;
			}
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if(DEBUG) Log.i("ActionMode", "Destroy");
		}
	}
    
	// Listener de los items en el ActionBar
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		if(DEBUG) Log.i("mFragmentChart", "ActionBar -> " + item.getTitle());
 		switch(item.getItemId()){
 		case R.id.zoomInLogic:
 			mChartView.zoomIn();
 			break;
 		case R.id.zoomOutLogic:
 			mChartView.zoomOut();
 			break;
 		}
		return true;
 	}
 	
 	/**
 	 * Viene aqui cuando se vuelve de la Activity de las preferences al ser llamada con startActivityForResult() de este
 	 * modo actualizo las preferencias
 	 */
 	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(DEBUG) Log.i("mFragmentChart", "Activity Result");
		if(DEBUG) Log.i("mFragmentChart", "resultCode: " + resultCode);
		if(DEBUG) Log.i("mFragmentChart", "requestCode: " + requestCode);
	}

 	// Reinicia el gŕafico y las variables involucradas
	private void restart() {
		for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n) {
			mSerie[n].clear();
		}
		mRenderDataset.setXAxisMax(xMax);
		mRenderDataset.setXAxisMin(0);
		time = 0;
		mChartView.repaint();
		Toast.makeText(mActivity, getString(R.string.FrecReinicio), Toast.LENGTH_SHORT).show();
	}

	/**
	 * Crea una ventana preguntando al usuario si desea guardar la sesion o una imagen del grafico
 	 * @author Andres Torti
 	 * @see http://developer.android.com/guide/topics/ui/menus.html
 	 */
	private void createDialog() {
		final CharSequence[] items = {getString(R.string.AnalyzerImagen), getString(R.string.AnalyzerSesion)};
		AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
		alert.setTitle(getString(R.string.AnalyzerDialogSaveTitle));

		alert.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        if(item == 0) {
		        	// TODO: guardar imagen
		        }
		        else {
		        	// TODO: guardar sesion
		        }
		    }
		});
		alert.show();
	}

	/**
	 * Los Handlers ejecutan sus operaciones en el Thread de la UI haciendo posible la modificacion de la misma desde Threads no UI.
	 * @author Andres Torti
	 * @see http://developer.android.com/guide/topics/fundamentals/processes-and-threads.html
	 * @see http://developer.android.com/reference/android/os/Handler.html
	 * @see http://developer.android.com/resources/articles/timed-ui-updates.html
	 * @see http://stackoverflow.com/questions/10405773/how-to-use-preferencefragment/10405850#comment13428324_10405850
	 */
	final private Runnable mUpdaterTask = new Runnable() {
		@Override
		public void run() {
			
			if(DEBUG) Log.i("mFragmentChart", "Updater Task");
			final double initTime = time;
			
			for(int channel = 0; channel < LogicAnalizerActivity.channelsNumber; ++channel){	
				LogicBitSet bitsData = decodedData[channel].getChannelBitsData();
				
				for(int n = 0; n < samplesNumber; ++n){
					if(bitsData.get(n)){
						// Nivel tomado como 0 + un alto de bit
						mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]+bitScale);
					}
					else{				
						mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]);
					}
					// Incremento el tiempo
					time += 1.0d/decodedData[0].getSampleFrequency();
				}
				if(channel < LogicAnalizerActivity.channelsNumber-1) time = initTime;
			}
			// Muevo el grafico al final de las lineas
			mRenderDataset.setXAxisMax(toCoordinate(time, timeScale)+1d);
			mRenderDataset.setXAxisMin(0);
			
			// Agrego un espacio para indicar que el buffer de muestreo llego hasta aqui
			time += (10*timeScale);
			for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n){
				if(mSerie[n].getItemCount() > 0){
					mSerie[n].add(mSerie[n].getX(mSerie[n].getItemCount()-1)+0.0000001d, MathHelper.NULL_VALUE);
				}
			}
			
			// Anotaciones
			for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
				List<TimePosition> stringData = decodedData[n].getDecodedData();
				
				for(int i = 0; i < stringData.size(); ++i){
					
					TimePosition timePosition = stringData.get(i);
					
					// Agrego el texto en el centro del area de tiempo que contiene el string
					mSerie[n].addAnnotation(timePosition.getString(),
							toCoordinate(timePosition.startTime()+((timePosition.endTime()-timePosition.startTime())/2.0d),
									timeScale), yChannel[n]+2f);
				
					// Agrego el recuadro
					mSerie[n].addRectangle(toCoordinate(timePosition.startTime(), timeScale)+0.0000001,
							yChannel[n]+3.5f,
							toCoordinate(timePosition.endTime(), timeScale),
							yChannel[n]+bitScale+0.5f);
				}
			}
			
			mChartView.repaint();	// Redibujo el grafico
			++currentSamples;
			
			// Si me paso de las muestras borro los canales
			if(currentSamples > maxSamples){
				for(int n = 0; n < LogicAnalizerActivity.channelsNumber; n++) mSerie[n].clear();
				currentSamples = 0;
			}
	
			// Cada vez que recibo un buffer del analizador logico, lo muestro todo y pauso
			mActionBarListener.onActionBarClickListener(R.id.PlayPauseLogic);
		}
	};
	
	/**
	 * Convierte el tiempo en mili-segundos a la escala del grafico segun la escala de tiempos
	 * @param time tiempo en mili-segundos
	 * @param timeScale cuantos mili-segundos equivalen a una unidad en el grafico
	 * @return coordenada equivalente
	 */
	private static double toCoordinate (double time, double timeScale){
		return (time/timeScale);
	}
	
	// Define los parametros de acuerdo a las preferencias
 	private void setChartPreferences() {
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n){
        	// Seteo el protocolo para cada canal
        	switch(Integer.valueOf(getPrefs.getString("protocol" + (n+1), ""+LogicAnalizerActivity.NA))){
	        	case LogicAnalizerActivity.I2C:		// I2C
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + " [I2C]");
	        		break;
	        	case LogicAnalizerActivity.UART:	// UART
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + " [UART]");
	        		break;
	        	case LogicAnalizerActivity.Clock:	// CLOCK
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + "[CLK]");
	        		break;
	        	case LogicAnalizerActivity.NA:		// NONE
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + "[---]");
	        		break;
        	}
        }
    	// Máxima cantidad de muestras para almacenar
        maxSamples = Integer.decode(getPrefs.getString("maxSamples","5"));
    	
        // Escala del eje X de acuerdo al sample rate
        if(decodedData[0].getSampleFrequency() == 40E6) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x25nS");
        	timeScale = 0.000000025d;		// 25nS
        }else if(decodedData[0].getSampleFrequency() == 20E6) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x50 nS");
        	timeScale = 0.000000050d;		// 50nS
        }else if(decodedData[0].getSampleFrequency() == 10E6) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x100 nS");
        	timeScale = 0.000000100d;		// 100nS
        }else if(decodedData[0].getSampleFrequency() == 4E6) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x250 nS");
        	timeScale = 0.000000250d;		// 250nS
        }else if(decodedData[0].getSampleFrequency() == 400000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x2.5 uS");
        	timeScale = 0.0000025d;			// 2.5uS
        }else if(decodedData[0].getSampleFrequency() == 2000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x500 uS");
        	timeScale = 0.000500d;			// 500uS
        }else if(decodedData[0].getSampleFrequency() == 10) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x100 mS");
        	timeScale = 0.1d;				// 100mS
        }
        // Actualizo los datos del grafico
        mChartView.repaint();
 	}

}