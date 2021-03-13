package com.cod3rboy.routinetask;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;

import androidx.preference.PreferenceManager;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.appbar.MaterialToolbar;

public class Tutorials {
    private interface TutorialAction {
        void onTutorialComplete();
    }

    public static void showFabAndDrawerTutorial(Activity activity, View fab, MaterialToolbar toolbar) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_fab_drawer), false);
        if (tutorialCompleted) return;
        new TapTargetSequence(activity)
                .targets(
                        TapTarget.forToolbarNavigationIcon(toolbar, activity.getString(R.string.tutorial_drawer_title), activity.getString(R.string.tutorial_drawer_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF),

                        TapTarget.forView(fab, activity.getString(R.string.tutorial_new_title), activity.getString(R.string.tutorial_new_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)
                )
                .considerOuterCircleCanceled(false)
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit()
                                .putBoolean(activity.getString(R.string.pref_tutorial_fab_drawer), true)
                                .apply();
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {

                    }
                }).start();
    }

    public static void showPomodoroTutorial(Activity activity, View pomodoroView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_pomodoro), false);
        if (tutorialCompleted) return;
        TapTargetView.
                showFor(activity,
                        TapTarget.forView(pomodoroView, activity.getString(R.string.tutorial_pomodoro_title), activity.getString(R.string.tutorial_pomodoro_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)
                                .targetRadius(110)

                        , new TapTargetView.Listener() {
                            @Override
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                prefs.edit()
                                        .putBoolean(activity.getString(R.string.pref_tutorial_pomodoro), true)
                                        .apply();
                            }
                        });
    }

    public static void showAddTaskTutorial(Activity activity, MaterialToolbar toolbar) {
        showTaskFormTutorial(activity, () -> showAddTaskActionTutorial(activity, toolbar));
    }

    public static void showEditTaskTutorial(Activity activity, MaterialToolbar toolbar) {
        showTaskFormTutorial(activity, () -> showEditTaskActionTutorial(activity, toolbar));
    }

    private static void showAddTaskActionTutorial(Activity activity, MaterialToolbar toolbar) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_add_task), false);
        if (tutorialCompleted) return;
        new TapTargetSequence(activity)
                .targets(
                        // Save Option Menu Item
                        TapTarget.forToolbarMenuItem(toolbar, R.id.action_create, activity.getString(R.string.tutorial_create_title), activity.getString(R.string.tutorial_create_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)
                )
                .considerOuterCircleCanceled(false)
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit()
                                .putBoolean(activity.getString(R.string.pref_tutorial_add_task), true)
                                .apply();
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {

                    }
                }).start();
    }

    private static void showEditTaskActionTutorial(Activity activity, MaterialToolbar toolbar) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_edit_task), false);
        if (tutorialCompleted) return;

        new TapTargetSequence(activity)
                .targets(
                        // Save Option Menu Item
                        TapTarget.forToolbarMenuItem(toolbar, R.id.action_save, activity.getString(R.string.tutorial_save_title), activity.getString(R.string.tutorial_save_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF),

                        // Delete Option Menu Item
                        TapTarget.forToolbarMenuItem(toolbar, R.id.action_delete, activity.getString(R.string.tutorial_delete_title), activity.getString(R.string.tutorial_delete_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)
                )
                .considerOuterCircleCanceled(false)
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit()
                                .putBoolean(activity.getString(R.string.pref_tutorial_edit_task), true)
                                .apply();
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {

                    }
                }).start();
    }

    private static void showTaskFormTutorial(Activity activity, TutorialAction action) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_form_task), false);
        if (tutorialCompleted) {
            if (action != null) action.onTutorialComplete();
            return;
        }
        new TapTargetSequence(activity)
                .targets(
                        // Color View
                        TapTarget.forView(activity.findViewById(R.id.color_item), activity.getString(R.string.tutorial_color_title), activity.getString(R.string.tutorial_color_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF),

                        // Weekday Chip
                        TapTarget.forView(activity.findViewById(R.id.no_repeat_chip), activity.getString(R.string.tutorial_weekday_title), activity.getString(R.string.tutorial_weekday_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF),

                        // Set Reminder button view
                        TapTarget.forView(activity.findViewById(R.id.btn_add_reminder), activity.getString(R.string.tutorial_reminder_title), activity.getString(R.string.tutorial_reminder_desc))
                                .tintTarget(false)
                                .targetRadius(90)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)

                )
                .considerOuterCircleCanceled(false)
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit()
                                .putBoolean(activity.getString(R.string.pref_tutorial_form_task), true)
                                .apply();
                        if (action != null) action.onTutorialComplete();
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {

                    }
                }).start();
    }

    public static void showTaskReminderTutorial(Activity activity, View reminderView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_reminder_task), false);
        if (tutorialCompleted) return;
        new TapTargetSequence(activity)
                .targets(
                        // Start time View
                        TapTarget.forView(reminderView.findViewById(R.id.time_text_view), activity.getString(R.string.tutorial_start_time_title), activity.getString(R.string.tutorial_start_time_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF),

                        // Duration View
                        TapTarget.forView(reminderView.findViewById(R.id.duration_text_view), activity.getString(R.string.tutorial_duration_title), activity.getString(R.string.tutorial_duration_desc))
                                .tintTarget(false)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF),

                        // Remove reminder view
                        TapTarget.forView(reminderView.findViewById(R.id.btn_clear_reminder), activity.getString(R.string.tutorial_remove_reminder_title), activity.getString(R.string.tutorial_remove_reminder_desc))
                                .tintTarget(false)
                                .targetRadius(90)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.9f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)

                )
                .considerOuterCircleCanceled(false)
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit()
                                .putBoolean(activity.getString(R.string.pref_tutorial_reminder_task), true)
                                .apply();
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {

                    }
                }).start();
    }

    public static void showTodayTaskListItemTutorial(Activity activity, View listItemView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_today_item), false);
        if (tutorialCompleted) return;

        TapTargetView.
                showFor(activity,
                        TapTarget.forView(listItemView, activity.getString(R.string.tutorial_today_item_title), activity.getString(R.string.tutorial_today_item_desc))
                                .tintTarget(false)
                                .targetRadius(130)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.95f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)
                        , new TapTargetView.Listener() {
                            @Override
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                prefs.edit()
                                        .putBoolean(activity.getString(R.string.pref_tutorial_today_item), true)
                                        .apply();
                            }
                        });
    }
    public static void showRoutineTasksListItemTutorial(Activity activity, View listItemView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_routine_item), false);
        if (tutorialCompleted) return;

        TapTargetView.
                showFor(activity,
                        TapTarget.forView(listItemView, activity.getString(R.string.tutorial_routine_item_title), activity.getString(R.string.tutorial_routine_item_desc))
                                .tintTarget(false)
                                .targetRadius(130)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.95f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)
                        , new TapTargetView.Listener() {
                            @Override
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                prefs.edit()
                                        .putBoolean(activity.getString(R.string.pref_tutorial_routine_item), true)
                                        .apply();
                            }
                        });
    }
    public static void showOneTimeTaskListItemTutorial(Activity activity, View listItemView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean tutorialCompleted = prefs.getBoolean(activity.getString(R.string.pref_tutorial_onetime_item), false);
        if (tutorialCompleted) return;

        TapTargetView.
                showFor(activity,
                        TapTarget.forView(listItemView, activity.getString(R.string.tutorial_onetime_item_title), activity.getString(R.string.tutorial_onetime_item_desc))
                                .tintTarget(false)
                                .targetRadius(130)
                                .targetCircleColor(R.color.colorAccent)
                                .outerCircleAlpha(0.95f)
                                .outerCircleColorInt(0x555555)
                                .textColorInt(0xFFFFFFFF)
                        , new TapTargetView.Listener() {
                            @Override
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                prefs.edit()
                                        .putBoolean(activity.getString(R.string.pref_tutorial_onetime_item), true)
                                        .apply();
                            }
                        });
    }
}
