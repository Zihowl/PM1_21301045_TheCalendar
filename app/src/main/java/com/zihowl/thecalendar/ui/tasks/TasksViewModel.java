package com.zihowl.thecalendar.ui.tasks;

import android.content.Context;
import android.widget.Toast;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.domain.usecase.task.AddTaskUseCase;
import com.zihowl.thecalendar.domain.usecase.task.DeleteTasksUseCase;
import com.zihowl.thecalendar.domain.usecase.task.GetTasksUseCase;
import com.zihowl.thecalendar.domain.usecase.task.UpdateTaskUseCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TasksViewModel extends ViewModel {

    private final GetTasksUseCase getTasksUseCase;
    private final AddTaskUseCase addTaskUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final DeleteTasksUseCase deleteTasksUseCase;

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

    public TasksViewModel(GetTasksUseCase get, AddTaskUseCase add, UpdateTaskUseCase update, DeleteTasksUseCase delete) {
        this.getTasksUseCase = get;
        this.addTaskUseCase = add;
        this.updateTaskUseCase = update;
        this.deleteTasksUseCase = delete;
        loadTasks();
    }

    public void loadTasks() {
        _pendingTasks.setValue(getTasksUseCase.getPending());
        _completedTasks.setValue(getTasksUseCase.getCompleted());
    }

    public void addTask(Task task, Context context) {
        addTaskUseCase.execute(task);
        loadTasks();
        Toast.makeText(context, "Tarea '" + task.getTitle() + "' añadida", Toast.LENGTH_SHORT).show();
    }

    public void updateTask(Task originalTask, String newTitle, String newDescription, String newSubjectName, Date newDate, Context context) {
        originalTask.setTitle(newTitle);
        originalTask.setDescription(newDescription);
        originalTask.setSubjectName(newSubjectName);
        originalTask.setDueDate(newDate);
        updateTaskUseCase.execute(originalTask);
        loadTasks();
        Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show();
    }

    public void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        updateTaskUseCase.execute(task);
        loadTasks();
    }

    public void deleteSelectedTasks(Context context) {
        int count = _selectedTasks.getValue() != null ? _selectedTasks.getValue().size() : 0;
        deleteTasksUseCase.execute(new ArrayList<>(_selectedTasks.getValue()));
        finishSelectionMode();
        loadTasks();
        Toast.makeText(context, count + (count > 1 ? " tareas eliminadas" : " tarea eliminada"), Toast.LENGTH_SHORT).show();
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