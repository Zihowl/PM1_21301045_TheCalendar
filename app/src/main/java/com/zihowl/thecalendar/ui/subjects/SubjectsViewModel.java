package com.zihowl.thecalendar.ui.subjects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SubjectsViewModel extends ViewModel {

    // LiveData para la lista de materias. Es privado para que solo el ViewModel lo modifique.
    private final MutableLiveData<List<Subject>> _subjects = new MutableLiveData<>();
    // LiveData público e inmutable que el Fragment observará.
    public final LiveData<List<Subject>> subjects = _subjects;

    // LiveData para manejar el modo de selección y los ítems seleccionados.
    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Set<Subject>> _selectedSubjects = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Subject>> selectedSubjects = _selectedSubjects;

    // Carga inicial de los datos
    public void loadSubjects() {
        if (_subjects.getValue() == null) {
            ArrayList<Subject> dummyList = new ArrayList<>();
            dummyList.add(new Subject("Cálculo Diferencial", "Lunes 07:00 - 08:40\nMiércoles 07:00 - 08:40", 3, 5));
            dummyList.add(new Subject("Programación Móvil", "Martes 09:00 - 11:00\nJueves 09:00 - 11:00", 1, 8));
            dummyList.add(new Subject("Bases de Datos", "Viernes 11:00 - 13:00", 0, 2));
            _subjects.setValue(dummyList);
        }
    }

    public void addSubject(String name, String schedule) {
        List<Subject> currentList = new ArrayList<>(_subjects.getValue() != null ? _subjects.getValue() : Collections.emptyList());
        currentList.add(new Subject(name, schedule, 0, 0));
        _subjects.setValue(currentList);
    }

    public void updateSubject(int position, String name, String schedule) {
        List<Subject> currentList = new ArrayList<>(_subjects.getValue() != null ? _subjects.getValue() : Collections.emptyList());
        if (position >= 0 && position < currentList.size()) {
            Subject subjectToUpdate = currentList.get(position);
            // Creamos un nuevo objeto para asegurar que LiveData detecte el cambio.
            currentList.set(position, new Subject(name, schedule, subjectToUpdate.getTasksPending(), subjectToUpdate.getNotesCount()));
            _subjects.setValue(currentList);
        }
    }

    public void deleteSelectedSubjects() {
        if (_subjects.getValue() == null || _selectedSubjects.getValue() == null) return;

        List<Subject> currentList = new ArrayList<>(_subjects.getValue());
        currentList.removeAll(_selectedSubjects.getValue());
        _subjects.setValue(currentList);
        finishSelectionMode();
    }

    // --- Lógica de Selección ---

    public void toggleSelection(Subject subject) {
        Set<Subject> selected = new LinkedHashSet<>(_selectedSubjects.getValue() != null ? _selectedSubjects.getValue() : Collections.emptySet());
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