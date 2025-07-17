package com.zihowl.thecalendar.ui.notes;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.ui.subjects.SubjectsViewModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AddNoteDialogFragment extends DialogFragment {

    private static final String KEY_NOTE = "note";

    private NotesViewModel viewModel;
    private SubjectsViewModel subjectsViewModel;
    private TextInputEditText editTextTitle, editTextContent;
    private Spinner spinnerSubject;
    private boolean isEditing = false;
    private Note originalNote;

    public static AddNoteDialogFragment newInstance() {
        return new AddNoteDialogFragment();
    }

    public static AddNoteDialogFragment newInstance(@NonNull Note note) {
        AddNoteDialogFragment fragment = new AddNoteDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_NOTE, (Serializable) note);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
        subjectsViewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);
        if (getArguments() != null) {
            originalNote = (Note) getArguments().getSerializable(KEY_NOTE);
            isEditing = (originalNote != null);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_add_note, null);

        editTextTitle = view.findViewById(R.id.editTextNoteTitle);
        editTextContent = view.findViewById(R.id.editTextNoteContent);
        spinnerSubject = view.findViewById(R.id.spinnerSubject);
        TextView dialogTitleView = view.findViewById(R.id.dialog_title);

        setupSubjectSpinner();

        dialogTitleView.setText(isEditing ? R.string.dialog_title_edit_note : R.string.dialog_title_new_note);

        if (isEditing) {
            editTextTitle.setText(originalNote.getTitle());
            editTextContent.setText(originalNote.getContent());
            selectSpinnerValue(originalNote.getSubjectName());
        }

        builder.setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (dialog, id) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> saveNote());
        });

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

    private void saveNote() {
        String title = Objects.requireNonNull(editTextTitle.getText()).toString().trim();
        String content = Objects.requireNonNull(editTextContent.getText()).toString().trim();
        String subjectName = spinnerSubject.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError(getString(R.string.error_title_empty));
            return;
        }

        if (subjectName.equals(getString(R.string.none))) {
            subjectName = null;
        }

        if (isEditing) {
            viewModel.updateNote(originalNote, title, content, subjectName);
        } else {
            viewModel.addNote(new Note(title, content, subjectName));
        }

        viewModel.finishSelectionMode();
        dismiss();
    }
}