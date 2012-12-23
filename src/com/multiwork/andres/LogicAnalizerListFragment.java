package com.multiwork.andres;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LogicAnalizerListFragment extends SherlockFragment{

	private static final boolean DEBUG = true;
	
	private static SherlockFragmentActivity mActivity;
	private static ActionBar mActionBar;
	private static TextView mRawData;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mActivity = getSherlockActivity();
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.logic_rawdata, container, false);
		mRawData = (TextView) v.findViewById(R.id.tvRawDataLogic); 
		return v;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actionbarmain, menu);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(DEBUG) Log.i("onResume()","Resume LogicAnalizerView");
		
		mActivity.invalidateOptionsMenu();
	}
	
}
