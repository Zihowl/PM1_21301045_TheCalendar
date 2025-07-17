package com.zihowl.thecalendar.ui.notes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.domain.usecase.note.AddNoteUseCase;
import com.zihowl.thecalendar.domain.usecase.note.DeleteNotesUseCase;
import com.zihowl.thecalendar.domain.usecase.note.GetNotesUseCase;
import com.zihowl.thecalendar.domain.usecase.note.UpdateNoteUseCase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NotesViewModel extends ViewModel {

    private final GetNotesUseCase getNotesUseCase;
    private final AddNoteUseCase addNoteUseCase;
    private final UpdateNoteUseCase updateNoteUseCase;
    private final DeleteNotesUseCase deleteNotesUseCase;

    private final MutableLiveData<List<Note>> _notes = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Note>> notes = _notes;

    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Set<Note>> _selectedNotes = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Note>> selectedNotes = _selectedNotes;

    public NotesViewModel(GetNotesUseCase get, AddNoteUseCase add, UpdateNoteUseCase update, DeleteNotesUseCase delete) {
        this.getNotesUseCase = get;
        this.addNoteUseCase = add;
        this.updateNoteUseCase = update;
        this.deleteNotesUseCase = delete;
        loadNotes();
    }

    public void loadNotes() { _notes.setValue(getNotesUseCase.execute()); }

    public void addNote(Note note) {
        addNoteUseCase.execute(note);
        loadNotes();
    }

    public void updateNote(Note note, String newTitle, String newContent, String newSubjectName) {
        note.setTitle(newTitle);
        note.setContent(newContent);
        note.setSubjectName(newSubjectName);
        updateNoteUseCase.execute(note);
        loadNotes();
    }

    public void deleteSelectedNotes() {
        deleteNotesUseCase.execute(new ArrayList<>(_selectedNotes.getValue()));
        finishSelectionMode();
        loadNotes();
    }

    public void toggleSelection(Note note) {
        Set<Note> selected = new LinkedHashSet<>(_selectedNotes.getValue() != null ? _selectedNotes.getValue() : new LinkedHashSet<>());
        if (selected.contains(note)) {
            selected.remove(note);
        } else {
            selected.add(note);
        }
        _selectedNotes.setValue(selected);

        if (selected.isEmpty() && Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            finishSelectionMode();
        } else if (!selected.isEmpty() && !Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            _isSelectionMode.setValue(true);
        }
    }

    public void finishSelectionMode() {
        _selectedNotes.setValue(new LinkedHashSet<>());
        _isSelectionMode.setValue(false);
    }
}