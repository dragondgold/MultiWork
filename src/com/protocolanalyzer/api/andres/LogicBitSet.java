package com.protocolanalyzer.api.andres;

import java.util.BitSet;

/**
 * Esta clase es una extensión de la clase BitSet. Agrega funcionalidades como busqueda de flancos de subida
 * y bajada y la mitad de un bit en alto
 */
public class LogicBitSet extends BitSet{

	private static final long serialVersionUID = 1L;
	private int currentSize;

	@Override
	public void set(int index, boolean state) {
		if(index > currentSize) currentSize = index;
		super.set(index, state);
	}

	@Override
	public void set(int index) {
		if(index > currentSize) currentSize = index;
		super.set(index);
	}
	
	@Override
	public void set(int fromIndex, int toIndex, boolean state) {
		if(toIndex > currentSize) currentSize = toIndex;
		super.set(fromIndex, toIndex, state);
	}

	@Override
	public void set(int fromIndex, int toIndex) {
		if(toIndex > currentSize) currentSize = toIndex;
		super.set(fromIndex, toIndex);
	}

	@Override
	public void clear() {
		currentSize = 0;
		super.clear();
	}

	@Override
	public void clear(int index) {
		if(index > currentSize) currentSize = index;
		super.clear(index);
	}
	
	@Override
	public void clear(int fromIndex, int toIndex) {
		if(toIndex > currentSize) currentSize = toIndex;
		super.clear(fromIndex, toIndex);
	}

	/**
	 * Index del último bit que se puso a '1' o '0'
	 */
	@Override
	public int length() {
		return currentSize;
	}

	/**
	 * Constructor
	 */
	LogicBitSet(){
		super();	// Creo un BitSet
		currentSize = 0;
	}
	
	/**
	 * Constructor con tamaño del BitSet
	 * @param size es el tamaño del BitSet
	 */
	LogicBitSet(int size){
		super(size);
	}
	
	/**
	 * Busca el index donde hay un flanco de bajada (devuelve donde se detecto ya el 0)
	 * @param index desde donde se debe empezar a buscar
	 * @return index del flanco de bajada, si no hay mas flancos de bajada retorna -1
	 */
	public int nextFallingEdge(int index){
		if(index >= 0) {
			int t = super.nextSetBit(index);
			if(t != -1) return super.nextClearBit(t);
		}
		return -1;
	}
	
	/**
	 * Busca el index donde hay un flanco de subida (devuelve donde se detecto ya el 1)
	 * @param index desde donde se debe empezar a buscar
	 * @return index del flanco de subida, si no hay mas flancos de subida retorna -1
	 */
	public int nextRisingEdge(int index){
		if(index >= 0) {
			return super.nextSetBit( super.nextClearBit(index) );
		}
		return -1;
	}
	
	/**
	 * Busca el siguiente bit y devuelve su index en el medio del mismo
	 * @param index desde donde se debe buscar el bit
	 * @return index del bit en su mitad, si no hay ninguno retorna -1
	 */
	public int nextSetBitToTest(int index) {
		int rising = nextRisingEdge(index);
		int fall = nextFallingEdge(rising);
		
		// Me aseguro que sean validos
		if(rising == -1 || fall == -1) return -1;
				
		return ( rising + ((fall - rising)/2) );
	}

}
