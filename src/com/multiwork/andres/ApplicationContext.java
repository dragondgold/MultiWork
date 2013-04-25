package com.multiwork.andres;

import java.io.InputStream;
import java.io.OutputStream;

import com.bluetoothutils.andres.BluetoothHelper;

import android.app.Application;

public class ApplicationContext extends Application {

	public BluetoothHelper mBluetoothHelper = null;
	public InputStream mInputStream = null;
	public OutputStream mOutputStream = null;
	
}
