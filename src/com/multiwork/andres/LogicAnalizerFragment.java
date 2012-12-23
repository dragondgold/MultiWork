package com.multiwork.andres;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.ActionMode;

import com.protocolanalyzer.andres.LogicData;

public class LogicAnalizerFragment extends SherlockFragment implements OnDataDecodedListener{
	
	/** Debugging */
	private static final boolean DEBUG = true;
	
	/** Valores del eje Y que son tomados como inicio ('0' lógico) para las Series de los canales de entrada */
	private static final float yChannel[] = {12f, 8f, 4f, 0};
    /** Cuanto se incrementa en el eje Y para hacer un '1' logico */
	private static final float bitScale = 1.00f;		
    /** Valor del eje X maximo inicial */
    private static final double xMax = 10;				
    /** Numero de canales de entrada */
    public static final int channelsNumber = 4;
    /** Colores de linea para cada canal */
    private static final int lineColor[] = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
    
    /** Vibrador del dispositivo */
    private static Vibrator vibration;
    /** Directorio para guardar las sesiones */
    private static String sesionDirectory;
    /** Directorio para guardar las imagenes */
    private static String imagesDirectory;
    
    private static final int CONFIRM_DIALOG = 0, PREFERENCES_CODE = 1, RESULT_OK = -1;
    
	/** ActionBar */
	private static ActionBar actionBar;		
	/** Handler para la actualizacion del grafico en el UI Thread */
    private static Handler mUpdaterHandler = new Handler();	
    /** Tiempo que va transcurriendo (eje x del grafico) */
    private static double time = 0.0d;		
    /** Cuantos segundos representa un cuadrito (una unidad) en el grafico */
    private static double timeScale; 
    
    /** Numero maximo de muestras en las series (osea en el grafico) */
    private static long maxSamples = 3000;
    
    /** Serie que muestra los '1' y '0' de cada canal */
    private static XYSeries[] mSerie = new XYSeries[channelsNumber];
    /** Renderer para cada Serie, indica color, tamaño, etc */
    private static XYSeriesRenderer[] mRenderer = new XYSeriesRenderer[channelsNumber];
    /** Dataset para agrupar las Series */
    private static XYMultipleSeriesDataset mSerieDataset = new XYMultipleSeriesDataset();
    /** Dataser para agrupar los Renderer */
    private static XYMultipleSeriesRenderer mRenderDataset = new XYMultipleSeriesRenderer();
	
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
	private static LogicData[] mData = new LogicData[channelsNumber];
	
	private static boolean firstTime = true;
	private static int samplesNumber = 0;
	
	@Override
	public double onDataDecodedListener(LogicData[] mLogicData, int samplesCount) {
		if(DEBUG) Log.i("mFragment","onDataDecoded()");
		if(DEBUG) Log.i("mFragment","Data: " + mLogicData.toString());
		
		mData = mLogicData;
		samplesNumber = samplesCount;
		mUpdaterHandler.post(mUpdaterTask);
		
		// Configuro las variables en base a las preferencias la primera vez unicamente
		if(firstTime){
			setChartPreferences();
			firstTime = false;
		}

		// Hago un cálculo de cual va a ser el tiempo despues de agregar todos los datos y se lo paso a la Activity
		double tempTime = time;
		for(int n=0; n < samplesCount; ++n) tempTime += 1.0d/LogicData.getSampleRate();	 // Tiempo de los datos
		tempTime += (10*timeScale) + (0.0000001d*timeScale);	// Tiempo del espaciado
			
		return tempTime;
	}
	
    /**
     * Creacion de la Activity
     */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i("onCreate()", "onCreate LogicAnalizerView");
		
		// Obtengo la Activity que contiene el Fragment
		mActivity = getSherlockActivity();
		
        actionBar = mActivity.getSupportActionBar();				// Obtengo el ActionBar
        actionBar.setDisplayHomeAsUpEnabled(true);					// El icono de la aplicacion funciona como boton HOME
        actionBar.setTitle(getString(R.string.AnalyzerName)) ;		// Nombre
        this.setHasOptionsMenu(true);
        
        // Crea las Serie que es una linea en el grafico (cada una de las entradas)
        mSerie[0] = new XYSeries(getString(R.string.AnalyzerChannel) + "1");	
        mSerie[1] = new XYSeries(getString(R.string.AnalyzerChannel) + "2");
        mSerie[2] = new XYSeries(getString(R.string.AnalyzerChannel) + "3");
        mSerie[3] = new XYSeries(getString(R.string.AnalyzerChannel) + "4");
        
