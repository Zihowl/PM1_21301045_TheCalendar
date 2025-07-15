package com.zihowl.thecalendar.ui.subjects;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Subject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AddSubjectDialogFragment extends DialogFragment {

    private static final String KEY_POSITION = "position";
    private static final String KEY_SUBJECT_NAME = "subject_name";
    private static final String KEY_SUBJECT_SCHEDULE = "subject_schedule";

    private SubjectsViewModel viewModel;
    private TextInputEditText editTextName;
    private LinearLayout containerScheduleBlocks;

    // --- Métodos de Instancia ---
    public static AddSubjectDialogFragment newInstance() {
        return new AddSubjectDialogFragment();
    }

    public static AddSubjectDialogFragment newInstance(int position, @NonNull Subject subject) {
        AddSubjectDialogFragment fragment = new AddSubjectDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_POSITION, position);
        args.putString(KEY_SUBJECT_NAME, subject.getName());
        args.putString(KEY_SUBJECT_SCHEDULE, subject.getSchedule());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_subject, null);

        editTextName = view.findViewById(R.id.editTextSubjectName);
        containerScheduleBlocks = view.findViewById(R.id.containerScheduleBlocks);
        Button buttonAddBlock = view.findViewById(R.id.buttonAddBlock);
        buttonAddBlock.setOnClickListener(v -> addScheduleBlock(null, null, null));

        boolean isEditing = getArguments() != null;
        String dialogTitle = isEditing ? "Editar Materia" : "Añadir Materia";

        if (isEditing) {
            editTextName.setText(getArguments().getString(KEY_SUBJECT_NAME));
            populateScheduleBlocksFromString(getArguments().getString(KEY_SUBJECT_SCHEDULE));
        }

        builder.setView(view)
                .setTitle(dialogTitle)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", (dialog, id) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> saveSubject(isEditing));
        });

        return dialog;
    }

    private void addScheduleBlock(@Nullable String day, @Nullable String startTime24h, @Nullable String endTime24h) {
        LayoutInflater inflater = getLayoutInflater();
        View blockView = inflater.inflate(R.layout.item_schedule_block, containerScheduleBlocks, false);

        Spinner spinnerDay = blockView.findViewById(R.id.spinnerDay);
        TextView textViewStartTime = blockView.findViewById(R.id.textViewStartTime);
        TextView textViewEndTime = blockView.findViewById(R.id.textViewEndTime);
        ImageButton buttonRemoveBlock = blockView.findViewById(R.id.buttonRemoveBlock);

        textViewStartTime.setOnClickListener(v -> showTimePicker(textViewStartTime));
        textViewEndTime.setOnClickListener(v -> showTimePicker(textViewEndTime));
        buttonRemoveBlock.setOnClickListener(v -> containerScheduleBlocks.removeView(blockView));

        if (day != null) {
            getResources().getStringArray(R.array.week_days);
            String[] daysArray = getResources().getStringArray(R.array.week_days);
            for (int i = 0; i < daysArray.length; i++) {
                if (daysArray[i].equalsIgnoreCase(day)) {
                    spinnerDay.setSelection(i);
                    break;
                }
            }
        }

        if (startTime24h != null) {
            textViewStartTime.setText(formatTo12Hour(startTime24h));
            textViewStartTime.setTag(startTime24h);
        }
        if (endTime24h != null) {
            textViewEndTime.setText(formatTo12Hour(endTime24h));
            textViewEndTime.setTag(endTime24h);
        }

        containerScheduleBlocks.addView(blockView);
    }

    private void showTimePicker(TextView timeTextView) {
        String existingTime24h = timeTextView.getTag() instanceof String ? (String) timeTextView.getTag() : "12:00";
        String[] timeParts = existingTime24h.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Seleccionar hora")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();

            String time24h = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            timeTextView.setText(formatTo12Hour(time24h));
            timeTextView.setTag(time24h);
        });

        picker.show(getParentFragmentManager(), "TimePicker");
    }

    private void saveSubject(boolean isEditing) {
        String name = Objects.requireNonNull(editTextName.getText()).toString().trim();
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("El nombre no puede estar vacío");
            return;
        }

        String scheduleString = buildScheduleString();
        if (scheduleString == null) return;

        if (isEditing) {
            assert getArguments() != null;
            int position = getArguments().getInt(KEY_POSITION);
            viewModel.updateSubject(position, name, scheduleString);
        } else {
            viewModel.addSubject(name, scheduleString);
        }

        viewModel.finishSelectionMode();
        dismiss();
    }

    @Nullable
    private String buildScheduleString() {
        StringBuilder scheduleBuilder = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < containerScheduleBlocks.getChildCount(); i++) {
            View blockView = containerScheduleBlocks.getChildAt(i);
            Spinner spinnerDay = blockView.findViewById(R.id.spinnerDay);
            TextView textViewStartTime = blockView.findViewById(R.id.textViewStartTime);
            TextView textViewEndTime = blockView.findViewById(R.id.textViewEndTime);

            String startTimeStr = textViewStartTime.getTag() instanceof String ? (String) textViewStartTime.getTag() : null;
            String endTimeStr = textViewEndTime.getTag() instanceof String ? (String) textViewEndTime.getTag() : null;

            if (startTimeStr == null || endTimeStr == null) {
                Toast.makeText(getContext(), "Selecciona hora de inicio y fin para todos los bloques.", Toast.LENGTH_SHORT).show();
                return null;
            }

            // --- NUEVO: Validación de horario invertido ---
            try {
                Date startTime = sdf.parse(startTimeStr);
                Date endTime = sdf.parse(endTimeStr);
                if (startTime != null && endTime != null && startTime.after(endTime)) {
                    Toast.makeText(getContext(), "La hora de fin no puede ser anterior a la hora de inicio.", Toast.LENGTH_LONG).show();
                    return null;
                }
            } catch (ParseException e) {
                // Esto no debería pasar si el formato es correcto
                Toast.makeText(getContext(), "Error en el formato de hora.", Toast.LENGTH_SHORT).show();
                return null;
            }

            String day = spinnerDay.getSelectedItem().toString();
            scheduleBuilder.append(day).append(" ").append(startTimeStr).append(" - ").append(endTimeStr);
            if (i < containerScheduleBlocks.getChildCount() - 1) {
                scheduleBuilder.append("\n");
            }
        }
        return scheduleBuilder.toString();
    }

    private void populateScheduleBlocksFromString(String schedule) {
        if (schedule == null || schedule.isEmpty()) return;

        String[] lines = schedule.split("\n");
        for (String line : lines) {
            try {
                String[] parts = line.split(" ");
                String day = parts[0];
                String[] times = line.substring(day.length()).trim().split(" - ");
                addScheduleBlock(day, times[0], times[1]);
            } catch (Exception e) {
                // Ignorar líneas con formato incorrecto
            }
        }
    }

    private String formatTo12Hour(String time24h) {
        if (time24h == null || time24h.isEmpty()) return "";
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf12.format(Objects.requireNonNull(sdf24.parse(time24h)));
        } catch (ParseException | NullPointerException e) {
            return time24h;
        }
    }
}