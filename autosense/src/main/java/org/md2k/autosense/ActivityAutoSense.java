package org.md2k.autosense;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.md2k.autosense.antradio.connection.ServiceAutoSenses;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeByteArray;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;

import java.util.HashMap;

/**
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

public class ActivityAutoSense extends Activity {

    private static final String TAG = ActivityAutoSense.class.getSimpleName();
    Context context;
    AutoSensePlatforms autoSensePlatforms = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        autoSensePlatforms=new AutoSensePlatforms(ActivityAutoSense.this);
        context = this;

        setContentView(R.layout.activity_auto_sense);
        setupButtonSettings();
    }

    TableRow createDefaultRow() {
        TableRow row = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        TextView tvSensor = new TextView(this);
        tvSensor.setText("sensor");
        tvSensor.setTypeface(null, Typeface.BOLD);
        tvSensor.setTextColor(getResources().getColor(R.color.holo_blue_dark));
        TextView tvCount = new TextView(this);
        tvCount.setText("count");
        tvCount.setTypeface(null, Typeface.BOLD);
        tvCount.setTextColor(getResources().getColor(R.color.holo_blue_dark));
        TextView tvFreq = new TextView(this);
        tvFreq.setText("freq.");
        tvFreq.setTypeface(null, Typeface.BOLD);
        tvFreq.setTextColor(getResources().getColor(R.color.holo_blue_dark));
        TextView tvSample = new TextView(this);
        tvSample.setText("samples");
        tvSample.setTypeface(null, Typeface.BOLD);
        tvSample.setTextColor(getResources().getColor(R.color.holo_blue_dark));
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
            String platform = autoSensePlatforms.get(i).getPlatformType()+":"+autoSensePlatforms.get(i).getPlatformId();
            TableRow row = new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);
            TextView tvSensor = new TextView(this);
            tvSensor.setText(platform.toLowerCase());
            TextView tvCount = new TextView(this);
            tvCount.setText("0");
            hm.put(platform + "_count", tvCount);
            TextView tvFreq = new TextView(this);
            tvFreq.setText("0");
            hm.put(platform + "_freq", tvFreq);
            TextView tvSample = new TextView(this);
            tvSample.setText("0");
            hm.put(platform + "_sample", tvSample);
            row.addView(tvSensor);
            row.addView(tvCount);
            row.addView(tvFreq);
            row.addView(tvSample);
            row.setBackgroundResource(R.drawable.border);
            ll.addView(row);
        }

    }

    void showDevices() {
        TextView textView = (TextView) findViewById(R.id.configuration_info);
        String str = "";
        for (int i = 0; i < autoSensePlatforms.size(); i++) {
                if (i != 0) str = str + "\n";
                str = str + autoSensePlatforms.get(i).getPlatformType().toLowerCase()+":"+autoSensePlatforms.get(i).getPlatformId()+" (loc: "+autoSensePlatforms.get(i).getLocation()+")";
        }
        textView.setText(str);
    }

    void serviceStatus() {
        TextView textView = (TextView) findViewById(R.id.service_info);
        if (ServiceAutoSenses.isRunning) textView.setText("Running");
        else textView.setText("Not Running");
    }

    private void setupButtonSettings() {
        final Button button_settings = (Button) findViewById(R.id.button_settings);
        button_settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ActivityAutoSenseSettings.class);
                startActivity(intent);
            }
        });
    }

    private void setupButtonService() {
        final Button buttonStopService = (Button) findViewById(R.id.button_stopservice);
        final Button buttonStartService = (Button) findViewById(R.id.button_startservice);
        buttonStartService.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!ServiceAutoSenses.isRunning) {
                    starttimestamp = 0;
                    Intent intent = new Intent(getBaseContext(), ServiceAutoSenses.class);
                    startService(intent);
                    TextView textView = (TextView) findViewById(R.id.service_info);
                    textView.setText("Running");
                }
            }
        });
        buttonStopService.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ServiceAutoSenses.isRunning) {
                    Intent intent = new Intent(getBaseContext(), ServiceAutoSenses.class);
                    stopService(intent);
                    TextView textView = (TextView) findViewById(R.id.service_info);
                    textView.setText("Not Running");
                }
            }
        });
    }

    long starttimestamp = 0;
    HashMap<String, TextView> hm = new HashMap<>();

    void updateServiceStatus() {
        TextView textView = (TextView) findViewById(R.id.service_info);
        if (starttimestamp == 0) starttimestamp = DateTime.getDateTime();
        double minutes = ((double) (DateTime.getDateTime() - starttimestamp) / (1000 * 60));
        textView.setText("Running (" + String.format("%.2f", minutes) + " minutes)");
    }

    void updateTable(Intent intent) {
        String sampleStr = "";
        String platform = intent.getStringExtra("platformType")+":"+intent.getStringExtra("platformId");
        int count = intent.getIntExtra("count", 0);
        hm.get(platform + "_count").setText(String.valueOf(count));

        double time = (intent.getLongExtra("timestamp", 0) - intent.getLongExtra("starttimestamp", 0)) / 1000.0;
        double freq = (double) count / time;
        hm.get(platform + "_freq").setText(String.format("%.1f", freq));


        DataType data = (DataType) intent.getSerializableExtra("data");
            byte[] sample = ((DataTypeByteArray) data).getSample();
            for (int i = 0; i < sample.length; i++) {
                if (i != 0) sampleStr += ",";
                if (i % 3 == 0 && i != 0) sampleStr += "\n";
                sampleStr = sampleStr + String.valueOf(sample[i]);
            }
        hm.get(platform + "_sample").setText(sampleStr);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateServiceStatus();
            updateTable(intent);

        }
    };

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("autosense"));
        serviceStatus();
        showDevices();
        prepareTable();
        setupButtonService();

        super.onResume();
    }
    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
