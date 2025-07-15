package com.zihowl.thecalendar.ui.tasks;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TasksViewModel extends ViewModel {

    private final MutableLiveData<List<Task>> _tasks = new MutableLiveData<>();
    public final LiveData<List<Task>> tasks = _tasks;

    public void loadTasks() {
        if (_tasks.getValue() == null) {
            ArrayList<Task> dummyList = new ArrayList<>();
            dummyList.add(new Task("Hacer resumen", "Resumen del capítulo 3.", new Date(), false, "Programación Móvil"));
            dummyList.add(new Task("Resolver ejercicios", "Ejercicios de la página 50.", new Date(), true, "Cálculo Diferencial"));
            _tasks.setValue(dummyList);
        }
    }

    public void addTask(Task task) {
        List<Task> currentList = new ArrayList<>(_tasks.getValue() != null ? _tasks.getValue() : new ArrayList<>());
        currentList.add(task);
        _tasks.setValue(currentList);
    }

    public void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        _tasks.setValue(_tasks.getValue()); // Notificar a los observadores
    }
}