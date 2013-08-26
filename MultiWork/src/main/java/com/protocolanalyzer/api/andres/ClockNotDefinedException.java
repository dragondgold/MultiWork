package com.protocolanalyzer.api.andres;

public class ClockNotDefinedException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	public ClockNotDefinedException (String message){
		super(message);
	}

}
