package org.md2k.autosense.antradio.connection;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.dsi.ant.channel.ChannelNotAvailableException;

import org.md2k.autosense.ActivityAutoSenseSettings;
import org.md2k.autosense.Constants;
import org.md2k.autosense.LoggerText;
import org.md2k.autosense.R;
import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.Report.LogStorage;
import org.md2k.utilities.UI.AlertDialogs;

import java.util.ArrayList;

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

public class ServiceAutoSenses extends Service {
    private static final String TAG = ServiceAutoSenses.class.getSimpleName();
    public static final String INTENT_RESTART = "intent_restart";
    AutoSensePlatforms autoSensePlatforms = null;
    DataKitAPI dataKitAPI;
    private ServiceAutoSense.ChannelServiceComm mChannelService;
    private boolean isStopping;

    private boolean mChannelServiceBound = false;
    private ServiceConnection mChannelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {

            mChannelService = (ServiceAutoSense.ChannelServiceComm) serviceBinder;

            mChannelService.setOnChannelChangedListener(new ServiceAutoSense.ChannelChangedListener() {
                @Override
                public void onChannelChanged(final ChannelInfo newInfo) {
                    if (newInfo.status == 1) {
                        mChannelService.clearChannel(newInfo.autoSensePlatform);
                        addNewChannel(newInfo.autoSensePlatform);
                    }
                }

                @Override
                public void onAllowAddChannel(boolean addChannelAllowed) {
                    for (int i = 0; i < autoSensePlatforms.size(); i++) {
                        addNewChannel(autoSensePlatforms.get(i));
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            clearAllChannels();
        }
    };

    private synchronized boolean readSettings() {
        autoSensePlatforms = new AutoSensePlatforms(getApplicationContext());
        return autoSensePlatforms.size() != 0;
    }

    private synchronized void connectDataKit() {
        dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
        try {
            dataKitAPI.connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    try {
                        autoSensePlatforms.register();
                        //Toast.makeText(ServiceAutoSenses.this, "AutoSense Started successfully", Toast.LENGTH_LONG).show();
                        startAutoSense();
                    }catch (Exception e){
                        LocalBroadcastManager.getInstance(ServiceAutoSenses.this).sendBroadcast(new Intent(Constants.INTENT_STOP));
                    }
                }
            });
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(ServiceAutoSenses.this).sendBroadcast(new Intent(Constants.INTENT_STOP));
        }
    }


    void startAutoSense() {
        doBindChannelService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isStopping=false;
        LogStorage.startLogFileStorageProcess(getApplicationContext().getPackageName());
        Log.w(TAG,"time="+ DateTime.convertTimeStampToDateTime(DateTime.getDateTime())+",timestamp="+ DateTime.getDateTime()+",service_start");
        if (Constants.LOG_TEXT)
            LoggerText.getInstance();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiverStop,
                new IntentFilter(Constants.INTENT_STOP));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverRestart, new IntentFilter(INTENT_RESTART));

        if (!readSettings()) {
            showAlertDialogConfiguration(this);
            clear();
            stopSelf();
        } else connectDataKit();
    }

    void showAlertDialogConfiguration(final Context context) {
        AlertDialogs.AlertDialog(this, "Error: AutoSense Settings", "Please configure AutoSense", R.drawable.ic_error_red_50dp, "Settings", "Cancel", null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    Intent intent = new Intent(context, ActivityAutoSenseSettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });
    }

    private void doBindChannelService() {
        Intent bindIntent = new Intent(this, ServiceAutoSense.class);
        startService(bindIntent);
        mChannelServiceBound = bindService(bindIntent, mChannelServiceConnection,
                Context.BIND_AUTO_CREATE);

        if (!mChannelServiceBound) // If the bind returns false, run the unbind
            doUnbindChannelService();
    }

    private void doUnbindChannelService() {
        try {
            if (mChannelServiceBound) {
                // Telling ChannelService to close all the channels
                if (mChannelService != null)
                    mChannelService.clearAllChannels();
                unbindService(mChannelServiceConnection);
                mChannelServiceBound = false;
            }
        } catch (Exception ignored) {

        }
    }
    synchronized void clear(){
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiverStop);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiverRestart);
        if(isStopping) return;
        isStopping=false;
        doUnbindChannelService();
        stopService(new Intent(this, ServiceAutoSense.class));

        mChannelServiceConnection = null;
        if(autoSensePlatforms!=null)
            autoSensePlatforms.unregister();
        if(dataKitAPI!=null)
            dataKitAPI.disconnect();
        if (Constants.LOG_TEXT)
            LoggerText.getInstance().close();
    }

    @Override
    public void onDestroy() {
        Log.w(TAG,"time="+ DateTime.convertTimeStampToDateTime(DateTime.getDateTime())+",timestamp="+ DateTime.getDateTime()+",service_stop");
        clear();
        super.onDestroy();
    }

    public void addNewChannel(AutoSensePlatform autoSensePlatform) {
        if (null != mChannelService) {
            ChannelInfo newChannelInfo;
            try {
                // Telling the ChannelService to add a new channel. This method
                // in ChannelService contains code required to acquire an ANT
                // channel from ANT Radio Service.
                newChannelInfo = mChannelService.addNewChannel(autoSensePlatform);
            } catch (ChannelNotAvailableException e) {
                // Occurs when a channel is not available. Printing out the
                // stack trace will show why no channels are available.
//                Toast.makeText(this, "Channel Not Available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearChannel(AutoSensePlatform autoSensePlatform){
        if(mChannelService!=null)
            mChannelService.clearChannel(autoSensePlatform);
    }

    private void clearAllChannels() {
        try {
            if (null != mChannelService) {
                // Telling ChannelService to close all the channels
                mChannelService.clearAllChannels();
            }
            mChannelService = null;
        } catch (Exception ignored) {

        }
    }

    private BroadcastReceiver mMessageReceiverRestart = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            AutoSensePlatform autoSensePlatform = (AutoSensePlatform) intent.getSerializableExtra(AutoSensePlatform.class.getSimpleName());
            String deviceId=intent.getStringExtra("device_id");
            String platformType=intent.getStringExtra("platform_type");
            String platformId = intent.getStringExtra("platform_id");
            ArrayList<AutoSensePlatform> autoSensePlatform = autoSensePlatforms.find(platformType, platformId, deviceId);
            if(autoSensePlatform!=null && autoSensePlatform.size()>0) {
                clearChannel(autoSensePlatform.get(0));
                addNewChannel(autoSensePlatform.get(0));
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private BroadcastReceiver mMessageReceiverStop = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Stop");
            Log.w(TAG,"time="+ DateTime.convertTimeStampToDateTime(DateTime.getDateTime())+",timestamp="+ DateTime.getDateTime()+",broadcast_receiver_stop_service");
            clear();
            stopSelf();
        }
    };

}
