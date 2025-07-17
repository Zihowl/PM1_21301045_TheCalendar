package com.zihowl.thecalendar.ui.subjects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.domain.usecase.subject.AddSubjectUseCase;
import com.zihowl.thecalendar.domain.usecase.subject.DeleteSubjectsUseCase;
import com.zihowl.thecalendar.domain.usecase.subject.GetSubjectsUseCase;
import com.zihowl.thecalendar.domain.usecase.subject.UpdateSubjectUseCase;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SubjectsViewModel extends ViewModel {

    // Casos de Uso inyectados
    private final GetSubjectsUseCase getSubjectsUseCase;
    private final AddSubjectUseCase addSubjectUseCase;
    private final UpdateSubjectUseCase updateSubjectUseCase;
    private final DeleteSubjectsUseCase deleteSubjectsUseCase;

    // LiveData para la UI
    private final MutableLiveData<List<Subject>> _subjects = new MutableLiveData<>();
    public final LiveData<List<Subject>> subjects = _subjects;

    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Set<Subject>> _selectedSubjects = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Subject>> selectedSubjects = _selectedSubjects;


    // El ViewModel debería recibir los casos de uso a través de un ViewModelFactory
    public SubjectsViewModel(GetSubjectsUseCase getSubjectsUseCase, AddSubjectUseCase addSubjectUseCase, UpdateSubjectUseCase updateSubjectUseCase, DeleteSubjectsUseCase deleteSubjectsUseCase) {
        this.getSubjectsUseCase = getSubjectsUseCase;
        this.addSubjectUseCase = addSubjectUseCase;
        this.updateSubjectUseCase = updateSubjectUseCase;
        this.deleteSubjectsUseCase = deleteSubjectsUseCase;
        loadSubjects();
    }

    public void loadSubjects() {
        _subjects.setValue(getSubjectsUseCase.execute());
    }

    public void addSubject(String name, String professorName, String schedule) {
        addSubjectUseCase.execute(name, professorName, schedule);
        loadSubjects(); // Recargar la lista para mostrar el nuevo elemento
    }

    public void updateSubject(int position, String name, String professorName, String schedule) {
        Subject originalSubject = _subjects.getValue().get(position);
        updateSubjectUseCase.execute(originalSubject, name, professorName, schedule);
        loadSubjects();
    }

    public void deleteSelectedSubjects() {
        deleteSubjectsUseCase.execute(List.copyOf(_selectedSubjects.getValue()));
        finishSelectionMode();
        loadSubjects();
    }

    // --- Lógica de Selección (se mantiene igual) ---
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