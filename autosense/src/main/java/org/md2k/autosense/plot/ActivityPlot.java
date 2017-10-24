package org.md2k.autosense.plot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDouble;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeFloat;
import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.mcerebrum.commons.plot.RealtimeLineChartActivity;
import org.md2k.autosense.ActivityMain;

public class ActivityPlot extends RealtimeLineChartActivity {
    DataSource dataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            dataSource = getIntent().getExtras().getParcelable(DataSource.class.getSimpleName());
        }catch (Exception e){
            finish();
        }
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(ActivityMain.INTENT_NAME));

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
        String ds = intent.getStringExtra("key");
        String pi = intent.getStringExtra("platformid");
        if (!ds.equals(dataSource.getType()) || !pi.equals(dataSource.getPlatform().getId())) return;
        getmChart().getDescription().setText(dataSource.getType());
        getmChart().getDescription().setPosition(1f, 1f);
        getmChart().getDescription().setEnabled(true);
        getmChart().getDescription().setTextColor(Color.WHITE);
        if (ds.equals(DataSourceType.LED))
            legends = new String[]{"LED 1", "LED 2", "LED 3"};
        else if (ds.equals(DataSourceType.ACCELEROMETER)) {
            legends = new String[]{"Accelerometer X", "Accelerometer Y", "Accelerometer Z"};
        } else if (ds.equals(DataSourceType.GYROSCOPE)) {
            legends = new String[]{"Gyroscope X", "Gyroscope Y", "Gyroscope Z"};
        } else legends = new String[]{ds};
        DataType data = intent.getParcelableExtra("data");
        if (data instanceof DataTypeFloat) {
            sample = new float[]{((DataTypeFloat) data).getSample()};
        } else if (data instanceof DataTypeFloatArray) {
            sample = ((DataTypeFloatArray) data).getSample();
        } else if (data instanceof DataTypeDoubleArray) {
            double[] samples = ((DataTypeDoubleArray) data).getSample();
            sample = new float[samples.length];
            for (int i = 0; i < samples.length; i++) {
                sample[i] = (float) samples[i];
            }
        } else if (data instanceof DataTypeDouble) {
            double samples = ((DataTypeDouble) data).getSample();
            sample = new float[]{(float) samples};
        }
        addEntry(sample, legends, 600);
    }

}
