package com.zihowl.thecalendar.ui.tasks;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.domain.usecase.task.GetTasksUseCase;
import com.zihowl.thecalendar.domain.usecase.task.UpdateTaskUseCase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TasksViewModel extends ViewModel {

    // --- Dependencias (Casos de Uso) ---
    private final GetTasksUseCase getTasksUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;

    // --- LiveData para la UI ---
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

    /**
     * Constructor que recibe las dependencias (Casos de Uso) a través del ViewModelFactory.
     */
    public TasksViewModel(GetTasksUseCase getTasksUseCase, UpdateTaskUseCase updateTaskUseCase) {
        this.getTasksUseCase = getTasksUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        loadTasks(); // Carga inicial de tareas
    }

    /**
     * Carga o recarga las listas de tareas pendientes y completadas desde el repositorio.
     */
    public void loadTasks() {
        _pendingTasks.setValue(getTasksUseCase.getPending());
        _completedTasks.setValue(getTasksUseCase.getCompleted());
    }

    /**
     * Actualiza una tarea existente con nueva información.
     * En una implementación completa, esto llamaría a un AddTaskUseCase.
     */
    public void addTask(Task task) {
        // En un futuro, aquí se llamaría a un AddTaskUseCase
        loadTasks();
    }

    /**
     * Actualiza las propiedades de una tarea y lo persiste en la base de datos.
     */
    public void updateTask(Task originalTask, String newTitle, String newDescription, String newSubjectName) {
        originalTask.setTitle(newTitle);
        originalTask.setDescription(newDescription);
        originalTask.setSubjectName(newSubjectName);
        updateTaskUseCase.execute(originalTask); // Guarda el objeto modificado
        loadTasks(); // Recarga las listas para reflejar el cambio
    }

    /**
     * Cambia el estado de completado de una tarea y lo persiste.
     */
    public void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        updateTaskUseCase.execute(task); // Guarda el nuevo estado
        loadTasks(); // Recarga las listas para mover la tarea a la sección correcta
    }

    // --- Lógica de selección y expansión (sin cambios) ---

    public void deleteSelectedTasks() {
        // En un futuro, aquí se llamaría a un DeleteTasksUseCase
        finishSelectionMode();
        loadTasks();
    }

    public void toggleSelection(Task task) {
        Set<Task> selected = new LinkedHashSet<>(_selectedTasks.getValue() != null ? _selectedTasks.getValue() : new LinkedHashSet<>());
        if (selected.contains(task)) {
            selected.remove(task);
        } else {
            selected.add(task);
        }
        _selectedTasks.setValue(selected);

        if (!selected.isEmpty() && !Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            _isSelectionMode.setValue(true);
        } else if (selected.isEmpty() && Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            finishSelectionMode();
        }
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