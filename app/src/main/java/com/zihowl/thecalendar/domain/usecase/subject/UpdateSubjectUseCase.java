package com.zihowl.thecalendar.domain.usecase.subject;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

public class UpdateSubjectUseCase {

    private final TheCalendarRepository repository;

    public UpdateSubjectUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(Subject originalSubject, String newName, String newProfessorName, String newSchedule) {
        // La lógica de negocio para la actualización va aquí.
        // Se mantiene la información de conteo que ya tenía el objeto.
        Subject updatedSubject = new Subject(newName, newProfessorName, newSchedule, originalSubject.getTasksPending(), originalSubject.getNotesCount());
        // Asignamos el ID original para asegurar que Realm actualice el objeto correcto.
        updatedSubject.setId(originalSubject.getId());
        repository.addSubject(updatedSubject); // 'addSubject' funciona como 'update' si la PK existe
    }
}