        for(int n=0; n < channelsNumber; ++n) {
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
        mRenderDataset.setYAxisMax(15);
        mRenderDataset.setXAxisMin(0);
        mRenderDataset.setXAxisMax(xMax);
        mRenderDataset.setPanEnabled(true);
        mRenderDataset.setShowGrid(true);
        mRenderDataset.setPointSize(4f);
        mRenderDataset.setExternalZoomEnabled(true);
        mRenderDataset.setPanEnabled(true, false);
        mRenderDataset.setZoomEnabled(true, false);
        mRenderDataset.setPanLimits(new double[] {0d , Double.MAX_VALUE, -1d, 15d});
        
        mChartView = ChartFactory.getLineChartView(mActivity, mSerieDataset, mRenderDataset);
     
        // Obtengo el OnActionBarClickListener de la Activity
     	try { mActionBarListener = (OnActionBarClickListener) mActivity; }
     	catch (ClassCastException e) { throw new ClassCastException(mActivity.toString() + " must implement OnActionBarClickListener"); }
        
        vibration = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        
        final Runnable longClickRun = new Runnable() {
			@Override
			public void run() {
				if(fingerStillDown && !isMoving) {
					if(DEBUG) Log.i("Runnable longClickRun()", "LONG CLICK");
					vibration.vibrate(80);					// Vibro e inicio el ActionMode
					mActivity.startActionMode(new ActionModeEnable());
				}
			}       	
        };
        
        mChartView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {		
				//if(DEBUG) Log.i("ClickListener", "X: " + event.getX());
				//if(DEBUG) Log.i("ClickListener", "Y: " + event.getY());
				
				//if(DEBUG) Log.i("ClickListener", "Event: " + event.getActionMasked());
				
				// Si me movi al menos 20 unidades en cualquier direccion ya se toma como scroll NO long-press
				if(Math.abs(event.getX() - x) > 20 || Math.abs(event.getY() - y) > 20) {
					//if(DEBUG) Log.i("ClickListener", "isMoving = true");
					isMoving = true;
				}
				// Obtengo las coordenadas iniciales para tomar el movimiento
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					//if(DEBUG) Log.i("ClickListener", "Pointer DOWN");
					//if(DEBUG) Log.i("ClickListener", "First X: " + event.getX());
					//if(DEBUG) Log.i("ClickListener", "First Y: " + event.getY());
					x = event.getX(); y = event.getY();
					fingerStillDown = true; isMoving = false;
					mChartView.postDelayed(longClickRun, 1000);		// En 1000mS se iniciara el Long-Press
				}
				// Si levanto el dedo ya no cuenta para el long-press
				else if(event.getAction() == MotionEvent.ACTION_UP){
					//if(DEBUG) Log.i("ClickListener", "Pointer UP");
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
        
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Renderizo el layout
		return inflater.inflate(R.layout.logicanalizer, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(DEBUG) Log.i("onResume()","Resume LogicAnalizerView");
		
		// Elimino primero el View porque si ya esta agregado genera una excepcion
		((LinearLayout) mActivity.findViewById(R.id.mChart)).removeViewInLayout(mChartView);
		// Agrego un View al layout que se renderizo en onCreateView. No puedo hacerlo antes porque dentro de 
		// onCreateView() el layout no se renderizo y por lo tanto es null.
		((LinearLayout) mActivity.findViewById(R.id.mChart)).addView(mChartView);
	}

	/**
	 * Activa el ActionMode del ActionBar
	 * @author Andres Torti
	 */
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

		/**
		 * Al presionar iconos en el ActionMode
		 */
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
			Log.i("ActionMode", "Destroy");
		}
	}
    
    /**
     * Listener de los iconos en el ActionBar
     * @author Andres Torti
     */
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		if(DEBUG) Log.i("ActionBar", "onOptionsItemSelected() -> LogicAnalizerView");
 		
