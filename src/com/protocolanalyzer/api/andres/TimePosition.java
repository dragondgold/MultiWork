package com.protocolanalyzer.api.andres;

public class TimePosition {

	private double[] time = new double[2];
	private String text;
	
	/**
	 * Constructor
	 * @param text String a agregar
	 * @param startTime tiempo incial
	 * @param stopTime tiempo final
	 */
	TimePosition(String text, double startTime, double stopTime){
		time[0] = startTime;
		time[1] = stopTime;
		this.text = text;
	}
	
	/**
	 * Devuelve un array de double con dos valores, en 0 es el tiempo de inicio y en 1 tiempo final
	 * @return
	 */
	public double[] getTimes(){
		return time;
	}
	
	/**
	 * Devuelve el String
	 * @return
	 */
	public String getString(){
		return text;
	}
	
}

