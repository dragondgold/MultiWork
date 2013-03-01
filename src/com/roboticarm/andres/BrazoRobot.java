package com.roboticarm.andres;

import java.io.IOException;
import java.io.InputStream;
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
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

public class BrazoRobot extends Activity implements OnTouchListener{

	private static final boolean DEBUG = true;
	
	/** Datos de transmicion */
	private static final byte Pad1 = 0;
	private static final byte Pad2 = 1;
	private static final byte Munieca = 2;
	private static final byte Pinza = 3;
	
	/** View que dibuja los joystick */
	private static JoystickView mView;
	private static int pointerID1;
	private static int pointerID2;
	
	/** Centro en x del circulo que se mueve */
	private static float xCenter1 ;
	/** Centro en y del circulo que se mueve */
	private static float yCenter1;
	/** Centro en x del circulo que se mueve */
	private static float xCenter2;
	/** Centro en y del circulo que se mueve */
	private static float yCenter2;
	/** Radio de los circulos estáticos (contorno) */
	private static float staticRadio;
	/** Radio de los circulos que se mueven */
	private static float joystickRadio;
	/** Padding de los circulos estaticos desde la mitad y bordes de pantalla */
	private static final int padding = 50;
	/** Padding de los botones de control del servo desde el centro de la pantalla */
	private static final int paddingCenter = 40;
	/** Relacion del tamaño de los joystick con los radios externos (estáticos) si es 2 por ejemplo es la mitad */
	private static final float ampFactor = 3f;
	/** Offset del centro de los joystick */
	private static final float centerOffset = 40;
	
	/** Joysticks */
	private static Joystick pad1, pad2;
	
	// Limites de los circulos
	private static float rightLimit1;
	private static float rightLimit2;
	private static float leftLimit1;
	private static float leftLimit2;
	
	private static float upperLimit1;
	private static float upperLimit2;
	private static float lowerLimit1;
	private static float lowerLimit2;
	
	// Muñeca
	private static final int servo1MaxAngle	= 180;
	private static final int servo1MinAngle	= 0;
	// Pinza
	private static final int servo2MaxAngle	= 140;
	private static final int servo2MinAngle	= 65;
	
	private static final int angleStep = 5;
	
	private static int servo1Angle = servo1MinAngle, servo2Angle = servo2MinAngle;
	
	private static BluetoothAdapter mBluetoothAdapter;
	/** Código que obtengo en onActivityResult() al recibir el resultado de activar el bluetooth */
	private static int REQUEST_ENABLE_BT = 1;
	/** UUID del Bluetooth */
	private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String BluetoothName = "linvor"; 
	
	private static BluetoothSocket mBluetoothSocket;
	private static BluetoothDevice mBluetoothDevice;
	private static OutputStream mBluetoothOut;
	private static InputStream mBluetoothInput;
	
	private static BluetoothDataTransfer mBluetoothDataTransfer;
	
	/** Inidica si hay un dispositivo BT conectado, en caso de no haberlo no envio datos */
	private static boolean isBTConnected = false;
	/** Inidica si ya se tomaron las medidas de la pantalla y se puede usar el touchscreen */
	private static boolean isSystemRdy = false;
	
	private static int screenHeight;
	private static int screenWidth;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(DEBUG) Log.i("BrazoRobot", "onCreate()");
		
		setContentView(R.layout.brazo_robot);
		mView = (JoystickView) findViewById(R.id.joystickView);
		mView.setOnTouchListener(this);
		
