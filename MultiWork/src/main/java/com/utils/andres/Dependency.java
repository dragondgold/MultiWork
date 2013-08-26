package com.utils.andres;

import java.util.HashMap;

public class Dependency{

	private int invalidationValue = -1;
	final String masterKey;
	final int masterValue;
	
	private HashMap<String, Pair<String, Integer>> dependencyMap = new HashMap<String, Pair<String, Integer>>();
	
	/**
	 * Crea una dependencia
	 * @param masterKey key maestra es la originaria desde donde se derivan las dependencias
	 * @param masterValue el valor que debe tener la llave maestra para que se evaluen las dependencias
	 * @param invalidationValue valor con el que se invalidan los valores de las keys en caso de que halla conflictos
	 */
	public Dependency(String masterKey, int masterValue, int invalidationValue) {
		this.masterKey = masterKey;
		this.masterValue = masterValue;
		this.invalidationValue = invalidationValue;
	}
	
	/**
	 * Agrega una dependencia secundaria que debe cumplirse si la dependencia maestra se cumple
	 * @param key
	 * @param value
	 */
	public void addSecondaryDependency (String key, int value){
		dependencyMap.put(key, new Pair<String, Integer>(null, value));
	}
	
	/**
	 * Agrega una dependencia secundaria referenciada que debe cumplirse si la dependencia maestra se cumple.
	 * El parámetro key debe llevar un asterisco '*' en donde se quiere reemplazar por el valor otorgado por la key 'key'.
	 * Un ejemplo, supongamos que el metodo es llamado así: addSecondaryReferencedDependency ("numero", "valor*", -30);
	 * 		Si el master key cumple la condición entonces se revisa el valor del key "numero", supongamos que el valor es 8,
	 * 			se revisa ahora "valor8" reemplazando el '*' por el valor del 'refKey' y si el valor del key "valor8" es -30 entonces
	 * 			la dependencia se cumple de otro modo no y se escribe en el key en este caso "numero" el valor de invalidación.
	 * @param refKey
	 * @param key
	 * @param value
	 */
	public void addSecondaryReferencedDependency (String refKey, String key, int value) {
		dependencyMap.put(key, new Pair<String, Integer>(refKey, value));
	}
	
	public String getMasterKey (){
		return masterKey;
	}
	
	public int getMasterValue (){
		return masterValue;
	}
	
	/**
	 * Obtiene un HashMap con todas las dependencias excepto la master
	 * @return
	 */
	public HashMap<String, Pair<String, Integer>> getDependencyMap(){
		return dependencyMap;
	}
	
	/**
	 * Valor con el cual se invalida en caso de que no se cumpla la dependencia
	 * @param value
	 */
	public void setInvalidationValue (int value) {
		invalidationValue = value;
	}
	
	public int getInvalidationValue (){
		return invalidationValue;
	}
	
}
