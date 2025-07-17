package com.zihowl.thecalendar.ui.subjects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import com.zihowl.thecalendar.domain.usecase.subject.AddSubjectUseCase;
import com.zihowl.thecalendar.domain.usecase.subject.DeleteSubjectsUseCase;
import com.zihowl.thecalendar.domain.usecase.subject.GetSubjectsUseCase;
import com.zihowl.thecalendar.domain.usecase.subject.UpdateSubjectUseCase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SubjectsViewModel extends ViewModel {

    private final GetSubjectsUseCase getSubjectsUseCase;
    private final AddSubjectUseCase addSubjectUseCase;
    private final UpdateSubjectUseCase updateSubjectUseCase;
    private final DeleteSubjectsUseCase deleteSubjectsUseCase;
    private final TheCalendarRepository repository;

    private final MutableLiveData<List<Subject>> _subjects = new MutableLiveData<>();
    public final LiveData<List<Subject>> subjects = _subjects;

    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Set<Subject>> _selectedSubjects = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Subject>> selectedSubjects = _selectedSubjects;

    public SubjectsViewModel(GetSubjectsUseCase get, AddSubjectUseCase add, UpdateSubjectUseCase update, DeleteSubjectsUseCase delete) {
        this.getSubjectsUseCase = get;
        this.addSubjectUseCase = add;
        this.updateSubjectUseCase = update;
        this.deleteSubjectsUseCase = delete;
        this.repository = TheCalendarRepository.getInstance(new RealmDataSource());
        loadSubjects();
    }

    public void loadSubjects() {
        _subjects.setValue(getSubjectsUseCase.execute());
    }

    public void addSubject(String name, String professorName, String schedule) {
        addSubjectUseCase.execute(name, professorName, schedule);
        loadSubjects();
    }

    public void updateSubject(Subject originalSubject, String newName, String newProfessorName, String newSchedule) {
        updateSubjectUseCase.execute(originalSubject, newName, newProfessorName, newSchedule);
        loadSubjects();
    }

    // --- LÓGICA DE BORRADO DEFINITIVA ---
    public void deleteSelectedSubjects() {
        Set<Subject> selected = _selectedSubjects.getValue();
        List<Subject> currentList = _subjects.getValue();

        if (selected != null && !selected.isEmpty() && currentList != null) {
            // 1. Ejecutar la operación de borrado en la base de datos
            deleteSubjectsUseCase.execute(new ArrayList<>(selected));

            // 2. Crear una nueva lista para la UI, quitando los elementos borrados
            List<Subject> newList = new ArrayList<>(currentList);
            newList.removeAll(selected);
            _subjects.setValue(newList); // Notificar a la UI con la lista ya modificada

            // 3. Limpiar el estado de selección
            finishSelectionMode();
        } else {
            finishSelectionMode();
        }
    }

    public void disassociateAndDelete(Subject subject) {
        List<Subject> currentList = _subjects.getValue();
        if (subject != null && currentList != null) {
            // 1. Ejecutar la operación de borrado en la base de datos
            repository.disassociateAndDeleteSubject(subject.getId());

            // 2. Crear una nueva lista para la UI, quitando el elemento borrado
            List<Subject> newList = new ArrayList<>(currentList);
            newList.remove(subject);
            _subjects.setValue(newList); // Notificar a la UI

            // 3. Limpiar el estado de selección
            finishSelectionMode();
        }
    }

    // --- MÉTODOS DE AYUDA Y SELECCIÓN (sin cambios) ---
    public boolean subjectHasContent(Subject subject) {
        List<Task> tasks = repository.getTasksForSubject(subject.getName());
        List<Note> notes = repository.getNotesForSubject(subject.getName());
        return (tasks != null && !tasks.isEmpty()) || (notes != null && !notes.isEmpty());
    }

    public int[] getSubjectContentCount(Subject subject) {
        List<Task> tasks = repository.getTasksForSubject(subject.getName());
        List<Note> notes = repository.getNotesForSubject(subject.getName());
        return new int[]{tasks != null ? tasks.size() : 0, notes != null ? notes.size() : 0};
    }

    public void toggleSelection(Subject subject) {
        Set<Subject> selected = new LinkedHashSet<>(_selectedSubjects.getValue() != null ? _selectedSubjects.getValue() : new LinkedHashSet<>());
        if (selected.contains(subject)) {
            selected.remove(subject);
        } else {
            selected.add(subject);
        }
        _selectedSubjects.setValue(selected);

        if (selected.isEmpty() && Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            finishSelectionMode();
        } else if (!selected.isEmpty() && !Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            _isSelectionMode.setValue(true);
        }
    }

    public void finishSelectionMode() {
        _selectedSubjects.setValue(new LinkedHashSet<>());
        _isSelectionMode.setValue(false);
    }
}