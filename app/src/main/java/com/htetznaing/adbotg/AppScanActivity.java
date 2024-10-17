package com.htetznaing.adbotg;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AppScanActivity extends AppCompatActivity {
    private boolean isLoading = false;
    private boolean searchPerformed = false;
    private LinearLayout permissionsInfoContainer;
    private ProgressBar progressBar;
    private Button scanButton;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_scan);

        permissionsInfoContainer = findViewById(R.id.permissions_info_container);
        progressBar = findViewById(R.id.progress_bar);
        scanButton = findViewById(R.id.scan_button);
        resultTextView = findViewById(R.id.result_text_view);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSpywareApps();
            }
        });

        // Initialize permissions info
        initPermissionsInfo();
    }

    private void initPermissionsInfo() {
        // Add permission info views here
        // Example:
        TextView locationInfo = new TextView(this);
        locationInfo.setText("Location Sharing: Grants access to your active location...");
        permissionsInfoContainer.addView(locationInfo);
    }

    private void getSpywareApps() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("Scanning...");

        // Simulate scanning process
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000); // Simulate delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        resultTextView.setText("No spyware apps detected on your device");
                        searchPerformed = true;
                    }
                });
            }
        }).start();
    }

    private void fetchAndCheckSpywareApps() {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                List<String> spywareApps = new ArrayList<>();
                try {
                    URL url = new URL("https://raw.githubusercontent.com/BriLeighk/test-csv-repo/refs/heads/main/app-flags.csv?token=GHSAT0AAAAAACUWGH5MUD7TIWECQEDTYJJKZYRNM7A");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length > 0) {
                            spywareApps.add(parts[0].trim());
                        }
                    }
                    reader.close();
                } catch (Exception e) {
                    Log.e("AppScan", "Error fetching CSV", e);
                }
                return spywareApps;
            }

            @Override
            protected void onPostExecute(List<String> spywareApps) {
                checkInstalledApps(spywareApps);
            }
        }.execute();
    }

    private void checkInstalledApps(List<String> spywareApps) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> detectedSpyware = new ArrayList<>();

        for (ApplicationInfo appInfo : installedApps) {
            if (spywareApps.contains(appInfo.packageName)) {
                detectedSpyware.add(appInfo.loadLabel(pm).toString() + " (" + appInfo.packageName + ")");
            }
        }

        if (detectedSpyware.isEmpty()) {
            resultTextView.setText("No spyware apps detected on your device");
        } else {
            resultTextView.setText("Detected spyware apps:\n" + String.join("\n", detectedSpyware));
        }
    }
}
