package com.cod3rboy.routinetask;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.routinetask.database.DBContract;
import com.cod3rboy.routinetask.database.models.ReminderModel;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.cod3rboy.routinetask.utilities.Time;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mikhaellopez.circleview.CircleView;

import java.util.ArrayList;
import java.util.Calendar;

import io.github.deweyreed.scrollhmspicker.ScrollHmsPicker;

/**
 * This class represents task form ui which holds the inputs of task detail.
 * The details filled in the form is used to create/update a task.
 */
public class TaskForm {

    private static final String KEY_FORM_REMINDER = "form_reminder";
    private static final String KEY_FORM_COLOR = "form_color";

    private Context context;
    private TextInputEditText titleView;
    private TextInputEditText descriptionView;
    private TextView startTimeView;
    private TextView durationView;
    private Chip noRepeatChip;
    private Chip everydayChip;
    private Chip sunChip;
    private Chip monChip;
    private Chip tueChip;
    private Chip wedChip;
    private Chip thuChip;
    private Chip friChip;
    private Chip satChip;

    public MaterialButton setReminderBtn;
    public ImageButton clearReminderBtn;
    public View reminderView;

    private RecyclerView rvColors;
    private ColorListAdapter mAdapter;

    private ReminderModel reminder;

    /**
     * Interface to notify listener when color is changed in the form
     */
    public interface OnColorChangeListener {
        void colorChanged(int color, int position, View colorView);
    }

    public interface ReminderToggleListener {
        void reminderToggled(boolean reminderSet, View reminderView);
    }

    private OnColorChangeListener mColorListener;
    private ReminderToggleListener mReminderToggleListener;


    /**
     * Constructor to initialize form widgets from root view and sets up listeners to the widgets.
     *
     * @param context     Activity context in which form is being displayed
     * @param addTaskView root view of the form ui
     */
    public TaskForm(final Context context, final View addTaskView) {
        this.context = context;
        initializeViews(addTaskView);
        setupListeners();
        reminder = null; // No reminder is set initially
    }

    /**
     * This constructor is used to create a task form with pre-filled details of given task.
     *
     * @param context     Activity context in which form is being displayed
     * @param addTaskView root view of the form ui
     * @param task        TaskModel object with which task form should be pre-filled
     */
    public TaskForm(final Context context, final View addTaskView, final TaskModel task) {
        this(context, addTaskView);
        loadTask(task);
    }

