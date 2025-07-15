package com.zihowl.thecalendar.ui.schedule;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ScheduleFragment extends Fragment {

    // --- Clase interna para la gestión de eventos ---
    private static class Event {
        final Subject subject;
        final float startMinutes;
        final float endMinutes;
        int column;
        int totalColumns;

        Event(Subject subject, float startMinutes, float endMinutes) {
            this.subject = subject;
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
        }
    }

    private SubjectsViewModel subjectsViewModel;
    // Usamos FrameLayout para superponer los eventos sobre la cuadrícula de horas
    private FrameLayout scheduleContainer;
    private LinearLayout hoursColumn;
    private Spinner daySelectorSpinner;
    private ScrollView scheduleScrollView;
    private TextView emptyScheduleText;
    private View timeGrid; // La cuadrícula de fondo

    private static final int HOUR_HEIGHT_DP = 60;
    private static final int DAY_WIDTH_DP = 300;
    private static final int START_HOUR = 0;
    private static final int END_HOUR = 24;

    private final Map<String, Integer> subjectColors = new HashMap<>();
    private int colorIndex = 0;
    private final int[] eventColors = {
            Color.parseColor("#EF5350"), Color.parseColor("#AB47BC"),
            Color.parseColor("#42A5F5"), Color.parseColor("#26A69A"),
            Color.parseColor("#FFEE58"), Color.parseColor("#FFA726"),
            Color.parseColor("#78909C"), Color.parseColor("#EC407A")
    };

    private List<String> weekDays;
    private String selectedDay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflamos un layout base diferente que facilita la superposición
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduleContainer = view.findViewById(R.id.schedule_container);
        hoursColumn = view.findViewById(R.id.hours_column);
        daySelectorSpinner = view.findViewById(R.id.day_selector_spinner);
        scheduleScrollView = view.findViewById(R.id.schedule_scroll_view);
        emptyScheduleText = view.findViewById(R.id.empty_schedule_text);

        weekDays = Arrays.asList(getResources().getStringArray(R.array.week_days));
        subjectsViewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);

        setupDaySelector();
        setupGrid(); // Dibuja la cuadrícula de fondo una sola vez
        observeSubjects();
    }

    private void setupDaySelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, weekDays);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySelectorSpinner.setAdapter(adapter);

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int todayIndex = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - 2;
        daySelectorSpinner.setSelection(todayIndex);
        selectedDay = weekDays.get(todayIndex);

        daySelectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDay = weekDays.get(position);
                observeSubjects();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupGrid() {
        hoursColumn.removeAllViews();
        // Crear las líneas de las horas
        for (int hour = START_HOUR; hour < END_HOUR; hour++) {
            hoursColumn.addView(createHourTextView(hour));
        }

        // Crear la cuadrícula de fondo
        timeGrid = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(DAY_WIDTH_DP),
                dpToPx(HOUR_HEIGHT_DP * (END_HOUR - START_HOUR))
        );

        // Dibujar las líneas horizontales en la cuadrícula
        GradientDrawable gridDrawable = new GradientDrawable();
        gridDrawable.setStroke(1, Color.LTGRAY);
        // Aquí podrías añadir más lógica para dibujar líneas si fuera necesario

        timeGrid.setBackground(gridDrawable);
        timeGrid.setLayoutParams(params);
        scheduleContainer.addView(timeGrid);
    }

    private void observeSubjects() {
        subjectsViewModel.subjects.observe(getViewLifecycleOwner(), subjects -> {
            if (subjects != null && selectedDay != null) {
                drawSubjectsForDay(subjects, selectedDay);
            }
        });
    }

    private void drawSubjectsForDay(List<Subject> subjects, String dayName) {
        // Limpiar solo los eventos, no la cuadrícula de fondo
        clearEvents();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        List<Event> eventsToday = new ArrayList<>();
        float firstEventStart = -1;

        for (Subject subject : subjects) {
            for (String line : subject.getSchedule().split("\n")) {
                if (!line.trim().startsWith(dayName)) continue;
                try {
                    String[] parts = line.split(" ");
                    String[] times = line.substring(parts[0].length()).trim().split(" - ");
                    float startMinutes = parseMinutes(sdf, times[0]);
                    float endMinutes = parseMinutes(sdf, times[1]);

                    if (startMinutes < endMinutes) {
                        eventsToday.add(new Event(subject, startMinutes, endMinutes));
                        if (firstEventStart == -1 || startMinutes < firstEventStart) {
                            firstEventStart = startMinutes;
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        if (eventsToday.isEmpty()) {
            emptyScheduleText.setVisibility(View.VISIBLE);
            scheduleScrollView.setAlpha(0); // Ocultar en lugar de remover
        } else {
            emptyScheduleText.setVisibility(View.GONE);
            scheduleScrollView.setAlpha(1);
            renderEvents(eventsToday);

            float scrollPos = (firstEventStart > 0) ? ((firstEventStart / 60f) * HOUR_HEIGHT_DP) : 0;
            scheduleScrollView.post(() -> scheduleScrollView.scrollTo(0, dpToPx((int) scrollPos)));
        }
    }

    // --- Algoritmo de renderizado robusto ---
    private void renderEvents(List<Event> events) {
        if (events.isEmpty()) return;

        // Ordenar por hora de inicio
        Collections.sort(events, Comparator.comparingDouble(e -> e.startMinutes));

        // Lista de grupos de eventos que se superponen
        List<List<Event>> collisionGroups = new ArrayList<>();
        for (Event event : events) {
            boolean placed = false;
            for (List<Event> group : collisionGroups) {
                if (group.stream().anyMatch(e -> doEventsOverlap(e, event))) {
                    group.add(event);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Event> newGroup = new ArrayList<>();
                newGroup.add(event);
                collisionGroups.add(newGroup);
            }
        }

        // Procesar cada grupo de colisión por separado
        for (List<Event> group : collisionGroups) {
            processCollisionGroup(group);
        }
    }

    private void processCollisionGroup(List<Event> group) {
        if (group.isEmpty()) return;

        // Determinar el número de columnas necesarias para este grupo
        List<List<Event>> columns = new ArrayList<>();
        columns.add(new ArrayList<>());

        for (Event event : group) {
            boolean placed = false;
            for (List<Event> column : columns) {
                if (column.isEmpty() || !doEventsOverlap(column.get(column.size() - 1), event)) {
                    column.add(event);
                    event.column = columns.indexOf(column);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Event> newColumn = new ArrayList<>();
                newColumn.add(event);
                event.column = columns.size();
                columns.add(newColumn);
            }
        }

        int totalColumns = columns.size();
        for (Event event : group) {
            event.totalColumns = totalColumns;
            drawEventView(event);
        }
    }


    private void drawEventView(Event event) {
        float height = ((event.endMinutes - event.startMinutes) / 60f) * HOUR_HEIGHT_DP;
        float topMargin = (event.startMinutes / 60f) * HOUR_HEIGHT_DP;

        int colWidth = DAY_WIDTH_DP / event.totalColumns;
        int leftMargin = event.column * dpToPx(colWidth);

        TextView eventView = createEventView(event.subject, event.startMinutes, event.endMinutes);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dpToPx(colWidth), dpToPx((int) height));
        params.topMargin = dpToPx((int) topMargin);
        params.leftMargin = leftMargin;
        params.gravity = Gravity.TOP | Gravity.START;

        scheduleContainer.addView(eventView, params);
    }

    private boolean doEventsOverlap(Event e1, Event e2) {
        return e1.startMinutes < e2.endMinutes && e2.startMinutes < e1.endMinutes;
    }

    private float parseMinutes(SimpleDateFormat sdf, String time) throws ParseException {
        Date date = sdf.parse(time);
        Objects.requireNonNull(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    private void clearEvents() {
        // Empezar en 1 para no remover la cuadrícula de fondo
        for (int i = scheduleContainer.getChildCount() - 1; i >= 1; i--) {
            scheduleContainer.removeViewAt(i);
        }
    }

    private TextView createHourTextView(int hour) {
        TextView textView = new TextView(requireContext());
        textView.setText(String.format(Locale.getDefault(), "%02d:00", hour));
        textView.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(HOUR_HEIGHT_DP)));
        return textView;
    }

    private TextView createEventView(Subject subject, float startMinutes, float endMinutes) {
        TextView eventView = new TextView(requireContext());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR_OF_DAY, (int) (startMinutes / 60));
        startCal.set(Calendar.MINUTE, (int) (startMinutes % 60));

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, (int) (endMinutes / 60));
        endCal.set(Calendar.MINUTE, (int) (endMinutes % 60));

        eventView.setText(String.format("%s\n%s - %s", subject.getName(), timeFormat.format(startCal.getTime()), timeFormat.format(endCal.getTime())));

        eventView.setTextColor(Color.WHITE);
        eventView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        eventView.setGravity(Gravity.CENTER);
        eventView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(4));
        background.setColor(getSubjectColor(subject.getName()));
        background.setStroke(dpToPx(1), Color.DKGRAY);
        eventView.setBackground(background);
        return eventView;
    }

    private int getSubjectColor(String subjectName) {
        if (!subjectColors.containsKey(subjectName)) {
            int color = eventColors[colorIndex % eventColors.length];
            subjectColors.put(subjectName, color);
            colorIndex = (colorIndex + 1) % eventColors.length;
        }
        return Objects.requireNonNull(subjectColors.get(subjectName));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}