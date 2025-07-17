package com.zihowl.thecalendar.domain.usecase.task;

import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import java.util.List;

public class GetTasksUseCase {
    private final TheCalendarRepository repository;

    public GetTasksUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public List<Task> getPending() {
        return repository.getPendingTasks();
    }

    public List<Task> getCompleted() {
        return repository.getCompletedTasks();
    }
}