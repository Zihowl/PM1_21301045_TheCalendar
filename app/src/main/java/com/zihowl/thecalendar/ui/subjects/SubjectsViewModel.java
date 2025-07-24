package com.zihowl.thecalendar.ui.subjects;

import android.content.Context;
import android.widget.Toast;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
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

    public SubjectsViewModel(GetSubjectsUseCase get, AddSubjectUseCase add, UpdateSubjectUseCase update,
                             DeleteSubjectsUseCase delete, TheCalendarRepository repository) {
        this.getSubjectsUseCase = get;
        this.addSubjectUseCase = add;
        this.updateSubjectUseCase = update;
        this.deleteSubjectsUseCase = delete;
        this.repository = repository;
        loadSubjects();
    }

    public void loadSubjects() {
        _subjects.setValue(getSubjectsUseCase.execute());
    }

    // MÉTODO MODIFICADO PARA MOSTRAR RETROALIMENTACIÓN (Toast)
    public void addSubject(String name, String professorName, String schedule, Context context) {
        addSubjectUseCase.execute(name, professorName, schedule);
        loadSubjects();
        // RQF-7: Mensaje de retroalimentación al usuario
        Toast.makeText(context, "Materia '" + name + "' guardada", Toast.LENGTH_SHORT).show();
    }

    public void updateSubject(Subject originalSubject, String newName, String newProfessorName, String newSchedule) {
        updateSubjectUseCase.execute(originalSubject, newName, newProfessorName, newSchedule);
        loadSubjects();
    }

    public void deleteSelectedSubjects(Context context) {
        Set<Subject> selected = _selectedSubjects.getValue();
        if (selected != null && !selected.isEmpty()) {
            int count = selected.size();
            deleteSubjectsUseCase.execute(new ArrayList<>(selected));
            loadSubjects();
            finishSelectionMode();
            Toast.makeText(context, count + (count > 1 ? " materias eliminadas" : " materia eliminada"), Toast.LENGTH_SHORT).show();
        } else {
            finishSelectionMode();
        }
    }

    public void disassociateAndDelete(Subject subject, Context context) {
        if (subject != null) {
            String name = subject.getName();
            repository.disassociateAndDeleteSubject(subject.getId());
            loadSubjects();
            finishSelectionMode();
            Toast.makeText(context, "Materia '" + name + "' eliminada", Toast.LENGTH_SHORT).show();
        }
    }

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