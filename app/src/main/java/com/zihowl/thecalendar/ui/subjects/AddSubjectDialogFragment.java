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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View blockView = inflater.inflate(R.layout.item_schedule_block, containerScheduleBlocks, false);

        Spinner spinnerDay = blockView.findViewById(R.id.spinnerDay);
        TextView textViewStartTime = blockView.findViewById(R.id.textViewStartTime);
        TextView textViewEndTime = blockView.findViewById(R.id.textViewEndTime);
        ImageButton buttonRemoveBlock = blockView.findViewById(R.id.buttonRemoveBlock);

        textViewStartTime.setOnClickListener(v -> showTimePicker(textViewStartTime));
        textViewEndTime.setOnClickListener(v -> showTimePicker(textViewEndTime));
        buttonRemoveBlock.setOnClickListener(v -> containerScheduleBlocks.removeView(blockView));

        if (day != null && getResources().getStringArray(R.array.week_days) != null) {
            String[] daysArray = getResources().getStringArray(R.array.week_days);
            for (int i = 0; i < daysArray.length; i++) {
                if (daysArray[i].equalsIgnoreCase(day)) {
                    spinnerDay.setSelection(i);
                    break;
                }
            }
        }

        // CAMBIO: Formatear la hora para mostrarla en 12h y guardar la de 24h en el tag
        if (startTime24h != null) {
            textViewStartTime.setText(formatTo12Hour(startTime24h));
            textViewStartTime.setTag(startTime24h); // Guardamos el formato 24h
        }
        if (endTime24h != null) {
            textViewEndTime.setText(formatTo12Hour(endTime24h));
            textViewEndTime.setTag(endTime24h); // Guardamos el formato 24h
        }

        containerScheduleBlocks.addView(blockView);
    }

    private void showTimePicker(TextView timeTextView) {
        String existingTime24h = timeTextView.getTag() instanceof String ? (String) timeTextView.getTag() : "12:00";
        String[] timeParts = existingTime24h.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // CAMBIO: Usar formato de 12 horas para el diálogo
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Seleccionar hora")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();

            // NUEVO: Guardar en formato 24h, mostrar en 12h.
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
        // Si buildScheduleString devuelve null, significa que hubo un error de validación.
        if (scheduleString == null) return;

        if (isEditing) {
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
        for (int i = 0; i < containerScheduleBlocks.getChildCount(); i++) {
            View blockView = containerScheduleBlocks.getChildAt(i);
            Spinner spinnerDay = blockView.findViewById(R.id.spinnerDay);
            TextView textViewStartTime = blockView.findViewById(R.id.textViewStartTime);
            TextView textViewEndTime = blockView.findViewById(R.id.textViewEndTime);

            // CAMBIO: Obtener la hora del tag (formato 24h) en lugar del texto
            String startTime = textViewStartTime.getTag() instanceof String ? (String) textViewStartTime.getTag() : null;
            String endTime = textViewEndTime.getTag() instanceof String ? (String) textViewEndTime.getTag() : null;

            // Validar que se haya seleccionado hora de inicio y fin
            if (startTime == null || endTime == null) {
                Toast.makeText(getContext(), "Por favor, selecciona una hora de inicio y fin para todos los bloques.", Toast.LENGTH_SHORT).show();
                return null; // Devuelve null para indicar error
            }

            String day = spinnerDay.getSelectedItem().toString();
            scheduleBuilder.append(day).append(" ").append(startTime).append("-").append(endTime);
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
                // CAMBIO: Dividir por " " para separar el día del resto.
                // Ejemplo: "Lunes 07:00 - 08:40" -> parts[0]="Lunes", parts[1]="07:00", parts[2]="-", parts[3]="08:40"
                String[] parts = line.split(" ");
                String day = parts[0];

                // Unir el resto en caso de que el nombre del día tuviera espacios (no aplica aquí, pero es más robusto)
                // String schedulePart = line.substring(day.length()).trim(); // "07:00 - 08:40"

                // CORRECCIÓN CLAVE: Dividir por " - " (con espacios) para obtener las horas.
                String[] times = line.substring(day.length()).trim().split(" - ");
                String startTime = times[0];
                String endTime = times[1];

                addScheduleBlock(day, startTime, endTime);
            } catch (Exception e) {
                // Ignorar líneas con formato incorrecto, por si acaso.
            }
        }
    }

    // NUEVO: Helper para formatear la hora de 24h a 12h con AM/PM
    private String formatTo12Hour(String time24h) {
        if (time24h == null || time24h.isEmpty()) return "";
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf12.format(Objects.requireNonNull(sdf24.parse(time24h)));
        } catch (ParseException | NullPointerException e) {
            return time24h; // Devuelve el original si falla
        }
    }
}