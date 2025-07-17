package com.zihowl.thecalendar.domain.usecase.subject;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import java.util.List;

public class DeleteSubjectsUseCase {

    private final TheCalendarRepository repository;

    public DeleteSubjectsUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(List<Subject> subjects) {
        repository.deleteSubjects(subjects);
    }
}