package com.zihowl.thecalendar.domain.usecase.task;

import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

public class UpdateTaskUseCase {
    private final TheCalendarRepository repository;

    public UpdateTaskUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(Task task) {
        repository.updateTask(task);
    }
}