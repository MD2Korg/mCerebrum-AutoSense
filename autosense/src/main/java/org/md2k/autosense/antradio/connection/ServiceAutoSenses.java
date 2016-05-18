package org.md2k.autosense.antradio.connection;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import com.dsi.ant.channel.ChannelNotAvailableException;

import org.md2k.autosense.Constants;
import org.md2k.autosense.LoggerText;
import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.utilities.UI.AlertDialogs;

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
    public static boolean isRunning = false;
    AutoSensePlatforms autoSensePlatforms = null;
    DataKitAPI dataKitAPI;
    private ServiceAutoSense.ChannelServiceComm mChannelService;

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

    private boolean readSettings() {
        autoSensePlatforms = new AutoSensePlatforms(getApplicationContext());
        return autoSensePlatforms.size() != 0;
    }

    private void connectDataKit() {
        dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
        try {
            dataKitAPI.connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    autoSensePlatforms.register();
                    Toast.makeText(ServiceAutoSenses.this, "AutoSense Started successfully", Toast.LENGTH_LONG).show();
                    startAutoSense();
                }
            });
        } catch (DataKitException e) {
            autoSensePlatforms.unregister();
            Toast.makeText(ServiceAutoSenses.this, "AutoSense Stopped. DataKitException", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }


    void startAutoSense() {
        doBindChannelService();
        isRunning = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Constants.LOG_TEXT)
            LoggerText.getInstance();

        isRunning = false;
        if (!readSettings()) {
            AlertDialogs.showAlertDialog(this, "Configuration Error", "Configuration file for AutoSense doesn't exist.\n\nPlease go to Menu -> Settings");
            stopSelf();
        } else connectDataKit();
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
        } catch (Exception e) {

        }
    }

    @Override
    public void onDestroy() {
        doUnbindChannelService();
        stopService(new Intent(this, ServiceAutoSense.class));

        mChannelServiceConnection = null;

        if (isRunning) {
            autoSensePlatforms.unregister();
            dataKitAPI.disconnect();
        }
        isRunning = false;
        if (Constants.LOG_TEXT)
            LoggerText.getInstance().close();

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

    private void clearAllChannels() {
        try {
            if (null != mChannelService) {
                // Telling ChannelService to close all the channels
                mChannelService.clearAllChannels();
            }
            mChannelService = null;
        }catch(Exception e){

        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
