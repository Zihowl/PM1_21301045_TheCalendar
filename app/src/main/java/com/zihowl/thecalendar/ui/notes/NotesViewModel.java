package com.zihowl.thecalendar.ui.notes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.domain.usecase.note.GetNotesUseCase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NotesViewModel extends ViewModel {

    // --- Dependencia (Caso de Uso) ---
    private final GetNotesUseCase getNotesUseCase;

    // --- LiveData para la UI ---
    private final MutableLiveData<List<Note>> _notes = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Note>> notes = _notes;

    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Set<Note>> _selectedNotes = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Note>> selectedNotes = _selectedNotes;

    /**
     * Constructor que recibe las dependencias (Casos de Uso) a través del ViewModelFactory.
     */
    public NotesViewModel(GetNotesUseCase getNotesUseCase) {
        this.getNotesUseCase = getNotesUseCase;
        loadNotes(); // Carga inicial de notas
    }

    /**
     * Carga o recarga la lista de notas desde el repositorio.
     */
    public void loadNotes() {
        _notes.setValue(getNotesUseCase.execute());
    }

    /**
     * Añade una nueva nota.
     * En una implementación completa, esto llamaría a un AddNoteUseCase.
     */
    public void addNote(Note note) {
        // En un futuro, aquí se llamaría a un AddNoteUseCase
        loadNotes();
    }

    /**
     * Actualiza una nota existente.
     * En una implementación completa, esto llamaría a un UpdateNoteUseCase.
     */
    public void updateNote(int position, String title, String content, String subjectName) {
        // En un futuro, aquí se llamaría a un UpdateNoteUseCase
        loadNotes();
    }

    /**
     * Elimina las notas seleccionadas.
     * En una implementación completa, esto llamaría a un DeleteNotesUseCase.
     */
    public void deleteSelectedNotes() {
        // En un futuro, aquí se llamaría a un DeleteNotesUseCase
        finishSelectionMode();
        loadNotes();
    }

    // --- Lógica de Selección (sin cambios) ---

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