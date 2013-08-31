package com.protocolanalyzer.andres;

import com.protocolanalyzer.api.Protocol;

public interface OnDataDecodedListener {

	/**
	 * Pasa los datos decodificados de cada canal
	 * @param data son los LogicData con la informacion de cada canal
	 * @param isConfig si es true se esta indicando que hubo un cambio en la configuracion
	 */
	public void onDataDecodedListener(Protocol[] data, boolean isConfig);

}
