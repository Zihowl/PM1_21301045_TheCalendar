package com.zihowl.thecalendar.ui.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.ui.ViewModelFactory;
import com.zihowl.thecalendar.ui.auth.LoginActivity;
import com.zihowl.thecalendar.ui.notes.AddNoteDialogFragment;
import com.zihowl.thecalendar.ui.notes.NotesViewModel;
import com.zihowl.thecalendar.ui.subjects.AddSubjectDialogFragment;
// --- RUTA DE IMPORTACIÓN CORREGIDA ---
import com.zihowl.thecalendar.ui.subjects.detail.SubjectDetailFragment;
import com.zihowl.thecalendar.ui.subjects.SubjectsViewModel;
import com.zihowl.thecalendar.ui.tasks.AddTaskDialogFragment;
import com.zihowl.thecalendar.ui.tasks.TasksViewModel;
import com.zihowl.thecalendar.data.repository.AuthRepository;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import com.zihowl.thecalendar.data.sync.SyncManager;
import com.zihowl.thecalendar.data.sync.SyncStatus;
import com.zihowl.thecalendar.data.source.remote.RetrofitClient;
import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FragmentManager.OnBackStackChangedListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private TabLayout tabLayout;
    private View contentMainView;

    private TextView headerUser;
    private TextView headerStatus;
    private android.widget.ImageView headerProfileImage;
    private View syncButton;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private AuthRepository authRepository;
    private SyncManager syncManager;
    private TheCalendarRepository repository;

    private SubjectsViewModel subjectsViewModel;
    private TasksViewModel tasksViewModel;
    private NotesViewModel notesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        contentMainView = findViewById(R.id.contentMainLayout);

        authRepository = new AuthRepository(this);
        syncManager = SyncManager.getInstance(this);
        repository = TheCalendarRepository.getInstance(new RealmDataSource(), authRepository.getSessionManager());

        NavigationView navigationView = findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        headerUser = header.findViewById(R.id.header_user);
        headerStatus = header.findViewById(R.id.header_sync_status);
        headerProfileImage = header.findViewById(R.id.header_profile_image);
        syncButton = header.findViewById(R.id.nav_sync_button);
        String photo = authRepository.getSessionManager().getProfileImage();
        if (!photo.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(com.zihowl.thecalendar.data.source.remote.RetrofitClient.getBaseUrl() + "/" + photo)
                    .circleCrop()
                    .into(headerProfileImage);
        }
        headerProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
        String name = authRepository.getSessionManager().getUsername();
        if (name.isEmpty()) {
            name = getString(R.string.default_username);
        }
        headerUser.setText(name);
        if (syncButton != null) {
            syncButton.setOnClickListener(v -> syncManager.scheduleSync());
        }
        syncManager.getStatus().observe(this, status -> {
            String text;
            boolean pending = repository.hasPendingOperations();
            if (status == SyncStatus.SYNCING) {
                text = getString(R.string.sync_syncing);
            } else if (status == SyncStatus.ERROR) {
                text = getString(R.string.sync_error);
            } else if (status == SyncStatus.OFFLINE) {
                text = getString(R.string.sync_offline);
            } else if (status == SyncStatus.CONNECTED) {
                text = pending ? getString(R.string.sync_pending)
                        : getString(R.string.sync_connected);
                subjectsViewModel.loadSubjects();
                tasksViewModel.loadTasks();
                notesViewModel.loadNotes();
            } else { // COMPLETE
                text = pending ? getString(R.string.sync_pending)
                        : getString(R.string.sync_complete);
                subjectsViewModel.loadSubjects();
                tasksViewModel.loadTasks();
                notesViewModel.loadNotes();
            }
            headerStatus.setText(text);
        });
        setupViewModels();

        if (authRepository.getSessionManager().getToken() != null) {
            syncManager.scheduleSync();
        }
        setupToolbarAndDrawer();
        setupViewPagerAndTabs();

        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    private void setupViewModels() {
        ViewModelFactory factory = ViewModelFactory.getInstance(this);
        subjectsViewModel = new ViewModelProvider(this, factory).get(SubjectsViewModel.class);
        tasksViewModel = new ViewModelProvider(this, factory).get(TasksViewModel.class);
        notesViewModel = new ViewModelProvider(this, factory).get(NotesViewModel.class);

        subjectsViewModel.loadSubjects();
        tasksViewModel.loadTasks();
        notesViewModel.loadNotes();

        subjectsViewModel.isSelectionMode.observe(this, isSelected -> updateUiLockState());
        tasksViewModel.isSelectionMode.observe(this, isSelected -> updateUiLockState());
        notesViewModel.isSelectionMode.observe(this, isSelected -> updateUiLockState());
    }

    private void setupToolbarAndDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View loginButton = navigationView.findViewById(R.id.nav_login_button);
        View logoutButton = navigationView.findViewById(R.id.nav_logout_button);
        boolean loggedIn = authRepository.getSessionManager().getToken() != null;
        if (loginButton != null) {
            loginButton.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
            loginButton.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, LoginActivity.class));
            });
        }
        if (logoutButton != null) {
            logoutButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
            logoutButton.setOnClickListener(v -> handleLogout());
        }
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupViewPagerAndTabs() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

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
                updateTitleBasedOnPage(position);
                invalidateOptionsMenu();
            }
        });
        viewPager.setCurrentItem(0);
    }

    public void showSubjectDetail(String subjectName) {
        // --- CÓDIGO SIN CAMBIOS, PERO AHORA DEBERÍA ENCONTRAR LA CLASE ---
        SubjectDetailFragment fragment = SubjectDetailFragment.newInstance(subjectName);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.detail_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public boolean isCurrentTab(int tabIndex) {
        return viewPager.getCurrentItem() == tabIndex;
    }

    private void updateTitleBasedOnPage(int position) {
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
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        boolean isDetailVisible = getSupportFragmentManager().getBackStackEntryCount() > 0;
        contentMainView.setVisibility(isDetailVisible ? View.GONE : View.VISIBLE);
        setUiNavigationLock(isDetailVisible);
        if (!isDetailVisible) {
            updateTitleBasedOnPage(viewPager.getCurrentItem());
        }
    }

    private void updateUiLockState() {
        boolean isAnyFragmentInEditMode =
                Boolean.TRUE.equals(subjectsViewModel.isSelectionMode.getValue()) ||
                        Boolean.TRUE.equals(tasksViewModel.isSelectionMode.getValue()) ||
                        Boolean.TRUE.equals(notesViewModel.isSelectionMode.getValue());

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            setUiNavigationLock(isAnyFragmentInEditMode);
        }
    }

    public void setUiNavigationLock(boolean lock) {
        viewPager.setUserInputEnabled(!lock);
        int tabLayoutHeight = tabLayout.getHeight();
        long animationDuration = 250;

        tabLayout.animate()
                .translationY(lock ? tabLayoutHeight : 0)
                .setDuration(animationDuration)
                .start();

        if (lock) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            toggle.setDrawerIndicatorEnabled(false);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toggle.setToolbarNavigationClickListener(v -> onBackPressed());
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
            toggle.setDrawerIndicatorEnabled(true);
            toggle.setToolbarNavigationClickListener(null);
            toggle.syncState();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add) {
            int currentTab = viewPager.getCurrentItem();
            switch (currentTab) {
                case 0:
                    new AddSubjectDialogFragment().show(getSupportFragmentManager(), "AddSubjectDialog");
                    return true;
                case 1:
                    AddTaskDialogFragment.newInstance().show(getSupportFragmentManager(), "AddTaskDialog");
                    return true;
                case 2:
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
        authRepository.logout();
        TheCalendarRepository.getInstance(new RealmDataSource(), authRepository.getSessionManager())
                .clearCurrentUserData();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri == null) return;
            // Show the selected image immediately while it's uploaded in
            // background. This gives instant feedback to the user.
            Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(headerProfileImage);

            java.io.File file = createFileFromUri(uri);
            if (file == null) return;
            authRepository.uploadProfileImage(file, new retrofit2.Callback<com.zihowl.thecalendar.data.model.ImageUploadResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.zihowl.thecalendar.data.model.ImageUploadResponse> call, retrofit2.Response<com.zihowl.thecalendar.data.model.ImageUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Glide.with(MainActivity.this)
                                .load(RetrofitClient.getBaseUrl() + "/" + response.body().getRuta())
                                .circleCrop()
                                .into(headerProfileImage);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.zihowl.thecalendar.data.model.ImageUploadResponse> call, Throwable t) {
                }
            });
        }
    }


    private java.io.File createFileFromUri(android.net.Uri uri) {
        try {
            java.io.InputStream input = getContentResolver().openInputStream(uri);
            java.io.File temp = java.io.File.createTempFile("profile", ".jpg", getCacheDir());
            java.io.OutputStream out = new java.io.FileOutputStream(temp);
            byte[] buffer = new byte[4096];
            int read;
            while (input != null && (read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            if (input != null) input.close();
            out.close();
            return temp;
        } catch (java.io.IOException e) {
            return null;
        }
    }
}