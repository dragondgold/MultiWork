package com.utils.andres;

import java.util.ArrayList;
import java.util.Map;

import android.content.SharedPreferences;

public class ConflictChecker {
	
	ArrayList<Dependency> list = new ArrayList<Dependency>();
	SharedPreferences mPreferences;
	
	/**
	 * Detector de conflicto en base a las dependencias
	 * @param mSharedPreferences preferencia a usar
	 */
	public ConflictChecker(SharedPreferences mSharedPreferences) {
		mPreferences = mSharedPreferences;
	}
	
	public void addDependency (Dependency mDependency) {
		list.add(mDependency);
	}
	
	/**
	 * Detecta conflictos y los repara en las preferencias, para que una dependencia se cumpla se debe cumplir la dependencia maestra
	 * y las sub-dependencias, de otro modo se las anula con el valor especificado en cada dependencia
	 * @return
	 */
	public boolean detectConflicts () {
		
		boolean conflictsCorrected = false;
		
		for(Dependency mDependency : list){
			// Si el master key cumple con la dependencia
			String masterValue = mPreferences.getString(mDependency.getMasterKey(), null);
			
			if (masterValue != null && Integer.valueOf(masterValue) == mDependency.getMasterValue()) {
				for(Map.Entry<String, Pair<String, Integer>> entry : mDependency.getDependencyMap().entrySet()){
					String key = entry.getKey();
					String refKey = entry.getValue().getFirst();
					int value = entry.getValue().getSecond();
					
					if (refKey != null) {
						if(mPreferences.getString(refKey, null) == null){
							//throw new NullPointerException("El key de referencia no existe, asegúrese de que exista");
                            invalidateDependency(mDependency, refKey, mDependency.getInvalidationValue());
                            conflictsCorrected = true;
                        }else{
							int refValue = Integer.valueOf(mPreferences.getString(refKey, null));
							String targetKey = key.replace("*", ""+refValue);

							try {
								if(Integer.valueOf(mPreferences.getString(targetKey, null)) != value){
									invalidateDependency(mDependency, refKey, mDependency.getInvalidationValue());
									conflictsCorrected = true;
								}
							} catch (NumberFormatException e) {
								invalidateDependency(mDependency, refKey, mDependency.getInvalidationValue());
								conflictsCorrected = true;
                            }
                        }
					}
					else{
						String keyString = mPreferences.getString(key, null);
						if(keyString != null && Integer.valueOf(keyString) != value){
							invalidateDependency(mDependency, null, mDependency.getInvalidationValue());
							conflictsCorrected = true;
						}
					}
				}
			}
		}
		return conflictsCorrected;
	}
	
	private void invalidateDependency (Dependency mDependency, String refKey, int invalidateValue){
		mPreferences.edit().putString(mDependency.getMasterKey(), ""+invalidateValue).apply();
        // Si no es null cambio directamente el valor del key de referencia
		if(refKey != null){
			mPreferences.edit().putString(refKey, ""+invalidateValue).apply();
		}
		
		for(String key : mDependency.getDependencyMap().keySet()){
			if(!key.contains("*")) mPreferences.edit().putString(key, ""+invalidateValue).apply();
		}
	}

}
