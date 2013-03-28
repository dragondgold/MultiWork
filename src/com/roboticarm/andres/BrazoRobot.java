package com.roboticarm.andres;

import com.bluetoothutils.andres.BTSingleSynchTransfer;
import com.multiwork.andres.MainMenu;
import com.multiwork.andres.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

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
	
	/** Código que obtengo en onActivityResult() al recibir el resultado de activar el bluetooth */
	private static final int REQUEST_ENABLE_BT = 1;
	private static BTSingleSynchTransfer mBTSingleSynchTransfer;
	
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
				mBTSingleSynchTransfer.btSendDataWithSync(new byte[] {(byte)servo1Angle}, Munieca);
			}
		});
		
		// Muñeca, gira derecha
		findViewById(R.id.rightGiro).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Button", "Button right");
				if(servo1Angle <= (servo1MaxAngle-angleStep)) servo1Angle += angleStep;
				mBTSingleSynchTransfer.btSendDataWithSync(new byte[] {(byte)servo1Angle}, Munieca);
			}
		});
		
		// Pinza, cierra
		findViewById(R.id.closeClaw).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Button", "Close claw");
				if(servo2Angle >= (servo2MinAngle+angleStep)) servo2Angle -= angleStep;
				mBTSingleSynchTransfer.btSendDataWithSync(new byte[] {(byte)servo2Angle}, Pinza);
			}
		});
		
		// Pinza, abre
		findViewById(R.id.openClaw).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Button", "Open claw");
				if(servo2Angle <= (servo2MaxAngle-angleStep)) servo2Angle += angleStep;
				mBTSingleSynchTransfer.btSendDataWithSync(new byte[] {(byte)servo2Angle}, Pinza);
			}
		});
		
		// Esto permite que se puedan tocar los View debajo de este View. El Canvas del joystick usa toda la pantalla
		// por lo que si tocamos sobre algún boton el sistema no lo toma ya que primero esta el Canvas de los
		// Joystick arriba. Esto permite que se toquen los botones abajo de este View. Viene desactivado por defecto
		// por seguridad. http://stackoverflow.com/questions/12398402/android-overlay-layout-on-top-of-all-windows-that-receives-touches
		mView.setFilterTouchesWhenObscured(false);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(DEBUG) Log.i("BrazoRobot", "onResume()");
		isSystemRdy = false;
		
		mBTSingleSynchTransfer = new BTSingleSynchTransfer(MainMenu.mOutputStream,
				MainMenu.mInputStream);
    	mBTSingleSynchTransfer.start();
	}
    
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
		if(isSystemRdy){
			if(DEBUG) Log.i("BrazoRobotTouch", "onTouch()");
			
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
						mBTSingleSynchTransfer.btSendDataWithSync(pad1.toYBytes(), Pad1);
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
						mBTSingleSynchTransfer.btSendDataWithSync(pad2.toYBytes(), Pad2);
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
						mBTSingleSynchTransfer.btSendDataWithSync(pad1.toYBytes(), Pad1);
			        }
			        // Si el ID es del segundo circulo
			        if(pointerID == pointerID2) {
			        	if(DEBUG) Log.i("BrazoRobotTouch", "Reset 2");
						//pad2.setX(xCenter2);
						pad2.setY(yCenter2);
						mBTSingleSynchTransfer.btSendDataWithSync(pad2.toYBytes(), Pad2);
			        }
				}
			mView.redraw();
			
			// Duermo el Thread por 20mS para reducir uso de CPU
			try { Thread.sleep(20); } 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		return true;	// Si el valor aqui es 'false' se retorna y se espera a que se suelte y se toque de nuevo la pantalla
						// Si el valor aqui es 'true' nos da continuamente las coordenadas (permite deslizarnos sin clicks)
	}

}
