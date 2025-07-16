package com.zihowl.thecalendar.ui.subjects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.ui.notes.NotesAdapter;
import com.zihowl.thecalendar.ui.notes.NotesViewModel;
import com.zihowl.thecalendar.ui.tasks.TasksAdapter;
import com.zihowl.thecalendar.ui.tasks.TasksViewModel;
import java.util.stream.Collectors;

public class SubjectDetailFragment extends Fragment {

    private static final String ARG_SUBJECT_NAME = "subject_name";

    private SubjectsViewModel subjectsViewModel;
    private TasksViewModel tasksViewModel;
    private NotesViewModel notesViewModel;

    private String subjectName;

    // Adapters para las listas internas
    private TasksAdapter tasksAdapter;
    private NotesAdapter notesAdapter;

    public static SubjectDetailFragment newInstance(String subjectName) {
        SubjectDetailFragment fragment = new SubjectDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT_NAME, subjectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            subjectName = getArguments().getString(ARG_SUBJECT_NAME);
        }
        // Obtener los ViewModels compartidos de MainActivity
        subjectsViewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);
        tasksViewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
        notesViewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Referencias a las vistas del layout
        TextView subjectNameTv = view.findViewById(R.id.detail_subject_name);
        TextView professorNameTv = view.findViewById(R.id.detail_professor_name);
        TextView scheduleTv = view.findViewById(R.id.detail_schedule);
        RecyclerView tasksRv = view.findViewById(R.id.recycler_view_tasks);
        RecyclerView notesRv = view.findViewById(R.id.recycler_view_notes);

        // Configurar adapters (puedes ignorar los listeners si no necesitas clics aquí)
        tasksAdapter = new TasksAdapter(task -> {}, task -> {}, task -> {}, header -> {});
        notesAdapter = new NotesAdapter((note, position) -> {}, (note, position) -> {});

        tasksRv.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksRv.setAdapter(tasksAdapter);
        notesRv.setLayoutManager(new LinearLayoutManager(getContext()));
        notesRv.setAdapter(notesAdapter);

        // Observar los datos y actualizar la UI
        subjectsViewModel.subjects.observe(getViewLifecycleOwner(), subjects -> {
            Subject currentSubject = subjects.stream()
                    .filter(s -> s.getName().equals(subjectName))
                    .findFirst()
                    .orElse(null);

            if (currentSubject != null) {
                subjectNameTv.setText(currentSubject.getName());
                scheduleTv.setText(currentSubject.getSchedule());
                if (currentSubject.getProfessorName() != null && !currentSubject.getProfessorName().isEmpty()) {
                    professorNameTv.setText(currentSubject.getProfessorName());
                    professorNameTv.setVisibility(View.VISIBLE);
                } else {
                    professorNameTv.setVisibility(View.GONE);
                }
            }
        });

        tasksViewModel.pendingTasks.observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                tasksAdapter.submitList(tasks.stream()
                        .filter(t -> subjectName.equals(t.getSubjectName()))
                        .collect(Collectors.toList()));
            }
        });

        notesViewModel.notes.observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                notesAdapter.submitList(notes.stream()
                        .filter(n -> subjectName.equals(n.getSubjectName()))
                        .collect(Collectors.toList()));
            }
        });
    }
}