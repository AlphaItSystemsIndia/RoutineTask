package com.cod3rboy.routinetask.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cod3rboy.routinetask.Tutorials;
import com.cod3rboy.routinetask.views.ClockSeekView;
import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.logging.Logger;
import com.warkiz.widget.*;

import com.cod3rboy.routinetask.events.PomodoroStart;
import com.cod3rboy.routinetask.events.PomodoroStop;
import com.cod3rboy.routinetask.events.PomodoroUpdate;
import com.cod3rboy.routinetask.services.PomodoroService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Use the {@link PomodoroFragment#getInstance} factory method to
 * create an instance of this fragment.
 */
public class PomodoroFragment extends Fragment {
    private static final String LOG_TAG = PomodoroFragment.class.getSimpleName();

    private ImageButton mStartButton;
    private ImageButton mStopButton;
    private ClockSeekView mClockSeekView;
    private boolean mAdjustingVolume;

    public PomodoroFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param fragmentType Fragment type to pass to host activity
     * @return A new instance of fragment PomodoroFragment.
     */
    public static PomodoroFragment getInstance(int fragmentType) {
        PomodoroFragment fragment = new PomodoroFragment();
        Bundle args = new Bundle();
        args.putInt(MainActivity.KEY_FRAGMENT_TYPE, fragmentType);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Dial is not updated when timer is over and fragment was paused (Phone sleep or app switch).
        // Following code prevents above situation by explicitly updating Dial Timer when Fragment is resumed
        if (PomodoroService.getService() != null)
            mClockSeekView.setDialTime(PomodoroService.getService().getSecondsLeft());

        EventBus.getDefault().register(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPomodoroStart(PomodoroStart event) {
        mClockSeekView.setTouchEnabled(false);
        mStartButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.VISIBLE);
        mClockSeekView.startDialAnimation();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPomodoroUpdate(PomodoroUpdate event) {
        mClockSeekView.setDialTime(event.getSecsLeft());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPomodoroStop(PomodoroStop event) {
        mStopButton.setVisibility(View.GONE);
        mStartButton.setVisibility(View.VISIBLE);
        mClockSeekView.setTouchEnabled(true);
        mClockSeekView.setDialTime(0); // Clear Dial
        mClockSeekView.stopDialAnimation();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int  fragmentType = (getArguments() == null) ? 0 : getArguments().getInt(MainActivity.KEY_FRAGMENT_TYPE);
        ((MainActivity) getActivity()).onSectionAttached(fragmentType);
        ((MainActivity) getActivity()).refreshActionBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_pomodoro, container, false);
        mClockSeekView = v.findViewById(R.id.clockseek);
        mStartButton = v.findViewById(R.id.startBtn);
        mStopButton = v.findViewById(R.id.stopBtn);
        mStartButton.setOnClickListener((View view) -> {
            int dialTime = mClockSeekView.getDialTime();
            if (dialTime < 60) return; // Atleast 1 minute required to start Pomodoro
            // Start Pomodoro Service
            Logger.d(LOG_TAG, "Pomodoro start button clicked");
            Intent i = new Intent(getContext(), PomodoroService.class);
            i.putExtra(PomodoroService.KEY_SECONDS, dialTime);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                getActivity().startForegroundService(i);
            else
                getActivity().startService(i);
        });
        mStopButton.setOnClickListener((View view) -> {
            // Stop Pomodoro Service
            if (PomodoroService.getService() != null) {
                PomodoroService.getService().stopPomodoro();
            }
            Logger.d(LOG_TAG, "Pomodoro stop button clicked");
        });

        TextView textView = (TextView) v.findViewById(R.id.pomodoroLink);
        textView.setClickable(true);
        final String linkTxt = getResources().getString(R.string.pomodoro_link);
        textView.setOnClickListener((View view) -> {
            Uri uri = Uri.parse(linkTxt); // missing 'http://' will cause crashed
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        final AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        final int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        IndicatorSeekBar seekBar = (IndicatorSeekBar) v.findViewById(R.id.volAdjustView);
        // Update System Media Volume when Volume Control changes
        seekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
                mAdjustingVolume = true;
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                int volume = (int) ((seekBar.getProgressFloat() / 100f) * maxVol);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
                mAdjustingVolume = false;
            }
        });
        // Load initial system media volume into volume control
        int progress = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        seekBar.setProgress((progress * 1.0f / maxVol) * 100);

        // Update Volume control when system media volume changes
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        if(mAdjustingVolume) return; // Skip changes triggered during manually adjusting volume
                        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                        float progress = (currentVolume * 1.0f / maxVol) * 100;
                        seekBar.setProgress(progress);
                        Logger.d(LOG_TAG, "System volume changed so adjusting seekbar. VOLUME = " + progress);
                    }
                });

        if (PomodoroService.getService() == null) {
            mClockSeekView.setTouchEnabled(true);
            mStopButton.setVisibility(View.GONE);
            mStartButton.setVisibility(View.VISIBLE);
        } else {
            mClockSeekView.setTouchEnabled(false);
            mStartButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.VISIBLE);
            mClockSeekView.startDialAnimation();
        }

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Show Pomodoro Tutorial
        Tutorials.showPomodoroTutorial(getActivity(), mClockSeekView);
    }
}
