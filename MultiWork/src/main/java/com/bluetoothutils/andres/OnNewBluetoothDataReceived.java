package com.bluetoothutils.andres;

import java.io.InputStream;
import java.io.OutputStream;

public interface OnNewBluetoothDataReceived {

	/**
	 * Esta interface se ejecuta en un Thread diferente para no bloquear el UI al recibir los datos por
	 * lo que si se desea modificar la UI debe hacerse desde el Thread correspondiente
	 * @param mBTIn InputStream del Bluetooth
	 * @param mBTOut OutputStream del Bluetooth
	 * @return true si se continua el listener, false de otro modo
	 */
	public boolean onNewBluetoothDataReceivedListener (InputStream mBTIn, OutputStream mBTOut);
	
}
