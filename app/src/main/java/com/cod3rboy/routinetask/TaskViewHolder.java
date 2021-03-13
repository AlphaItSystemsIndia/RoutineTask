package com.cod3rboy.routinetask;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Paint;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.cod3rboy.routinetask.database.models.ReminderModel;
import com.cod3rboy.routinetask.database.models.TaskModel;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;


/**
 * Helper class to hold references to the row widgets contained in a row of recyclerview
 * Instance of this class is created and tagged with row's view (automatically) to avoid finding the child
 * widgets during subsequent data binding process.
 */
public class TaskViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = TaskViewHolder.class.getSimpleName();

    // Widgets references in List Item View
    private TextView titleTextView;
    private TextView descTextView;
    private TextView timeTextView;
    private TextView durationTextView;
    private TextView noRepeatTag;
    private TextView sunTag;
    private TextView monTag;
    private TextView tueTag;
    private TextView wedTag;
    private TextView thuTag;
    private TextView friTag;
    private TextView satTag;
    public CheckBox checkBox;

    private View mFirstChild;
    private CompoundButton.OnCheckedChangeListener checkboxListener;

    /**
     * Constructor to create instance of TaskViewHolder.
     *
     * @param root root view of list item
     */
    TaskViewHolder(Context context, View root) {
        super(root);
        // Get the references to the child widgets of root view and store the
        // references in instance variables for reuse.
        mFirstChild = root.findViewById(R.id.first_child);
        titleTextView = root.findViewById(R.id.text_view_title);
        descTextView = root.findViewById(R.id.text_view_desc);
        timeTextView = root.findViewById(R.id.text_view_time);
        durationTextView = root.findViewById(R.id.text_view_duration);
        noRepeatTag = root.findViewById(R.id.tag_no_repeat);
        sunTag = root.findViewById(R.id.tag_sun);
        monTag = root.findViewById(R.id.tag_mon);
        tueTag = root.findViewById(R.id.tag_tue);
        wedTag = root.findViewById(R.id.tag_wed);
        thuTag = root.findViewById(R.id.tag_thu);
        friTag = root.findViewById(R.id.tag_fri);
        satTag = root.findViewById(R.id.tag_sat);
        checkBox = root.findViewById(R.id.item_checkbox);
        setupListeners(context);
    }

    private void setupListeners(Context context) {
        if (checkBox != null) {
            // Setup Checkbox listener
            checkBox.setOnCheckedChangeListener((view, isChecked) -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                // This callback is always executed after view binding process which means
                // We have set tag in xml layout of today grid item so that we can decide whether to update its title.
                // @todo We need to look for alternative to tag.
                Object tag = checkBox.getTag();
                if (isChecked) {
                    titleTextView.setPaintFlags(titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    if (tag != null) checkBox.setText(context.getString(R.string.task_completed));
                } else {
                    titleTextView.setPaintFlags(titleTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    if (tag != null)
                        checkBox.setText(context.getString(R.string.task_not_completed));
                }
                // Notify listener
                if (checkboxListener != null)
                    checkboxListener.onCheckedChanged(view, isChecked);
            });
        }
    }

    public void setCheckboxListener(CompoundButton.OnCheckedChangeListener checkboxListener) {
        this.checkboxListener = checkboxListener;
    }

    /**
     * Helper method to bind the task data with stored references of the widgets contained in the row's view holder.
     *
     * @param task task to bind with view
     */
    void bindData(Context context, TaskModel task, boolean isSelected) {
        titleTextView.setText(task.getTitle());
        descTextView.setText(task.getDescription());
        ReminderModel reminder = task.getReminder();
        if (reminder != null) {
            if (Utilities.getTimeFormatPreference(context).equals(context.getString(R.string.settings_time_format_value_24)))
                timeTextView.setText(reminder.getStartTime().to24TimeFormat());
            else
                timeTextView.setText(reminder.getStartTime().to12TimeFormat());
            if (reminder.getDurationInMinutes() > 0) {
                durationTextView.setVisibility(TextView.VISIBLE);
                durationTextView.setText(Utilities.formatDuration(context, reminder.getDurationInMinutes()));
            } else {
                durationTextView.setVisibility(TextView.GONE);
                durationTextView.setText("");
            }
        } else {
            timeTextView.setText(context.getString(R.string.no_reminder));
            durationTextView.setVisibility(TextView.GONE);
            durationTextView.setText("");
        }

        // Setting visibility of tags
        if (task.isRepeatSunday()) {
            sunTag.setVisibility(TextView.VISIBLE);
        } else {
            sunTag.setVisibility(TextView.GONE);
        }
        if (task.isRepeatMonday()) {
            monTag.setVisibility(TextView.VISIBLE);
        } else {
            monTag.setVisibility(TextView.GONE);
        }
        if (task.isRepeatTuesday()) {
            tueTag.setVisibility(TextView.VISIBLE);
        } else {
            tueTag.setVisibility(TextView.GONE);
        }
        if (task.isRepeatWednesday()) {
            wedTag.setVisibility(TextView.VISIBLE);
        } else {
            wedTag.setVisibility(TextView.GONE);
        }
        if (task.isRepeatThursday()) {
            thuTag.setVisibility(TextView.VISIBLE);
        } else {
            thuTag.setVisibility(TextView.GONE);
        }
        if (task.isRepeatFriday()) {
            friTag.setVisibility(TextView.VISIBLE);
        } else {
            friTag.setVisibility(TextView.GONE);
        }
        if (task.isRepeatSaturday()) {
            satTag.setVisibility(TextView.VISIBLE);
        } else {
            satTag.setVisibility(TextView.GONE);
        }

        // Set the first day of the week
        FlexboxLayout.LayoutParams layoutParams = (FlexboxLayout.LayoutParams) sunTag.getLayoutParams();
        if (Utilities.getFirstDayOfWeekPreference(context) == Calendar.SUNDAY) {
            // Put sunday first in the layout order
            layoutParams.setOrder(1); // All Tags have order 2. Set sunday to 1 to move it to the front.
        } else {
            layoutParams.setOrder(2);
        }

        if (task.getRepeatCountInWeek() <= 0) {
            noRepeatTag.setVisibility(TextView.VISIBLE);
        } else {
            noRepeatTag.setVisibility(TextView.GONE);
        }

        if (checkBox != null) {
            checkBox.setChecked(task.getStatus() == TaskModel.TaskStatus.COMPLETED);
            Object tag = checkBox.getTag();
            if (checkBox.isChecked()) {
                titleTextView.setPaintFlags(titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                if (tag != null) checkBox.setText(context.getString(R.string.task_completed));
            } else {
                titleTextView.setPaintFlags(titleTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                if (tag != null) checkBox.setText(context.getString(R.string.task_not_completed));
            }
        }

        // Highlight task if selected
        setSelected(context, isSelected, task.getColor());
    }

    public void hideCheckbox() {
        if (checkBox == null) return;
        checkBox.setVisibility(View.INVISIBLE);
    }

    public void showCheckbox() {
        if (checkBox == null) return;
        checkBox.setVisibility(View.VISIBLE);
    }

    private void setBackgroundColor(int color) {
        mFirstChild.setBackgroundColor(color);
    }

    private void setSelected(Context context, boolean selected, int taskColor) {
        // Cast item view into root card view
        MaterialCardView cardView = (MaterialCardView) this.itemView;
        final float DPTOPX_SCALE = context.getResources().getDisplayMetrics().density;
        if (selected) {
            // Set Stroke
            cardView.setStrokeColor(0xFFFFFFFF);
            cardView.setStrokeWidth((int) (1.5 * DPTOPX_SCALE));
            // Highlight background color
            setBackgroundColor(0x66FFFFFF & taskColor);
        } else {
            cardView.setStrokeWidth(0);
            cardView.setStrokeColor(0x00000000);
            setBackgroundColor(taskColor);
        }
    }
}
