package com.roboticarm.andres;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class JoystickView extends View{
	
	private static Joystick J1, J2;
	private static float staticRadio, joystickRadio;
	
	private static Paint mCirclePaint = new Paint();
	private static Paint mJoystickPaint = new Paint();
	
	/** Color de los circulos estaticos */
	protected static final int circleColor = Color.WHITE;
	/** Color de los joystick */
	protected static final int joystickColor = Color.RED;
	/** Grosor de los circulos estaticos */
	protected static final float circleStroke = 8;
	
	public JoystickView(Context context) {
		super(context);
		
		// Propiedades de los circulos est�ticos
		mCirclePaint = new Paint();
		mCirclePaint.setColor(circleColor);
		mCirclePaint.setStrokeWidth(circleStroke);
		mCirclePaint.setStyle(Style.STROKE);
		mCirclePaint.setAntiAlias(true);
		
		// Propiedades de los circulos
		mJoystickPaint = new Paint();
		mJoystickPaint.setColor(joystickColor);
		mJoystickPaint.setAntiAlias(true);
		mJoystickPaint.setStyle(Style.FILL);
	}
	
	/**
	 * Configuro los parametros necesarios
	 * @param sR radio del círculo estático
	 * @param jR radio del círculo del joystick
	 * @param j1 Joystick 1
	 * @param j2 Joystick 2
	 */
	public void setConfigurations (float sR, float jR, Joystick j1, Joystick j2){
		staticRadio = sR;
		joystickRadio = jR;
		
		J1 = j1;
		J2 = j2;
	}
	
	/**
	 * Redibuja el View
	 */
	public void redraw(){
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// Dibujo los círculos
		canvas.drawCircle(J1.getXCenter(), J1.getYCenter(), staticRadio, mCirclePaint);
		canvas.drawCircle(J2.getXCenter(), J2.getYCenter(), staticRadio, mCirclePaint);
		
		// Dibujo los Joystick en las posiciones dadas
		canvas.drawCircle(J1.getX(), J1.getY(), joystickRadio, mJoystickPaint);
		canvas.drawCircle(J2.getX(), J2.getY(), joystickRadio, mJoystickPaint);
		
	}

	
	
}
