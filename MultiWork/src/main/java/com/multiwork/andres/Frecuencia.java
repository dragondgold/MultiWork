package com.multiwork.andres;

public class Frecuencia {

	public static final int Hz = 0;
	public static final int KHz = 1;
	public static final int MHz = 2;
	public static final int GHz = 3;
	
	private long Frequency;
	private boolean firstTime = true;
	
	private long maxFrequency = 0;
	private long minFrequency = 0;
	private int rango = Hz;
	
	/**
	 * Setea el el rango de la frecuencia (Hz, KHz, MHz, GHz)
	 * @param range (constantes definidas en esta clase)
	 */
	public void setRange(int range) {
		rango = range;
	}
	
	/**
	 * Obtiene el rango de la frecuencia (Hz, KHz, MHz, GHz)
	 */
	public int getRange() {
		return rango;
	}
	
	/** 
	 * Reinicia los m�ximos y m�nimos
	 */
	public void restart () {
		firstTime = true;
	}
	
	/**
	 * Setea la frecuencia en [Hz]
	 * @param freq, frecuencia en [Hz]
	 */
	public void setFrequency (long freq) {
		Frequency = freq;
		
		if(firstTime) {		// Si es la primera vez que seteo la frecuencia se toma como inicio para los m�ximos y m�nimos
			maxFrequency = freq;
			minFrequency = freq;
			firstTime = false;
		}
		if(freq > maxFrequency) maxFrequency = freq;
		else if(freq < minFrequency) minFrequency = freq;
	}
	
	/**
	 * Obtiene la frecuencia en [Hz]
	 * @return frecuencia en [Hz]
	 */
	public long getFrequencyHz () {
		return Frequency;
	}
	
	/**
	 * Obtiene la frecuencia en [KHz]
	 * @return frecuencia en [KHz]
	 */
	public double getFrequencyKHz () {
		return Frequency/1000.0d;
	}
	
	/**
	 * Obtiene la frecuencia en [MHz]
	 * @return frecuencia en [MHz]
	 */
	public double getFrequencyMHz () {
		return Frequency/1000000.0d;
	}
	
	/**
	 * Obtiene la frecuencia en [GHz]
	 * @return frecuencia en [GHz]
	 */
	public double getFrequencyGHz () {
		return Frequency/1000000000.0d;
	}
	
	/**
	 * Obtiene la frecuencia de acuerdo al rango seteado
	 * @return
	 */
	public String getStringFrequencyRanged() {
		switch(rango) {
			case Hz:		// Hz
				return ("" + getFrequencyHz());
			case KHz:		// KHz
				return String.format("%.1f", getFrequencyKHz());
			case MHz:		// MHz
				return String.format("%.1f", getFrequencyMHz());
			case GHz:
				return String.format("%.1f", getFrequencyGHz());
			default:
				return ("" + getFrequencyHz());
		}
	}
	
	/**
	 * Obtiene el per�odo
	 * @return per�odo en mS, uS o nS de acuerdo a la frecuencia siendo
	 * if(Frecuencia < 1000)	--> mS
	 * if(Frecuencia > 1000 && Frecuencia < 1000000)	--> uS
	 * else 					--> nS	
	 */
	public double getPeriod () {
		if(Frequency < 1000) return (1.0d/getFrequencyKHz());
		else if(Frequency > 1000 && Frequency < 1000000) return (1.0d/getFrequencyMHz());
		else return (1.0d/getFrequencyGHz());
	}
	
	/**
	 * Obtiene el per�odo en un String con su correspondiente unidad
	 * @return per�odo en mS, uS o nS de acuerdo a la frecuencia siendo
	 * if(Frecuencia < 1000)	--> mS
	 * if(Frecuencia > 1000 && Frecuencia < 1000000)	--> uS
	 * else 					--> nS	
	 * Ejemplo: "10.04 uS"
	 */
	public String getPeriodString () {
		if(Frequency < 1000) return ("" + String.format("%.4f", 1.0d/getFrequencyKHz()) + " mS");
		else if(Frequency > 1000 && Frequency < 1000000) return ("" + String.format("%.4f", 1.0d/getFrequencyMHz()) + " uS");
		else return ("" + String.format("%.4f", 1.0d/getFrequencyGHz()) + " nS");
	}
	
	/**
	 * Frecuencia m�xima hasta el momento en [Hz]
	 */
	public long getMaxFrec () {
		return maxFrequency;
	}
	
	/**
	 * Frecuencia m�nima hasta el momento [Hz]
	 */
	public long getMinFrec () {
		return minFrequency;
	}
	
}
