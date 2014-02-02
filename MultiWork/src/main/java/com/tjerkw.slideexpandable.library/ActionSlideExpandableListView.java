package com.tjerkw.slideexpandable.library;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * A more specific expandable listview in which the expandable area
 * consist of some buttons which are context actions for the item itself.
 *
 * It handles event binding for those buttons and allow for adding
 * a listener that will be invoked if one of those buttons are pressed.
 *
 * @author tjerk
 * @date 6/26/12 7:01 PM
 */
public class ActionSlideExpandableListView extends SlideExpandableListView {

	private OnActionClickListener listener;
    private ActionMode mActionMode;

	private int[] buttonIds = null;

    private MultiChoiceModeListener multiChoiceModeListener;
    private boolean modalMode = false;
    private boolean actionModeEnabled = false;
    private ListAdapter mAdapter;

    private int itemCheckedCount = 0;
    private SparseBooleanArray checkedArray = new SparseBooleanArray();

	public ActionSlideExpandableListView(Context context) {
		super(context);
	}

	public ActionSlideExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ActionSlideExpandableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

    @Override
    public void setItemChecked(int position, boolean value) {
        if(value) ++itemCheckedCount;
        else{
            if(itemCheckedCount != 0) --itemCheckedCount;
        }

        checkedArray.put(position, value);
    }

    @Override
    public boolean isItemChecked(int position) {
        return checkedArray.get(position);
    }

    @Override
    public int getCheckedItemCount() {
        return itemCheckedCount;
    }

    public boolean isActionModeEnabled(){
        return actionModeEnabled;
    }

    private void setInternalListeners(){
        ((AbstractSlideExpandableListAdapter)getAdapter()).setOnActionItemClickListener(new OnActionItemClickListener() {
            @Override
            public void onItemClick(View itemView, View clickedView, int position) {
                if(modalMode && actionModeEnabled){
                    setItemChecked(position, !isItemChecked(position));

                    if(mAdapter instanceof ArrayAdapter)
                        ((ArrayAdapter)mAdapter).notifyDataSetChanged();
                    else if(mAdapter instanceof BaseAdapter)
                        ((BaseAdapter)mAdapter).notifyDataSetChanged();

                    /*
                    View v = ((ListView)itemView).getChildAt(position);

                    if(isItemChecked(position)) v.setBackgroundResource(android.R.color.holo_blue_light);
                    else v.setBackgroundResource(android.R.color.background_light);*/

                    multiChoiceModeListener.onItemCheckedStateChanged(mActionMode, position, clickedView.getId(), isItemChecked(position));
                }
            }
        });

        ((AbstractSlideExpandableListAdapter)getAdapter()).setOnActionItemLongClickListener(new OnActionLongItemClickListener() {
            @Override
            public void onLongItemClick(final View itemView, View clickedView, final int position) {
                if(modalMode && !actionModeEnabled){
                    mActionMode = startActionMode(new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                            Log.i("ListExpandable", "createActionMode()");
                            if(multiChoiceModeListener.onCreateActionMode(actionMode, menu)){
                                actionModeEnabled = true;

                                setItemChecked(position, true);
                                if(mAdapter instanceof ArrayAdapter)
                                    ((ArrayAdapter)mAdapter).notifyDataSetChanged();
                                else if(mAdapter instanceof BaseAdapter)
                                    ((BaseAdapter)mAdapter).notifyDataSetChanged();
                                /*
                                View v = ((ListView)itemView).getChildAt(position);
                                v.setBackgroundResource(android.R.color.holo_blue_light);*/

                                return true;
                            }
                            return false;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                            return multiChoiceModeListener.onPrepareActionMode(actionMode, menu);
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                            return multiChoiceModeListener.onActionItemClicked(actionMode, menuItem);
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode actionMode) {
                            actionModeEnabled = false;
                            setEnabled(false);
                            setClickable(false);
                            for(int n = 0; n < getChildCount(); ++n){
                                getChildAt(n).setBackgroundResource(android.R.color.background_light);
                                setItemChecked(n, false);
                            }
                            setEnabled(true);
                            setClickable(true);
                            multiChoiceModeListener.onDestroyActionMode(actionMode);
                        }
                    });
                }
            }
        });
    }

	public void setItemActionListener(OnActionClickListener listener, int ... buttonIds) {
		this.listener = listener;
		this.buttonIds = buttonIds;
	}

    @Override
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        multiChoiceModeListener = listener;
    }

    @Override
    public void setChoiceMode(int choiceMode) {
        if(choiceMode == CHOICE_MODE_MULTIPLE_MODAL){
            modalMode = true;
            super.setChoiceMode(CHOICE_MODE_MULTIPLE);
        }
        else super.setChoiceMode(choiceMode);
    }

    /**
	 * Interface for callback to be invoked whenever an action is clicked in
	 * the expandle area of the list item.
	 */
	public interface OnActionClickListener {
		/**
		 * Called when an action item is clicked.
		 *
		 * @param itemView the view of the list item
		 * @param clickedView the view clicked
		 * @param position the position in the listview
		 */
		public void onClick(View itemView, View clickedView, int position);
	}

    public interface OnActionItemClickListener {
        public void onItemClick(View itemView, View clickedView, int position);
    }

    public interface OnActionLongItemClickListener {
        public void onLongItemClick(View itemView, View clickedView, int position);
    }

    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
		super.setAdapter(new WrapperListAdapterImpl(adapter) {
			@Override
			public View getView(final int position, View view, ViewGroup viewGroup) {
				final View listView = wrapped.getView(position, view, viewGroup);

				// add the action listeners
				if(buttonIds != null && listView != null) {
					for(int id : buttonIds) {
						View buttonView = listView.findViewById(id);
						if(buttonView != null) {
							buttonView.findViewById(id).setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View view) {
                                    Log.i("Expandable", "onClick()");
                                    if(listener != null) {
										listener.onClick(listView, view, position);
									}
								}
							});
						}
					}
				}
				return listView;
			}
		});
        setInternalListeners();
	}
}
