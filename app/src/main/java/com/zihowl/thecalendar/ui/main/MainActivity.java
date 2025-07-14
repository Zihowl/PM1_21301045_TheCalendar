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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayoutMediator;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.databinding.ActivityMainBinding;
import com.zihowl.thecalendar.ui.subjects.AddSubjectDialogFragment;

// Ya NO se implementa la interfaz SelectionListener, porque el Fragment se encarga de todo.
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbarAndDrawer();
        setupViewPagerAndTabs();
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, binding.appBarMain.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupViewPagerAndTabs() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        ViewPager2 viewPager = binding.appBarMain.contentMain.viewPager;
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.appBarMain.contentMain.tabLayout, viewPager, (tab, position) -> {
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
                // Esta llamada es importante para que el menú se actualice
                // (ej. para ocultar el botón "+" en la pestaña de Horario)
                invalidateOptionsMenu();
            }
        });
        viewPager.setCurrentItem(3);
    }

    // Solo tenemos UN onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // onPrepareOptionsMenu y la lógica de selección se eliminan de aquí.
    // El Fragment se encargará de ello.

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Solo manejamos el botón "+" aquí, ya que es el único que siempre es global.
        if (item.getItemId() == R.id.action_add) {
            int currentTab = binding.appBarMain.contentMain.viewPager.getCurrentItem();
            switch (currentTab) {
                case 0: // Tareas
                    Toast.makeText(this, "Agregar nueva Tarea...", Toast.LENGTH_SHORT).show();
                    break;
                case 1: // Notas
                    Toast.makeText(this, "Agregar nueva Nota...", Toast.LENGTH_SHORT).show();
                    break;
                case 3: // Materias
                    AddSubjectDialogFragment dialog = new AddSubjectDialogFragment();
                    dialog.show(getSupportFragmentManager(), "AddSubjectDialog");
                    break;
            }
            return true;
        }
        // Si no es el botón "+", dejamos que el fragmento intente manejarlo.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
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
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}