		// Se llama al listener cuando el layout terminó de dibujarse porque si tomaramos las medidas antes nos darian 0
		// porque el layout aún no se dibuja en onCreate() ni en onResume()
		mView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
			@Override
			public void onGlobalLayout() {
				if(!isSystemRdy){
					
					screenHeight = mView.getHeight();
					screenWidth = mView.getWidth();
							
					if(DEBUG) Log.i("BrazoRobot", "Screen Height: " + screenHeight);
					if(DEBUG) Log.i("BrazoRobot", "Screen Width: " + screenWidth);
							
					// Creo los PAD
					pad1 = new Joystick();
					pad2 = new Joystick();
							
					// Pad 1
					xCenter1 = (screenWidth / 4) - centerOffset;						// Extremo izquierdo
					yCenter1 = screenHeight / 2;
					// Pad 2
					xCenter2 = (screenWidth - (screenWidth / 4)) + centerOffset;		// Extremo derecho
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
							
					// Pad 1
					pad1.setXCenter(xCenter1);
					pad1.setYCenter(yCenter1);
					pad1.setX(xCenter1);
					pad1.setY(yCenter1);
					pad1.setMaxDistance(staticRadio);
					pad1.setMaxProportional(500);
							
					// Pad 2
					pad2.setXCenter(xCenter2);
					pad2.setYCenter(yCenter2);
					pad2.setX(xCenter2);
					pad2.setY(yCenter2);
					pad2.setMaxDistance(staticRadio);
					pad2.setMaxProportional(500);
					
					// Configuro los radios
					mView.setConfigurations(staticRadio, joystickRadio, pad1, pad2);
							
					if(DEBUG) Log.i("BrazoRobot", "xCenter1: " + pad1.getXCenter());
					if(DEBUG) Log.i("BrazoRobot", "xCenter2: " + pad2.getXCenter());
					
					// Posición rotacion de muñeca
					RelativeLayout.LayoutParams params = (LayoutParams) findViewById(R.id.rightGiro).getLayoutParams();
					params.leftMargin = (int)((screenWidth/2f) + paddingCenter);
					findViewById(R.id.rightGiro).setLayoutParams(params);
					
					params = (LayoutParams) findViewById(R.id.leftGiro).getLayoutParams();
					params.leftMargin = (int)((screenWidth/2f) - paddingCenter - findViewById(R.id.leftGiro).getWidth());
					findViewById(R.id.leftGiro).setLayoutParams(params);
					
					// Posición apertura y cierre de pinza
					params = (LayoutParams) findViewById(R.id.openClaw).getLayoutParams();
					params.leftMargin = (int)((screenWidth/2f) + paddingCenter);
					findViewById(R.id.openClaw).setLayoutParams(params);
					
					params = (LayoutParams) findViewById(R.id.closeClaw).getLayoutParams();
					params.leftMargin = (int)((screenWidth/2f) - paddingCenter - findViewById(R.id.closeClaw).getWidth());
					findViewById(R.id.closeClaw).setLayoutParams(params);
					
					isSystemRdy = true;		
					mView.redraw();
				}
			}
		});
		
		// Muñeca, gira izquierda
		findViewById(R.id.leftGiro).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Button", "Button left");
				if(servo1Angle >= (servo1MinAngle+angleStep)) servo1Angle -= angleStep;
				mBluetoothDataTransfer.btSendDataWithSync(new byte[] {(byte)servo1Angle}, Munieca);
			}
		});
		
		// Muñeca, gira derecha
		findViewById(R.id.rightGiro).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Button", "Button right");
				if(servo1Angle <= (servo1MaxAngle-angleStep)) servo1Angle += angleStep;
				mBluetoothDataTransfer.btSendDataWithSync(new byte[] {(byte)servo1Angle}, Munieca);
			}
		});
		
		// Pinza, cierra
		findViewById(R.id.closeClaw).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Button", "Close claw");
				if(servo2Angle >= (servo2MinAngle+angleStep)) servo2Angle -= angleStep;
				mBluetoothDataTransfer.btSendDataWithSync(new byte[] {(byte)servo2Angle}, Pinza);
			}
		});
		
		// Pinza, abre
		findViewById(R.id.openClaw).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Button", "Open claw");
				if(servo2Angle <= (servo2MaxAngle-angleStep)) servo2Angle += angleStep;
				mBluetoothDataTransfer.btSendDataWithSync(new byte[] {(byte)servo2Angle}, Pinza);
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
		if(DEBUG) Log.i("BrazoRobot", "onPause()");
		
		// Elimino el OutputStream y desconecto el dispositivo
		if(mBluetoothOut != null){
			try { mBluetoothOut.flush(); }
			catch (IOException e) { e.printStackTrace(); }
		}
		if(mBluetoothSocket != null){
			try { mBluetoothSocket.close(); }
			catch (IOException e) { e.printStackTrace(); }
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(DEBUG) Log.i("BrazoRobot", "onResume()");
		isSystemRdy = false;
		
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
			        if(DEBUG) Log.i("BrazoRobotBT", "Name: " + device.getName() + " -- Address:  " + device.getAddress());
			        // Si el dispositivo coincide con el que busco lo asigno
			        if(device.getName().equals(BluetoothName)){
			        	mBluetoothDevice = device;
						// Establezco una conexión Bluetooth para enviar datos
						establishConnection();
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
	        		establishConnection();
	        		return;
	        	}
	            Log.i("BrazoRobotBTDiscover", "Name: " + temp.getName() + " -- Address:  " + temp.getAddress());
	        }
	    }
	};
	
	// Establezco una conexión con el dispositivo que ya definí anteriormente
	public void establishConnection () {
		if(DEBUG) Log.i("BrazoBT", "Connecting...");
		
		// Establesco una conexión con el dispositivo bluetooth asignado en mBluetoothDevice
        try { mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID); }
        catch (IOException e1) { e1.printStackTrace(); }
        	
        // Runnable donde se encuentra el código a ejecutar por un Handler o Thread
        final Runnable mRunnable = new Runnable() {
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
    	        
    	        // Obtengo el InputStream para recibir datos desde el Bluetooth
    			try { mBluetoothInput = mBluetoothSocket.getInputStream(); }
    			catch (IOException e) { e.printStackTrace(); }
    			
    		    // Verifico si hubo una excepción entonces no se pudo conectar al dispositivo, de otro modo sí
    		    if(noException){
    		    	runOnUiThread(new Runnable() {
    					public void run() { 
    						Toast.makeText(BrazoRobot.this, "Conectado a " + mBluetoothDevice.getName(), Toast.LENGTH_SHORT).show(); 
    					}
    		    	});
    		        Log.i("BrazoBT", "Conectado a " + mBluetoothDevice.getName());
    		    }
    		    else{
    		    	runOnUiThread(new Runnable() {
    					public void run() { 
    						Toast.makeText(BrazoRobot.this, "Error de conexion", Toast.LENGTH_SHORT).show(); 
    					}
    		    	});
    		    	Log.i("BrazoBT", "Error");
    		    	finish();
    		    }
    	        if(noException){
    	        	mBluetoothDataTransfer = new BluetoothDataTransfer(mBluetoothOut, mBluetoothInput);
    	        	isBTConnected = noException;
    	        	mBluetoothDataTransfer.start();
    	        	//mBTThread.start();
    	        }
    	        else{
    	        	finish();
    	        }
    	    }
        };
        // Thread que ejecuta al Runnable
        final Thread mThread = new Thread(){
        	@Override
            public void run() {
               mRunnable.run();
            }
        };
        mThread.start();
    };
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ENABLE_BT){
			if(resultCode == RESULT_CANCELED){
				finish();
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// Evita que se modifique pad1 y pad2 si aún no han sido creados porque las medidas de la pantalla no se tomaron aún
		if(isSystemRdy && isBTConnected){
			//if(DEBUG) Log.i("BrazoRobotTouch", "--------------------------");
			if(DEBUG) Log.i("BrazoRobotTouch", "onTouch()");
			//if(DEBUG) Log.i("BrazoRobotTouch", "v: " + v.getId());
			//if(DEBUG) Log.i("BrazoRobotTouch", "mView: " + mView.getId());
			
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
					//pad1.setX(tempX[n]);
					if( Math.abs(pad1.getY() - tempY[n]) > 10){
						pad1.setY(tempY[n]);
						mBluetoothDataTransfer.btSendDataWithSync(pad1.toYBytes(), Pad1);
					}
					pointerID1 = event.getPointerId(n);		// Obtengo el ID del touch este
				}
			}
			// Compruebo que coordenadas estan dentro del segundo circulo
			for(int n=0; n < touchEvents; ++n) {
				if(tempX[n] < rightLimit2 && tempX[n] > leftLimit2 && tempY[n] > upperLimit2 && tempY[n] < lowerLimit2) {
					//pad2.setX(tempX[n]);
					if( Math.abs(pad2.getY() - tempY[n]) > 10){
						pad2.setY(tempY[n]);
						mBluetoothDataTransfer.btSendDataWithSync(pad2.toYBytes(), Pad2);
					}
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
						//pad1.setX(xCenter1);
						pad1.setY(yCenter1);
						mBluetoothDataTransfer.btSendDataWithSync(pad1.toYBytes(), Pad1);
			        }
			        // Si el ID es del segundo circulo
			        if(pointerID == pointerID2) {
			        	if(DEBUG) Log.i("BrazoRobotTouch", "Reset 2");
						//pad2.setX(xCenter2);
						pad2.setY(yCenter2);
						mBluetoothDataTransfer.btSendDataWithSync(pad2.toYBytes(), Pad2);
			        }
				}
			mView.redraw();
			
			//if(DEBUG) Log.i("BrazoRobotTouchC", "x1: " + pad1.getX() + "   y1: " + pad1.getY());
			//if(DEBUG) Log.i("BrazoRobotTouchC", "x2: " + pad2.getX() + "   y2: " + pad2.getY());
			
			// Duermo el Thread por 20mS para reducir uso de CPU
			try { Thread.sleep(20); } 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		return true;	// Si el valor aqui es 'false' se retorna y se espera a que se suelte y se toque de nuevo la pantalla
						// Si el valor aqui es 'true' nos da continuamente las coordenadas (permite deslizarnos sin clicks)
	}

}
