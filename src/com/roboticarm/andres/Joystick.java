package com.roboticarm.andres;

public class Joystick {
	
	private float xCenter;
	private float yCenter;
	private float x;
	private float y;
	
	/**
	 * Indica si la distancia en X es negativa o positiva
	 * @return true si es negativa, sino false
	 */
	public boolean isYNegative (){
		return ((y - yCenter) < 0) ? true : false;
	}
	
	/**
	 * Indica si la distancia en Y es negativa o positiva
	 * @return true si es negativa, sino false
	 */
	public boolean isXNegative (){
		return ((x - xCenter) < 0) ? true : false;
	}
	
	/**
	 * Obtiene la distancia desde la cordenada al centro en X
	 */
	public float getXDistanceFromCenter (){
		return Math.abs(x - xCenter);
	}
	
	/**
	 * Obtiene la distancia desde la cordenada al centro en Y
	 */
	public float getYDistanceFromCenter (){
		return Math.abs(y - yCenter);
	}
	
	/**
	 * Obtiene la coordenada X del centro
	 */
	public float getXCenter (){
		return xCenter;
	}
	
	/**
	 * Obtiene la coordenada Y del centro
	 */
	public float getYCenter (){
		return yCenter;
	}
	
	/**
	 * Obtiene la coordenada x actual del joystick
	 */
	public float getX (){
		return x;
	}
	
	/**
	 * Obtiene la coordenada y actual del joystick
	 */
	public float getY (){
		return y;
	}
	
	/**
	 * Setea la coordenada x del centro
	 */
	public void setXCenter (float xC){
		xCenter = xC;
	}
	
	/**
	 * Setea la coordenada y del centro
	 */
	public void setYCenter (float yC){
		yCenter = yC;
	}
	
	/**
	 * Setea la coordenada x del joystick
	 */
	public void setX (float xP){
		x = xP;
	}
	
	/**
	 * Setea la coordenada y del joystick
	 */
	public void setY (float yP){
		y = yP;
	}
}
