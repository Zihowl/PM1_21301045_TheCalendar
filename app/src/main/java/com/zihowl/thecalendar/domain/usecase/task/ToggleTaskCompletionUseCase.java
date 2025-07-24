package com.zihowl.thecalendar.domain.usecase.task;

import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

public class ToggleTaskCompletionUseCase {
    private final TheCalendarRepository repository;

    public ToggleTaskCompletionUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(Task task) {
        task.setCompleted(!task.isCompleted());
        repository.updateTask(task);
    }
}