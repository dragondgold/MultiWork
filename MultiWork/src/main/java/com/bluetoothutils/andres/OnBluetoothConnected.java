package com.bluetoothutils.andres;

import java.io.InputStream;
import java.io.OutputStream;

public interface OnBluetoothConnected {

	public void onBluetoothConnected (InputStream mInputStream, OutputStream mOutputStream);
	
}
