package com.zihowl.thecalendar.domain.usecase.task;

import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import java.util.List;

public class DeleteTasksUseCase {
    private final TheCalendarRepository repository;

    public DeleteTasksUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(List<Task> tasks) {
        repository.deleteTasks(tasks);
    }
}