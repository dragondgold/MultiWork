package com.protocolanalyzer.andres;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.multiwork.andres.R;

import java.util.ArrayList;

public class AnalyzerExpandableListView extends BaseExpandableListAdapter {

    private Context context;
    private ArrayList<String> mHeadersTitles;
    private ArrayList<String> mChildList;

    public AnalyzerExpandableListView(Context ctx, ArrayList<String> headersList, ArrayList<String> childList){
        context = ctx;
        mHeadersTitles = headersList;
        mChildList = childList;
    }

    @Override
    public int getGroupCount() {
        return mHeadersTitles.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return 1;
    }

    @Override
    public Object getGroup(int i) {
        return mHeadersTitles.get(i);
    }

    @Override
    public Object getChild(int i, int i2) {
        return mChildList.get(i);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i2) {
        return i2;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup viewGroup) {
        String headerTitle = (String) getGroup(groupPosition);

        if(view == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.group_row, null);
        }

        TextView title = (TextView) view.findViewById(R.id.tvGroupName);
        title.setTypeface(Typeface.createFromAsset(context.getAssets(), "SourceCodePro-Semibold.ttf"));
        title.setText(headerTitle);

        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean b, View view, ViewGroup viewGroup) {
        String childString = (String) getChild(groupPosition, childPosition);

        if(view == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.child_row, null);
        }

        TextView title = (TextView) view.findViewById(R.id.tvChild);
        title.setText(childString);

        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }
}
