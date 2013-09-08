package com.protocolanalyzer.andres;

import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.IndexXYMap;
import org.achartengine.util.MathHelper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
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
import com.protocolanalyzer.api.LogicBitSet;
import com.protocolanalyzer.api.Protocol;
import com.protocolanalyzer.api.TimePosition;

@SuppressLint("ValidFragment")
public class LogicAnalizerChartFragment extends SherlockFragment implements OnDataDecodedListener, OnDataClearedListener{
	
	/** Debugging */
	private static final boolean DEBUG = true;
	
	private static final String TAG = "logic:mFragmentChart";
	private static final double initialTimeScale = 0.000500d;	// 500 uS
	
	/** Valores del eje Y que son tomados como inicio ('0' lógico) para las Series de los canales de entrada */
	private static final float yChannel[] = {0, 5, 10, 15, 20, 25, 30, 35};
    /** Cuanto se incrementa en el eje Y para hacer un '1' lógico */
	private static final float bitScale = 1;		
    /** Valor del eje X maximo inicial */
    private static final double xMax = 100;
    /** Valor del eje X mínimo inicial */
    private static final double xMin = -100;
    /** Colores de linea para cada canal */
    private static final int lineColor[] = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
    										Color.MAGENTA, Color.CYAN, Color.LTGRAY, Color.WHITE};
    
    /** Escala de tiempo (cuanto equivale un cuadro del gráfico */
    private static final double timeScaleValues[] = { 	0.000000001d,		// 1nS
                                                        0.000000010d,		// 10nS
                                                        0.000000025d,		// 25nS
                                                        0.000000050d,		// 50nS
                                                        0.000000100d,		// 100nS
                                                        0.000000250d,		// 250nS
                                                        0.0000025d,			// 2.5uS
                                                        0.000500d,			// 500uS
                                                        0.001d,				// 1mS
                                                        0.01d,				// 10mS
                                                        0.1d				// 1mS
    };
        
    /** Vibrador del dispositivo */
    private static Vibrator mVibrator;
    
	/** ActionBar */
	private static ActionBar mActionBar;		
	/** Handler para la actualización del gráfico en el UI Thread */
    private static Handler mUpdaterHandler = new Handler();	
    /** Tiempo transcurrido en mS (eje x) */
    private static double time = 0;		
    /** Cuantos segundos representa un cuadro (una unidad) en el gráfico */
    private static double timeScale; 
    
    /** Serie que muestra los '1' y '0' de cada canal */
    private static XYSeries[] mSerie;
    /** Renderer para cada Serie, indica color, tamaño, etc */
    private static XYSeriesRenderer[] mRenderer;
    
    /** Rectángulos delimitadores */
    private static XYSeries[] rectangleSeries;
    /** Renderer de los rectángulos */
    private static XYSeriesRenderer[] rectangleRenderer;
    
    /** Dataset para agrupar las Series */
    private static XYMultipleSeriesDataset mSerieDataset;
    /** Dataset para agrupar los Renderer */
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
	
	/** Protocolo para cada canal */
	private static Protocol[] decodedData;
	
	private static int samplesNumber = 0;
	
	// Constructor
	public LogicAnalizerChartFragment(Protocol[] data) {
		if(DEBUG) Log.i(TAG,"LogicAnalizerChartFragment() Constructor");
		decodedData = data;
	}
	
	// Constructor por defecto
	public LogicAnalizerChartFragment() {
		decodedData = null;
	}

	@Override
	public void onDataDecodedListener(Protocol[] data, boolean isConfig) {
		if(DEBUG) Log.i(TAG,"onDataDecoded() - isConfig: " + isConfig);
		// Si se cambiaron las configuraciones las actualizo
		if(isConfig) setChartPreferences();
		else{
			decodedData = data;
			
			samplesNumber = data[0].getBitsNumber();
			if(samplesNumber > 0) mUpdaterHandler.post(mUpdaterTask);
		}
	}
	
	@Override
	public void onDataCleared() {
		if(DEBUG) Log.i(TAG,"onDataCleared()");
		restart();
	}
	
	/**
	 * Configura el label del eje X del gráfico en base a la escala de tiempo
	 */
	private void updateXLabels(){
		int currentIndex = 0;
		for(int n = 0; n < timeScaleValues.length; ++n){
			if(timeScaleValues[n] == timeScale){
				currentIndex = n;
				break;
			}
		}
		
		// Si es mayor a 1000uS, lo muestro como mS
		if(timeScaleValues[currentIndex] * 1E6 >= 1000){
			mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x" + String.format("%.2f", timeScaleValues[currentIndex]*1E3) + " mS");
		}
		// Si es mayor a 1000nS lo muestro como uS
		else if(timeScaleValues[currentIndex] * 1E9 >= 1000){
			mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x" + String.format("%.2f", timeScaleValues[currentIndex]*1E6) + " μS");
		}
		// Sino lo muestro como nS
		else{
			mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x" + String.format("%.2f", timeScaleValues[currentIndex]*1E9) + " nS");
		}
	}

    /**
     * Determina el mínimo valor de X de todas las series
     */
    private double minSeriesX (){
        double min = mSerie[0].getMinX();
        for(XYSeries series : mSerie){
            min = Math.min(min, series.getMinX());
        }
        return min;
    }

    /**
     * Determina el máximo valor de X de todas las series
     */
    private double maxSeriesX (){
        double max = mSerie[0].getMaxX();
        for(XYSeries series : mSerie){
            max = Math.max(max, series.getMaxX());
        }
        return max;
    }

	/**
	 * Hace un zoom in del gráfico teniendo en cuenta las escalas en timeScaleValues
	 */
	private void zoomIn(){
        // Verifico en que escala de tiempo estoy
		int currentIndex = 0;
		for(int n = 0; n < timeScaleValues.length; ++n){
			if(timeScaleValues[n] == timeScale){
				currentIndex = n;
				break;
			}
		}

        // Si la escala es válida
		if(currentIndex != 0){
			double prevTimeScale = timeScale;
			timeScale = timeScaleValues[currentIndex-1];

            // Paso las coordenadas a sus X originales es decir el tiempo sin la escala aplicada
			double prevX1 = mRenderDataset.getXAxisMin()*prevTimeScale;
			double prevX2 = mRenderDataset.getXAxisMax()*prevTimeScale;

            // Paso el tiempo original a la nueva escala de tiempos
			double minX = toCoordinate(prevX1, timeScale);
			double maxX = toCoordinate(prevX2, timeScale);

            // Reemplazo cada valor del eje X por su nuevo equivalente con la nueva escala
			for(int n = 0; n < mSerie.length; ++n){
				XYSeries series = mSerie[n];

                @SuppressWarnings("unchecked")
                IndexXYMap<Double, Double> map = (IndexXYMap<Double, Double>)series.getXYMap().clone();

                // Reemplazo los valores de las series
				series.clearSeriesValues();
				for(java.util.Map.Entry<Double, Double> entry : map.entrySet()){
					series.add(toCoordinate(entry.getKey()*prevTimeScale, timeScale), entry.getValue());
				}

                // Reemplazo las anotaciones y los rectángulos
				for(int j = 0; j < series.getAnnotationCount(); ++j){
					double x = toCoordinate(series.getAnnotationX(j)*prevTimeScale, timeScale);
					double y = series.getAnnotationY(j);
					
					series.replaceAnnotation(j, series.getAnnotationAt(j), x, y);
					
					Double[] cord = rectangleSeries[n].getRectangle(j);
					x = toCoordinate(cord[0]*prevTimeScale, timeScale);
					double x2 = toCoordinate(cord[2]*prevTimeScale, timeScale);
					
					rectangleSeries[n].replaceRectangle(j, x, cord[1], x2, cord[3]);
				}
			}
            mRenderDataset.setPanLimits(new double[] {minSeriesX() - (20*(maxX-minX))/100,
                                                      maxSeriesX() + (20*(maxX-minX))/100,
                                                        -1d, yChannel[yChannel.length-1]+4});
			mRenderDataset.setXAxisMax(maxX);
			mRenderDataset.setXAxisMin(minX);
			
			updateXLabels();
			if(DEBUG) Log.i(TAG, "Redrawing Zoomed Chart");
			mChartView.zoomIn();
		}
	}
	
	/**
	 * Hace un zoom out del gráfico teniendo en cuenta las escalas en timeScaleValues
	 */
	private void zoomOut(){
		int currentIndex = 0;
		for(int n = 0; n < timeScaleValues.length; ++n){
			if(timeScaleValues[n] == timeScale){
				currentIndex = n;
				break;
			}
		}

		if(currentIndex != timeScaleValues.length-1){
			double prevTimeScale = timeScale;
			timeScale = timeScaleValues[currentIndex+1];

			double prevX1 = mRenderDataset.getXAxisMin()*prevTimeScale;
			double prevX2 = mRenderDataset.getXAxisMax()*prevTimeScale;
			
			double minX = toCoordinate(prevX1, timeScale);
			double maxX = toCoordinate(prevX2, timeScale);
			
			for(int n = 0; n < mSerie.length; ++n){
				XYSeries series = mSerie[n];
				
				@SuppressWarnings("unchecked")
				IndexXYMap<Double, Double> map = (IndexXYMap<Double, Double>)series.getXYMap().clone();
				
				series.clearSeriesValues();
				for(java.util.Map.Entry<Double, Double> entry : map.entrySet()){
					series.add(toCoordinate(entry.getKey()*prevTimeScale, timeScale), entry.getValue());
				}
				for(int j = 0; j < series.getAnnotationCount(); ++j){
					double x = toCoordinate(series.getAnnotationX(j)*prevTimeScale, timeScale);
					double y = series.getAnnotationY(j);
					
					series.replaceAnnotation(j, series.getAnnotationAt(j), x, y);
					
					Double[] cord = rectangleSeries[n].getRectangle(j);
					x = toCoordinate(cord[0]*prevTimeScale, timeScale);
					double x2 = toCoordinate(cord[2]*prevTimeScale, timeScale);
					
					rectangleSeries[n].replaceRectangle(j, x, cord[1], x2, cord[3]);
				}
			}
            mRenderDataset.setPanLimits(new double[] {minSeriesX() - (20*(maxX-minX))/100,
                                                      maxSeriesX() + (20*(maxX-minX))/100,
                                                        -1d, yChannel[yChannel.length-1]+4});

			mRenderDataset.setXAxisMax(maxX);
			mRenderDataset.setXAxisMin(minX);
			
			updateXLabels();
			if(DEBUG) Log.i(TAG, "Redrawing Zoomed Chart");
			mChartView.zoomOut();
		}
	}
	
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Log.i(TAG, "onActivityCreated()");
		
        mActionBar = mActivity.getSupportActionBar();				// Obtengo el ActionBar
        mActionBar.setDisplayHomeAsUpEnabled(true);					// El icono de la aplicación funciona como botón HOME
        mActionBar.setTitle(getString(R.string.AnalyzerName)) ;		// Nombre
        this.setHasOptionsMenu(true);								
        
        setChartPreferences();
     
        // Obtengo el OnActionBarClickListener de la Activity que creo este Fragment
     	try { mActionBarListener = (OnActionBarClickListener) mActivity; }
     	catch (ClassCastException e) { throw new ClassCastException(mActivity.toString() + " must implement OnActionBarClickListener"); }
        
     	// Vibrador
        mVibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        
        /** LongClick del gráfico. El onLongClickListener() no funciona */
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
				// Si me moví al menos 20 unidades en cualquier dirección ya se toma como scroll NO long-press
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
				// return false; da lugar a que se analicen otros eventos de touch (como cuando deslizamos el gráfico). Si fuera
				// true el gráfico no se desplazaría porque este se activa primero y evita al otro
				return false;
			}
        });   
        
        if(decodedData != null){
        	samplesNumber = decodedData[0].getBitsNumber();
        	mUpdaterHandler.post(mUpdaterTask);
        }
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
		Log.i(TAG, "onCreateView()");
		
		// Obtengo la Activity que contiene el Fragment
		mActivity = getSherlockActivity();
		
		mSerieDataset = new XYMultipleSeriesDataset();
		mRenderDataset = new XYMultipleSeriesRenderer();
		
		mSerie = new XYSeries[LogicAnalizerActivity.channelsNumber];
		rectangleSeries = new XYSeries[LogicAnalizerActivity.channelsNumber];
		
		mRenderer = new XYSeriesRenderer[LogicAnalizerActivity.channelsNumber];
		rectangleRenderer = new XYSeriesRenderer[LogicAnalizerActivity.channelsNumber];
		
		for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n) {
	    	// Crea las Serie que es una linea en el gráfico (cada una de las entradas)
	    	mSerie[n] = new XYSeries(getString(R.string.AnalyzerName) + n);
	    	
	    	mRenderer[n] = new XYSeriesRenderer();			// Creo el renderer de la Serie
	    	mRenderDataset.addSeriesRenderer(mRenderer[n]);	// Agrego el renderer al Dataset
	    	mSerieDataset.addSeries(mSerie[n]);				// Agrego la serie al Dataset
	    	
	    	mRenderer[n].setColor(lineColor[n]);			// Color de la Serie
	    	mRenderer[n].setFillPoints(true);
	    	mRenderer[n].setPointStyle(PointStyle.CIRCLE);
	    	mRenderer[n].setLineWidth(2f);
	    	
	    	mRenderer[n].setAnnotationsTextSize(10);
	    	mRenderer[n].setAnnotationsColor(Color.BLACK);
	    	mRenderer[n].setAnnotationsTextAlign(Align.CENTER);
	    	
	    	// Rectángulos
	    	rectangleSeries[n] = new XYSeries("");
	    	rectangleSeries[n].setIsRectangleSeries(true);
	    	rectangleRenderer[n] = new XYSeriesRenderer();
	    	rectangleRenderer[n].setColor(lineColor[n]);
	    	rectangleRenderer[n].setShowLegendItem(false);
            rectangleRenderer[n].setLineWidth(2f);
	    	
	    	mSerieDataset.addSeries(rectangleSeries[n]);
	    	mRenderDataset.addSeriesRenderer(rectangleRenderer[n]);
	    }

        // Configuraciones generales
        mRenderDataset.setYTitle(getString(R.string.AnalyzerYTitle));
        mRenderDataset.setAntialiasing(true);
        mRenderDataset.setYAxisMax(yChannel[yChannel.length/2]+4);
        mRenderDataset.setXAxisMin(xMin);
        mRenderDataset.setXAxisMax(xMax);
        mRenderDataset.setPanEnabled(true);
        mRenderDataset.setShowGrid(true);
        mRenderDataset.setPointSize(4f);
        mRenderDataset.setExternalZoomEnabled(true);
        mRenderDataset.setPanEnabled(true, true);
        mRenderDataset.setZoomEnabled(true, false);
        mRenderDataset.setPanLimits(new double[] {xMin , Double.MAX_VALUE, -1d, yChannel[yChannel.length-1]+4});
        mRenderDataset.setXLabels(20);
        mRenderDataset.setMarginsColor(Color.rgb(230,230,230));
        mRenderDataset.setBackgroundColor(Color.rgb(230,230,230));
        mRenderDataset.setGridColor(Color.BLACK);
        mRenderDataset.setAxesColor(Color.BLACK);
        mRenderDataset.setLabelsColor(Color.BLACK);
        mRenderDataset.setApplyBackgroundColor(true);
        mRenderDataset.setGridWidth(0.5f);
        mRenderDataset.setXLabelsColor(Color.BLACK);
        mRenderDataset.setYLabelsColor(0, Color.BLACK);
        time = 0;
        
        mChartView = ChartFactory.getLineChartView(mActivity, mSerieDataset, mRenderDataset);
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Renderizado por software, el hardware trae problemas con paths muy largos en el Canvas (bug de Android)
            mChartView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
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
		if(DEBUG) Log.i(TAG,"onResume()");
	}
    
	// Activa el ActionMode del ActionBar
	private final class ActionModeEnable implements ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if(DEBUG) Log.i(TAG, "Action Mode onCreate()");
			MenuInflater inflater = mActivity.getSupportMenuInflater();
			inflater.inflate(R.menu.actionmodelogic, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if(DEBUG) Log.i(TAG, "Action Mode onPrepare");
			return false;
		}

		// Al presionar iconos en el ActionMode
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if(DEBUG) Log.i(TAG, "Item clicked: " + item.getItemId() + " - " + item.getTitle());
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
			if(DEBUG) Log.i(TAG, "Destroy");
		}
	}
    
	// Listener de los items en el ActionBar
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		if(DEBUG) Log.i(TAG, "ActionBar -> " + item.getTitle());
 		switch(item.getItemId()){
 		case R.id.zoomInLogic:
 			zoomIn();
 			break;
 		case R.id.zoomOutLogic:
 			zoomOut();
 			break;
 		}
		return true;
 	}
 	
 	/**
 	 * Viene aquí cuando se vuelve de la Activity de las preferences al ser llamada con startActivityForResult() de este
 	 * modo actualizo las preferencias
 	 */
 	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(DEBUG) Log.i(TAG, "Activity Result");
		if(DEBUG) Log.i(TAG, "resultCode: " + resultCode);
		if(DEBUG) Log.i(TAG, "requestCode: " + requestCode);
	}

 	// Reinicia el gráfico y las variables involucradas
	private void restart() {
		for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n) {
			mSerie[n].clear();
		}
		mRenderDataset.setXAxisMax(xMax);
		mRenderDataset.setXAxisMin(xMin);
		time = 0;
		mChartView.repaint();
		Toast.makeText(mActivity, getString(R.string.Reinicio), Toast.LENGTH_SHORT).show();
	}

	/**
	 * Crea una ventana preguntando al usuario si desea guardar la sesión o una imagen del gráfico
 	 * See http://developer.android.com/guide/topics/ui/menus.html
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
	 * Los Handlers ejecutan sus operaciones en el Thread de la UI haciendo posible la modificación de la misma desde Threads no UI.	 * @author Andres Torti
	 * See http://developer.android.com/guide/topics/fundamentals/processes-and-threads.html
	 * See http://developer.android.com/reference/android/os/Handler.html
	 * See http://developer.android.com/resources/articles/timed-ui-updates.html
	 * See http://stackoverflow.com/questions/10405773/how-to-use-preferencefragment/10405850#comment13428324_10405850
	 */
	final private Runnable mUpdaterTask = new Runnable() {
		@Override
		public void run() {
			
			if(DEBUG) Log.i(TAG, "Updater Task - Samples: " + samplesNumber);
			// Borro todos los valores previos
			for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
				mSerie[n].clear();
				rectangleSeries[n].clear();
			}
			
			final double initTime = 0; time = 0;
			
			// Coloco los bits en el canal
			for(int channel = 0; channel < LogicAnalizerActivity.channelsNumber; ++channel){	
				LogicBitSet bitsData = decodedData[channel].getChannelBitsData();
				
				boolean bitState = false;
				
				// En vez de poner todos los puntos en la serie coloco solo cada vez que se cambia de estado
				// de este modo agrego muchos menos elementos y el gráfico se hace usable ya que con 16000 muestras
				// la performance es muy baja
				for(int n = 0; n < samplesNumber; ++n){
					// Punto inicial
					if(n == 0){
						bitState = bitsData.get(0);
						if(bitsData.get(n)){	// 1
							mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]+bitScale);
						}
						else{					// 0
							mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]);
						}
					// Punto si hay un cambio de estado
					}else if(bitsData.get(n) != bitState){
						bitState = bitsData.get(n);
						double tTime = time - 1.0d/decodedData[0].getSampleFrequency(); 
						
						// Estado anterior
						if(bitsData.get(n-1)) mSerie[channel].add(toCoordinate(tTime, timeScale), yChannel[channel]+bitScale);
						else mSerie[channel].add(toCoordinate(tTime, timeScale), yChannel[channel]);
						
						// Estado actual
						if(bitsData.get(n)) mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]+bitScale);
						else mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]);
					// Punto al final
					}else if(n == (samplesNumber-1)){
						if(bitsData.get(n)) mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]+bitScale);
						else mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]);
					}
					// Incremento el tiempo
					time += 1.0d/decodedData[0].getSampleFrequency();
				}
				if(channel < LogicAnalizerActivity.channelsNumber-1) time = initTime;
			}
			// Solo si estoy en la escala inicial reacomodo el gráfico
			if(timeScale == initialTimeScale){
				// Muevo el gráfico al final de las lineas, le sumo el 10% del valor máximo y mínimo para dar un margen
				mRenderDataset.setXAxisMax(toCoordinate(time, timeScale)+(10*toCoordinate(time, timeScale))/100);
				mRenderDataset.setXAxisMin(0-(10*toCoordinate(time, timeScale))/100);
			}
			
			// Agrego un espacio para indicar que el buffer de muestreo llego hasta aquí
			time += (toCoordinate(mSerie[0].getItemCount() * (1d/decodedData[0].getSampleFrequency()), timeScale)*30)/100;
			for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n){
				if(mSerie[n].getItemCount() > 0){
					mSerie[n].add(mSerie[n].getX(mSerie[n].getItemCount()-1), MathHelper.NULL_VALUE);
				}
			}
			
			// Anotaciones
			for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
				List<TimePosition> stringData = decodedData[n].getDecodedData(); 
				if(DEBUG) Log.i(TAG, "Channel " + n  + " annotations: " + stringData.size());
				
				for(TimePosition timePosition : stringData){
					
					// Agrego el texto en el centro del area de tiempo que contiene el string
					mSerie[n].addAnnotation(timePosition.getString(),
							toCoordinate(timePosition.startTime() + (timePosition.endTime() - timePosition.startTime())/2, timeScale), yChannel[n]+2f);
				
					// Rectángulos delimitadores
					rectangleSeries[n].addRectangle(toCoordinate(timePosition.startTime(), timeScale),
													yChannel[n]+bitScale+3f,
													toCoordinate(timePosition.endTime(), timeScale),
													yChannel[n]+bitScale+0.5f);
				}
			}
			
			mChartView.repaint();	// Actualizo el gráfico
	
			// Cada vez que recibo un buffer del analizador lógico, lo muestro y pauso
			mActionBarListener.onActionBarClickListener(R.id.PlayPauseLogic);
		}
	};
	
	/**
	 * Convierte el tiempo en segundos a la escala del gráfico según la escala de tiempos
	 * @param time tiempo en segundos
	 * @param timeScale cuantos segundos equivalen a una unidad en el gráfico
	 * @return coordenada equivalente
	 */
	private static double toCoordinate (double time, double timeScale){
		return (time/timeScale);
	}
	
	// Define los parámetros de acuerdo a las preferencias
 	private void setChartPreferences() {
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n){
        	// Configuro el protocolo para cada canal
        	switch(Integer.valueOf(getPrefs.getString("protocol" + (n+1), ""+LogicAnalizerActivity.UART))){
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
        
        // Escala inicial
    	timeScale = initialTimeScale;
    	updateXLabels();
        
    	if(DEBUG) Log.i(TAG, "Time Scale: " + timeScale);
    	
        // Actualizo los datos del gráfico
        mChartView.repaint();
 	}

}