package com.zihowl.thecalendar.ui.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.ui.auth.LoginActivity;
import com.zihowl.thecalendar.ui.notes.AddNoteDialogFragment;
import com.zihowl.thecalendar.ui.subjects.AddSubjectDialogFragment;
import com.zihowl.thecalendar.ui.subjects.SubjectDetailFragment;
import com.zihowl.thecalendar.ui.tasks.AddTaskDialogFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;
    private View contentMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentMain = findViewById(R.id.contentMain);

        setupToolbarAndDrawer();
        setupViewPagerAndTabs();
    }
    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Lógica para el botón de logout en el footer
        View logoutButton = navigationView.findViewById(R.id.nav_logout_button);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> handleLogout());
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupViewPagerAndTabs() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String title = switch (position) {
                case 0 -> "Materias";
                case 1 -> "Tareas";
                case 2 -> "Notas";
                case 3 -> "Horario";
                default -> "";
            };
            SpannableString boldTitle = new SpannableString(title);
            boldTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, boldTitle.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            tab.setText(boldTitle);
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                String newTitle = switch (position) {
                    case 0 -> "Materias";
                    case 1 -> "Tareas";
                    case 2 -> "Notas";
                    case 3 -> "Horario";
                    default -> getString(R.string.app_name);
                };
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(newTitle);
                }
                invalidateOptionsMenu();
            }
        });
        viewPager.setCurrentItem(0); // Iniciar en la pestaña de Materias
    }

    public void showSubjectDetail(String subjectName) {
        SubjectDetailFragment fragment = SubjectDetailFragment.newInstance(subjectName);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.app_bar_main, fragment)
                .addToBackStack(null)
                .commit();
    }

    public boolean isCurrentTab(int tabIndex) {
        return viewPager.getCurrentItem() == tabIndex;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            // Restaurar el título original de la pestaña
            if (getSupportActionBar() != null) {
                String title = switch (viewPager.getCurrentItem()) {
                    case 0 -> "Materias";
                    case 1 -> "Tareas";
                    case 2 -> "Notas";
                    case 3 -> "Horario";
                    default -> getString(R.string.app_name);
                };
                getSupportActionBar().setTitle(title);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            int currentTab = viewPager.getCurrentItem();
            switch (currentTab) {
                case 0: // Materias
                    new AddSubjectDialogFragment().show(getSupportFragmentManager(), "AddSubjectDialog");
                    return true;
                case 1: // Tareas
                    AddTaskDialogFragment.newInstance().show(getSupportFragmentManager(), "AddTaskDialog");
                    return true;
                case 2: // Notas
                    new AddNoteDialogFragment().show(getSupportFragmentManager(), "AddNoteDialog");
                    return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleLogout() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}