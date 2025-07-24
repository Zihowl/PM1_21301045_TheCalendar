package com.zihowl.thecalendar.domain.usecase.subject;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

import java.util.List;

public class GetSubjectsUseCase {

    private final TheCalendarRepository repository;

    public GetSubjectsUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public List<Subject> execute() {
        // Aquí podrías añadir lógica de negocio, como ordenar o filtrar.
        List<Subject> subjects = repository.getSubjects();
        subjects.sort((s1, s2) -> s1.getName().compareTo(s2.getName())); // Ejemplo: ordenar alfabéticamente
        return subjects;
    }
}