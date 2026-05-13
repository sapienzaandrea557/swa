package com.example.remotecontrol;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IRRemote";
    private ConsumerIrManager irManager;
    private TextView statusText;

    // Esempi di codici IR (Samsung, LG, Sony)
    private static final int FREQ_38 = 38000;
    private static final int FREQ_40 = 40000;

    // Samsung Power (38kHz)
    private static final int[] SAMSUNG_POWER = {4500, 4500, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 4500};

    // LG Power (38kHz)
    private static final int[] LG_POWER = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 40000};

    // Sony Power (40kHz)
    private static final int[] SONY_POWER = {2400, 600, 1200, 600, 600, 600, 1200, 600, 600, 600, 1200, 600, 600, 600, 600, 600, 1200, 600, 600, 600, 600, 600, 600, 600, 600, 600};

    private int[] currentPattern = SAMSUNG_POWER;
    private int currentFreq = FREQ_38;

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
        if (irManager == null) {
            statusText.setText("ERRORE: Servizio IR non disponibile");
            statusText.setTextColor(android.graphics.Color.RED);
            Log.e(TAG, "ConsumerIrManager is null");
            return;
        }

        if (!irManager.hasIrEmitter()) {
            statusText.setText("ERRORE: Trasmettitore IR non rilevato");
            statusText.setTextColor(android.graphics.Color.RED);
            Log.e(TAG, "Device does not have IR emitter");
        } else {
            StringBuilder info = new StringBuilder("Hardware IR pronto\nFrequenze supportate:\n");
            ConsumerIrManager.CarrierFrequencyRange[] ranges = irManager.getCarrierFrequencies();
            for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                info.append(range.getMinFrequency()).append(" - ").append(range.getMaxFrequency()).append(" Hz\n");
            }
            statusText.setText(info.toString());
            statusText.setTextColor(android.graphics.Color.GREEN);
            Log.d(TAG, "IR Emitter ready. Ranges: " + info.toString());
        }
    }

    private void setupButtons() {
        Button btnPower = findViewById(R.id.btn_power);
        btnPower.setOnClickListener(v -> sendIrCode(currentPattern, currentFreq));

        // Aggiungiamo un modo per cambiare marca (Semplificato)
        Button btnSamsung = findViewById(R.id.btn_vol_up); // Riutilizziamo i bottoni esistenti per ora
        btnSamsung.setText("SAMSUNG");
        btnSamsung.setOnClickListener(v -> {
            currentPattern = SAMSUNG_POWER;
            currentFreq = FREQ_38;
            Toast.makeText(this, "Selezionato Samsung", Toast.LENGTH_SHORT).show();
        });

        Button btnLG = findViewById(R.id.btn_vol_down);
        btnLG.setText("LG");
        btnLG.setOnClickListener(v -> {
            currentPattern = LG_POWER;
            currentFreq = FREQ_38;
            Toast.makeText(this, "Selezionato LG", Toast.LENGTH_SHORT).show();
        });

        Button btnSony = findViewById(R.id.btn_sony);
        btnSony.setOnClickListener(v -> {
            currentPattern = SONY_POWER;
            currentFreq = FREQ_40;
            Toast.makeText(this, "Selezionato Sony", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendIrCode(int[] pattern, int freq) {
        if (irManager != null && irManager.hasIrEmitter()) {
            try {
                // Verifica se la frequenza è supportata
                boolean supported = false;
                for (ConsumerIrManager.CarrierFrequencyRange range : irManager.getCarrierFrequencies()) {
                    if (freq >= range.getMinFrequency() && freq <= range.getMaxFrequency()) {
                        supported = true;
                        break;
                    }
                }

                if (!supported) {
                    Toast.makeText(this, "Frequenza " + freq + "Hz non supportata!", Toast.LENGTH_LONG).show();
                    return;
                }

                irManager.transmit(freq, pattern);
                Toast.makeText(this, "Segnale inviato (" + freq + "Hz)", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Transmitted IR signal at " + freq + "Hz");
            } catch (Exception e) {
                Log.e(TAG, "Error transmitting IR", e);
                Toast.makeText(this, "Errore invio: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Hardware IR non disponibile", Toast.LENGTH_SHORT).show();
        }
    }
}
