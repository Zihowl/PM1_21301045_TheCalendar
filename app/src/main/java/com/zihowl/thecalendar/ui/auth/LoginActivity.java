package com.zihowl.thecalendar.ui.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.zihowl.thecalendar.R;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private CheckBox showPasswordCheckBox;

    private final ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                resetUI();
            }
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

    private void resetUI() {
        usernameEditText.setText("");
        passwordEditText.setText("");
        showPasswordCheckBox.setChecked(false);
        usernameEditText.clearFocus();
        passwordEditText.clearFocus();
    }
}