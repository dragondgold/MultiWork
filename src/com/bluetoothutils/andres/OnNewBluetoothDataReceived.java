package com.bluetoothutils.andres;

import java.io.InputStream;
import java.io.OutputStream;

public interface OnNewBluetoothDataReceived {

	/**
	 * @param mBTIn InputStream del Bluetooth
	 * @param mBTOut OutputStream del Bluetooth
	 * @return true si se continua el listener, false de otro modo
	 */
	public boolean onNewBluetoothDataReceivedListener (InputStream mBTIn, OutputStream mBTOut);
	
}
