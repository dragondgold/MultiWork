package com.roboticarm.andres;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import com.multiwork.andres.R;

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
import android.widget.Toast;

public class BrazoRobot extends Activity implements OnTouchListener{

	private static final boolean DEBUG = true;
	
	/** Datos de transmicion */
	private static final byte StartByte = 0x0A;
	private static final byte pad1Byte = 0;
	private static final byte pad2Byte = 1;
	private static final byte data = 20;
	private static final byte config = 25;
	
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
	protected static float x1Distance, y1Distance, x2Distance, y2Distance;
	
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
		mView = new JoystickView(this);
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
				//Pad 2
				xCenter2 = screenWidth - (screenWidth / 4);		// Extremo derecho
				yCenter2 = screenHeight / 2;
				// Radios
				staticRadio = ((screenWidth / 2) - (2*padding)) / 2;
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
		setContentView(mView);
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
			
			// Calculo las distancias de X e Y de cada joystick desde el centro
			x1Distance = pad1.getXDistanceFromCenter();
			y1Distance = pad1.getYDistanceFromCenter();
			
			x2Distance = pad2.getXDistanceFromCenter();
			y2Distance = pad2.getYDistanceFromCenter();
			mView.redraw();
			
			BTSendData((int)x1Distance, (int)y1Distance, pad1Byte);
			BTSendData((int)x2Distance, (int)y2Distance, pad2Byte);
			
			if(DEBUG) Log.i("BrazoRobotTouch", "x1Distance: " + x1Distance + "   y1Distance: " + y1Distance);
			if(DEBUG) Log.i("BrazoRobotTouch", "x2Distance: " + x2Distance + "   y2Distance: " + y2Distance);
			
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
		        
		        // Envío las distancias máximas que el PAD puede cubrir
			    try { mBluetoothOut.write(new byte[] { StartByte,									// Byte de Start
			    										config,										// Indico que es configuracion 
			    										(byte)((int)Math.ceil(staticRadio) & 0xFF),				// LSB
			    						      			(byte)(((int)Math.ceil(staticRadio) >> 8) & 0xFF)		// MSB
			    }); 
			    }
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
	 * @param x coordenada x
	 * @param y coordenada y
	 * @param number identificador
	 */
	private void BTSendData (int x, int y, byte number){
		if(isBTConnected){
			// Envío byte de Start, identificador de dato y las dos coordenadas
			Log.i("BTSendBrazo", "x: " + x + " y: " + y + " Number: " + number);
			Log.i("BTSendBrazo", "Binary x: " + Integer.toBinaryString(x) + " Binary y: " + Integer.toBinaryString(y));
			
			try { mBluetoothOut.write(new byte[] { StartByte,					// Start byte
													 data,						// Indico que son datos, no configuracion
													 number,					// Indico las coordenadas de que pad son
													 (byte)(x & 0xFF),			// x -> LSB
													 (byte)((x >> 8) & 0xFF),	// x -> MSB
													 (byte) ((pad1.isXNegative()) ? 1 : 0),		// Indico si la distancia es negativa o no
													 (byte)(y & 0xFF),			// y -> LSB
													 (byte)((y >> 8) & 0xFF),	// y -> MSB
													 (byte) ((pad1.isYNegative()) ? 1 : 0),		// Indico si la distancia es negativa o no
													});
			}				
			catch (IOException e) { e.printStackTrace(); }
		}
		else{
			Log.i("BTSendBrazo", "Dispositivo Bluetooth no conectado, datos no enviados");
			Log.i("BTSendBrazo", "x: " + x + " y: " + y + " Number: " + number);
			Log.i("BTSendBrazo", "Binary x: " + Integer.toBinaryString(x) + " Binary y: " + Integer.toBinaryString(y));
		}
	}

}
