package com.zihowl.thecalendar.ui.tasks;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.ui.subjects.SubjectsViewModel;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class AddTaskDialogFragment extends DialogFragment {

    private static final String KEY_IS_EDITING = "is_editing";
    private static final String KEY_TASK = "task";

    private TasksViewModel tasksViewModel;
    private SubjectsViewModel subjectsViewModel;
    private TextInputEditText editTextTitle, editTextDescription;
    private Spinner spinnerSubject;
    private TextView dueDateText;
    private ImageButton clearDateButton;
    private boolean isEditing = false;
    private Task originalTask;
    private Date selectedDate;

    public static AddTaskDialogFragment newInstance() {
        return new AddTaskDialogFragment();
    }

    public static AddTaskDialogFragment newInstance(Task task) {
        AddTaskDialogFragment fragment = new AddTaskDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_IS_EDITING, true);
        args.putSerializable(KEY_TASK, (Serializable) task);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tasksViewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
        subjectsViewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);
        if (getArguments() != null) {
            isEditing = getArguments().getBoolean(KEY_IS_EDITING);
            originalTask = (Task) getArguments().getSerializable(KEY_TASK);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_add_task, null);

        editTextTitle = view.findViewById(R.id.editTextTaskTitle);
        editTextDescription = view.findViewById(R.id.editTextTaskDescription);
        spinnerSubject = view.findViewById(R.id.spinnerSubjectForTask);
        dueDateText = view.findViewById(R.id.textViewTaskDueDate);
        clearDateButton = view.findViewById(R.id.buttonClearDate);
        TextView dialogTitleView = view.findViewById(R.id.dialog_task_title);

        setupSubjectSpinner();

        if (isEditing && originalTask != null) {
            dialogTitleView.setText("Editar Tarea");
            editTextTitle.setText(originalTask.getTitle());
            editTextDescription.setText(originalTask.getDescription());
            selectSpinnerValue(originalTask.getSubjectName());
            selectedDate = originalTask.getDueDate();
            updateDateLabel();
        } else {
            dialogTitleView.setText(R.string.dialog_title_new_task);
        }

        dueDateText.setOnClickListener(v -> showDatePicker());
        clearDateButton.setOnClickListener(v -> {
            selectedDate = null;
            updateDateLabel();
        });

        builder.setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (d, id) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> saveTask()));
        return dialog;
    }

    private void setupSubjectSpinner() {
        List<String> subjectNames = new ArrayList<>();
        subjectNames.add(getString(R.string.none));
        List<Subject> subjects = subjectsViewModel.subjects.getValue();
        if (subjects != null) {
            subjectNames.addAll(subjects.stream().map(Subject::getName).collect(Collectors.toList()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjectNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
    }

    private void selectSpinnerValue(String subjectName) {
        if (subjectName == null) return;
        for (int i = 0; i < spinnerSubject.getAdapter().getCount(); i++) {
            if (subjectName.equals(spinnerSubject.getAdapter().getItem(i))) {
                spinnerSubject.setSelection(i);
                break;
            }
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .setSelection(selectedDate != null ? selectedDate.getTime() : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Se debe ajustar la zona horaria para evitar problemas de un día antes/después
            TimeZone tz = TimeZone.getDefault();
            long offset = tz.getOffset(selection);
            selectedDate = new Date(selection + offset);
            updateDateLabel();
        });
        datePicker.show(getParentFragmentManager(), "DatePicker");
    }

    private void updateDateLabel() {
        if (selectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale.getDefault());
            dueDateText.setText(sdf.format(selectedDate));
            clearDateButton.setVisibility(View.VISIBLE);
        } else {
            dueDateText.setText("Seleccionar fecha");
            clearDateButton.setVisibility(View.GONE);
        }
    }

    private void saveTask() {
        String title = Objects.requireNonNull(editTextTitle.getText()).toString().trim();
        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError(getString(R.string.error_title_empty));
            return;
        }

        String description = Objects.requireNonNull(editTextDescription.getText()).toString().trim();
        String subjectName = spinnerSubject.getSelectedItem().toString();
        if (subjectName.equals(getString(R.string.none))) {
            subjectName = null;
        }

        if (isEditing) {
            tasksViewModel.updateTask(originalTask, title, description, subjectName, selectedDate, requireContext());
        } else {
            tasksViewModel.addTask(new Task(title, description, selectedDate, subjectName), requireContext());
        }

        tasksViewModel.finishSelectionMode();
        dismiss();
    }
}