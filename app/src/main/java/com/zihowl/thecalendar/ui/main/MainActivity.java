package com.zihowl.thecalendar.ui.main;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
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
import com.zihowl.thecalendar.ui.subjects.AddSubjectDialogFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbarAndDrawer();
        setupViewPagerAndTabs();
    }

    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

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
            String title;
            switch (position) {
                case 0: title = "Tareas"; break;
                case 1: title = "Notas"; break;
                case 2: title = "Horario"; break;
                case 3: title = "Materias"; break;
                default: title = ""; break;
            }
            SpannableString boldTitle = new SpannableString(title);
            boldTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, boldTitle.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            tab.setText(boldTitle);
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                String newTitle;
                switch (position) {
                    case 0: newTitle = "Tareas"; break;
                    case 1: newTitle = "Notas"; break;
                    case 2: newTitle = "Horario"; break;
                    case 3: newTitle = "Materias"; break;
                    default: newTitle = getString(R.string.app_name); break;
                }
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(newTitle);
                }
                invalidateOptionsMenu();
            }
        });
        viewPager.setCurrentItem(3);
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
                case 0:
                    Toast.makeText(this, "Agregar nueva Tarea...", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(this, "Agregar nueva Nota...", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    AddSubjectDialogFragment dialog = new AddSubjectDialogFragment();
                    dialog.show(getSupportFragmentManager(), "AddSubjectDialog");
                    break;
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}