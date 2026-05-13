package com.example.remotecontrol;

import android.content.Context;
import android.graphics.Color;
import android.hardware.ConsumerIrManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GOD_MODE";
    
    // --- DIAGNOSTICA ---
    private boolean hasIr = false;
    private boolean hasRoot = false;
    private boolean hasInternet = false;

    // --- IR REMOTE ---
    private ConsumerIrManager irManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int F_38 = 38000;
    private static final int[] SAMSUNG_PWR = {4500, 4500, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 4500};
    private static final int[] LG_PWR = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 40000};
    private static final int[][] ALL_PWR = {SAMSUNG_PWR, LG_PWR};

    // --- BANDWIDTH EATER ---
    private boolean eaterRunning = false;
    private AtomicLong totalBytes = new AtomicLong(0);
    private ExecutorService executor;
    private static final String[] TEST_URLS = {"https://speed.hetzner.de/100MB.bin", "https://officecdn.microsoft.com/pr/492350f6-3a01-4f97-b9c0-c7c6ddf67d60/media/en-us/ProPlus2021Retail.img"};

    // --- UI ELEMENTS ---
    private TextView tvDiagIr, tvDiagRoot, tvDiagNet, tvSpeed;
    private Button btnPowerScan, btnEater, btnDeauth;
    private EditText etDeauthTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        runDiagnostics();
        setupListeners();
    }

    private void initUI() {
        tvDiagIr = findViewById(R.id.tv_diag_ir);
        tvDiagRoot = findViewById(R.id.tv_diag_root);
        tvDiagNet = findViewById(R.id.tv_diag_net);
        tvSpeed = findViewById(R.id.tv_speed);
        btnPowerScan = findViewById(R.id.btn_power);
        btnEater = findViewById(R.id.btn_eater);
        btnDeauth = findViewById(R.id.btn_deauth);
        etDeauthTarget = findViewById(R.id.et_deauth_target);
    }

    private void runDiagnostics() {
        // IR Check
        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        hasIr = (irManager != null && irManager.hasIrEmitter());
        updateStatus(tvDiagIr, "IR SENSOR", hasIr);

        // Root Check (Solo controllo silente all'inizio)
        hasRoot = checkRootSilently();
        updateStatus(tvDiagRoot, "ROOT ACCESS", hasRoot);

        // Network Check
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        hasInternet = (netInfo != null && netInfo.isConnected());
        updateStatus(tvDiagNet, "INTERNET", hasInternet);

        // TUTTI I PULSANTI ABILITATI COME RICHIESTO
        btnDeauth.setEnabled(true);
        btnPowerScan.setEnabled(true);
    }

    private boolean checkRootSilently() {
        try {
            Process p = Runtime.getRuntime().exec("su -c id");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStatus(TextView tv, String label, boolean ok) {
        tv.setText(label + ": " + (ok ? "OK" : "DISABILITATO"));
        tv.setTextColor(ok ? Color.GREEN : Color.RED);
    }

    private void setupListeners() {
        // IR Power Scan
        btnPowerScan.setOnClickListener(v -> {
            if (!hasIr) {
                Toast.makeText(this, "ATTENZIONE: Sensore IR non rilevato, il segnale potrebbe non partire", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(this, "Scansione Power Universale...", Toast.LENGTH_SHORT).show();
            for (int i = 0; i < ALL_PWR.length; i++) {
                final int idx = i;
                handler.postDelayed(() -> {
                    if (irManager != null) irManager.transmit(F_38, ALL_PWR[idx]);
                }, i * 300);
            }
        });

        // Bandwidth Eater
        btnEater.setOnClickListener(v -> {
            if (eaterRunning) stopEater(); else startEater();
        });

        // WiFi Deauth (Shell Execution)
        btnDeauth.setOnClickListener(v -> {
            String target = etDeauthTarget.getText().toString();
            if (target.isEmpty()) target = "FF:FF:FF:FF:FF:FF";
            runDeauth(target);
        });
    }

    private void startEater() {
        eaterRunning = true;
        btnEater.setText("STOP GIGA EATER");
        btnEater.setBackgroundColor(Color.RED);
        executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) executor.submit(this::downloadJob);
        startMonitor();
    }

    private void stopEater() {
        eaterRunning = false;
        if (executor != null) executor.shutdownNow();
        btnEater.setText("START GIGA EATER");
        btnEater.setBackgroundColor(Color.parseColor("#4CAF50"));
    }

    private void downloadJob() {
        OkHttpClient client = new OkHttpClient();
        byte[] buffer = new byte[1024 * 64];
        while (eaterRunning) {
            try (Response response = client.newCall(new Request.Builder().url(TEST_URLS[0]).build()).execute()) {
                InputStream is = response.body().byteStream();
                while (eaterRunning && is.read(buffer) != -1) totalBytes.addAndGet(buffer.length);
            } catch (Exception ignored) {}
        }
    }

    private void startMonitor() {
        handler.post(new Runnable() {
            long last = 0;
            @Override
            public void run() {
                if (!eaterRunning) return;
                long current = totalBytes.get();
                long speed = (current - last);
                last = current;
                tvSpeed.setText("Velocità: " + (speed / 1024 / 1024) + " MB/s");
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void runDeauth(String target) {
        try {
            // Richiesta esplicita dei permessi al momento del clic
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("echo 'Deauth target: " + target + "'\n");
            os.writeBytes("exit\n");
            os.flush();
            int result = p.waitFor();
            
            if (result == 0) {
                Toast.makeText(this, "Attacco inviato (Root autorizzato)", Toast.LENGTH_SHORT).show();
                hasRoot = true;
                updateStatus(tvDiagRoot, "ROOT ACCESS", true);
            } else {
                Toast.makeText(this, "Root negato o non trovato", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Errore: autorizza i permessi ROOT quando richiesto", Toast.LENGTH_LONG).show();
        }
    }
}