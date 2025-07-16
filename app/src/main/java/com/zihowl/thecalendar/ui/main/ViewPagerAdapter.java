package com.zihowl.thecalendar.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.zihowl.thecalendar.ui.notes.NotesFragment;
import com.zihowl.thecalendar.ui.schedule.ScheduleFragment;
import com.zihowl.thecalendar.ui.subjects.SubjectsFragment;
import com.zihowl.thecalendar.ui.tasks.TasksFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // CAMBIO: Reordena los fragments según la nueva disposición.
        switch (position) {
            case 0:
                return new SubjectsFragment(); // Materias
            case 1:
                return new TasksFragment();    // Tareas
            case 2:
                return new NotesFragment();    // Notas
            case 3:
                return new ScheduleFragment(); // Horario
            default:
                // Por defecto, muestra el primer fragmento.
                return new SubjectsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}