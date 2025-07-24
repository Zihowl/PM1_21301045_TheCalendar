package com.zihowl.thecalendar.domain.usecase.task;

import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

public class AddTaskUseCase {
    private final TheCalendarRepository repository;

    public AddTaskUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(Task task) {
        repository.addTask(task);
    }
}