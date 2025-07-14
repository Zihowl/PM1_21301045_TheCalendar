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
        switch (position) {
            case 0:
                return new TasksFragment();
            case 1:
                return new NotesFragment();
            case 2:
                return new ScheduleFragment();
            case 3:
                return new SubjectsFragment();
            default:
                return new TasksFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}