package com.cod3rboy.routinetask.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cod3rboy.routinetask.AsyncWork;
import com.cod3rboy.routinetask.BuildConfig;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.Utilities;
import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.database.PomodoroStatsManager;
import com.cod3rboy.routinetask.events.RandomStatsGenerated;
import com.cod3rboy.routinetask.events.StatisticsReset;
import com.cod3rboy.routinetask.logging.Logger;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
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

public class PomodoroStatsFragment extends Fragment {
    private static final String LOG_TAG = PomodoroStatsFragment.class.getSimpleName();
    private AsyncWork<Void, LinkedHashMap<String,Integer>> mStatsLoader;
    private PieChart mWeekPieChart;
    private HorizontalBarChart mBarChart;
    private View mNoStatsView;
    private View mDividerView;

    private static final int[] PIE_CHART_COLORS = new int[]{
            0xFF0066FF,
            0xFFFF781B,
            0xFF33A1CC,
            0xFF99CC33,
            0xFFB61BFF,
            0xFF00BFFF,
            0xFFCDCD00,
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param fragmentType Fragment type to pass to host activity
     * @return A new instance of fragment PomodoroStatsFragment
     */
    public static PomodoroStatsFragment getInstance(int fragmentType) {
        PomodoroStatsFragment fragment = new PomodoroStatsFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pomodoro_stats, container, false);
        mWeekPieChart = v.findViewById(R.id.week_pie_chart);
        mBarChart = v.findViewById(R.id.bar_chart);
        mNoStatsView = v.findViewById(R.id.no_stats_view);
        mDividerView = v.findViewById(R.id.divider);

        customizePieChart(mWeekPieChart);
        customizeBarChart(mBarChart);
        // Subscribe to Random statistics generation event
        EventBus.getDefault().register(this);
        return v;
    }

    private void customizePieChart(PieChart pieChart){
        pieChart.setDrawCenterText(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterTextColor(getResources().getColor(R.color.white));
        pieChart.setHoleColor(getResources().getColor(R.color.colorPrimary));
        pieChart.setCenterTextSize(24f);
        pieChart.setHoleRadius(70);
    }
    private void customizeBarChart(HorizontalBarChart barChart){
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(false);
        barChart.getDescription().setEnabled(false);
        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setTextColor(getResources().getColor(R.color.color_chart_axis_label));
        xAxis.setDrawAxisLine(true);
        xAxis.setTextSize(12f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawAxisLine(false);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setDrawGridLines(true);
        rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        rightAxis.setDrawLabels(false);
        rightAxis.setDrawAxisLine(false);

        barChart.getLegend().setEnabled(false);
        barChart.setClipValuesToContent(true); // prevents bleeding of values outside content rect.
    }

    private void loadDataToCharts(LinkedHashMap<String, Integer> data){
        // Load Data into Barchart
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        int i = 0;
        for (String date : data.keySet()) {
            barEntries.add(new BarEntry(i, data.get(date)));
            dates.add(date);
            i++;
        }
        mBarChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Utilities.niceHumanDateFormat(dates.get((int) value), "yyyy-MM-dd", "dd MMM yyyy");
            }
        });

        BarDataSet barDataSet;

