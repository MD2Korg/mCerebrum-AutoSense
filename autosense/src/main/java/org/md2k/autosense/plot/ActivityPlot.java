package org.md2k.autosense.plot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.mcerebrum.commons.plot.RealtimeLineChartActivity;

public class ActivityPlot extends RealtimeLineChartActivity {
    String dataSourceType;

    /*
    String platformId;
    String deviceId;
    String dataSourceId;
    String platformType;
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        DataSource dataSource=intent.getParcelableExtra(DataSource.class.getSimpleName());
        if(dataSource==null) {
            finish();
            return;
        }
        dataSourceType = dataSource.getType();

/*
        platformId = intent.getStringExtra("platformid");
        platformType = intent.getStringExtra("platformtype");
        deviceId = intent.getStringExtra("deviceId");
        dataSourceId = intent.getStringExtra("datasourceid");
*/
//        if (dataSourceType == null) finish();

    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("DATA"));

        super.onResume();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePlot(intent);
        }
    };

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        super.onPause();
    }

    void updatePlot(Intent intent) {
        float[] sample = new float[1];
        String[] legends;
        String ds = intent.getStringExtra("datasourcetype");
        double d = intent.getDoubleExtra("data", 0);
        if (!ds.equals(dataSourceType)) return;
        getmChart().getDescription().setText(dataSourceType);
        getmChart().getDescription().setPosition(1f, 1f);
        getmChart().getDescription().setEnabled(true);
        getmChart().getDescription().setTextColor(Color.WHITE);
        legends = new String[]{ds};
        sample = new float[]{(float) d};
        addEntry(sample, legends,300);
    }

}
