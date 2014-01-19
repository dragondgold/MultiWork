package com.roboticarm.andres;

public class Joystick{
	
	private float xCenter;
	private float yCenter;
	private float x;
	private float y;
	private float maxProportional;
	private float maxDistance;
	
	/**
	 * Retorna el valor proporcional a la distancia X desde el centro en bytes, primero el LSB, luego
	 * el MSB y luego 1 o 0 dependiendo de si la distancia es o no negativa
	 * @return
	 */
	public byte[] toXBytes () {
		return new byte[] { 	(byte)((int)getXProportionalToDistance() & 0xFF),
								(byte)(((int)getXProportionalToDistance() >> 8) & 0xFF),
								isXNegative() ? (byte)1 : (byte)0 };
	}
	
	/**
	 * Retorna el valor proporcional a la distancia Y desde el centro en bytes, primero el LSB, luego
	 * el MSB y luego 1 o 0 dependiendo de si la distancia es o no negativa
	 * @return
	 */
	public byte[] toYBytes () {
		return new byte[] { 	(byte)((int)getYProportionalToDistance() & 0xFF),
								(byte)(((int)getYProportionalToDistance() >> 8) & 0xFF),
								isYNegative() ? (byte)1 : (byte)0 };
	}
	
	/**
	 * Retorna el valor proporcional a la distancia X desde el centro en bytes, primero el LSB, luego
	 * el MSB y luego 1 o 0 dependiendo de si la distancia es o no negativa y luego se repite el mismo
	 * patrón para la distancia en Y
	 * @return
	 */
	public byte[] toXYBytes () {
		return new byte[] { 	(byte)((int)getXProportionalToDistance() & 0xFF),
								(byte)(((int)getXProportionalToDistance() >> 8) & 0xFF),
								isXNegative() ? (byte)1 : (byte)0,
										
								(byte)((int)getYProportionalToDistance() & 0xFF),
								(byte)(((int)getYProportionalToDistance() >> 8) & 0xFF),
								isYNegative() ? (byte)1 : (byte)0};
	}
	
	/**
	 * Obtiene un número de 0 al máximo configurado con setMaxProportional() de acuerdo a la
	 * distancia de la coordenada desde el centro
	 * @return
	 */
	public float getXProportionalToDistance(){
		return ((500f*getXDistanceFromCenter())/maxDistance);
	}
	
	/**
	 * Obtiene un número de 0 al máximo configurado con setMaxProportional() de acuerdo a la
	 * distancia de la coordenada desde el centro
	 * @return
	 */
	public float getYProportionalToDistance(){
		return ((500f*getYDistanceFromCenter())/maxDistance);
	}
	
	/**
	 * Obtiene el número mayor a ser devuelto al obtener el número proporcional a la distancia
	 * de la coordenada al centro
	 * @return
	 */
	public float getMaxProportional() {
		return maxProportional;
	}

	/**
	 * Setea el número mayor a ser devuelto al obtener el número proporcional a la distancia
	 * de la coordenada al centro, el máximo es 65535
	 * @param maxProportional
	 */
	public void setMaxProportional(float maxProportional) {
		if(maxProportional > 65535) maxProportional = 65535;
		else this.maxProportional = maxProportional;
	}

	
	/**
	 * Obtiene la distancia máxima desde el centro
	 * @return
	 */
	public float getMaxDistance() {
		return maxDistance;
	}

	/**
	 * Distancia máxima desde el centro
	 * @param maxDistance
	 */
	public void setMaxDistance(float maxDistance) {
		this.maxDistance = maxDistance;
	}

	/**
	 * Indica si la distancia en X es negativa o positiva
	 * @return true si es negativa, sino false
	 */
	public boolean isYNegative (){
		return ((y - yCenter) < 0);
	}
	
	/**
	 * Indica si la distancia en Y es negativa o positiva
	 * @return true si es negativa, sino false
	 */
	public boolean isXNegative (){
		return ((x - xCenter) < 0);
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
