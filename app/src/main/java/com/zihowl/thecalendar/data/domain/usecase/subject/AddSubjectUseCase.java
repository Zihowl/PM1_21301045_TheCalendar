package com.zihowl.thecalendar.domain.usecase.subject;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

public class AddSubjectUseCase {

    private final TheCalendarRepository repository;

    public AddSubjectUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(String name, String professorName, String schedule) {
        // Lógica de validación o transformación podría ir aquí.
        Subject newSubject = new Subject(name, professorName, schedule, 0, 0);
        repository.addSubject(newSubject);
    }
}