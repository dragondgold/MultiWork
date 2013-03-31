package com.protocolanalyzer.andres;

import com.protocolanalyzer.api.andres.LogicData;

public interface OnDataDecodedListener {

	/**
	 * Pasa los datos decodificados de cada canal
	 * @param mLogicData son los LogicData con la informacion de cada canal
	 * @param samplesCount numero de muestreos que se tomaron
	 * @param isConfig si es true se esta indicando que hubo un cambio en la configuracion
	 * @return el tiempo actual que lleva el grafico
	 */
	public double onDataDecodedListener(LogicData[] mLogicData, int samplesCount, boolean isConfig);

}
