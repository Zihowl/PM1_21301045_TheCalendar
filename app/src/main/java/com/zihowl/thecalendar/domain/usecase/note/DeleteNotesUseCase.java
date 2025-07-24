package com.zihowl.thecalendar.domain.usecase.note;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import java.util.List;

public class DeleteNotesUseCase {
    private final TheCalendarRepository repository;

    public DeleteNotesUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public void execute(List<Note> notes) {
        repository.deleteNotes(notes);
    }
}