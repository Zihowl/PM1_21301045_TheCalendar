package com.zihowl.thecalendar.domain.usecase.note;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import java.util.List;

public class GetNotesUseCase {
    private final TheCalendarRepository repository;

    public GetNotesUseCase(TheCalendarRepository repository) {
        this.repository = repository;
    }

    public List<Note> execute() {
        return repository.getNotes();
    }
}