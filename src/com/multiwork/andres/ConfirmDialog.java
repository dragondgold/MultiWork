package com.multiwork.andres;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;

public class ConfirmDialog extends Activity{
	
	private static final boolean DEBUG = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(DEBUG) Log.i("Dialog", "createDialogConfirm()");
		// Creo el dialogo con los dos botones
		AlertDialog.Builder confirm = new AlertDialog.Builder(this);
		confirm.setTitle(getString(R.string.AnalyzerDialogSaveTitle));
		confirm.setMessage(getString(R.string.AnalyzerDialogOverwrite));
		final Activity mActivity = this;
						
		// Boton Si
		confirm.setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {	
				mActivity.setResult(RESULT_OK, mActivity.getIntent());
				finish();
			}
		});
						
		// Boton No
		confirm.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				mActivity.setResult(RESULT_CANCELED, mActivity.getIntent());
				finish();
			}
		});
		
		// Al cancelar el dialogo (sacarlo con el boton de atras por ejemplo) termino la Activity con finish() sino queda el título de la
		// Activity mostrandose
		confirm.setOnCancelListener(new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.i("DialogIntent", "onCancel()");
				finish();
			}
		});
		
		confirm.show();
		
	}

}
