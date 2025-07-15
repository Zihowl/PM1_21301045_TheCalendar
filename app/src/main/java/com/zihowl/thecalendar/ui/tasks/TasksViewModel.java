package com.zihowl.thecalendar.ui.tasks;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zihowl.thecalendar.data.model.Task;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TasksViewModel extends ViewModel {

    private final MutableLiveData<List<Task>> _pendingTasks = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Task>> pendingTasks = _pendingTasks;

    private final MutableLiveData<List<Task>> _completedTasks = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Task>> completedTasks = _completedTasks;

    private final MutableLiveData<Set<Task>> _selectedTasks = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Task>> selectedTasks = _selectedTasks;

    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Boolean> _isPendingExpanded = new MutableLiveData<>(true);
    public final LiveData<Boolean> isPendingExpanded = _isPendingExpanded;

    private final MutableLiveData<Boolean> _isCompletedExpanded = new MutableLiveData<>(true);
    public final LiveData<Boolean> isCompletedExpanded = _isCompletedExpanded;

    public void loadTasks() {
        if (Objects.requireNonNull(pendingTasks.getValue()).isEmpty() && Objects.requireNonNull(completedTasks.getValue()).isEmpty()) {
            ArrayList<Task> dummyList = new ArrayList<>();
            dummyList.add(new Task("Hacer resumen", "Resumen del capítulo 3.", new Date(), false, "Programación Móvil"));
            dummyList.add(new Task("Resolver ejercicios", "Ejercicios de la página 50.", new Date(), true, "Cálculo Diferencial"));
            dummyList.add(new Task("Tarea de BD", "Investigar sobre SQL Injection.", null, false, "Bases de Datos"));

            _pendingTasks.setValue(dummyList.stream().filter(t -> !t.isCompleted()).collect(Collectors.toList()));
            _completedTasks.setValue(dummyList.stream().filter(Task::isCompleted).collect(Collectors.toList()));
        }
    }

    public void addTask(Task task) {
        List<Task> currentList = new ArrayList<>(Objects.requireNonNull(_pendingTasks.getValue()));
        currentList.add(0, task);
        _pendingTasks.setValue(currentList);
    }

    public void updateTask(Task originalTask, String newTitle, String newDescription, String newSubjectName) {
        List<Task> pending = new ArrayList<>(Objects.requireNonNull(_pendingTasks.getValue()));
        List<Task> completed = new ArrayList<>(Objects.requireNonNull(_completedTasks.getValue()));

        Task updatedTask = new Task(newTitle, newDescription, originalTask.getDueDate(), originalTask.isCompleted(), newSubjectName);

        int index = pending.indexOf(originalTask);
        if (index != -1) {
            pending.set(index, updatedTask);
            _pendingTasks.setValue(pending);
        } else {
            index = completed.indexOf(originalTask);
            if (index != -1) {
                completed.set(index, updatedTask);
                _completedTasks.setValue(completed);
            }
        }
    }

    public void toggleTaskCompletion(Task task) {
        // ---- CAMBIO CLAVE: Crear una NUEVA instancia de la tarea ----
        Task updatedTask = new Task(task.getTitle(), task.getDescription(), task.getDueDate(), !task.isCompleted(), task.getSubjectName());

        List<Task> pending = new ArrayList<>(Objects.requireNonNull(_pendingTasks.getValue()));
        List<Task> completed = new ArrayList<>(Objects.requireNonNull(_completedTasks.getValue()));

        if (updatedTask.isCompleted()) {
            if (pending.remove(task)) {
                completed.add(0, updatedTask);
            }
        } else {
            if (completed.remove(task)) {
                pending.add(0, updatedTask);
            }
        }
        _pendingTasks.setValue(pending);
        _completedTasks.setValue(completed);
    }

    public void deleteSelectedTasks() {
        if (_selectedTasks.getValue() == null || _selectedTasks.getValue().isEmpty()) return;

        List<Task> pending = new ArrayList<>(Objects.requireNonNull(_pendingTasks.getValue()));
        List<Task> completed = new ArrayList<>(Objects.requireNonNull(_completedTasks.getValue()));

        pending.removeAll(_selectedTasks.getValue());
        completed.removeAll(_selectedTasks.getValue());

        _pendingTasks.setValue(pending);
        _completedTasks.setValue(completed);
        finishSelectionMode();
    }

    public void toggleSelection(Task task) {
        Set<Task> selected = new LinkedHashSet<>(Objects.requireNonNull(_selectedTasks.getValue()));
        Task taskInList = findTaskInLists(task);
        if (taskInList == null) return;

        if (selected.contains(taskInList)) {
            selected.remove(taskInList);
        } else {
            selected.add(taskInList);
        }
        _selectedTasks.setValue(selected);

        if (!selected.isEmpty() && !Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            _isSelectionMode.setValue(true);
        } else if (selected.isEmpty() && Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            finishSelectionMode();
        }
    }

    private Task findTaskInLists(Task task) {
        return Objects.requireNonNull(_pendingTasks.getValue()).stream()
                .filter(t -> t.equals(task))
                .findFirst()
                .orElse(Objects.requireNonNull(_completedTasks.getValue()).stream()
                        .filter(t -> t.equals(task))
                        .findFirst()
                        .orElse(null));
    }

    public void finishSelectionMode() {
        _selectedTasks.setValue(new LinkedHashSet<>());
        _isSelectionMode.setValue(false);
    }

    public void togglePendingExpansion() {
        _isPendingExpanded.setValue(!Boolean.TRUE.equals(_isPendingExpanded.getValue()));
    }

    public void toggleCompletedExpansion() {
        _isCompletedExpanded.setValue(!Boolean.TRUE.equals(_isCompletedExpanded.getValue()));
    }
}