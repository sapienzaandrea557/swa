package com.smart.giga.eater;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends AppCompatActivity {

    private static final String[] TEST_URLS = {
            "https://officecdn.microsoft.com/pr/492350f6-3a01-4f97-b9c0-c7c6ddf67d60/media/en-us/ProPlus2021Retail.img",
            "https://download.visualstudio.microsoft.com/download/pr/893592a6/VSCodeUserSetup-x64-1.85.1.exe",
            "https://it.download.nvidia.com/Windows/551.86/551.86-desktop-win10-win11-64bit-international-dch-whql.exe",
            "https://releases.ubuntu.com/22.04.4/ubuntu-22.04.4-desktop-amd64.iso",
            "https://speed.hetzner.de/10GB.bin"
    };

    private boolean isRunning = false;
    private AtomicLong totalBytes = new AtomicLong(0);
    private long startTime;
    private int activeWorkers = 0;
    private int errors = 0;
    private ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvStatus, tvSpeed, tvTotal, tvTime;
    private ProgressBar progressBar;
    private EditText etTargetGb;
    private Button btnStartStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvSpeed = findViewById(R.id.tv_speed);
        tvTotal = findViewById(R.id.tv_total);
        tvTime = findViewById(R.id.tv_time);
        progressBar = findViewById(R.id.progress_bar);
        etTargetGb = findViewById(R.id.et_target_gb);
        btnStartStop = findViewById(R.id.btn_start_stop);

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) {
                stopEater();
            } else {
                startEater();
            }
        });
    }

    private void startEater() {
        isRunning = true;
        totalBytes.set(0);
        errors = 0;
        startTime = System.currentTimeMillis();
        btnStartStop.setText("STOP GOD-MODE");
        btnStartStop.setBackgroundColor(Color.RED);
        tvStatus.setText("SATURAZIONE IN CORSO...");
        tvStatus.setTextColor(Color.GREEN);

        double targetGb = 0;
        try {
            String input = etTargetGb.getText().toString();
            if (!input.isEmpty()) targetGb = Double.parseDouble(input);
        } catch (Exception ignored) {}

        final double finalTargetGb = targetGb;

        executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> downloadWorker(finalTargetGb));
        }

        startMonitor();
    }

    private void stopEater() {
        isRunning = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        btnStartStop.setText("START GOD-MODE");
        btnStartStop.setBackgroundColor(Color.parseColor("#4CAF50"));
        tvStatus.setText("IDLE");
        tvStatus.setTextColor(Color.YELLOW);
    }

    private void downloadWorker(double targetGb) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Random random = new Random();
        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer

        while (isRunning) {
            String url = TEST_URLS[random.nextInt(TEST_URLS.length)];
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android 15; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    InputStream is = response.body().byteStream();
                    int read;
                    while (isRunning && (read = is.read(buffer)) != -1) {
                        long currentTotal = totalBytes.addAndGet(read);
                        if (targetGb > 0 && (currentTotal / (1024.0 * 1024.0 * 1024.0)) >= targetGb) {
                            isRunning = false;
                            handler.post(() -> Toast.makeText(this, "Target GB raggiunto!", Toast.LENGTH_LONG).show());
                            break;
                        }
                    }
                } else {
                    errors++;
                }
            } catch (Exception e) {
                errors++;
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void startMonitor() {
        handler.post(new Runnable() {
            long lastBytes = 0;
            @Override
            public void run() {
                if (!isRunning) return;

                long currentBytes = totalBytes.get();
                long delta = currentBytes - lastBytes;
                lastBytes = currentBytes;

                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                tvTime.setText("Tempo: " + formatTime(elapsed));
                tvTotal.setText("Totale: " + formatBytes(currentBytes));
                tvSpeed.setText("Velocità: " + formatBytes(delta) + "/s");

                double targetGb = 0;
                try {
                    String input = etTargetGb.getText().toString();
                    if (!input.isEmpty()) targetGb = Double.parseDouble(input);
                } catch (Exception ignored) {}

                if (targetGb > 0) {
                    int progress = (int) ((currentBytes / (targetGb * 1024.0 * 1024.0 * 1024.0)) * 100);
                    progressBar.setProgress(Math.min(100, progress));
                } else {
                    progressBar.setProgress(0);
                }

                handler.postDelayed(this, 1000);
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String formatTime(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}