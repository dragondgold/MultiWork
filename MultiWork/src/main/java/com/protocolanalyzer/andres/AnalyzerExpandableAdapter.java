package com.protocolanalyzer.andres;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.multiwork.andres.R;
import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

import java.util.ArrayList;

public class AnalyzerExpandableAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final ArrayList<String> mDetails;
    private final ActionSlideExpandableListView mListView;

    static class ViewHolder {
        TextView title;
        TextView details;
    }

    public AnalyzerExpandableAdapter(Context ctx, ArrayList<String> titles, ArrayList<String> details,
                                     ActionSlideExpandableListView listView){
        super(ctx, R.layout.expandable_list_item, titles);
        context = ctx;
        mDetails = details;
        mListView = listView;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        String headerTitle = getItem(position);
        ViewHolder mViewHolder;

        if(view == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.expandable_list_item, null);

            // Obtengo los TextView para no tener que llamar findViewById() cada vez que se ejecuta getView()
            mViewHolder = new ViewHolder();
            mViewHolder.title = (TextView) view.findViewById(R.id.title_view);
            mViewHolder.title.setTypeface(Typeface.createFromAsset(context.getAssets(), "SourceCodePro-Semibold.ttf"));

            mViewHolder.details = (TextView) view.findViewById(R.id.detail_view);
            mViewHolder.details.setTypeface(Typeface.createFromAsset(context.getAssets(), "SourceCodePro-Semibold.ttf"));

            // Guardo el ViewHolder en el View
            view.setTag(mViewHolder);
        }else{
            // Obtengo el ViewHolder
            mViewHolder = (ViewHolder) view.getTag();
        }

        if(mListView.isItemChecked(position)) view.setBackgroundResource(android.R.color.holo_blue_light);
        else view.setBackgroundResource(android.R.color.background_light);

        mViewHolder.title.setText(headerTitle);
        mViewHolder.details.setText(mDetails.get(position));

        return view;
    }
}
