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
	
	private static final String TAG = "logic:mFragmentChart";
	
	/** Valores del eje Y que son tomados como inicio ('0' lógico) para las Series de los canales de entrada */
	private static final float yChannel[] = {0, 5, 10, 15, 20, 25, 30, 35};
    /** Cuanto se incrementa en el eje Y para hacer un '1' logico */
	private static final float bitScale = 1;		
    /** Valor del eje X maximo inicial */
    private static final double xMax = 100;
    /** Valor del eje X minimo inicial */
    private static final double xMin = -100;
    /** Colores de linea para cada canal */
    private static final int lineColor[] = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
    											Color.MAGENTA, Color.CYAN, Color.LTGRAY, Color.WHITE};
    
    /** Escala de tiempo (cuanto equivale un cuadro del grafico */
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
	/** Handler para la actualizacion del grafico en el UI Thread */
    private static Handler mUpdaterHandler = new Handler();	
    /** Tiempo transcurrido en mS (eje x) */
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
    
    /** Rectangulos delimitadores */
    private static XYSeries[] rectangleSeries;
    /** Renderer de los rectangulos */
    private static XYSeriesRenderer[] rectangleRenderer;
    
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
		if(DEBUG) Log.i(TAG,"LogicAnalizerChartFragment() Constructor");
		decodedData = data;
		samplesNumber = samplesCount;
	}
	
	public LogicAnalizerChartFragment() {
		decodedData = null;
	}
	
	@Override
	public double onDataDecodedListener(Protocol[] data, int samplesCount, boolean isConfig) {
		if(DEBUG) Log.i(TAG,"onDataDecoded() - isConfig: " + isConfig);
		// Si se cambiaron las configuraciones las actualizo
		if(isConfig) setChartPreferences();
		else{
			decodedData = data;
			samplesNumber = samplesCount;
			
			if(samplesNumber > 0) mUpdaterHandler.post(mUpdaterTask);;
		}
		return 0;
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
			mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x" + String.format("%.2f", timeScaleValues[currentIndex]*1E6) + " uS");
		}
		// Sino lo muestro como nS
		else{
			mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x" + String.format("%.2f", timeScaleValues[currentIndex]*1E9) + " nS");
		}
	}
	
	/**
	 * Hace un zoom in del gráfico teniendo en cuenta las escalas en timeScaleValues
	 */
	private void zoomIn(){
		int currentIndex = 0;
		for(int n = 0; n < timeScaleValues.length; ++n){
			if(timeScaleValues[n] == timeScale){
				currentIndex = n;
				break;
			}
		}
		
		if(currentIndex == 0) return;
		else{
			double prevTimeScale = timeScale;
			timeScale = timeScaleValues[currentIndex-1];

			double prevX1 = mRenderDataset.getXAxisMin()*prevTimeScale;
			double prevX2 = mRenderDataset.getXAxisMax()*prevTimeScale;
			
			double minX = toCoordinate(prevX1, timeScale);
			double maxX = toCoordinate(prevX2, timeScale);
			double width = toCoordinate(mSerie[0].getItemCount() * (1d/decodedData[0].getSampleFrequency()), timeScale);
			
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
			mRenderDataset.setXAxisMax(maxX - (width*40)/100);
			mRenderDataset.setXAxisMin(minX + (width*40)/100);
			
			updateXLabels();
			if(DEBUG) Log.i(TAG, "Redrawing Zoomed Chart");
			mChartView.repaint();
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
		
		if(currentIndex == timeScaleValues.length-1) return;
		else{
			double prevTimeScale = timeScale;
			timeScale = timeScaleValues[currentIndex+1];

			double prevX1 = mRenderDataset.getXAxisMin()*prevTimeScale;
			double prevX2 = mRenderDataset.getXAxisMax()*prevTimeScale;
			
			double minX = toCoordinate(prevX1, timeScale);
			double maxX = toCoordinate(prevX2, timeScale);
			double width = toCoordinate(mSerie[0].getItemCount() * (1d/decodedData[0].getSampleFrequency()), timeScale);
			
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
			mRenderDataset.setXAxisMax(maxX + (width*40)/100);
			mRenderDataset.setXAxisMin(minX - (width*40)/100);
			
			updateXLabels();
			mChartView.repaint();
		}
	}
	
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Log.i(TAG, "onCreate()");
		
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
	    	// Crea las Serie que es una linea en el grafico (cada una de las entradas)
	    	mSerie[n] = new XYSeries(getString(R.string.AnalyzerName) + n);
	    	
	    	mRenderer[n] = new XYSeriesRenderer();			// Creo el renderer de la Serie
	    	mRenderDataset.addSeriesRenderer(mRenderer[n]);	// Agrego el renderer al Dataset
	    	mSerieDataset.addSeries(mSerie[n]);				// Agrego la serie al Dataset
	    	
	    	mRenderer[n].setColor(lineColor[n]);			// Color de la Serie
	    	mRenderer[n].setFillPoints(true);
	    	mRenderer[n].setPointStyle(PointStyle.CIRCLE);
	    	mRenderer[n].setLineWidth(2f);
	    	
	    	mRenderer[n].setAnnotationsTextSize(10);
	    	mRenderer[n].setAnnotationsColor(Color.WHITE);
	    	mRenderer[n].setAnnotationsTextAlign(Align.CENTER);
	    	
	    	// Rectangulos
	    	rectangleSeries[n] = new XYSeries("");
	    	rectangleSeries[n].setIsRectangleSeries(true);
	    	rectangleRenderer[n] = new XYSeriesRenderer();
	    	rectangleRenderer[n].setColor(lineColor[n]);
	    	rectangleRenderer[n].setShowLegendItem(false);
	    	
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
        
        mChartView = ChartFactory.getLineChartView(mActivity, mSerieDataset, mRenderDataset);
        // Renderizado por software, el hardware trae problemas con paths muy largos en el Canvas (bug de Android)
        mChartView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
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
 	 * Viene aqui cuando se vuelve de la Activity de las preferences al ser llamada con startActivityForResult() de este
 	 * modo actualizo las preferencias
 	 */
 	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(DEBUG) Log.i(TAG, "Activity Result");
		if(DEBUG) Log.i(TAG, "resultCode: " + resultCode);
		if(DEBUG) Log.i(TAG, "requestCode: " + requestCode);
	}

 	// Reinicia el gŕafico y las variables involucradas
	private void restart() {
		for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n) {
			mSerie[n].clear();
		}
		mRenderDataset.setXAxisMax(xMax);
		mRenderDataset.setXAxisMin(xMin);
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
			
			if(DEBUG) Log.i(TAG, "Updater Task");
			final double initTime = time;
			
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
			// Muevo el grafico al final de las lineas, le sumo el 10% del valor máximo y mínimo para dar un margen
			mRenderDataset.setXAxisMax(toCoordinate(time, timeScale)+(10*toCoordinate(time, timeScale))/100);
			mRenderDataset.setXAxisMin(0-(10*toCoordinate(time, timeScale))/100);
			
			// Agrego un espacio para indicar que el buffer de muestreo llego hasta aqui
			time += (toCoordinate(mSerie[0].getItemCount() * (1d/decodedData[0].getSampleFrequency()), timeScale)*30)/100;
			for(int n=0; n < LogicAnalizerActivity.channelsNumber; ++n){
				if(mSerie[n].getItemCount() > 0){
					mSerie[n].add(mSerie[n].getX(mSerie[n].getItemCount()-1), MathHelper.NULL_VALUE);
				}
			}
			
			// Anotaciones
			for(int n = 0; n < LogicAnalizerActivity.channelsNumber; ++n){
				List<TimePosition> stringData = decodedData[n].getDecodedData(); 
				
				for(TimePosition timePosition : stringData){
					
					// Agrego el texto en el centro del area de tiempo que contiene el string
					mSerie[n].addAnnotation(timePosition.getString(),
							toCoordinate(timePosition.startTime() + (timePosition.endTime() - timePosition.startTime())/2, timeScale), yChannel[n]+2f);
				
					// Rectangulos delimitadores
					rectangleSeries[n].addRectangle(toCoordinate(timePosition.startTime(), timeScale),
													yChannel[n]+bitScale+3.5f,
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
				time = 0;
			}
	
			// Cada vez que recibo un buffer del analizador logico, lo muestro todo y pauso
			mActionBarListener.onActionBarClickListener(R.id.PlayPauseLogic);
		}
	};
	
	/**
	 * Convierte el tiempo en segundos a la escala del grafico segun la escala de tiempos
	 * @param time tiempo en segundos
	 * @param timeScale cuantos segundos equivalen a una unidad en el grafico
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
    	// Máxima cantidad de muestras para almacenar
        maxSamples = Integer.decode(getPrefs.getString("maxSamples","5"));
        
    	timeScale = 0.000500d;	// 500 uS
    	updateXLabels();
        
    	if(DEBUG) Log.i(TAG, "Time Scale: " + timeScale);
    	
        // Actualizo los datos del grafico
        mChartView.repaint();
 	}

}