package com.example.remotecontrol;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IRRemote";
    private ConsumerIrManager irManager;
    private TextView statusText;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Frequenze comuni
    private static final int F_36 = 36000;
    private static final int F_38 = 38000;
    private static final int F_40 = 40000;
    private static final int F_56 = 56000;

    // --- CODICI POWER ---
    private static final int[] SAMSUNG_PWR = {4500, 4500, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 4500};
    private static final int[] LG_PWR = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 40000};
    private static final int[] SONY_PWR = {2400, 600, 1200, 600, 600, 600, 1200, 600, 600, 600, 1200, 600, 600, 600, 600, 600, 1200, 600, 600, 600, 600, 600, 600, 600, 600, 600};
    private static final int[] NEC_PWR = {9000, 4500, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 4500};

    // --- CODICI VOL+ ---
    private static final int[] SAMSUNG_VOL_UP = {4500, 4500, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 4500};
    private static final int[] LG_VOL_UP = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 40000};

    // Array per ciclo universale
    private static final int[][] ALL_POWER_PATTERNS = {SAMSUNG_PWR, LG_PWR, SONY_PWR, NEC_PWR};
    private static final int[] ALL_POWER_FREQS = {F_38, F_38, F_40, F_38};

    private static final int[][] ALL_VOL_UP_PATTERNS = {SAMSUNG_VOL_UP, LG_VOL_UP};
    private static final int[] ALL_VOL_UP_FREQS = {F_38, F_38};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);

        checkIrStatus();
        setupButtons();
    }

    private void checkIrStatus() {
        if (irManager == null || !irManager.hasIrEmitter()) {
            statusText.setText("ERRORE: Hardware IR non pronto");
            statusText.setTextColor(android.graphics.Color.RED);
        } else {
            statusText.setText("SISTEMA IR UNIVERSALE PRONTO");
            statusText.setTextColor(android.graphics.Color.GREEN);
        }
    }

    private void setupButtons() {
        // POWER UNIVERSALE (GOD-MODE)
        Button btnPower = findViewById(R.id.btn_power);
        btnPower.setOnClickListener(v -> sendUniversal(ALL_POWER_PATTERNS, ALL_POWER_FREQS, "POWER UNIVERSALE"));

        // VOLUME SU UNIVERSALE
        Button btnVolUp = findViewById(R.id.btn_vol_up);
        btnVolUp.setOnClickListener(v -> sendUniversal(ALL_VOL_UP_PATTERNS, ALL_VOL_UP_FREQS, "VOLUME + UNIVERSALE"));

        // Per compatibilità con il vecchio layout, riassegnamo i tasti
        Button btnSamsung = findViewById(R.id.btn_vol_up); 
        btnSamsung.setText("VOL + (TUTTI)");

        Button btnLG = findViewById(R.id.btn_vol_down);
        btnLG.setText("VOL - (TUTTI)");
        btnLG.setOnClickListener(v -> Toast.makeText(this, "Invio sequenza Volume -...", Toast.LENGTH_SHORT).show());
    }

    private void sendUniversal(int[][] patterns, int[] freqs, String label) {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, "Hardware IR non disponibile", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Esecuzione " + label + "...", Toast.LENGTH_SHORT).show();
        
        // Ciclo rapido su tutte le frequenze e pattern
        for (int i = 0; i < patterns.length; i++) {
            final int patternIdx = i;
            handler.postDelayed(() -> {
                try {
                    irManager.transmit(freqs[patternIdx], patterns[patternIdx]);
                    Log.d(TAG, "Sent pattern " + patternIdx + " at " + freqs[patternIdx] + "Hz");
                } catch (Exception e) {
                    Log.e(TAG, "Error at idx " + patternIdx, e);
                }
            }, i * 200); // 200ms di intervallo tra un segnale e l'altro
        }
    }
}