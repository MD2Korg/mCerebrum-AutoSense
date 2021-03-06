package org.md2k.autosense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.md2k.autosense.antradio.connection.ServiceAutoSenses;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.autosense.plot.ActivityPlotChoice;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeByteArray;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.mcerebrum.commons.permission.Permission;
import org.md2k.mcerebrum.commons.permission.PermissionCallback;
import org.md2k.mcerebrum.core.access.appinfo.AppInfo;

import java.util.HashMap;

import es.dmoral.toasty.Toasty;
import io.fabric.sdk.android.Fabric;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class ActivityMain extends AppCompatActivity {

    private static final String TAG = ActivityMain.class.getSimpleName();
    HashMap<String, TextView> hashMapData = new HashMap<>();
    AutoSensePlatforms autoSensePlatforms = null;
    Handler mHandler;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            {
                long time = AppInfo.serviceRunningTime(ActivityMain.this, Constants.SERVICE_NAME);
                if (time < 0) {
                    ((TextView) findViewById(R.id.button_app_status)).setText("START");
                    findViewById(R.id.button_app_status).setBackground(ContextCompat.getDrawable(ActivityMain.this, R.drawable.button_status_off));

                } else {

                    ((TextView) findViewById(R.id.button_app_status)).setText(DateTime.convertTimestampToTimeStr(time));
                    findViewById(R.id.button_app_status).setBackground(ContextCompat.getDrawable(ActivityMain.this, R.drawable.button_status_on));
                }
                mHandler.postDelayed(this, 1000);
            }
        }
    };
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTable(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit, new Crashlytics());
        mHandler = new Handler();
        setContentView(R.layout.activity_main);

        Permission.requestPermission(this, new PermissionCallback() {
            @Override
            public void OnResponse(boolean isGranted) {
                if (!isGranted) {
                    Toasty.error(getApplicationContext(), "!PERMISSION DENIED !!! Could not continue...", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if(getIntent().hasExtra("RUN") && getIntent().getBooleanExtra("RUN", false)) {
                        Intent intent = new Intent(ActivityMain.this, ServiceAutoSenses.class);
                        startService(intent);
                        finish();
                    }else if(getIntent().hasExtra("PERMISSION") && getIntent().getBooleanExtra("PERMISSION", false)) {
                        finish();
                    } else {
                        load();
                    }
                }
            }
        });
    }
    void load(){

        final Button buttonService = (Button) findViewById(R.id.button_app_status);

        buttonService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ServiceAutoSenses.class);
                if (AppInfo.isServiceRunning(getBaseContext(), Constants.SERVICE_NAME)) {
                    stopService(intent);
                } else {
                    startService(intent);
                }
            }
        });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    void configureAppStatus() {
        findViewById(R.id.button_app_status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityMain.this, ServiceAutoSenses.class);
                if (((Button) findViewById(R.id.button_app_status)).getText().equals("OFF")) {
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                break;
            case R.id.action_settings:
                intent = new Intent(this, ActivitySettings.class);
                startActivity(intent);
                break;

            case R.id.action_plot:
                intent = new Intent(this, ActivityPlotChoice.class);
                startActivity(intent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    TableRow createDefaultRow() {
        TableRow row = new TableRow(this);
        TextView tvSensor = new TextView(this);
        tvSensor.setText("sensor");
        tvSensor.setTypeface(null, Typeface.BOLD);
        tvSensor.setTextColor(getResources().getColor(R.color.teal_A700));
        TextView tvCount = new TextView(this);
        tvCount.setText("count");
        tvCount.setTypeface(null, Typeface.BOLD);
        tvCount.setTextColor(getResources().getColor(R.color.teal_A700));
        TextView tvFreq = new TextView(this);
        tvFreq.setText("freq.");
        tvFreq.setTypeface(null, Typeface.BOLD);
        tvFreq.setTextColor(getResources().getColor(R.color.teal_A700));
        TextView tvSample = new TextView(this);
        tvSample.setText("samples");
        tvSample.setTypeface(null, Typeface.BOLD);
        tvSample.setTextColor(getResources().getColor(R.color.teal_A700));
        row.addView(tvSensor);
        row.addView(tvCount);
        row.addView(tvFreq);
        row.addView(tvSample);
        return row;
    }

    void prepareTable() {
        TableLayout ll = (TableLayout) findViewById(R.id.tableLayout);
        ll.removeAllViews();
        ll.addView(createDefaultRow());
        for (int i = 0; i < autoSensePlatforms.size(); i++) {
            String platform = autoSensePlatforms.get(i).getPlatformType() + ":" + autoSensePlatforms.get(i).getDeviceId();
            TableRow row = new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);
            TextView tvSensor = new TextView(this);
            tvSensor.setText(platform.toLowerCase());
            TextView tvCount = new TextView(this);
            tvCount.setText("0");
            hashMapData.put(platform + "_count", tvCount);
            TextView tvFreq = new TextView(this);
            tvFreq.setText("0");
            hashMapData.put(platform + "_freq", tvFreq);
            TextView tvSample = new TextView(this);
            tvSample.setText("0");
            hashMapData.put(platform + "_sample", tvSample);
            row.addView(tvSensor);
            row.addView(tvCount);
            row.addView(tvFreq);
            row.addView(tvSample);
            row.setBackgroundResource(R.drawable.border);
            ll.addView(row);
        }
    }

    void updateTable(Intent intent) {
        String sampleStr = "";
        String platform = intent.getStringExtra("platformType") + ":" + intent.getStringExtra("deviceId");
        long count = intent.getLongExtra("count", 0);
        hashMapData.get(platform + "_count").setText(String.valueOf(count));

        double time = (intent.getLongExtra("timestamp", 0) - intent.getLongExtra("starttimestamp", 0)) / 1000.0;
        double freq = (double) count / time;
        hashMapData.get(platform + "_freq").setText(String.format("%.1f", freq));


        DataType data = (DataType) intent.getParcelableExtra("data");
        byte[] sample = ((DataTypeByteArray) data).getSample();
        for (int i = 0; i < sample.length; i++) {
            if (i != 0) sampleStr += ",";
            if (i % 3 == 0 && i != 0) sampleStr += "\n";
            sampleStr = sampleStr + String.valueOf(sample[i]);
        }
        hashMapData.get(platform + "_sample").setText(sampleStr);
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Constants.INTENT_RECEIVED_DATA));
        autoSensePlatforms = new AutoSensePlatforms(getApplicationContext());
        prepareTable();
        mHandler.post(runnable);
        super.onResume();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(runnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }
}
