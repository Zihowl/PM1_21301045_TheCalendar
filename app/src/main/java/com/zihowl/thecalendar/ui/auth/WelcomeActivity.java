package com.zihowl.thecalendar.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zihowl.thecalendar.ui.main.MainActivity;
import com.zihowl.thecalendar.R;

public class WelcomeActivity extends AppCompatActivity {

    private TextView welcomeMessageTextView;
    private Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // --- Inicializar las vistas con los nuevos IDs ---
        welcomeMessageTextView = findViewById(R.id.welcome_message_text_view);
        getStartedButton = findViewById(R.id.get_started_button);

        // --- Obtener el nombre de usuario y construir el mensaje ---
        // Se usa un valor por defecto "Usuario" si el extra no llega
        String usuario = getIntent().getStringExtra("NOMBRE_USUARIO");
        if (usuario == null || usuario.isEmpty()) {
            usuario = "null";
        }

        // Se establece el texto de bienvenida
        welcomeMessageTextView.setText("¡Bienvenido, " + usuario + "!");

        // --- Añadir funcionalidad al botón ---
        getStartedButton.setOnClickListener(v -> {
            // 1. Crear el Intent para ir de esta Activity (WelcomeActivity) a MainActivity.
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);

            // --- BANDERAS PARA LIMPIAR LA PILA ---
            // FLAG_ACTIVITY_NEW_TASK: Inicia la actividad en una nueva tarea.
            // FLAG_ACTIVITY_CLEAR_TASK: Limpia todas las actividades anteriores de la tarea.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // 2. Iniciar la MainActivity.
            startActivity(intent);

            // 3. Finalizar WelcomeActivity para que no se quede en el historial.
            finish();
        });
    }
}