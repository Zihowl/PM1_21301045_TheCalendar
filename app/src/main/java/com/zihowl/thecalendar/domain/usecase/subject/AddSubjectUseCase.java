package com.zihowl.thecalendar.domain.usecase.subject;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

public class AddSubjectUseCase {

    private final TheCalendarRepository repository;

    public AddSubjectUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a subject if no other subject with the same name exists.
     *
     * @return true if the subject was created, false if a duplicate name exists.
     */
    public boolean execute(String name, String professorName, String schedule) {
        if (repository.subjectExists(name)) {
            return false;
        }
        Subject newSubject = new Subject(name, professorName, schedule, 0, 0);
        repository.addSubject(newSubject);
        return true;
    }
}