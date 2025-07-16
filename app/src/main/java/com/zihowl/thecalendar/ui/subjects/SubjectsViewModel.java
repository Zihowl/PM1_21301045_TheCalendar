package com.zihowl.thecalendar.ui.subjects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SubjectsViewModel extends ViewModel {

    private final MutableLiveData<List<Subject>> _subjects = new MutableLiveData<>();
    public final LiveData<List<Subject>> subjects = _subjects;

    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Set<Subject>> _selectedSubjects = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Subject>> selectedSubjects = _selectedSubjects;

    public void loadSubjects() {
        if (_subjects.getValue() == null) {
            ArrayList<Subject> dummyList = new ArrayList<>();
            dummyList.add(new Subject("Cálculo Diferencial", "Dr. Alan Turing", "Lunes 07:00 - 08:40\nMiércoles 07:00 - 08:40", 0, 0));
            dummyList.add(new Subject("Programación Móvil", "Dra. Ada Lovelace", "Martes 09:00 - 11:00\nJueves 09:00 - 11:00", 0, 0));
            dummyList.add(new Subject("Bases de Datos", null, "Viernes 11:00 - 13:00", 0, 0));
            _subjects.setValue(dummyList);
        }
    }

    // --- NUEVO: Método para actualizar contadores ---
    public void updateSubjectStats(List<Task> pendingTasks, List<Note> allNotes) {
        List<Subject> currentSubjects = _subjects.getValue();
        if (currentSubjects == null || pendingTasks == null || allNotes == null) return;

        for (Subject subject : currentSubjects) {
            // Contar tareas pendientes para esta materia
            long taskCount = pendingTasks.stream()
                    .filter(task -> !task.isCompleted() && subject.getName().equals(task.getSubjectName()))
                    .count();
            subject.setTasksPending((int) taskCount);

            // Contar notas para esta materia
            long noteCount = allNotes.stream()
                    .filter(note -> subject.getName().equals(note.getSubjectName()))
                    .count();
            subject.setNotesCount((int) noteCount);
        }
        // Notificar a los observadores con la lista actualizada
        _subjects.setValue(new ArrayList<>(currentSubjects));
    }


    public void addSubject(String name, String professorName, String schedule) {
        List<Subject> currentList = new ArrayList<>(_subjects.getValue() != null ? _subjects.getValue() : Collections.emptyList());
        currentList.add(new Subject(name, professorName, schedule, 0, 0));
        _subjects.setValue(currentList);
    }

    public void updateSubject(int position, String name, String professorName, String schedule) {
        List<Subject> currentList = new ArrayList<>(_subjects.getValue() != null ? _subjects.getValue() : Collections.emptyList());
        if (position >= 0 && position < currentList.size()) {
            Subject subjectToUpdate = currentList.get(position);
            currentList.set(position, new Subject(name, professorName, schedule, subjectToUpdate.getTasksPending(), subjectToUpdate.getNotesCount()));
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