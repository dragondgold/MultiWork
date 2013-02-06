package com.roboticarm.andres;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import com.multiwork.andres.R;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.Toast;

public class BrazoRobot extends Activity implements OnTouchListener{

	private static final boolean DEBUG = true;
	
	/** Datos de transmicion */
	private static final byte StartByte = 0x0A;
	
	/** View que dibuja los joystick */
	private static JoystickView mView;
	protected static int pointerID1;
	protected static int pointerID2;
	
	/** Centro en x del circulo que se mueve */
	protected static float xCenter1 ;
	/** Centro en y del circulo que se mueve */
	protected static float yCenter1;
	/** Centro en x del circulo que se mueve */
	protected static float xCenter2;
	/** Centro en y del circulo que se mueve */
	protected static float yCenter2;
	/** Radio de los circulos estáticos (contorno) */
	protected static float staticRadio;
	/** Radio de los circulos que se mueven */
	protected static float joystickRadio;
	/** Padding de los circulos estaticos desde la mitad y bordes de pantalla */
	private static final int padding = 50;
	/** Relacion del tamaño de los joystick con los radios externos (estáticos) si es 2 por ejemplo es la mitad */
	private static final float ampFactor = 2f;
	
	/** Joysticks */
	protected static Joystick pad1, pad2;
	
	/** Distancia del joystick */
	protected static int x1Distance, y1Distance, x2Distance, y2Distance;
	private static int servo1Angle = 0, servo2Angle = 0;
	
	// Limites de los circulos
	protected static float rightLimit1;
	protected static float rightLimit2;
	protected static float leftLimit1;
	protected static float leftLimit2;
	
	protected static float upperLimit1;
	protected static float upperLimit2;
	protected static float lowerLimit1;
	protected static float lowerLimit2;
	
	private static BluetoothAdapter mBluetoothAdapter;
	/** Código que obtengo en onActivityResult() al recibir el resultado de activar el bluetooth */
	private static int REQUEST_ENABLE_BT = 1;
	/** UUID del Bluetooth */
	private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String BluetoothName = "Andres-NT-0"; 
	
	private static BluetoothSocket mBluetoothSocket;
	private static BluetoothDevice mBluetoothDevice;
	private static OutputStream mBluetoothOut;
	
	/** Inidica si hay un dispositivo BT conectado, en caso de no haberlo no envio datos */
	private static boolean isBTConnected = false;
	/** Inidica si ya se tomaron las medidas de la pantalla y se puede usar el touchscreen */
	private static boolean isSystemRdy = false;
	
	private static int screenHeight = 0;
	private static int screenWidth = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(DEBUG) Log.i("BrazoRobot", "onCreate()");
		//mView = new JoystickView(this);
		//mView.setOnTouchListener(this);	
		
		setContentView(R.layout.brazo_robot);
		mView = (JoystickView) findViewById(R.id.joystickView);
		mView.setOnTouchListener(this);
		
