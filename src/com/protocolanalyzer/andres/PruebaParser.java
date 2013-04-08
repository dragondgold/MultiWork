package com.protocolanalyzer.andres;

import java.util.BitSet;

import com.protocolanalyzer.api.andres.I2CDecoder;
import com.protocolanalyzer.api.andres.LogicData;
import com.protocolanalyzer.api.andres.LogicHelper;
import com.protocolanalyzer.api.andres.UARTDecoder;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class PruebaParser extends Activity {
	
	private static final int UART = 0;
	private static final int I2C = 1;
	private static final int state = I2C;

	@SuppressWarnings("unused")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView text = new TextView(this);
		text.setText("Strings decodificados del protocolo " + I2C + " :\n");
		text.setMovementMethod(new ScrollingMovementMethod());
		
		setContentView(text);
		
		if(state == UART){
			BitSet dataUART;
			LogicData channel = new LogicData();
			
			dataUART = LogicHelper.bitParser("1101101010011", 21, 1);
			
			channel.setBaudRate(9600);			// 9600 Baudios
			LogicData.setSampleRate(200000);	// 200KHz
			channel.setLogicBits(dataUART);		// Copio el BitSet al canal
			channel.set9BitsTransmission(true);
			
			UARTDecoder.UARTDecode(channel);
	
			Log.i("Parser", "Strings Decoded");
			for(int n = 0; n < channel.getStringCount(); ++n){
				Log.i("Parser", "String " + n + ": " + channel.getString(n));
				Log.i("Parser", "String " + n + " position: " + channel.getPositionAt(n));
				// Escribo en el TextView
				text.append("\nString " + n + ": " + channel.getString(n));
				text.append("\nString " + n + " position: " + channel.getPositionAt(n)[0]);
			}
		}
		else if(state == I2C){
			BitSet dataI2C, clock;
			LogicData dataChannel, clockChannel;
			
			Log.i("Parser", "Parsing");

			//								  S		  Adress       A 		Byte		A  ST
			dataI2C = LogicHelper.bitParser("100 11010010011100101 0 111010011110000111 0 001", 5, 300);
			clock = LogicHelper.bitParser(  "110 01010101010101010 1 001010101010101010 1 011", 5, 300);
			
			dataChannel = new LogicData();
			clockChannel = new LogicData();
			
			LogicData.setSampleRate(2000000);
			dataChannel.setLogicBits(dataI2C);
			clockChannel.setLogicBits(clock);
			
			Log.i("Parser", "Parsed");	
			text.append("Data:  100-11010010011100101-0-111010011110000111-0-001\n");
			text.append("Clock: 110-01010101010101010-1-001010101010101010-1-011\n");
				
			long start = System.nanoTime();
			Log.i("Parser", "Decoding");
			I2CDecoder.i2cProtocolDecode(dataChannel, clockChannel);
			Log.i("Parser", "Decoded");	
			long stop = System.nanoTime();
			
			for(int n = 0; n < dataChannel.getStringCount(); ++n){
				Log.i("Parser", "String " + n + ": " + dataChannel.getString(n));
				Log.i("Parser", "String " + n + " position: " + dataChannel.getPositionAt(n)[0]*1000000 + " uS");
				// Escribo en el TextView
				text.append("\nString " + n + ": " + dataChannel.getString(n));
				text.append("\nString " + n + " position: " + dataChannel.getPositionAt(n)[0]*1000000 + " uS");
			}
			text.append("\nData Decoded in " + ((stop-start)/1000000) + " mS");

			Log.i("Parser", "Strings Decoded in " + ((stop-start)/1000000) + " mS");
			Toast.makeText(this, "Data Decoded in " + ((stop-start)/1000000) + " mS", Toast.LENGTH_LONG).show();
		}
		
	}

}