        if (mBarChart.getData() != null &&
                mBarChart.getData().getDataSetCount() > 0) {
            barDataSet = (BarDataSet) mBarChart.getData().getDataSetByIndex(0);
            barDataSet.setValues(barEntries);
            mBarChart.getData().notifyDataChanged();
            mBarChart.notifyDataSetChanged();

        } else {
            barDataSet = new BarDataSet(barEntries, "Pomodoro Statistics");
            barDataSet.setDrawIcons(false);
            barDataSet.setColor(getResources().getColor(R.color.color_chart_bar_normal));
            barDataSet.setHighlightEnabled(false);
            barDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int v = (int) value;
                    return Utilities.formatSecondsToChartValue(v);
                }
            });
            barDataSet.setValueTextColor(getResources().getColor(R.color.white));
            barDataSet.setValueTextSize(12f);
            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(barDataSet);
            BarData barData = new BarData(dataSets);
            mBarChart.setData(barData);
        }
        mBarChart.setVisibleXRange(7, 7);
        mBarChart.moveViewTo(0f, dates.size()-1,YAxis.AxisDependency.LEFT);
        mBarChart.animateY(1000, Easing.EaseInOutQuad);

        // Load last 7 days data into PieChart
        ArrayList<PieEntry> pieChartEntries = new ArrayList<>();
        String[] dateKeys = new String[dates.size()];
        long totalWeekTime = 0;
        data.keySet().toArray(dateKeys);
        for(int j=0; j<Math.min(7,dateKeys.length);j++){
            String dateKey = dateKeys[dateKeys.length-1-j];
            int seconds = data.get(dateKey);
            if(seconds <= 0) continue;
            PieEntry entry = new PieEntry(seconds, Utilities.getNiceHumanWeekDay(dateKey, "yyyy-MM-dd"));
            pieChartEntries.add(entry);
            totalWeekTime += seconds;
        }
        PieDataSet pieDataSet = new PieDataSet(pieChartEntries, "Pomodoro Statistics");
        pieDataSet.setValueTextColor(getResources().getColor(R.color.white));
        pieDataSet.setValueTextSize(10f);
        pieDataSet.setDrawValues(false);
        pieDataSet.setColors(PIE_CHART_COLORS);
        PieData pieData = new PieData(pieDataSet);
        mWeekPieChart.setData(pieData);
        mWeekPieChart.animateXY(1000, 1000, Easing.EaseInOutQuad);
        final long weekTime = totalWeekTime;
        mWeekPieChart.setCenterText(String.format(Locale.getDefault(),"Last 7 days\n%s", Utilities.formatSecondsToChartValue(weekTime)));
        mWeekPieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                String centerText = "Focus Time\n" + Utilities.formatSecondsToChartValue((int) e.getY());
                mWeekPieChart.setCenterText(centerText);
            }
            @Override
            public void onNothingSelected() {
                mWeekPieChart.setCenterText(String.format(Locale.getDefault(),"Last 7 days\n%s", Utilities.formatSecondsToChartValue(weekTime)));
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_pomodoro_stats, menu);
        if(!BuildConfig.DEBUG) menu.removeItem(R.id.action_random_stats_pomodoro);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnRandomStatsGenerated(RandomStatsGenerated event){
        loadPomodoroStatistics();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnStatisticsReset(StatisticsReset event){ loadPomodoroStatistics(); }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int fragmentType = (getArguments() == null) ? 0 : getArguments().getInt(MainActivity.KEY_FRAGMENT_TYPE);
        MainActivity activity = (MainActivity) getActivity();
        activity.onSectionAttached(fragmentType);
        activity.refreshActionBar();
        Logger.d(LOG_TAG, "onActivityCreated() with fragmentType - " + fragmentType);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPomodoroStatistics();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unsubscribe to Random statistics generation event
        EventBus.getDefault().unregister(this);
    }

    private void loadPomodoroStatistics() {
        mStatsLoader = new AsyncWork<>(new AsyncWork.Work<Void, LinkedHashMap<String, Integer>>() {
            @Override
            public LinkedHashMap<String, Integer> execute(Void... args) {
                LinkedHashMap<String, Integer> data = new LinkedHashMap<>();
                HashMap<String, Integer> results = new PomodoroStatsManager(getContext()).getPomodoroStats();
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
                Logger.d(LOG_TAG, "Fetched Pomodoro Stats data : " + data.toString());
                return data;
            }
        });
        mStatsLoader.registerOnLoadComplete(new AsyncWork.OnLoaderCompleted<LinkedHashMap<String, Integer>>() {
            @Override
            public void complete(LinkedHashMap<String, Integer> data) {
                if (data.size() > 0){
                    loadDataToCharts(data);
                    // Hide empty view and show chart views
                    mNoStatsView.setVisibility(View.GONE);
                    mBarChart.setVisibility(View.VISIBLE);
                    mWeekPieChart.setVisibility(View.VISIBLE);
                    mDividerView.setVisibility(View.VISIBLE);
                }else{
                    // Show empty view and hide chart views
                    mNoStatsView.setVisibility(View.VISIBLE);
                    mBarChart.setVisibility(View.GONE);
                    mWeekPieChart.setVisibility(View.GONE);
                    mDividerView.setVisibility(View.GONE);
                }
            }
        });
        mStatsLoader.start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_random_stats_pomodoro:
                PomodoroStatsManager mg = new PomodoroStatsManager(getActivity().getApplicationContext());
                showStatsGenerationDialog("Random Pomodoro Statistics", mg::generateRandomPomodoroStats);
                break;
            case R.id.action_stats_reset:
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle(R.string.reset_dialog_title)
                        .setMessage(String.format(
                                getResources().getString(R.string.reset_dialog_message),
                                getResources().getString(R.string.drawer_item_pomodoro)))
                        .setNeutralButton(R.string.reset_dialog_action_dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.reset_dialog_action_reset, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new PomodoroStatsManager(getContext()).resetPomodoroStatistics();
                                dialog.dismiss();
                            }
                        }).show();
        }
        return true;
    }

    interface OnDialogSuccess {
        void dialogOK(String dateString, int rangeMin, int rangeMax);
    }

    private void showStatsGenerationDialog(String title, PomodoroStatsFragment.OnDialogSuccess onSuccess) {
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
