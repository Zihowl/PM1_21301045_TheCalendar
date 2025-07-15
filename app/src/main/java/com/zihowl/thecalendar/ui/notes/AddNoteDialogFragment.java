package com.zihowl.thecalendar.ui.notes;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors; // Asegúrate de que este import exista

public class AddNoteDialogFragment extends DialogFragment {

    private static final String KEY_POSITION = "position";
    private static final String KEY_NOTE_TITLE = "note_title";
    private static final String KEY_NOTE_CONTENT = "note_content";
    private static final String KEY_NOTE_SUBJECT = "note_subject";

    private NotesViewModel viewModel;
    private SubjectsViewModel subjectsViewModel;
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextContent;
    private Spinner spinnerSubject;

    @SuppressWarnings("unused")
    public static AddNoteDialogFragment newInstance() {
        return new AddNoteDialogFragment();
    }

    public static AddNoteDialogFragment newInstance(int position, @NonNull Note note) {
        AddNoteDialogFragment fragment = new AddNoteDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_POSITION, position);
        args.putString(KEY_NOTE_TITLE, note.getTitle());
        args.putString(KEY_NOTE_CONTENT, note.getContent());
        args.putString(KEY_NOTE_SUBJECT, note.getSubjectName());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
        subjectsViewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_note, null);

        editTextTitle = view.findViewById(R.id.editTextNoteTitle);
        editTextContent = view.findViewById(R.id.editTextNoteContent);
        spinnerSubject = view.findViewById(R.id.spinnerSubject);
        TextView dialogTitleView = view.findViewById(R.id.dialog_title);

        setupSubjectSpinner();

        boolean isEditing = getArguments() != null;
        dialogTitleView.setText(isEditing ? R.string.dialog_title_edit_note : R.string.dialog_title_new_note);

        if (isEditing) {
            editTextTitle.setText(getArguments().getString(KEY_NOTE_TITLE));
            editTextContent.setText(getArguments().getString(KEY_NOTE_CONTENT));
            selectSpinnerValue(getArguments().getString(KEY_NOTE_SUBJECT));
        }

        builder.setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (dialog, id) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> saveNote(isEditing));
        });

        return dialog;
    }

    private void setupSubjectSpinner() {
        List<Subject> subjects = subjectsViewModel.subjects.getValue();
        List<String> subjectNames = new ArrayList<>();
        subjectNames.add(getString(R.string.none));
        if (subjects != null) {
            // CORRECCIÓN: Usar collect(Collectors.toList()) para máxima compatibilidad
            subjectNames.addAll(subjects.stream().map(Subject::getName).collect(Collectors.toList()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjectNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
    }

    private void selectSpinnerValue(String subjectName) {
        if (subjectName == null) return;
        SpinnerAdapter adapter = spinnerSubject.getAdapter();

        // CORRECCIÓN: El warning de "Unchecked Cast" se soluciona con este if
        if (adapter instanceof ArrayAdapter) {
            @SuppressWarnings("unchecked") // Ahora el cast es seguro, suprimimos el warning aquí
            ArrayAdapter<String> stringArrayAdapter = (ArrayAdapter<String>) adapter;
            for (int i = 0; i < stringArrayAdapter.getCount(); i++) {
                if (subjectName.equals(stringArrayAdapter.getItem(i))) {
                    spinnerSubject.setSelection(i);
                    break;
                }
            }
        }
    }

    private void saveNote(boolean isEditing) {
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
            assert getArguments() != null;
            int position = getArguments().getInt(KEY_POSITION);
            viewModel.updateNote(position, title, content, subjectName);
        } else {
            viewModel.addNote(new Note(title, content, subjectName));
        }

        viewModel.finishSelectionMode();
        dismiss();
    }
}