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

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AddSubjectDialogFragment extends DialogFragment {

    private static final String KEY_SUBJECT = "subject";

    private SubjectsViewModel viewModel;
    private TextInputEditText editTextName;
    private TextInputEditText editTextProfessorName;
    private LinearLayout containerScheduleBlocks;
    private boolean isEditing = false;
    private Subject originalSubject;

    public static AddSubjectDialogFragment newInstance() {
        return new AddSubjectDialogFragment();
    }

    public static AddSubjectDialogFragment newInstance(@NonNull Subject subject) {
        AddSubjectDialogFragment fragment = new AddSubjectDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SUBJECT, (Serializable) subject);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);
        if (getArguments() != null) {
            originalSubject = (Subject) getArguments().getSerializable(KEY_SUBJECT);
            isEditing = (originalSubject != null);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_subject, null);

        editTextName = view.findViewById(R.id.editTextSubjectName);
        editTextProfessorName = view.findViewById(R.id.editTextProfessorName);
        containerScheduleBlocks = view.findViewById(R.id.containerScheduleBlocks);
        Button buttonAddBlock = view.findViewById(R.id.buttonAddBlock);
        buttonAddBlock.setOnClickListener(v -> addScheduleBlock(null, null, null));

        String dialogTitle = isEditing ? "Editar Materia" : "Nueva Materia";

        if (isEditing && originalSubject != null) {
            editTextName.setText(originalSubject.getName());
            editTextProfessorName.setText(originalSubject.getProfessorName());
            populateScheduleBlocksFromString(originalSubject.getSchedule());
        }

        builder.setView(view)
                .setTitle(dialogTitle)
                .setPositiveButton("Guardar", null) // Se configura el listener abajo
                .setNegativeButton("Cancelar", (dialog, id) -> dismiss());

        AlertDialog dialog = builder.create();

        // Se usa setOnShowListener para evitar que el diálogo se cierre automáticamente al presionar "Guardar"
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> saveSubject());
        });

        return dialog;
    }

    private void addScheduleBlock(@Nullable String day, @Nullable String startTime24h, @Nullable String endTime24h) {
        View blockView = getLayoutInflater().inflate(R.layout.item_schedule_block, containerScheduleBlocks, false);

        Spinner spinnerDay = blockView.findViewById(R.id.spinnerDay);
        TextView textViewStartTime = blockView.findViewById(R.id.textViewStartTime);
        TextView textViewEndTime = blockView.findViewById(R.id.textViewEndTime);
        ImageButton buttonRemoveBlock = blockView.findViewById(R.id.buttonRemoveBlock);

        textViewStartTime.setOnClickListener(v -> showTimePicker(textViewStartTime));
        textViewEndTime.setOnClickListener(v -> showTimePicker(textViewEndTime));
        buttonRemoveBlock.setOnClickListener(v -> containerScheduleBlocks.removeView(blockView));

        if (day != null) {
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
            textViewStartTime.setTag(startTime24h); // Guardamos la hora en formato 24h en el tag
        }
        if (endTime24h != null) {
            textViewEndTime.setText(formatTo12Hour(endTime24h));
            textViewEndTime.setTag(endTime24h); // Guardamos la hora en formato 24h en el tag
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
            String time24h = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
            timeTextView.setText(formatTo12Hour(time24h));
            timeTextView.setTag(time24h);
        });

        picker.show(getParentFragmentManager(), "TimePicker");
    }

    private void saveSubject() {
        String name = Objects.requireNonNull(editTextName.getText()).toString().trim();
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("El nombre no puede estar vacío");
            return;
        }

        String professorName = Objects.requireNonNull(editTextProfessorName.getText()).toString().trim();
        String scheduleString = buildScheduleString();

        // Si el horario es inválido, buildScheduleString ya mostró un Toast y devolvió null.
        if (scheduleString == null) {
            return;
        }

        if (isEditing) {
            viewModel.updateSubject(originalSubject, name, professorName, scheduleString);
            Toast.makeText(getContext(), "Materia '" + name + "' actualizada", Toast.LENGTH_SHORT).show();
        } else {
            // Pasamos el contexto para que el ViewModel pueda mostrar el Toast
            viewModel.addSubject(name, professorName, scheduleString, requireContext());
        }

        viewModel.finishSelectionMode();
        dismiss(); // Cierra el diálogo solo si todo es correcto
    }

    @Nullable
    private String buildScheduleString() {
        StringBuilder scheduleBuilder = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < containerScheduleBlocks.getChildCount(); i++) {
            View blockView = containerScheduleBlocks.getChildAt(i);
            TextView tvStart = blockView.findViewById(R.id.textViewStartTime);
            TextView tvEnd = blockView.findViewById(R.id.textViewEndTime);
            String startTimeStr = tvStart.getTag() instanceof String ? (String) tvStart.getTag() : null;
            String endTimeStr = tvEnd.getTag() instanceof String ? (String) tvEnd.getTag() : null;

            if (startTimeStr == null || endTimeStr == null) {
                Toast.makeText(getContext(), "Define hora de inicio y fin para todos los bloques.", Toast.LENGTH_SHORT).show();
                return null;
            }

            try {
                Date startTime = sdf.parse(startTimeStr);
                Date endTime = sdf.parse(endTimeStr);
                if (startTime != null && endTime != null && startTime.after(endTime)) {
                    Toast.makeText(getContext(), "La hora de fin no puede ser anterior a la de inicio.", Toast.LENGTH_LONG).show();
                    return null;
                }
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Formato de hora inválido.", Toast.LENGTH_SHORT).show();
                return null;
            }

            Spinner spinnerDay = blockView.findViewById(R.id.spinnerDay);
            scheduleBuilder.append(spinnerDay.getSelectedItem().toString()).append(" ").append(startTimeStr).append(" - ").append(endTimeStr);
            if (i < containerScheduleBlocks.getChildCount() - 1) {
                scheduleBuilder.append("\n");
            }
        }
        return scheduleBuilder.toString();
    }

    private void populateScheduleBlocksFromString(String schedule) {
        if (schedule == null || schedule.isEmpty()) return;
        containerScheduleBlocks.removeAllViews(); // Limpiamos vistas previas
        for (String line : schedule.split("\n")) {
            try {
                String[] parts = line.split(" ");
                String day = parts[0];
                String[] times = line.substring(day.length()).trim().split(" - ");
                addScheduleBlock(day, times[0], times[1]);
            } catch (Exception ignored) {}
        }
    }

    private String formatTo12Hour(String time24h) {
        if (time24h == null || time24h.isEmpty()) return "Hora";
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf12.format(Objects.requireNonNull(sdf24.parse(time24h)));
        } catch (Exception e) {
            return "Hora";
        }
    }
}