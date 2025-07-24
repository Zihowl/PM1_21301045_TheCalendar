package com.zihowl.thecalendar.domain.usecase.note;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;

public class UpdateNoteUseCase {
    private final TheCalendarRepository repository;

    public UpdateNoteUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(Note note) {
        repository.updateNote(note);
    }
}