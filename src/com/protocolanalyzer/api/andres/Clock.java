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

}
