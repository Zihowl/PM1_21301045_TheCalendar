package com.zihowl.thecalendar.ui.auth;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.repository.AuthRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    // --- Declarar componentes de la UI ---
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private CheckBox showPasswordCheckBox;

    // --- Expresiones Regulares para Validaciones ---
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // --- Inicializar Vistas ---
        usernameEditText = findViewById(R.id.username_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        registerButton = findViewById(R.id.register_button);
        showPasswordCheckBox = findViewById(R.id.show_password_checkbox);
        TextView registrationTitleTextView = findViewById(R.id.registration_title_text_view);

        // --- Lógica para el botón de registro ---
        AuthRepository authRepository = new AuthRepository(this);
        registerButton.setOnClickListener(v -> {
            if (validateForm()) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString();

                authRepository.register(username, password, new Callback<Boolean>() {
                    @Override
                    public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                        if (Boolean.TRUE.equals(response.body())) {
                            Toast.makeText(SignUpActivity.this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Error al registrar", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Boolean> call, Throwable t) {
                        Toast.makeText(SignUpActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // --- Lógica para mostrar/ocultar contraseña ---
        setupShowPasswordCheckbox();
    }

    private boolean validateForm() {
        // Limpiar errores previos
        usernameEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);

        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // 1. Validar nombre de usuario
        if (username.isEmpty()) {
            usernameEditText.setError("El nombre de usuario no puede estar vacío");
            usernameEditText.requestFocus();
            return false;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            usernameEditText.setError("De 3 a 20 caracteres: letras, números y guion bajo");
            usernameEditText.requestFocus();
            return false;
        }

        // 2. Validar contraseña (longitud y complejidad por separado)
        if (password.isEmpty()) {
            passwordEditText.setError("La contraseña no puede estar vacía");
            passwordEditText.requestFocus();
            return false;
        }
        if (password.length() < 8) {
            passwordEditText.setError("Debe tener al menos 8 caracteres");
            passwordEditText.requestFocus();
            return false;
        }
        if (!isPasswordComplex(password)) {
            passwordEditText.setError("Debe cumplir 3 de 4: mayúscula, minúscula, número, símbolo");
            passwordEditText.requestFocus();
            return false;
        }

        // 3. Validar que las contraseñas coincidan
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.setError("Confirma tu contraseña");
            confirmPasswordEditText.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Las contraseñas no coinciden");
            confirmPasswordEditText.requestFocus();
            passwordEditText.setError("Las contraseñas no coinciden");
            return false;
        }

        return true;
    }

    private boolean isPasswordComplex(String password) {
        int criteriaMet = 0;
        if (UPPERCASE_PATTERN.matcher(password).matches()) criteriaMet++;
        if (LOWERCASE_PATTERN.matcher(password).matches()) criteriaMet++;
        if (NUMBER_PATTERN.matcher(password).matches()) criteriaMet++;
        if (SYMBOL_PATTERN.matcher(password).matches()) criteriaMet++;

        return criteriaMet >= 3;
    }

    private void setupShowPasswordCheckbox() {
        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int selection = passwordEditText.getSelectionEnd();

            if (isChecked) {
                // Mostrar solo la primera contraseña
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // Ocultar solo la primera contraseña
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }

            passwordEditText.setTypeface(Typeface.SANS_SERIF);
            passwordEditText.setSelection(selection);
        });
    }
}