 		switch(item.getItemId()){
 		case android.R.id.home:
 			Intent intent = new Intent(mActivity, MainMenu.class);
 			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Si la aplicacion ya esta abierta ir a ella no abrir otra nueva
 			startActivity(intent);
 			break;
 		case R.id.settingsLogic:
 			mActivity.startActivityForResult(new Intent(mActivity, LogicAnalizerPrefs.class), PREFERENCES_CODE);
 			break;
 		case R.id.PlayPauseLogic:
 	 		// Paso el ID del boton presionado a la Activity
 	 		mActionBarListener.onActionBarClickListener(item.getItemId());
 			break;
 		case R.id.zoomInLogic:
 			mChartView.zoomIn();
 			break;
 		case R.id.zoomOutLogic:
 			mChartView.zoomOut();
 			break;
 		case R.id.listLogic:
 			FragmentTransaction transaction = getFragmentManager().beginTransaction();
 			// Reemplazo este Fragment con el de la lista de datos, addToBackStack() hace que al presionar la tecla
 			// de atras se vuelva a este Fragment y no se destruya el mismo
 			transaction.replace(R.id.chartFragment, new LogicAnalizerListFragment());
 			transaction.addToBackStack(null);
 			transaction.commit();
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
		Log.i("mFragment", "Activity Result");
		Log.i("mFragment", "resultCode: " + resultCode);
		Log.i("mFragment", "requestCode: " + requestCode);
		
		// Cambio en las preferencias
		if(requestCode == PREFERENCES_CODE){
			if(resultCode == RESULT_OK) {
				Log.i("ActivityResult", "Preferences Setted");
				// Aviso a la Activity que cambiaron las preferencias
				mActionBarListener.onActionBarClickListener(R.id.settingsLogic);
				setChartPreferences();
			}
		}
		// Dialogo de confirmación para guardar imagen
		else if(requestCode == CONFIRM_DIALOG){
			if(resultCode == RESULT_OK){
				if(DEBUG) Log.i("ActivityResult", "Confirm Dialog OK");
				Bitmap bitmap = mChartView.toBitmap();	// Creo un nuevo BitMap
				try {
					FileOutputStream output = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getPath() 
							+ imagesDirectory + data.getExtras().getString("text") + ".jpeg")); // Guardo la imagen con el nombre
					bitmap.compress(CompressFormat.JPEG, 95, output);			// Formato JPEG
				} catch (FileNotFoundException e) { e.printStackTrace(); }
				mActivity.runOnUiThread(new Runnable() {
					@Override													// Toast en el UI Thread
					public void run() {
						Toast.makeText(mActivity, getString(R.string.AnalyzerDialogFileSaved), Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

	/**
 	 * Reinicia el grafico para empezar un muestreo nuevo
 	 * @author Andres Torti
 	 */
	private void restart() {
		for(int n = 0; n < channelsNumber; ++n) {
			mSerie[n].clear();
		}
		mRenderDataset.setXAxisMax(xMax);
		mRenderDataset.setXAxisMin(0);
		time = 0;
		mChartView.repaint();
		Toast.makeText(mActivity, "Reiniciado", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Crea una ventana preguntando al usuario si desea guardar la sesion o una imagen del grafico
 	 * @author Andres Torti
 	 * @see http://developer.android.com/guide/topics/ui/menus.html
 	 */
	private void createDialog() {
		final CharSequence[] items = {getString(R.string.AnalyzerImagen), getString(R.string.AnalyzerSesion)};
		AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
		alert.setTitle("Guardar");

		alert.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        if(item == 0) {
		        	saveImageDialog();
		        }
		        else {
		        	saveSesionDialog();
		        }
		    }
		});
		alert.show();
	}
	
	/**
	 * Guarda un screenshot del grafico actual en la tarjeta de memoria
	 * @author Andres Torti
	 */
	private void saveImageDialog() {
		// Creo el dialogo
		AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
		alert.setTitle(getString(R.string.AnalyzerDialogSaveTitle));
		alert.setMessage(getString(R.string.AnalyzerDialogFileName));
		
		// Creamos un EditView para que el usuario escriba
		final EditText input = new EditText(mActivity);
		alert.setView(input);
		
		// Creamos el boton OK y su onClickListener
		alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable text = input.getText();		// Obtengo el texto que escribio el usuario
					// Creo un nuevo archivo con el nombre del usuario y extension .jpeg
					// Verifico que pueda escribir en la SD
					if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
						// Creo el directorio si ya existe no hace nada
						new File(Environment.getExternalStorageDirectory().getPath() 
								+ imagesDirectory).mkdirs();
						// Creo el archivo
						final File imageFile = new File(Environment.getExternalStorageDirectory().getPath() 
								+ imagesDirectory + text.toString() + ".jpeg");
						
						// Si el archivo ya existe pregunto por confirmacion
						if(imageFile.exists()){
							if(DEBUG) Log.i("Dialog", "File exists");
							// Creo un diálogo preguntando por confirmación de sobreescribir el archivo y paso el nombre del archivo
							startActivityForResult(new Intent(mActivity, ConfirmDialog.class).putExtra("text", text.toString()), CONFIRM_DIALOG);
						}
						// Si no existe el archivo directamente lo guardo
						else {
							if(DEBUG) Log.i("Dialog", "File doesn't exists)");
							Bitmap bitmap = mChartView.toBitmap();		// Creo un nuevo BitMap
							try {	
								// Formato JPEG, 95% de calidad guardado en imageFile
								bitmap.compress(CompressFormat.JPEG, 95, new FileOutputStream(imageFile));
							} catch (FileNotFoundException e) { e.printStackTrace(); }	
							Toast.makeText(mActivity, getString(R.string.AnalyzerDialogFileSaved), Toast.LENGTH_SHORT).show();
						}
					}
					// Si no se puede escribir en la tarjeta SD muestro un Toast con error
					else {
						Toast.makeText(mActivity, getString(R.string.AnalyzerDialogFileNotSaved), Toast.LENGTH_LONG).show();
					}
				dialog.dismiss();
			}
		});

		// Boton cancelar
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();	  
			}
		});
		alert.show();
	}
	
	/**
	 * Guarda la sesion actual, osea los datos contenidos en las series y el grafico
	 * @author Andres Torti
	 */
	//TODO: hay que hacer que se pueda abrir la sesion con un explorador de archivos
	//TODO: guardar las anotaciones del grafico tambien
	private void saveSesionDialog() {

		AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
		dialog.setTitle(getString(R.string.AnalyzerDialogSaveTitle));
		dialog.setMessage(getString(R.string.AnalyzerDialogFileName));
		
		// Creamos un EditView para que el usuario escriba
		final EditText textInput = new EditText(mActivity);
		dialog.setView(textInput);
		
		dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				new Thread(new Runnable() {		// Creo un nuevo Thread para evitar bloquear el UI Thread
					@Override					// (Guardar el archivos lleva algunos segundos a veces)
					public void run() {
						try {
							// Creo el directorio por si no existe
							new File(Environment.getExternalStorageDirectory().getPath() 
									+ sesionDirectory).mkdirs();
							// Creo el archivo
							final File path = new File(Environment.getExternalStorageDirectory().getPath() 
									+ sesionDirectory + textInput.getText().toString() + ".ms");
							final FileOutputStream fos = new FileOutputStream(path);
							
							// Guardo las Series
							ObjectOutputStream os = new ObjectOutputStream(fos);
							os.writeInt(channelsNumber);	// Numero de canales que voy a guardar
							for(int n = 0; n < channelsNumber; ++n) {
								os.writeObject(mSerie[n]);
							}
							os.close();
						} catch (IOException e) { e.printStackTrace(); }
						mActivity.runOnUiThread(new Runnable() {	// El Toast debe mostrarse en el Thread de la UI
							@Override
							public void run() {
								Toast.makeText(mActivity, getString(R.string.AnalyzerDialogSesionSaved), Toast.LENGTH_SHORT).show();
							}
						});
					}
				}).start();
			}
		});
		
		dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		dialog.show();
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
			
			if(DEBUG) {
				for(int n=0; n < channelsNumber; ++n) {
					for(int i = 0; i < mData[n].getStringCount(); ++i) {
						Log.i("Data", "Data[" + n + "]: " + mData[n].getString(i));
					}
				}
			}

	    	// Si los bit son 1 le sumo 1 a los valores tomados como 0 logicos
			for(int n=0; n <samplesNumber; ++n){
				for(int channel=0; channel < channelsNumber; ++channel){	
					if(mData[channel].getBits().get(n)){	// Si es 1
						// Nivel tomado como 0 + un alto de bit
						mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]+bitScale);
					}
					else{									// Si es 0
						mSerie[channel].add(toCoordinate(time, timeScale), yChannel[channel]);
					}
				}
				//Si llego al maximo del cuadro (borde derecho) aumento el maximo y el minimo para dibujar un tiempo mas
				//(desplazamiento del cuadro) de esta manera si deslizamos el cuadro horizontalmente tendremos los datos
				if(toCoordinate(time, timeScale) >= xMax){
					//if(DEBUG) Log.i("Move", "Chart moved");
					mRenderDataset.setXAxisMax(mRenderDataset.getXAxisMax()+1d);
					mRenderDataset.setXAxisMin(mRenderDataset.getXAxisMin()+1d);
				}
				time += 1.0d/LogicData.getSampleRate();	// Incremento el tiempo
			}
			// Agrego un espacio para indicar que el buffer de muestreo llego hasta aqui
			time += (10*timeScale);
			for(int n=0; n < channelsNumber; ++n){
				mSerie[n].add(mSerie[n].getX(mSerie[n].getItemCount()-1)+0.0000001d, MathHelper.NULL_VALUE);
			}
			
			for(int n = 0; n < channelsNumber; ++n){
				for(int i = 0; i < mData[n].getStringCount(); ++i){
					
					// Agrego el texto en el centro del area de tiempo que contiene el string
					mSerie[n].addAnnotation(mData[n].getString(i),
							toCoordinate(mData[n].getPositionAt(i)[0]+((mData[n].getPositionAt(i)[1]-mData[n].getPositionAt(i)[0])/2.0d),
									timeScale),
							yChannel[n]+2f);
				
					// Agrego el recuadro
					mSerie[n].addRectangle(toCoordinate(mData[n].getPositionAt(i)[0], timeScale)+0.0000001,
							yChannel[n]+3.5f,
							toCoordinate(mData[n].getPositionAt(i)[1], timeScale),
							yChannel[n]+bitScale+0.5f);
				}
			}
			
			//if(DEBUG) Log.i("Chart", "redraw()");
			mChartView.repaint();					// Redibujo el grafico
			
			// Si algun canal se paso del maximo de muestras, borro todas las muestras
			for(int n = 0; n < channelsNumber; n++) {
				if(mSerie[n].getItemCount() > maxSamples) {
					for(int k = 0; k < channelsNumber; k++) {
						if(DEBUG) Log.i("Chart", "Data Cleared");
						mSerie[k].clear();
						n = channelsNumber;	// Si un canal se paso, borro todos osea que no necesito seguir comprobando
					}
				}
			}
			// Cada vez que recibo un buffer del analizador logico, lo muestro todo y pauso
			mActionBarListener.onActionBarClickListener(R.id.PlayPauseLogic);
		}
	};
	
	/**
	 * Convierte el tiempo en segundo a la escala del grafico segunda la escala de tiempos
	 * @param time tiempo en segundos
	 * @param timeScale cuantos segundos equivalen a una unidad en el grafico
	 * @return coordenada equivalente
	 */
	private static double toCoordinate (double time, double timeScale){
		return (time/timeScale);
	}
	
	/**
	 * @author Andres Torti
	 * Define los parametros de acuerdo a las preferencias
	 */
 	private void setChartPreferences() {
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        for(int n=0; n < channelsNumber; ++n){
        	// Seteo el protocolo para cada canal
        	switch(Byte.decode(getPrefs.getString("protocol" + (n+1), "0"))){
	        	case 0:		// I2C
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + " [I2C]");
	        		break;
	        	case 1:		// UART
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + " [UART]");
	        		break;
	        	case 2:		// CLOCK
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + "[CLK]");
	        		break;
	        	case 3:		// NONE
	        		mSerie[n].setTitle(getString(R.string.AnalyzerChannel) + " " + (n+1) + "[---]");
	        		break;
        	}
        }
    	// Directorios para guardar las imagenes y sesiones
    	imagesDirectory = getPrefs.getString("logicImageSave","Multi/Work/Images/");
    	sesionDirectory = getPrefs.getString("logicSesionSave","Multi/Work/Sesion/");
    	// Máxima cantidad de muestras para almacenar
        maxSamples = Long.decode(getPrefs.getString("maxSamples", "3000"));
    	
        // Escala del eje X de acuerdo al sample rate
        if(LogicData.getSampleRate() == 40000000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x25nS");
        	timeScale = 0.000000025d;		// 25nS
        }else if(LogicData.getSampleRate() == 20000000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x50 nS");
        	timeScale = 0.000000050d;		// 50nS
        }else if(LogicData.getSampleRate() == 10000000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x100 nS");
        	timeScale = 0.000000100d;		// 100nS
        }else if(LogicData.getSampleRate() == 4000000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x250 nS");
        	timeScale = 0.000000250d;		// 250nS
        }else if(LogicData.getSampleRate() == 400000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x2.5 uS");
        	timeScale = 0.0000025d;			// 2.5uS
        }else if(LogicData.getSampleRate() == 2000) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x500 uS");
        	timeScale = 0.000500d;			// 500uS
        }else if(LogicData.getSampleRate() == 10) {
        	mRenderDataset.setXTitle(getString(R.string.AnalyzerXTitle) + " x100 mS");
        	timeScale = 0.1d;				// 100mS
        }
        // Actualizo los datos del grafico
        mChartView.repaint();
 	}

}