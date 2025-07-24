package com.zihowl.thecalendar.domain.usecase.subject;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteSubjectsUseCase {

    private final TheCalendarRepository repository;

    public DeleteSubjectsUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    /**
     * Extrae los IDs de una lista de materias y solicita al repositorio
     * que realice un borrado en cascada.
     * @param subjects La lista de materias a eliminar.
     */
    public void execute(List<Subject> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }

        List<Integer> subjectIds = subjects.stream()
                .map(Subject::getId)
                .collect(Collectors.toList());

        repository.cascadeDeleteSubjects(subjectIds);
    }
}