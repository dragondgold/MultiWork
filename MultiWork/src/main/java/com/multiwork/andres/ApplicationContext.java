package com.multiwork.andres;

import java.io.InputStream;
import java.io.OutputStream;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import com.bluetoothutils.andres.BluetoothHelper;

import android.app.Application;

@ReportsCrashes(
	formKey = "", // will not be used
	mailTo = "torti.max@gmail.com",
	mode = ReportingInteractionMode.NOTIFICATION,
	resToastText = R.string.Error,
    resNotifTickerText = R.string.crash_notif_ticker_text,
    resNotifTitle = R.string.crash_notif_title,
    resNotifText = R.string.crash_notif_text,
    resDialogText = R.string.crash_dialog_text,
    resDialogTitle = R.string.crash_dialog_title
)
public class ApplicationContext extends Application {

	public BluetoothHelper mBluetoothHelper = null;
	public InputStream mInputStream = null;
	public OutputStream mOutputStream = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		ACRA.init(this);
	}
	
}
