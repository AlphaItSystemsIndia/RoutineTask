package com.cod3rboy.routinetask;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cod3rboy.routinetask.database.models.TaskModel;

import java.util.ArrayList;
import java.util.List;

public class ChartListAdapter implements ListAdapter {
    private Context mContext;
    private List<DataSetObserver> mObservers;
    private ArrayList<TaskModel> mDataSet;

    public ChartListAdapter(Context context, ArrayList<TaskModel> dataSet) {
        mContext = context;
        mObservers = new ArrayList<>();
        mDataSet = dataSet;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        if (observer != null) mObservers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) mObservers.remove(observer);
    }

    @Override
    public int getCount() {
        return mDataSet.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        TaskModel model = mDataSet.get(position);
        return model.getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.chart_task_list_item, null);
            ChartListItemViewHolder vh = new ChartListItemViewHolder(view);
            vh.setData(mDataSet.get(position));
            view.setTag(vh);
            view.setBackgroundColor(mDataSet.get(position).getColor());
        } else {
            ChartListItemViewHolder vh = (ChartListItemViewHolder) view.getTag();
            vh.setData(mDataSet.get(position));
            view.setBackgroundColor(mDataSet.get(position).getColor());
        }
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    public void setDataSet(ArrayList<TaskModel> newDataSet) {
        mDataSet.clear();
        mDataSet.addAll(newDataSet);
        for (DataSetObserver o : mObservers) {
            o.onChanged();
        }
    }


    class ChartListItemViewHolder {
        private TextView mTaskTitle;
        private TextView mTaskDescription;

        public ChartListItemViewHolder(View v) {
            mTaskTitle = v.findViewById(R.id.tv_task_title);
            mTaskDescription = v.findViewById(R.id.tv_task_short_desc);
        }

        public void setData(TaskModel data) {
            mTaskTitle.setText(data.getTitle());
            mTaskDescription.setText(data.getDescription());
        }
    }
}
