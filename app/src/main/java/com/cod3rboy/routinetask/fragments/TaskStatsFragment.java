package com.cod3rboy.routinetask.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cod3rboy.routinetask.AsyncWork;
import com.cod3rboy.routinetask.BuildConfig;
import com.cod3rboy.routinetask.ChartListAdapter;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.activities.EditTaskActivity;
import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.database.TaskStatsManager;
import com.cod3rboy.routinetask.events.RandomStatsGenerated;
import com.cod3rboy.routinetask.events.StatisticsReset;
import com.cod3rboy.routinetask.logging.Logger;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;


public class TaskStatsFragment extends Fragment {
    private static final String LOG_TAG = TaskStatsFragment.class.getSimpleName();
    public static final int EDIT_TASK_REQUEST_CODE = 156; // For Activity which edits a new task

    private AsyncWork<Void, LinkedHashMap<String, Integer>> mStatsLoader;
    private BarChart mTasksBarChart;
    private TextView mDateView;
    private ImageButton mChartPrevButton;
    private ImageButton mChartNextButton;
    private ListView mTasksListView;
    private int mSelectedBarIndex;
    private View mNoStatsView;
    private View mEmptyListView;
    private View mChartControlView;
    private String mSelectedDate;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param fragmentType Fragment type to pass to host activity
     * @return A new instance of fragment TaskStatsFragment
     */
    public static TaskStatsFragment getInstance(int fragmentType) {
        TaskStatsFragment fragment = new TaskStatsFragment();
        Bundle args = new Bundle();
        args.putInt(MainActivity.KEY_FRAGMENT_TYPE, fragmentType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int fragmentType = (getArguments() == null) ? 0 : getArguments().getInt(MainActivity.KEY_FRAGMENT_TYPE);
        MainActivity activity = (MainActivity) getActivity();
        activity.onSectionAttached(fragmentType);
        activity.refreshActionBar();
        Logger.d(LOG_TAG, "onActivityCreated() with fragmentType - " + fragmentType);
    }

    /**
     * Called when an activity, started by this fragment for result, finishes after invoking setResult.
     * This method is used to restart the list loading task based on the results of the Activity
     * which adds new task or Activity which modifies existing task.
     *
     * @param requestCode Request code set during startActivityForResult
     * @param resultCode  Result code set during setResult
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDIT_TASK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Logger.d(LOG_TAG, String.format("onActivityResult() - Reloading pomodoro list from database"));
            Highlight[] highlights = mTasksBarChart.getHighlighted();
            if(highlights.length > 0){
                updateTasksListView(mSelectedDate);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_task_stats, menu);
        if (!BuildConfig.DEBUG) menu.removeItem(R.id.action_random_stats_task);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_stats, container, false);
        mTasksBarChart = v.findViewById(R.id.tasks_bar_chart);
        mDateView = v.findViewById(R.id.text_view_date);
        mChartPrevButton = v.findViewById(R.id.btn_date_prev);
        mChartNextButton = v.findViewById(R.id.btn_date_next);
        mTasksListView = v.findViewById(R.id.list_view_completed_tasks);
        mNoStatsView = v.findViewById(R.id.no_stats_view);
        mEmptyListView = v.findViewById(R.id.empty_list_view);
        mChartControlView = v.findViewById(R.id.chart_control_view);
        customizeBarChart(mTasksBarChart);

        /*mTasksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TaskModel model = (TaskModel) mTasksListView.getAdapter().getItem(position);
                // Start activity to edit selected adapter task.
                Intent i = new Intent(getActivity(), EditTaskActivity.class);
                i.putExtra(EditTaskActivity.KEY_TASK_PARCEL, model);
                startActivityForResult(i, TaskListFragment.EDIT_TASK_REQUEST_CODE);
            }
        });*/

        // Subscribe to Random statistics generation event
        EventBus.getDefault().register(this);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unsubscribe to Random statistics generation event
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnRandomStatsGenerated(RandomStatsGenerated event) {
        loadTaskStatistics();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnStatisticsReset(StatisticsReset event){ loadTaskStatistics(); }

    private void customizeBarChart(BarChart barChart) {
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(false);

        barChart.getDescription().setEnabled(false);

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);

        barChart.setDoubleTapToZoomEnabled(false);

        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setTypeface(tfLight);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setTextColor(getResources().getColor(R.color.color_chart_axis_label));
        xAxis.setDrawAxisLine(true);
        xAxis.setTextSize(12f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawAxisLine(false);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setDrawLabels(false);
        rightAxis.setDrawAxisLine(false);

        barChart.getLegend().setEnabled(false);
        barChart.setClipValuesToContent(true); // prevents bleeding of values outside content rect.
    }

    private void loadDataToChart(BarChart barChart, LinkedHashMap<String, Integer> chartData) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        int i = 0;
        for (String date : chartData.keySet()) {
            barEntries.add(new BarEntry(i, chartData.get(date)));
            dates.add(date);
            i++;
        }
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Utilities.getWeekDay(dates.get((int) value), "yyyy-MM-dd");
            }
        });

        BarDataSet barDataSet;

        if (barChart.getData() != null &&
                barChart.getData().getDataSetCount() > 0) {
            barDataSet = (BarDataSet) barChart.getData().getDataSetByIndex(0);
            barDataSet.setValues(barEntries);
            barChart.getData().notifyDataChanged();
            barChart.notifyDataSetChanged();

        } else {
            barDataSet = new BarDataSet(barEntries, "Tasks Completed");
            barDataSet.setDrawIcons(false);
            barDataSet.setColor(getResources().getColor(R.color.color_chart_bar_normal));
            barDataSet.setHighLightColor(getResources().getColor(R.color.color_chart_bar_pressed));
            barDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int v = (int) value;
                    String format = (v > 1) ? "%d Tasks" : "%d Task";
                    return String.format(Locale.getDefault(), format, v);
                }
            });
            barDataSet.setValueTextColor(getResources().getColor(R.color.white));
            barDataSet.setValueTextSize(10f);

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(barDataSet);

            BarData data = new BarData(dataSets);

            barChart.setData(data);
        }
        barChart.setVisibleXRange(7, 7);
        mSelectedBarIndex = dates.size() - 1;
        barChart.animateY(1000, Easing.EaseInOutQuad);

        // Set Up things
        setupChartListeners(dates);
        updateChartControls(dates, true);
        updateTasksListView(dates.get(mSelectedBarIndex));
    }

    private void setupChartListeners(ArrayList<String> chartDates) {
        mChartPrevButton.setOnClickListener(v -> {
            if (mSelectedBarIndex == 0) return;
            mSelectedBarIndex--;
            updateChartControls(chartDates, true);
            updateTasksListView(chartDates.get(mSelectedBarIndex));
        });
        mChartNextButton.setOnClickListener(v -> {
            if (mSelectedBarIndex == chartDates.size() - 1) return;
            mSelectedBarIndex++;
            updateChartControls(chartDates, true);
            updateTasksListView(chartDates.get(mSelectedBarIndex));
        });
        mTasksBarChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                mSelectedBarIndex = (int) h.getX();
                updateChartControls(chartDates, false);
                updateTasksListView(chartDates.get(mSelectedBarIndex));
            }

            @Override
            public void onNothingSelected() {
                mTasksBarChart.highlightValue(mSelectedBarIndex, 0, false);
            }
        });
    }

    private void updateChartControls(ArrayList<String> chartDates, boolean moveView) {
        mDateView.setText(Utilities.getFormattedChartDate(chartDates.get(mSelectedBarIndex), "yyyy-MM-dd"));

        if (mSelectedBarIndex >= chartDates.size() - 1) mChartNextButton.setEnabled(false);
        else if (!mChartNextButton.isEnabled()) mChartNextButton.setEnabled(true);

        if (mSelectedBarIndex <= 0) mChartPrevButton.setEnabled(false);
        else if (!mChartPrevButton.isEnabled()) mChartPrevButton.setEnabled(true);

        if (moveView)
            mTasksBarChart.moveViewTo(mSelectedBarIndex, 0f, YAxis.AxisDependency.LEFT);

        Highlight[] selectedHighlights = mTasksBarChart.getHighlighted();
        if (selectedHighlights != null && selectedHighlights.length >= 1 && ((int) selectedHighlights[0].getX()) != mSelectedBarIndex)
            mTasksBarChart.highlightValue(mSelectedBarIndex, 0, false);
        else if (selectedHighlights == null || selectedHighlights.length == 0) {
            mTasksBarChart.highlightValue(mSelectedBarIndex, 0, false);
        }
    }

    private void updateTasksListView(String date) {
        AsyncWork<String, ArrayList<TaskModel>> loadCompletedTasks = new AsyncWork<>(new AsyncWork.Work<String, ArrayList<TaskModel>>() {
            @Override
            public ArrayList<TaskModel> execute(String... args) {
                mSelectedDate = args[0];
                return TaskStatsManager.getInstance().getCompletedTasksOnDate(args[0]);
            }
        });
        loadCompletedTasks.registerOnLoadComplete(new AsyncWork.OnLoaderCompleted<ArrayList<TaskModel>>() {
            @Override
            public void complete(ArrayList<TaskModel> data) {
                ListAdapter adapter = mTasksListView.getAdapter();
                if (adapter == null) {
                    adapter = new ChartListAdapter(getContext(), data);
                    mTasksListView.setAdapter(adapter);
                    Logger.d(LOG_TAG, "New adapter set for listview of completed tasks");
                } else {
                    ((ChartListAdapter) adapter).setDataSet(data);
                    Logger.d(LOG_TAG, "Existing adapter dataset is updated for listview of completed tasks");
                }
                if (data.size() <= 0) {
                    mTasksListView.setVisibility(View.GONE);
                    mEmptyListView.setVisibility(View.VISIBLE);
                } else {
                    mTasksListView.setVisibility(View.VISIBLE);
                    mEmptyListView.setVisibility(View.GONE);
                }
            }
        });
        loadCompletedTasks.start(date);
        Logger.d(LOG_TAG, "Loading listview with completed tasks");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadTaskStatistics();
    }

    private void loadTaskStatistics() {
        mStatsLoader = new AsyncWork<>(new AsyncWork.Work<Void, LinkedHashMap<String, Integer>>() {
            @Override
            public LinkedHashMap<String, Integer> execute(Void... args) {
                LinkedHashMap<String, Integer> data = new LinkedHashMap<>();
                HashMap<String, Integer> results = TaskStatsManager.getInstance().getTasksCountStats(getContext());
                if (results.size() == 0) return data; // Return empty data
                // Get the minimum date from string
                Calendar startDate = Calendar.getInstance();
                Calendar endDate = Calendar.getInstance();
                startDate.setTime(Objects.requireNonNull(Utilities.getDateObject(Collections.min(results.keySet(), String::compareTo))));
                endDate.setTime(Objects.requireNonNull(Utilities.getDateObject(Utilities.getTodayDateString())));
                while (startDate.compareTo(endDate) <= 0) {
                    String date = Utilities.formatDateObject(startDate.getTime());
                    Integer value = results.get(date);
                    value = (value == null) ? 0 : value;
                    data.put(date, value);
                    startDate.add(Calendar.DAY_OF_MONTH, 1);
                }
                Logger.d(LOG_TAG, "Fetched Tasks Stats data : " + data.toString());
                return data;
            }
        });
        mStatsLoader.registerOnLoadComplete(new AsyncWork.OnLoaderCompleted<LinkedHashMap<String, Integer>>() {
            @Override
            public void complete(LinkedHashMap<String, Integer> data) {
                if (data.size() > 0) {
                    loadDataToChart(mTasksBarChart, data);
                    // Hide no stats view and display chart views
                    mNoStatsView.setVisibility(View.GONE);
                    mTasksBarChart.setVisibility(View.VISIBLE);
                    mChartControlView.setVisibility(View.VISIBLE);
                    mTasksListView.setVisibility(View.VISIBLE);
                } else {
                    // Show no stats view and hide chart views
                    mNoStatsView.setVisibility(View.VISIBLE);
                    mTasksBarChart.setVisibility(View.GONE);
                    mChartControlView.setVisibility(View.GONE);
                    mTasksListView.setVisibility(View.GONE);
                }
            }
        });
        mStatsLoader.start();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_random_stats_task:
                showStatsGenerationDialog(
                        "Random Task Statistics",
                        (dateString, rangeMin, rangeMax) ->
                                TaskStatsManager.getInstance().generateRandomTaskStats(dateString, rangeMin, rangeMax)
                );
                break;
            case R.id.action_stats_reset:
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle(R.string.reset_dialog_title)
                        .setMessage(String.format(
                                getResources().getString(R.string.reset_dialog_message),
                                "tasks"))
                        .setNeutralButton(R.string.reset_dialog_action_dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.reset_dialog_action_reset, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TaskStatsManager.getInstance().resetTasksStatistics(getContext());
                                dialog.dismiss();
                            }
                        }).show();
        }
        return true;
    }

    interface OnDialogSuccess {
        void dialogOK(String dateString, int rangeMin, int rangeMax);
    }

    private void showStatsGenerationDialog(String title, OnDialogSuccess onSuccess) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_random_stats);
        TextView dialogTitleView = dialog.findViewById(R.id.stats_dialog_title);
        dialogTitleView.setText(title);
        EditText inputDate = dialog.findViewById(R.id.input_date);
        EditText inputMinValue = dialog.findViewById(R.id.input_min_value);
        EditText inputMaxValue = dialog.findViewById(R.id.input_max_value);
        Button btnOK = dialog.findViewById(R.id.btn_dialog_ok);
        Button btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        btnCancel.setOnClickListener(view -> dialog.dismiss());
        btnOK.setOnClickListener(view -> {
            if (onSuccess != null)
                onSuccess.dialogOK(
                        inputDate.getText().toString(),
                        Integer.parseInt(inputMinValue.getText().toString()),
                        Integer.parseInt(inputMaxValue.getText().toString())
                );
            dialog.dismiss();
        });
        dialog.show();
    }
}
