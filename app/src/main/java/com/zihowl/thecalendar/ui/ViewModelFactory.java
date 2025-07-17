package com.zihowl.thecalendar.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import com.zihowl.thecalendar.domain.usecase.note.*;
import com.zihowl.thecalendar.domain.usecase.subject.*;
import com.zihowl.thecalendar.domain.usecase.task.*;
import com.zihowl.thecalendar.ui.notes.NotesViewModel;
import com.zihowl.thecalendar.ui.subjects.SubjectsViewModel;
import com.zihowl.thecalendar.ui.tasks.TasksViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private static volatile ViewModelFactory INSTANCE;
    private final TheCalendarRepository repository;

    public static ViewModelFactory getInstance() {
        if (INSTANCE == null) {
            synchronized (ViewModelFactory.class) {
                if (INSTANCE == null) {
                    RealmDataSource dataSource = new RealmDataSource();
                    TheCalendarRepository repo = TheCalendarRepository.getInstance(dataSource);
                    INSTANCE = new ViewModelFactory(repo);
                }
            }
        }
        return INSTANCE;
    }

    private ViewModelFactory(TheCalendarRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SubjectsViewModel.class)) {
            return (T) new SubjectsViewModel(
                    new GetSubjectsUseCase(repository),
                    new AddSubjectUseCase(repository),
                    new UpdateSubjectUseCase(repository),
                    new DeleteSubjectsUseCase(repository)
            );
        }
        if (modelClass.isAssignableFrom(TasksViewModel.class)) {
            return (T) new TasksViewModel(
                    new GetTasksUseCase(repository),
                    new AddTaskUseCase(repository),
                    new UpdateTaskUseCase(repository),
                    new DeleteTasksUseCase(repository)
            );
        }
        if (modelClass.isAssignableFrom(NotesViewModel.class)) {
            return (T) new NotesViewModel(
                    new GetNotesUseCase(repository),
                    new AddNoteUseCase(repository),
                    new UpdateNoteUseCase(repository),
                    new DeleteNotesUseCase(repository)
            );
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}