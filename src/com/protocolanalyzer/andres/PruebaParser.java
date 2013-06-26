package com.protocolanalyzer.andres;

import com.protocolanalyzer.api.andres.Clock;
import com.protocolanalyzer.api.andres.I2CProtocol;
import com.protocolanalyzer.api.andres.LogicBitSet;
import com.protocolanalyzer.api.andres.LogicHelper;
import com.protocolanalyzer.api.andres.UARTProtocol;
import com.protocolanalyzer.api.andres.Protocol.ProtocolType;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class PruebaParser extends Activity {
	
	private static final ProtocolType mType = ProtocolType.I2C;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView text = new TextView(this);
		text.setText("Strings decodificados del protocolo " + mType + " :\n");
		text.setMovementMethod(new ScrollingMovementMethod());
		
		setContentView(text);
		
		long start, end;
		
		if(mType == ProtocolType.UART){
			LogicBitSet data;
			UARTProtocol channelUART = new UARTProtocol(200000);
			
			Log.i("Parser", "Parsing");
			data = LogicHelper.bitParser("1101101010011", 21, 1);
			
			channelUART.setBaudRate(9600);			// 9600 Baudios
			channelUART.setChannelBitsData(data);	// Bits
			channelUART.set9BitsMode(false);
			
			start = System.currentTimeMillis();
			Log.i("Parser", "Decoding");
			channelUART.decode(0);
			Log.i("Parser", "Decoded");	
			end = System.currentTimeMillis();
	
			Log.i("Parser", "Data decoded in: " + (end-start) + " mS");
			for(int n = 0; n < channelUART.getDecodedData().size(); ++n){
				Log.i("Parser", "String " + n + ": " + channelUART.getDecodedData().get(n).getString());
				Log.i("Parser", "String " + n + " position: " + channelUART.getDecodedData().get(n).startTime());
				// Escribo en el TextView
				text.append("\nString " + n + ": " + channelUART.getDecodedData().get(n).getString());
				text.append("\nString " + n + " position: " + channelUART.getDecodedData().get(n).startTime());
			}
		}
		else if(mType == ProtocolType.I2C){
			LogicBitSet dataI2C, clkI2C;
			
			I2CProtocol channelI2C = new I2CProtocol(200000);
			Clock clockI2C = new Clock(200000);

			Log.i("Parser", "Parsing");
			//								  S		  Address        A 		Byte		  A  ST
			dataI2C = LogicHelper.bitParser("100  11010010011100101  0 111010011110000111 0 001", 5, 300);
			clkI2C = LogicHelper.bitParser( "110  01010101010101010  1 001010101010101010 1 011", 5, 300);
			
			channelI2C.setChannelBitsData(dataI2C);
			channelI2C.setClockSource(clockI2C);
			clockI2C.setChannelBitsData(clkI2C);
			
			Log.i("Parser", "Parsed");	
			text.append("Data:  100-11010010011100101-0-111010011110000111-0-001\n");
			text.append("Clock: 110-01010101010101010-1-001010101010101010-1-011\n");
				
			start = System.currentTimeMillis();
			Log.i("Parser", "Decoding");
			channelI2C.decode(0);
			Log.i("Parser", "Decoded");	
			end = System.currentTimeMillis();
			
			text.append("Data Decoded in " + (end-start) + " mS\n");
			
			for(int n = 0; n < channelI2C.getDecodedData().size(); ++n){
				Log.i("Parser", "String " + n + ": " + channelI2C.getDecodedData().get(n).getString());
				Log.i("Parser", "String " + n + " position: " + channelI2C.getDecodedData().get(n).startTime()*1000 + " uS");
				// Escribo en el TextView
				text.append("\nString " + n + ": " + channelI2C.getDecodedData().get(n).getString());
				text.append("\nString " + n + " position: " + channelI2C.getDecodedData().get(n).startTime()*1000 + " uS");
			}
			Log.i("Parser", "Data Decoded in " + (end-start) + " mS");;
			Toast.makeText(this, "Data Decoded in " + (end-start) + " mS", Toast.LENGTH_LONG).show();
		}
		
	}

}
