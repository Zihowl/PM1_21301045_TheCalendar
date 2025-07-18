package com.zihowl.thecalendar.ui.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.zihowl.thecalendar.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private CheckBox showPasswordCheckBox;

    private final ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> resetUI()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.username_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        showPasswordCheckBox = findViewById(R.id.show_password_checkbox);
        TextView registerTextView = findViewById(R.id.register_text_view);
        Button loginButton = findViewById(R.id.login_button);

        // --- Cargar el último usuario guardado al iniciar ---
        loadUserFromProperties();

        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int selection = passwordEditText.getSelectionEnd();
            if (isChecked) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            passwordEditText.setTypeface(Typeface.SANS_SERIF);
            passwordEditText.setSelection(selection);
        });

        // --- Listener para el botón de Login ---
        loginButton.setOnClickListener(view -> {
            // Obtener el texto del campo de usuario ---
            String username = usernameEditText.getText().toString().trim();

            // --- Guardar el usuario en el archivo .properties ---
            saveUserToProperties(username);

            Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
            // Usar la variable en lugar del texto fijo ---
            intent.putExtra("NOMBRE_USUARIO", username);

            activityLauncher.launch(intent);
        });

        registerTextView.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            activityLauncher.launch(intent);
        });
    }

    /**
     * Guarda el nombre de usuario en un archivo config.properties
     * en el almacenamiento interno de la app.
     * @param username El nombre de usuario a guardar.
     */
    private void saveUserToProperties(String username) {
        Properties properties = new Properties();
        properties.setProperty("last_logged_user", username);
        try {
            // Usa openFileOutput para obtener un stream seguro al almacenamiento interno
            FileOutputStream fos = openFileOutput("config.properties", MODE_PRIVATE);
            properties.store(fos, "User Configuration");
            fos.close();
        } catch (Exception e) {
            e.printStackTrace(); // Manejar el error apropiadamente en una app real
        }
    }

    /**
     * Carga el último nombre de usuario desde config.properties
     * y lo muestra en el campo de texto correspondiente.
     */
    private void loadUserFromProperties() {
        Properties properties = new Properties();
        try {
            // Usa openFileInput para leer el archivo del almacenamiento interno
            FileInputStream fis = openFileInput("config.properties");
            properties.load(fis);
            fis.close();
            // Obtiene la propiedad "last_logged_user", con "" como valor por defecto si no existe
            String lastUser = properties.getProperty("last_logged_user", "");
            usernameEditText.setText(lastUser);
            // Opcional: muestra un Toast para confirmar que se cargó el usuario
            if (!lastUser.isEmpty()) {
                Toast.makeText(this, "Último usuario: " + lastUser, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetUI() {
        usernameEditText.setText("");
        passwordEditText.setText("");
        showPasswordCheckBox.setChecked(false);
        usernameEditText.clearFocus();
        passwordEditText.clearFocus();
    }
}