package com.zihowl.thecalendar.ui.subjects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Subject;
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
    private final TheCalendarRepository repository; // Se mantiene para la lógica de borrado complejo

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
        // Obtenemos la instancia del repositorio para usar sus métodos directamente.
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

    /**
     * Elimina las materias seleccionadas. Este método se usa para el borrado múltiple
     * y por defecto realiza un borrado en cascada para cada materia.
     */
    public void deleteSelectedSubjects() {
        if (_selectedSubjects.getValue() != null && !_selectedSubjects.getValue().isEmpty()) {
            deleteSubjectsUseCase.execute(new ArrayList<>(_selectedSubjects.getValue()));
        }
        finishSelectionMode();
        loadSubjects();
    }

    /**
     * Elimina una sola materia pero mantiene sus tareas y notas, desvinculándolas.
     * @param subject La materia a eliminar.
     */
    public void disassociateAndDelete(Subject subject) {
        repository.disassociateAndDeleteSubject(subject); // <-- LÓGICA RESTAURADA
        finishSelectionMode();
        loadSubjects();
    }

    /**
     * Elimina una sola materia y todo su contenido asociado (tareas y notas).
     * @param subject La materia a eliminar en cascada.
     */
    public void cascadeDelete(Subject subject) {
        repository.cascadeDeleteSubject(subject); // <-- LÓGICA RESTAURADA
        finishSelectionMode();
        loadSubjects();
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