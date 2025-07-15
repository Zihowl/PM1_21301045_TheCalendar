package com.zihowl.thecalendar.ui.schedule;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.ui.subjects.SubjectsViewModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ScheduleFragment extends Fragment {

    private SubjectsViewModel subjectsViewModel;
    private GridLayout scheduleGrid;
    private LinearLayout daysHeader;
    private LinearLayout hoursColumn;

    private static final int HOUR_HEIGHT_DP = 60;
    private static final int DAY_WIDTH_DP = 120;
    private static final int START_HOUR = 7;
    private static final int END_HOUR = 22;

    private final Map<String, Integer> subjectColors = new HashMap<>();
    private int colorIndex = 0;
    private final int[] eventColors = {
            Color.parseColor("#EF5350"), Color.parseColor("#AB47BC"),
            Color.parseColor("#42A5F5"), Color.parseColor("#26A69A"),
            Color.parseColor("#FFEE58"), Color.parseColor("#FFA726"),
            Color.parseColor("#78909C"), Color.parseColor("#EC407A")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduleGrid = view.findViewById(R.id.schedule_grid);
        daysHeader = view.findViewById(R.id.days_header);
        hoursColumn = view.findViewById(R.id.hours_column);

        subjectsViewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);

        setupGrid();
        observeSubjects();
    }

    private void setupGrid() {
        scheduleGrid.removeAllViews();
        daysHeader.removeAllViews();
        hoursColumn.removeAllViews();

        List<String> weekDays = Arrays.asList(getResources().getStringArray(R.array.week_days));
        scheduleGrid.setColumnCount(weekDays.size());

        for (String day : weekDays) {
            TextView dayView = createHeaderTextView(day);
            daysHeader.addView(dayView);
        }

        for (int hour = START_HOUR; hour < END_HOUR; hour++) {
            TextView hourView = createHourTextView(hour);
            hoursColumn.addView(hourView);

            for (int i = 0; i < weekDays.size(); i++) {
                View cell = createCellView();
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(hour - START_HOUR, 1, 1f),
                        GridLayout.spec(i, 1, 1f)
                );
                params.width = dpToPx(DAY_WIDTH_DP);
                params.height = dpToPx(HOUR_HEIGHT_DP);
                cell.setLayoutParams(params);
                scheduleGrid.addView(cell);
            }
        }
    }

    private void observeSubjects() {
        subjectsViewModel.subjects.observe(getViewLifecycleOwner(), subjects -> {
            if (subjects == null) return;
            clearEventsFromGrid();
            drawSubjects(subjects);
        });
    }

    private void drawSubjects(List<Subject> subjects) {
        List<String> weekDays = Arrays.asList(getResources().getStringArray(R.array.week_days));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Map<String, List<View>> collisionMap = new HashMap<>();

        for (Subject subject : subjects) {
            String[] scheduleLines = subject.getSchedule().split("\n");
            for (String line : scheduleLines) {
                try {
                    String[] parts = line.split(" ");
                    String dayName = parts[0];
                    String[] times = line.substring(dayName.length()).trim().split(" - ");
                    Date startTime = sdf.parse(times[0]);
                    Date endTime = sdf.parse(times[1]);

                    if (startTime == null || endTime == null) continue;
                    int dayIndex = weekDays.indexOf(dayName);
                    if (dayIndex == -1) continue;

                    float startMinutes = (startTime.getHours() * 60 + startTime.getMinutes());
                    float endMinutes = (endTime.getHours() * 60 + endTime.getMinutes());

                    float topMargin = ((startMinutes - (START_HOUR * 60f)) / 60f) * HOUR_HEIGHT_DP;
                    float height = ((endMinutes - startMinutes) / 60f) * HOUR_HEIGHT_DP;

                    TextView eventView = createEventView(subject, startTime, endTime);

                    String key = dayIndex + "-" + (int)(startMinutes / 60);
                    if (!collisionMap.containsKey(key)) {
                        collisionMap.put(key, new ArrayList<>());
                    }
                    Objects.requireNonNull(collisionMap.get(key)).add(eventView);

                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = dpToPx(DAY_WIDTH_DP);
                    params.height = dpToPx((int) height);
                    params.topMargin = dpToPx((int) topMargin);
                    params.columnSpec = GridLayout.spec(dayIndex);
                    params.rowSpec = GridLayout.spec(0, END_HOUR - START_HOUR);
                    params.setGravity(Gravity.TOP);

                    eventView.setLayoutParams(params);
                    scheduleGrid.addView(eventView);

                } catch (ParseException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
        adjustForCollisions(collisionMap);
    }

    private void adjustForCollisions(Map<String, List<View>> collisionMap) {
        for (List<View> collidingViews : collisionMap.values()) {
            if (collidingViews.size() > 1) {
                int count = collidingViews.size();
                int newWidth = dpToPx(DAY_WIDTH_DP) / count;
                for (int i = 0; i < count; i++) {
                    View view = collidingViews.get(i);
                    GridLayout.LayoutParams params = (GridLayout.LayoutParams) view.getLayoutParams();
                    params.width = newWidth;
                    params.leftMargin = i * newWidth;
                    view.setLayoutParams(params);
                }
            }
        }
    }

    private void clearEventsFromGrid() {
        for (int i = scheduleGrid.getChildCount() - 1; i >= 0; i--) {
            View child = scheduleGrid.getChildAt(i);
            if (child.getTag() != null && child.getTag().equals("event")) {
                scheduleGrid.removeViewAt(i);
            }
        }
    }

    private TextView createHeaderTextView(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(DAY_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT));
        return textView;
    }

    private TextView createHourTextView(int hour) {
        TextView textView = new TextView(getContext());
        textView.setText(String.format(Locale.getDefault(), "%02d:00", hour));
        textView.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(HOUR_HEIGHT_DP)));
        return textView;
    }

    private View createCellView() {
        View cell = new View(getContext());
        // CREACIÓN DEL BORDE EN CÓDIGO
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        border.setStroke(1, Color.parseColor("#E0E0E0"));
        cell.setBackground(border);
        return cell;
    }

    private TextView createEventView(Subject subject, Date start, Date end) {
        TextView eventView = new TextView(getContext());
        eventView.setTag("event");

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        eventView.setText(String.format("%s\n%s - %s", subject.getName(), timeFormat.format(start), timeFormat.format(end)));

        eventView.setTextColor(Color.WHITE);
        eventView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        eventView.setGravity(Gravity.TOP | Gravity.START);
        eventView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

        // CREACIÓN DEL FONDO EN CÓDIGO
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(4));
        int color = getSubjectColor(subject.getName());
        background.setColor(color);
        background.setStroke(dpToPx(1), Color.DKGRAY);
        eventView.setBackground(background);

        return eventView;
    }

    private int getSubjectColor(String subjectName) {
        if (!subjectColors.containsKey(subjectName)) {
            int color = eventColors[colorIndex % eventColors.length];
            subjectColors.put(subjectName, color);
            colorIndex++;
        }
        return Objects.requireNonNull(subjectColors.get(subjectName));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}