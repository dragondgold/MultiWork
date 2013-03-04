package com.bluetoothutils.andres;

import java.io.InputStream;
import java.io.OutputStream;

public interface OnNewBluetoothDataReceived {

	public void onNewBluetoothDataReceivedListener (InputStream mBTIn, OutputStream mBTOut);
	
}
