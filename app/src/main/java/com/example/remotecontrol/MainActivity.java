package com.example.remotecontrol;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ConsumerIrManager irManager;

    // Esempi di codici IR (Semplificati per scopi dimostrativi)
    // In un'app reale, questi verrebbero caricati da un database di codici NEC/RC5/etc.
    private static final int FREQUENCY = 38000; // 38kHz standard

    // Codice Power Samsung (Esempio)
    private static final int[] SAMSUNG_POWER = {4500, 4500, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 4500};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);

        if (!irManager.hasIrEmitter()) {
            Toast.makeText(this, "Il dispositivo non ha un trasmettitore IR!", Toast.LENGTH_LONG).show();
        }

        setupButtons();
    }

    private void setupButtons() {
        Button btnPower = findViewById(R.id.btn_power);
        btnPower.setOnClickListener(v -> sendIrCode(SAMSUNG_POWER));

        Button btnVolUp = findViewById(R.id.btn_vol_up);
        btnVolUp.setOnClickListener(v -> {
            // Logica per volume su
            Toast.makeText(this, "Volume Su", Toast.LENGTH_SHORT).show();
        });

        Button btnVolDown = findViewById(R.id.btn_vol_down);
        btnVolDown.setOnClickListener(v -> {
            // Logica per volume giù
            Toast.makeText(this, "Volume Giù", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendIrCode(int[] pattern) {
        if (irManager != null && irManager.hasIrEmitter()) {
            irManager.transmit(FREQUENCY, pattern);
            Toast.makeText(this, "Segnale inviato", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Impossibile inviare il segnale", Toast.LENGTH_SHORT).show();
        }
    }
}