    /**
     * This method finds and stores the references to form widgets and also sets up the initial UI state.
     *
     * @param formRootView root view of the form ui
     */
    private void initializeViews(final View formRootView) {
        titleView = formRootView.findViewById(R.id.input_title);
        titleView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        titleView.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE);
        descriptionView = formRootView.findViewById(R.id.input_desc);
        descriptionView.setRawInputType(InputType.TYPE_CLASS_TEXT);
        startTimeView = formRootView.findViewById(R.id.time_text_view);
        durationView = formRootView.findViewById(R.id.duration_text_view);
        ChipGroup weekDayChipGroup = formRootView.findViewById(R.id.weekday_chip_group);
        noRepeatChip = weekDayChipGroup.findViewById(R.id.no_repeat_chip);
        everydayChip = weekDayChipGroup.findViewById(R.id.everyday_chip);
        sunChip = weekDayChipGroup.findViewById(R.id.sunday_chip);
        monChip = weekDayChipGroup.findViewById(R.id.monday_chip);
        tueChip = weekDayChipGroup.findViewById(R.id.tuesday_chip);
        wedChip = weekDayChipGroup.findViewById(R.id.wednesday_chip);
        thuChip = weekDayChipGroup.findViewById(R.id.thursday_chip);
        friChip = weekDayChipGroup.findViewById(R.id.friday_chip);
        satChip = weekDayChipGroup.findViewById(R.id.saturday_chip);
        // Set the first day of week chip. default is monday
        if (Utilities.getFirstDayOfWeekPreference(context) == Calendar.SUNDAY) {
            int mondayIndex = weekDayChipGroup.indexOfChild(monChip);
            weekDayChipGroup.removeView(sunChip);
            weekDayChipGroup.addView(sunChip, mondayIndex);
        }
        setReminderBtn = formRootView.findViewById(R.id.btn_add_reminder);
        reminderView = formRootView.findViewById(R.id.reminder_view);
        clearReminderBtn = reminderView.findViewById(R.id.btn_clear_reminder);
        // RecyclerView of color list items
        rvColors = formRootView.findViewById(R.id.rv_color);
        // Set Horizontal layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rvColors.setLayoutManager(layoutManager);
        mAdapter = new ColorListAdapter(LayoutInflater.from(context));
        rvColors.setAdapter(mAdapter);
        setSelectedColorPosition(0);
        // Initially only no repeat chip is selected
        resetRepeatChips();
        noRepeatChip.setChecked(true);
        // Initially reminder is not set
        toggleReminderView(false);
    }

    /**
     * This method registers event listeners on form widgets.
     */
    private void setupListeners() {
        // Set color item click listener
        mAdapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = rvColors.getChildAdapterPosition(v);
                mAdapter.setTouchedPosition(position);
                if (mColorListener != null) mColorListener.colorChanged(
                        mAdapter.getSelectedColor(),
                        mAdapter.getTouchedPosition(),
                        v
                );
            }
        });

        // Register click listener on start time text view
        startTimeView.setOnClickListener(v -> showTimePicker(context));

        // Register click listener on duration time click listener
        durationView.setOnClickListener(v -> showDurationPicker(context));

        // Register check state change listeners to all repeat chips
        noRepeatChip.setOnClickListener(this::onRepeatChipChecked);
        everydayChip.setOnClickListener(this::onRepeatChipChecked);
        sunChip.setOnClickListener(this::onRepeatChipChecked);
        monChip.setOnClickListener(this::onRepeatChipChecked);
        tueChip.setOnClickListener(this::onRepeatChipChecked);
        wedChip.setOnClickListener(this::onRepeatChipChecked);
        thuChip.setOnClickListener(this::onRepeatChipChecked);
        friChip.setOnClickListener(this::onRepeatChipChecked);
        satChip.setOnClickListener(this::onRepeatChipChecked);

        // Register click listener on set reminder button
        setReminderBtn.setOnClickListener(v -> showTimePicker(context));

        // Register click listener on clear reminder button
        clearReminderBtn.setOnClickListener(v -> {
            reminder = null;
            toggleReminderView(false);
        });
    }

    /**
     * This method displays a dialog used to select start time for reminder.
     *
     * @param context Activity context in which form is being displayed
     */
    private void showTimePicker(final Context context) {
        // Get current date/time
        Calendar now = Calendar.getInstance();
        if (reminder != null) {
            Time starTime = reminder.getStartTime();
            // Initialize with previously set start time
            now.set(Calendar.HOUR_OF_DAY, starTime.getHours());
            now.set(Calendar.MINUTE, starTime.getMinutes());
        }
        boolean is24HourView = Utilities.getTimeFormatPreference(context).equals(context.getString(R.string.settings_time_format_value_24));
        TimePickerDialog startTimePicker = new TimePickerDialog(
                context,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        if (reminder == null) {// When reminder created for first time
                            reminder = new ReminderModel(new Time(hourOfDay, minute), 0);
                            toggleReminderView(true);
                        } else { // When reminder is changed/modified
                            reminder.setStartTime(new Time(hourOfDay, minute));
                        }
                        setFormattedStartTime(reminder);
                        durationView.setText(Utilities.formatDuration(context, reminder.getDurationInMinutes()));
                    }
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                is24HourView);
        // Show dialog
        startTimePicker.show();
    }

    /**
     * This method sets startTimeView's text to the reminder start time
     * according to the user's time format preference.
     *
     * @param reminder ReminderModel object
     */
    private void setFormattedStartTime(ReminderModel reminder) {
        if (Utilities.getTimeFormatPreference(context).equals(context.getString(R.string.settings_time_format_value_24)))
            startTimeView.setText(reminder.getStartTime().to24TimeFormat());
        else
            startTimeView.setText(reminder.getStartTime().to12TimeFormat());
    }

    /**
     * This method displays a dialog used to specify duration for reminder.
     *
     * @param context Activity context in which form is being displayed
     */
    private void showDurationPicker(final Context context) {
        AlertDialog hmsPickerDialog = new MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_hms_picker)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                }).show();
        ScrollHmsPicker hmsPickerView = hmsPickerDialog.findViewById(R.id.scrollHmsPicker);
        if (getReminder() != null) {
            // Initialize with previously set duration
            long durationInMinutes = getReminder().getDurationInMinutes();
            int hours = (int) durationInMinutes / 60;
            int minutes = (int) durationInMinutes % 60;
            hmsPickerView.setHours(hours);
            hmsPickerView.setMinutes(minutes);
        }
        hmsPickerDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                context.getString(android.R.string.ok),
                (dialog, which) -> {
                    if (reminder != null) { // Ensure reminder is already created
                        int hours = hmsPickerView.getHours();
                        int minutes = hmsPickerView.getMinutes();
                        int durationInMinutes = hours * 60 + minutes;
                        reminder.setDurationInMinutes(durationInMinutes);
                        durationView.setText(Utilities.formatDuration(context, reminder.getDurationInMinutes()));
                    }
                }
        );
        hmsPickerDialog.show();
    }

    /**
     * This method is invoked when a user clicks any repeat chip widget in the form.
     *
     * @param source repeat chip widget which was checked/unchecked by user
     */
    private void onRepeatChipChecked(View source) {
        Chip repeatChip = (Chip) source;
        if (repeatChip == noRepeatChip) {
            // Source chip is a no repeat chip
            noRepeatChip.setChecked(true);
            everydayChip.setChecked(false);
            sunChip.setChecked(false);
            monChip.setChecked(false);
            tueChip.setChecked(false);
            wedChip.setChecked(false);
            thuChip.setChecked(false);
            friChip.setChecked(false);
            satChip.setChecked(false);
        } else if (repeatChip == everydayChip) {
            // Source chip is a everyday repeat Chip
            noRepeatChip.setChecked(false);
            everydayChip.setChecked(true);
            sunChip.setChecked(true);
            monChip.setChecked(true);
            tueChip.setChecked(true);
            wedChip.setChecked(true);
            thuChip.setChecked(true);
            friChip.setChecked(true);
            satChip.setChecked(true);
        } else {
            // Source chip is a week day chip
            noRepeatChip.setChecked(false);
            everydayChip.setChecked(false);
            if (repeatChip.isChecked()) {
                // Weekday chip was checked
                boolean allWeekdaysChecked = isSundayChecked() && isMondayChecked() && isTuesdayChecked()
                        && isWednesdayChecked() && isThursdayChecked() && isFridayChecked() && isSaturdayChecked();
                // Check everyday chip if last unchecked weekday chip was checked
                if (allWeekdaysChecked) everydayChip.setChecked(true);
            } else {
                // Weekday chip was unchecked
                boolean noWeekdayChecked = !(isSundayChecked() || isMondayChecked() || isTuesdayChecked()
                        || isWednesdayChecked() || isThursdayChecked() || isFridayChecked() || isSaturdayChecked());
                // Check no repeat chip if last checked weekday chip was unchecked
                if (noWeekdayChecked) noRepeatChip.setChecked(true);
            }
        }
    }

    /**
     * This method is used to compare the form state with task state i.e. does data in the form
     * and the task are matched. If any data in the form differs from task data then this method
     * returns false.
     *
     * @return true if form data is matched with task data otherwise false
     */
    public boolean hasTaskState(TaskModel task) {
        // Determine reminder equality
        boolean reminderChanged = true;
        if (task.getReminder() != null && getReminder() != null) {
            reminderChanged = !task.getReminder().equals(getReminder());
        } else if (task.getReminder() == null && getReminder() == null) {
            reminderChanged = false;
        }
        return getTitle().equals(task.getTitle())
                && getDescription().equals(task.getDescription())
                && isSundayChecked() == task.isRepeatSunday()
                && isMondayChecked() == task.isRepeatMonday()
                && isTuesdayChecked() == task.isRepeatTuesday()
                && isWednesdayChecked() == task.isRepeatWednesday()
                && isThursdayChecked() == task.isRepeatThursday()
                && isFridayChecked() == task.isRepeatFriday()
                && isSaturdayChecked() == task.isRepeatSaturday()
                && getSelectedColor() == task.getColor()
                && !reminderChanged;
    }

    /**
     * This method loads the state of task form ui with the details of the given task.
     *
     * @param task TaskModel object with which task form should be loaded
     */
    private void loadViewStateFromTask(TaskModel task) {
        // Set title
        titleView.setText(task.getTitle());
        // Set description
        descriptionView.setText(task.getDescription());
        // Set Repeat Days
        boolean repeatedTask = false;
        boolean repeatTaskDaily = true;
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            repeatedTask |= task.isRepeatForDay(i);
            repeatTaskDaily &= task.isRepeatForDay(i);
        }
        resetRepeatChips();
        if (repeatedTask) {
            // Task is a repeated task
            if (repeatTaskDaily) {
                // Check only every day chip
                everydayChip.setChecked(true);
            }
            // Check chips of all days for which task is repeated
            if (task.isRepeatSunday()) sunChip.setChecked(true);
            if (task.isRepeatMonday()) monChip.setChecked(true);
            if (task.isRepeatTuesday()) tueChip.setChecked(true);
            if (task.isRepeatWednesday()) wedChip.setChecked(true);
            if (task.isRepeatThursday()) thuChip.setChecked(true);
            if (task.isRepeatFriday()) friChip.setChecked(true);
            if (task.isRepeatSaturday()) satChip.setChecked(true);
        } else {
            // Task is a one time task
            noRepeatChip.setChecked(true);
        }
        // Set Reminder
        reminder = ReminderModel.copy(task.getReminder());
        if (reminder == null) {
            toggleReminderView(false);
        } else {
            // Set Start Time
            setFormattedStartTime(reminder);
            // Set Duration
            durationView.setText(Utilities.formatDuration(context, reminder.getDurationInMinutes()));
            toggleReminderView(true);
        }
        // Set Color
        setSelectedColor(task.getColor());
    }

    /**
     * This method changes the visibility of reminder view depending on whether the reminder is
     * set or not in the task form.
     *
     * @param reminderSet boolean whether reminder is set or not
     */
    private void toggleReminderView(boolean reminderSet) {
        if (reminderSet) {
            reminderView.setVisibility(View.VISIBLE);
            setReminderBtn.setVisibility(View.GONE);
        } else {
            reminderView.setVisibility(View.GONE);
            setReminderBtn.setVisibility(View.VISIBLE);
        }
        if (mReminderToggleListener != null)
            mReminderToggleListener.reminderToggled(reminderSet, reminderView);
    }

    /**
     * This method resets the checked state of all repeat chip widgets to unchecked state.
     */
    private void resetRepeatChips() {
        noRepeatChip.setChecked(false);
        everydayChip.setChecked(false);
        sunChip.setChecked(false);
        monChip.setChecked(false);
        tueChip.setChecked(false);
        wedChip.setChecked(false);
        thuChip.setChecked(false);
        friChip.setChecked(false);
        satChip.setChecked(false);
    }

    /**
     * This method loads details of given task into task form.
     *
     * @param task TaskModel object to load into form
     */
    public void loadTask(TaskModel task) {
        if (task != null) loadViewStateFromTask(task);
    }

    /**
     * Method to register the listener for the form color.
     *
     * @param listener listener to invoke when form color is changed
     */
    public void setOnColorChangeListener(OnColorChangeListener listener) {
        mColorListener = listener;
    }

    public void setReminderToggleListener(ReminderToggleListener listener) {
        mReminderToggleListener = listener;
    }

    /**
     * Method to get the adapter position of selected form color.
     *
     * @return adapter position of selected form color
     */
    public int getSelectedColorPosition() {
        return mAdapter.getTouchedPosition();
    }

    /**
     * Method to select a form color using its adapter position.
     *
     * @param pos adapter position of form color
     */
    public void setSelectedColorPosition(int pos) {
        mAdapter.setTouchedPosition(pos);
    }

    /**
     * This method returns the selected color in hex number format.
     *
     * @return selected color in hex number format
     */
    public int getSelectedColor() {
        return mAdapter.getSelectedColor();
    }

    /**
     * This method is used to set form color to a given color in hex number format.
     *
     * @param color color in hex number format
     */
    public void setSelectedColor(int color) {
        mAdapter.setSelectedColor(color);
    }

    /**
     * This method is used to get the task title entered by user.
     *
     * @return title for the task
     */
    public String getTitle() {
        Editable titleText = titleView.getText();
        if (titleText != null) {
            return titleText.toString();
        }
        return "";
    }

    /**
     * This method is used to get the task description entered by user.
     *
     * @return description for the task
     */
    public String getDescription() {
        Editable descriptionText = descriptionView.getText();
        if (descriptionText != null) {
            return descriptionText.toString();
        }
        return "";
    }

    public boolean isSundayChecked() {
        return sunChip.isChecked();
    }

    public boolean isMondayChecked() {
        return monChip.isChecked();
    }

    public boolean isTuesdayChecked() {
        return tueChip.isChecked();
    }

    public boolean isWednesdayChecked() {
        return wedChip.isChecked();
    }

    public boolean isThursdayChecked() {
        return thuChip.isChecked();
    }

    public boolean isFridayChecked() {
        return friChip.isChecked();
    }

    public boolean isSaturdayChecked() {
        return satChip.isChecked();
    }

    public boolean isNoRepeatChecked() {
        return noRepeatChip.isChecked();
    }

    /**
     * This method is used to get the reminder set in the task form.
     *
     * @return ReminderModel if reminder is set, or null if reminder is not set.
     */
    public ReminderModel getReminder() {
        return reminder;
    }

    /**
     * This method is used to validate the task form data. It must be invoked prior to using the
     * filled data.
     *
     * @return true if form data is valid or false if form data is invalid
     */
    public boolean validate() {
        boolean validated = true;
        // only title string validation
        if (getTitle().trim().length() <= 0) validated = false;
        if (!validated)
            titleView.setError(TaskApplication.getAppContext().getString(R.string.form_title_error));
        return validated;
    }

    /**
     * This method is used to save the form view state into the outState bundle of the host
     * lifecycle object so that form state can be restored later when it is recreated.
     *
     * @param outState Bundle of the host lifecycle object within which form resides. The view state
     *                 of form is saved in this bundle to restore it later.
     */
    public void saveViewState(@NonNull Bundle outState) {
        // For now we are only saving the reminder and color states because other view states are saved automatically.
        outState.putParcelable(KEY_FORM_REMINDER, getReminder());
        outState.putInt(KEY_FORM_COLOR, getSelectedColor());
    }

    /**
     * This method is used to restore the state of recreated form when its host lifecycle object was
     * destroyed and recreated. The view state that was saved previously in the bundle can now be
     * restored by using the same savedInstanceState bundle.
     *
     * @param savedInstanceState bundle holding the previously saved view state
     */
    public void restoreViewState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // For now we are only restoring the reminder and color states because other view states are restored automatically.
            reminder = savedInstanceState.getParcelable(KEY_FORM_REMINDER);
            if (reminder != null) {
                // Set Start Time
                setFormattedStartTime(reminder);
                // Set Duration
                durationView.setText(Utilities.formatDuration(context, reminder.getDurationInMinutes()));
                toggleReminderView(true);
            }
            int color = savedInstanceState.getInt(KEY_FORM_COLOR, getSelectedColor());
            setSelectedColor(color); // Set form background color
        }
    }

    /**
     * ViewHolder class for form colors recycler view
     */
    class ColorViewHolder extends RecyclerView.ViewHolder {
        private CircleView colorView;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.color_circle);
        }

        public void setCircleColor(int color) {
            colorView.setCircleColor(color);
        }

        public void setBorderColor(int color) {
            colorView.setBorderColor(color);
        }
    }

    /**
     * Adapter class for form colors recycler view
     */
    class ColorListAdapter extends RecyclerView.Adapter<ColorViewHolder> {
        private LayoutInflater mInflater;
        private ArrayList<Integer> mColors;
        private View.OnClickListener mListener;
        private int mTouchedPos = -1;

        public ColorListAdapter(LayoutInflater inflater) {
            mInflater = inflater;
            mColors = new ArrayList<>();
            for (int color : DBContract.BG_COLORS) {
                mColors.add(color);
            }
        }

        public void setOnItemClickListener(View.OnClickListener listener) {
            mListener = listener;
        }

        public void setTouchedPosition(int pos) {
            mTouchedPos = pos;
            notifyDataSetChanged();
        }

        public int getTouchedPosition() {
            return mTouchedPos;
        }

        public void setSelectedColor(int color) {
            int i = 0;
            for (; i < mColors.size(); i++) {
                if (mColors.get(i) == color) {
                    setTouchedPosition(i);
                    break;
                }
            }
            // If color not found then select default color
            if (i == mColors.size())
                setTouchedPosition(0);
        }

        public int getSelectedColor() {
            return mColors.get(mTouchedPos);
        }

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = mInflater.inflate(R.layout.color_list_item, parent, false);
            v.setOnClickListener(view -> {
                if (mListener != null) mListener.onClick(view);
            });
            ColorViewHolder vh = new ColorViewHolder(v);
            return vh;
        }


        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            int color = mColors.get(position);
            holder.setCircleColor(color);
            if (position == mTouchedPos) {
                holder.setBorderColor(0xFFFFFFFF);
            } else {
                holder.setBorderColor(color);
            }
        }

        @Override
        public int getItemCount() {
            return mColors.size();
        }
    }
}
