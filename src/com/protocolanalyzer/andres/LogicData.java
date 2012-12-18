package com.protocolanalyzer.andres;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Esta clase contiene una lista de Strings que indica los datos decodificados de los siguiente protocolos, aquí se almacenan
 * los datos bits con la clase LogicBit y se indica tipo de protocolo, frecuencia de clocks, velocidad de muestreo, etc
 * @author andres
 */
public class LogicData {
	
	/** Tipos de protocolo */
	public enum Protocol{
		UART, I2C, SPI, CLOCK, NONE
	}
	
	/** Contiene un String con las posiciones iniciales y finales del mismo en el tiempo */
	private List<TimePosition> mData = new ArrayList<TimePosition>();
	/** Bits para ser decodificados */
	private LogicBit data = new LogicBit();
	/** Protocolo usado */
	private Protocol mProtocol;
	/** Velocidad en baudios en caso del protocolo UART */
	private long baudRate;
	/** Determina si la comunicación del UART transmite el noveno bit o no */
	private boolean is9Bits = false;
	/** Numero de canal de donde viene el clock del protocolo (si tiene) */
	private int clockSource;
	/** Tiempo desde que se comienza a decodificar */
	private double startTime;
	/** Frecuencia del clock en protocolos como I2C, SPI, USART, etc */
	private double clockFrequency;
	/** Numero de muestras por segundo [Hz]*/
	private static long sampleRate;
	/** Numero de canales creados */
	private static int channelNumbers;
	
	/**
	 * Constructor
	 */
	public LogicData (){
		++channelNumbers;	// Incrementa el numero de canales creados
	}
	
	/**
	 * Destructor
	 */
	@Override
	protected void finalize() throws Throwable {
		--channelNumbers;	// Elimino un canal creado
		super.finalize();
	}
	
	/**
	 * Obtiene la frecuencia del clock en protocolos como I2C, SPI, USART, etc
	 * @return frecuancia en [Hz]
	 */
	public double getClockFrequency() {
		return clockFrequency;
	}
	
	/** 
	 * Retorna un LogicBit (BitSet con propiedades agregadas) con todos los bits del canal
	 * @return LogicBit
	 */
	public LogicBit getBits () {
		return data;
	}
	
	/**
	 * Coloca un bit a 1 en la posicion indicada
	 * @param index
	 */
	public void setBit (int index) {
		data.set(index);
	}
	
	public void setLogicBits (BitSet bit){
		data = new LogicBit();
		for(int n=0; n < bit.length(); ++n){
			data.set(n, bit.get(n));
		}
	}
	
	/**
	 * Coloca un bit a 0 en la posicion indicada
	 * @param index
	 */
	public void clearBit (int index) {
		data.clear(index);
	}
	
	/**
	 * Setea el protocolo a usar
	 * @author Andres Torti
	 * @param type es un tipo de protocolo (ej: Protocol.I2C/SPI...)
	 */
	public void setProtocol (Protocol type){
		mProtocol = type;
	}
	
	/**
	 * Obtiene el protocolo
	 * @author Andres Torti
	 * @return tipo de protocolo del tipo Protocol (ej: Protocol.I2C/SPI...)
	 */
	public Protocol getProtocol (){
		return mProtocol;
	}
	
	/** 
	 * Define la velocidad en baudios del protocolo UART
	 * @param baudios
	 */
	public void setBaudRate (long baudios) {
		baudRate = Math.abs(baudios);
	}
	
	/**
	 * Obtiene la velocidad en Baudios del protocolo UART
	 * @return baudios
	 */
	public long getBaudRate () {
		return baudRate;
	}
	
	/**
	 * Determina si la transmicion del UART enviara 9 bits de dato
	 * @param state true envia 9 bits, false de otro modo
	 */
	public void set9BitsTransmission (boolean state){
		is9Bits = state;
	}
	
	/**
	 * Comprueba si la transmicion es de 9 bits
	 * @return true si es de 9 bits, false de otro modo
	 */
	public boolean is9BitsTransmission (){
		return is9Bits;
	}
	
	/**
	 * Setea el canal que actua como fuente de clock
	 * @author Andres Torti
	 * @param sourceChannel es el numero del canal (de 1 en adelante)
	 */
	public void setClockSource (int sourceChannel){
		if(sourceChannel < 0) clockSource = 1;
		else if(sourceChannel > channelNumbers) clockSource = channelNumbers;
		else clockSource = sourceChannel;
	}
	
	/**
	 * Obtiene el canal que actua como fuente de clock
	 * @author Andres Torti
	 * @return int, numero del canal
	 */
	public int getClockSource (){
		return clockSource;
	}
	
	/**
	 * Define el tiempo en el que se inicia el muestreo (0 o otro numero)
	 * @param time
	 */
	public void setStartTime (double time) {
		startTime = Math.abs(time);
	}
	
	/**
	 * Obtiene el tiempo en el que se inicio el muestreo
	 */
	public double getStartTime () {
		return startTime;
	}
	
	/**
	 * Setea la velocidad de muestreo en Hz
	 * @param Hz_Frequency frecuencia de muestreo en Hz
	 */
	public static void setSampleRate (long Hz_Frequency) {
		sampleRate = Math.abs(Hz_Frequency);
	}
	
	/**
	 * Obtiene la frecuencia de muestreo en Hz
	 * @return frecuencia de muestreo en Hz
	 */
	public static long getSampleRate () {
		return sampleRate;
	}
	

	/**
	 * Agrega un String en la posicion dada
	 * @param text String a agregar
	 * @param startTime tiempo de inicio
	 * @param stopTime tiempo final
	 */
	public void addString (String text, double startTime, double stopTime){
		if(stopTime >= startTime){
			mData.add(new TimePosition(text, startTime, stopTime));
		}else{
			mData.add(new TimePosition(text, startTime, startTime));
		}
	}
	
	
	/**
	 * Agrega un String en la posicion dada tomando el tiempo de inicio configurado en el canal
	 * @param text String a agregar
	 * @param startTime tiempo de inicio
	 * @param stopTime tiempo final
	 */
	public void addStringS (String text, double startTime, double stopTime){
		if(stopTime >= startTime){
			mData.add(new TimePosition(text, startTime+this.startTime, stopTime+this.startTime));
		}else{
			mData.add(new TimePosition(text, startTime+this.startTime, startTime+this.startTime));
		}
	}
	
	/**
	 * Obtiene un string almacenado
	 * @author Andres Torti
	 * @param index es la posicion del String a obtener
	 * @return String en la posicion dada
	 */
	public String getString (int index){
		return mData.get(index).getString();
	}
	
	/**
	 * Obtiene la cantidad de String
	 * @author Andres Torti
	 * @return int, cantidad de String agregados
	 */
	public int getStringCount (){
		return mData.size();
	}
	
	/**
	 * Obtiene la posicion en el tiempo
	 * @author Andres Torti
	 * @param index donde obtener la posicion
	 * @return double[] con las posiciones de inicio y final, siendo la posicion 0 del array el inicio y 1 final
	 */
	public double[] getPositionAt (int index){
		return mData.get(index).getTimes();
	}
	
	/**
	 * Elimina todos los datos decodificados
	 */
	public void clearDecodedData() {
		mData.clear();
	}
	
	/**
	 * Elimina los bits del canal
	 */
	public void clearDataBits() {
		if(data != null)
			data.clear();
	}
	
	/**
	 * Elimina los bits del canal y libera la memoria usada
	 */
	public void freeDataMemory() {
		data = new LogicBit();
	}

	/**
	 * Obtiene la cantidad de muestreos almacenados
	 */
	public int getSize() {
		return data.length();
	}
}