		// Se llama al listener cuando el layout terminó de dibujarse porque si tomaramos las medidas antes nos darian 0
		// porque el layout aún no se dibuja en onCreate() ni en onResume()
		mView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
			@Override
			public void onGlobalLayout() {
				//mView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				screenHeight = mView.getHeight();
				screenWidth = mView.getWidth();
						
				if(DEBUG) Log.i("BrazoRobot", "Screen Height: " + screenHeight);
				if(DEBUG) Log.i("BrazoRobot", "Screen Width: " + screenWidth);
						
				// Creo los PAD
				pad1 = new Joystick();
				pad2 = new Joystick();
						
				// Pad 1
				xCenter1 = screenWidth / 4;		// Extremo izquierdo
				yCenter1 = screenHeight / 2;
				// Pad 2
				xCenter2 = screenWidth - (screenWidth / 4);		// Extremo derecho
				yCenter2 = screenHeight / 2;
				// Radios
				staticRadio = ((screenWidth / 2) - (3*padding)) / 2;
				joystickRadio = staticRadio / ampFactor;
				// Límites
				rightLimit1 = xCenter1 + staticRadio;
				rightLimit2 = xCenter2 + staticRadio;
				leftLimit1 = xCenter1 - staticRadio;
				leftLimit2 = xCenter2 - staticRadio;
				
				upperLimit1 = yCenter1 - staticRadio;
				upperLimit2 = yCenter2 - staticRadio;
				lowerLimit1 = yCenter1 + staticRadio;
				lowerLimit2 = yCenter2 + staticRadio;
						
				// Configuraciones
				pad1.setXCenter(xCenter1);
				pad1.setYCenter(yCenter1);
				pad1.setX(xCenter1);
				pad1.setY(yCenter1);
						
				// Pad 2
				pad2.setXCenter(xCenter2);
				pad2.setYCenter(yCenter2);
				pad2.setX(xCenter2);
				pad2.setY(yCenter2);
				
				// Configuro los radios
				mView.setConfigurations(staticRadio, joystickRadio, pad1, pad2);
						
				if(DEBUG) Log.i("BrazoRobot", "xCenter1: " + pad1.getXCenter());
				if(DEBUG) Log.i("BrazoRobot", "xCenter2: " + pad2.getXCenter());
				
				isSystemRdy = true;		
				mView.redraw();
			}
		});
		
		// Compruebo que el dispositivo tenga Bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Si no hay Bluetooth en el dispositivo muestro un dialogo alertando al usuario y salgo de la Activity
		    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		    dialog.setTitle(getString(R.string.NoBTAlertTitle));
		    dialog.setMessage(getString(R.string.NoBTAlertText));
		    dialog.setPositiveButton(getString(R.string.Ok), new OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
		    });
		}
		// Si el dispositivo tiene Bluetooth me conecto
		else{
			// Compruebo que el Bluetooth esté activado, sino pido al usuario que lo active
			if (!mBluetoothAdapter.isEnabled()) {
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
			// Compruebo si el dispositivo no esta en los dispositivos emparejados (paired)
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
			    // Loop a travez de los dispositivos emparejados (paired)
			    for (BluetoothDevice device : pairedDevices) {
			        Log.i("BrazoRobotBT", "Name: " + device.getName() + " -- Address:  " + device.getAddress());
			        // Si el dispositivo coincide con el que busco lo asigno
			        if(device.getName().equals(BluetoothName)){
			        	mBluetoothDevice = device;
						// Establezco una conexión Bluetooth para enviar datos
						EntablishConnection();
			        	break;
			        }
			    }
			}
			// Sino escaneo por dispositivos
			else{
				IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
				registerReceiver(mReceiver, filter);
			}
		}
		
		findViewById(R.id.leftGiro).setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(DEBUG) Log.i("Button", "Button left");
				if(servo1Angle > 0) --servo1Angle;
				BTSendData();
				return false;
			}
		});
		
		findViewById(R.id.rightGiro).setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(DEBUG) Log.i("Button", "Button right");
				if(servo1Angle < 180) ++servo1Angle;
				BTSendData();
				return false;
			}
		});
		
		// Esto permite que se puedan tocar los View debajo de este View. El Canvas del joystick usa toda la pantalla
		// por lo que si tocamos sobre algún boton el sistema no lo toma ya que primero esta el Canvas de los
		// Joystick arriba. Esto permite que se toquen los botones abajo de este View. Viene desactivado por defecto
		// por seguridad. http://stackoverflow.com/questions/12398402/android-overlay-layout-on-top-of-all-windows-that-receives-touches
		mView.setFilterTouchesWhenObscured(false);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//unregisterReceiver(mReceiver);
		if(DEBUG) Log.i("BrazoRobot", "onPause()");
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(DEBUG) Log.i("BrazoRobot", "onResume()");
	}
	
	// BroadcastReceiver para ACTION_FOUND cuando sea escanea los dispositivos Bluetooth
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        Log.i("BrazoRobotBTDiscover", "Receiver");
	        // Si se encuentra algún dispositivo
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Obtengo el dispositivo Bluetooth
	        	BluetoothDevice temp = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	// Si coincide con el dispositivo que busco, lo asigno y salgo
	        	if(temp.getName().equals(BluetoothName)){
	        		mBluetoothDevice = temp;
	        		// Establezco una conexión Bluetooth para enviar datos
	        		EntablishConnection();
	        		return;
	        	}
	            Log.i("BrazoRobotBTDiscover", "Name: " + temp.getName() + " -- Address:  " + temp.getAddress());
	        }
	    }
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ENABLE_BT){
			if(resultCode == RESULT_CANCELED){

			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		if(DEBUG) Log.i("BrazoRobotTouch", "----------------------------------------------");
		if(DEBUG) Log.i("BrazoRobotTouch", "onTouch()");
		if(DEBUG) Log.i("BrazoRobotTouch", "v: " + v.getId());
		if(DEBUG) Log.i("BrazoRobotTouch", "mView: " + mView.getId());
		
		// Evita que se modifique pad1 y pad2 si aún no han sido creados porque las medidas de la pantalla no se tomaron aún
		if(isSystemRdy){
			int touchEvents = event.getPointerCount();
			float tempX[] = new float[touchEvents];
			float tempY[] = new float[touchEvents];
			
			// Busco las coordenadas de todos los eventos Multi-Touch
			// http://stackoverflow.com/questions/8059127/android-multi-touch
			// http://android-developers.blogspot.com.ar/2010/06/making-sense-of-multitouch.html
			for(int n=0; n < touchEvents; ++n) {
				tempX[n] = event.getX(n);
				tempY[n] = event.getY(n);
				//if(DEBUG) Log.i("BrazoRobotP", "pointerID[" + n + "]: " + event.getPointerId(n));
			}
			
			// Debug
			if(DEBUG) Log.i("BrazoRobotTouch", "PointerCount: " + touchEvents);
			if(DEBUG) {
				for(int n=0; n < tempX.length; ++n) {
					Log.i("BrazoRobotTouch","tempX[" + n + "]: " + tempX[n] +
									   "   tempY[" + n + "]: " + tempY[n]);
				}
			}
	
			// Compruebo que coordenadas estan dentro del primer circulo
			for(int n=0; n < touchEvents; ++n) {
				if(tempX[n] < rightLimit1 && tempX[n] > leftLimit1 && tempY[n] > upperLimit1 && tempY[n] < lowerLimit1) {
					pad1.setX(tempX[n]);
					pad1.setY(tempY[n]);
					pointerID1 = event.getPointerId(n);		// Obtengo el ID del touch este
				}
			}
			// Compruebo que coordenadas estan dentro del segundo circulo
			for(int n=0; n < touchEvents; ++n) {
				if(tempX[n] < rightLimit2 && tempX[n] > leftLimit2 && tempY[n] > upperLimit2 && tempY[n] < lowerLimit2) {
					pad2.setX(tempX[n]);
					pad2.setY(tempY[n]);
					pointerID2 = event.getPointerId(n);		// Obtengo el ID del touch este
				}
			}
			
			// Cuando se suelta el touch
			// http://android-developers.blogspot.com.ar/2010/06/making-sense-of-multitouch.html
			if( ( (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) ||
					  ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) ) {
					if(DEBUG) Log.i("BrazoRobotTouch", "Pointer_UP - UP");
					
					// Obtengo el ID del touch que se solto
					final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) 
			                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			        final int pointerID = event.getPointerId(pointerIndex);
			        
			        if(DEBUG) Log.i("BrazoRobotTouch", "pointerID: " + pointerID);
					if(DEBUG) Log.i("BrazoRobotTouch", "ID1: " + pointerID1);
					if(DEBUG) Log.i("BrazoRobotTouch", "ID2: " + pointerID2);
			        
					// Si el ID es el del primer circulo
			        if(pointerID == pointerID1) {
			        	if(DEBUG) Log.i("BrazoRobotTouch", "Reset 1");
						pad1.setX(xCenter1);
						pad1.setY(yCenter1);
			        }
			        // Si el ID es del segundo circulo
			        if(pointerID == pointerID2) {
			        	if(DEBUG) Log.i("BrazoRobotTouch", "Reset 2");
						pad2.setX(xCenter2);
						pad2.setY(yCenter2);
			        }
				}
			
			BTSendData();
			mView.redraw();
			
			//if(DEBUG) Log.i("BrazoRobotTouchC", "x1: " + pad1.getX() + "   y1: " + pad1.getY());
			//if(DEBUG) Log.i("BrazoRobotTouchC", "x2: " + pad2.getX() + "   y2: " + pad2.getY());
			
			// Duermo el Thread por 10mS para reducir uso de CPU
			try { Thread.sleep(10); } 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		return true;	// Si el valor aqui es 'false' se retorna y se espera a que se suelte y se toque de nuevo la pantalla
						// Si el valor aqui es 'true' nos da continuamente las coordenadas (permite deslizarnos sin clicks)
	}
	
	/**
	 * Establezco una conexión con el dispositivo que ya definií anteriormente
	 */
	public void EntablishConnection () {
		if(DEBUG) Log.i("BrazoBT", "EntablishConnection()");
		
		// Establesco una conexión con el dispositivo bluetooth asignado en mBluetoothDevice
        try { mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID); }
        catch (IOException e1) { e1.printStackTrace(); }
		
        /*
		Method m = null;
		try { m = mBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class }); }
		catch (NoSuchMethodException e2) { e2.printStackTrace(); }
		
		try {
			mBluetoothSocket = (BluetoothSocket) m.invoke(mBluetoothDevice, Integer.valueOf(1));
		} catch (IllegalArgumentException e2) {
			e2.printStackTrace();
		} catch (IllegalAccessException e2) {
			e2.printStackTrace();
		} catch (InvocationTargetException e2) {
			e2.printStackTrace();
		}*/
		
        new Thread(new Runnable(){
			@Override
			public void run() {
				boolean noException = true;
				// Desactivo el descubrimiento de dispositivos porque hace lenta la conexion
				if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
		        
		        // Me conecto al dispositivo, esto se bloqueará hasta que se conecte por eso debe hacerse
		        // en un Thread diferente
		        try { mBluetoothSocket.connect(); } 
		        catch (IOException e) { 
		        	try { mBluetoothSocket.close(); }
		        	catch (IOException e1) { e1.printStackTrace(); }
		        	Log.i("BrazoBT", "Connection Exception");
		        	noException = false;
		        }
		        
		        // Obtengo el OutputStream para enviar datos al Bluetooth
		        try { mBluetoothOut = mBluetoothSocket.getOutputStream(); }
		        catch (IOException e) { e.printStackTrace(); }
			
			    // Verifico si hubo una excepción entonces no se pudo conectar al dispositivo, de otro modo sí
			    if(noException){
			        Toast.makeText(getApplicationContext(), "Conectado", Toast.LENGTH_SHORT).show();
			        Log.i("BrazoBT", "Conectado");
			    }
			    else{
			    	Toast.makeText(getApplicationContext(), "Error de conexion", Toast.LENGTH_SHORT).show();
			    }
		        isBTConnected = noException;
			}
        }).run();
    }
	
	/**
	 * Envío los datos por Bluetooth
	 */
	private void BTSendData (){
		if(isBTConnected){
			// Calculo un valor entre 0 y 500 dependiendo de la distancia del joystick del centro
			x1Distance = (int) ((staticRadio* pad1.getXDistanceFromCenter())/500f);
			y1Distance = (int) ((staticRadio* pad1.getYDistanceFromCenter())/500f);
			
			x2Distance = (int) ((staticRadio* pad2.getXDistanceFromCenter())/500f);
			y2Distance = (int) ((staticRadio* pad2.getYDistanceFromCenter())/500f);
			
			if(DEBUG) Log.i("BrazoRobotTouch", "Servo 1: " + servo1Angle);
			if(DEBUG) Log.i("BrazoRobotTouch", "x1Distance: " + x1Distance + "   y1Distance: " + y1Distance);
			if(DEBUG) Log.i("BrazoRobotTouch", "x2Distance: " + x2Distance + "   y2Distance: " + y2Distance);
			
			try { mBluetoothOut.write(new byte[] { StartByte,					// Start byte
													 (byte)(x1Distance & 0xFF),			// x1 -> LSB
													 (byte)((x1Distance >> 8) & 0xFF),	// x1 -> MSB
													 (byte)(y1Distance & 0xFF),			// y1 -> LSB
													 (byte)((y1Distance >> 8) & 0xFF),	// y1 -> MSB
													 
													 (byte)(x2Distance & 0xFF),			// x2 -> LSB
													 (byte)((x2Distance >> 8) & 0xFF),	// x2 -> MSB
													 (byte)(y2Distance & 0xFF),			// y2 -> LSB
													 (byte)((y2Distance >> 8) & 0xFF),	// y2 -> MSB
													
													 (byte)(servo1Angle & 0xFF),		// servo 1 -> LSB
													 (byte)(servo2Angle & 0xFF)			// servo 2 -> LSB
													});
			}				
			catch (IOException e) { e.printStackTrace(); }
		}
		else{
			Log.i("BTSendBrazo", "Dispositivo Bluetooth no conectado, datos no enviados");
		}
	}

}
