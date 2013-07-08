package com.protocolanalyzer.api.andres;

public class EmptyProtocol extends Protocol{

	public EmptyProtocol(long freq) {
		super(freq);
	}

	@Override
	public void decode(double startTime) {
		
	}

	@Override
	public ProtocolType getProtocol() {
		return ProtocolType.NONE;
	}

}
