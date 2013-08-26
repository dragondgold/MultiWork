package com.protocolanalyzer.api.andres;

public class Clock extends Protocol{

	public Clock(long freq) {
		super(freq);
	}

	@Override
	public void decode(double startTime) {
	}

	@Override
	public ProtocolType getProtocol() {
		return ProtocolType.CLOCK;
	}
	
	/**
	 * Obtiene la frecuencia de clock en Hz calculada en base al tiempo de muestreo.
	 * @return frecuencia del clock; -1 si no se pudo calcular.
	 */
	public int getCalculatedFrequency (){
		// Resto entre dos flancos de subida (que me asegure antes que hubiera)
		// para calcular la frecuencia del clock
		int firstEdge, secondEdge;
		
		firstEdge = logicData.nextRisingEdge(0);
		if(firstEdge != -1) secondEdge = logicData.nextRisingEdge(firstEdge);
		else return -1;
		
		if(secondEdge != -1){
			return (int)(1/((secondEdge - firstEdge) * 1.0d/sampleFrec));
		}
		else return -1;
	}

}
