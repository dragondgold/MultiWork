package com.multiwork.andres;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class IntentCaller{

	static String TAG;
	
	static void callIntent (String source, Context ctx,  int ItemPosition) throws ClassNotFoundException{
		
		String[] classNames = {"LCView", "FrecView", "LogicAnalizerView"};	//nombre de las clases
		
		Log.i(TAG, "Context Class: " + ctx.getClass().toString() + " Source: " + source + " Target: " + classNames[ItemPosition]
		+ " -> callIntent()");
		
		if(!source.toString().equals(classNames[ItemPosition])){			//si la clase no se llama a si misma
			Class<?> myClass = Class.forName("com.multiwork.andres." + classNames[ItemPosition]);	//creo una clase con el nombre a llamar
			Intent i = new Intent(ctx, myClass);			//creo el intent con el Context que me pasaron y el nombre de la clase
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);		//si la Activity ya esta ejecutada voy a ella no instancio una nueva
			ctx.startActivity(i);							//ejecuto la Activity con el contexto que me pasaron
			return;
		}
		
	}
	
}
