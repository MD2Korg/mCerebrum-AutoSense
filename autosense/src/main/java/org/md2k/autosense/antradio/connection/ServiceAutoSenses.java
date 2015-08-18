package org.md2k.autosense.antradio.connection;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.dsi.ant.channel.ChannelNotAvailableException;

import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;

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

public class ServiceAutoSenses extends Service {
    private static final String TAG = ServiceAutoSenses.class.getSimpleName();
    AutoSensePlatforms autoSensePlatforms = null;
    public static boolean isRunning = false;

    private ServiceAutoSense.ChannelServiceComm mChannelService;

    private boolean mChannelServiceBound = false;
    DataKitApi dataKitApi = null;

    @Override
    public void onCreate(){
        super.onCreate();
        dataKitApi = new DataKitApi(getBaseContext());

        if (!dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                autoSensePlatforms = AutoSensePlatforms.getInstance(ServiceAutoSenses.this);
                autoSensePlatforms.register(dataKitApi);
                doBindChannelService();

            }
        })){
            Log.e(TAG,"DataKit Service is not available");
            isRunning = false;
        } else {
            isRunning = true;
        }
        if (!isRunning) stopSelf();

    }
    private void doBindChannelService()
    {
        Log.v(TAG, "doBindChannelService...");

        Intent bindIntent = new Intent(this, ServiceAutoSense.class);
        startService(bindIntent);
        mChannelServiceBound = bindService(bindIntent, mChannelServiceConnection,
                Context.BIND_AUTO_CREATE);

        if (!mChannelServiceBound) // If the bind returns false, run the unbind
            // method to update the GUI
            doUnbindChannelService();
        Log.i(TAG, "  Channel Service binding = " + mChannelServiceBound);
        Log.v(TAG, "...doBindChannelService");
    }

    private void doUnbindChannelService()
    {
        Log.v(TAG, "doUnbindChannelService...");

        if (mChannelServiceBound)
        {
            // Telling ChannelService to close all the channels
            mChannelService.clearAllChannels();

            unbindService(mChannelServiceConnection);

            mChannelServiceBound = false;
        }

        Log.v(TAG, "...doUnbindChannelService");
    }
    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy...");
        doUnbindChannelService();
        stopService(new Intent(this, ServiceAutoSense.class));

        mChannelServiceConnection = null;

        Log.v(TAG, "...onDestroy");
//        autoSensePlatforms.unregister();
        if (isRunning) dataKitApi.disconnect();
        isRunning=false;

        super.onDestroy();
    }
    private ServiceConnection mChannelServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder)
        {
            Log.v(TAG, "mChannelServiceConnection.onServiceConnected...");

            mChannelService = (ServiceAutoSense.ChannelServiceComm) serviceBinder;

            mChannelService.setOnChannelChangedListener(new ServiceAutoSense.ChannelChangedListener() {
                @Override
                public void onChannelChanged(final ChannelInfo newInfo) {
//                    Log.d(TAG,"onChannelChanged()"+newInfo.status);
                    if (newInfo.status == 1) {
                        Log.e(TAG, "onChannelChanged() error=" + newInfo.status);
                        mChannelService.clearChannel(newInfo.autoSensePlatform);
                        addNewChannel(newInfo.autoSensePlatform);
                    }

//                    Log.d(TAG, "onChannelChanged()");


/*                    Integer index = mIdChannelListIndexMap.get(newInfo.DEVICE_NUMBER);

                    if (null != index && index.intValue() < mChannelDisplayList.size()) {
                        mChannelDisplayList.set(index.intValue(), getDisplayText(newInfo));
                    }
*/
                }

                @Override
                public void onAllowAddChannel(boolean addChannelAllowed) {
                    for (int i = 0; i < autoSensePlatforms.size(); i++) {
                        addNewChannel(autoSensePlatforms.get(i));
                    }
                }
            });
            Log.v(TAG, "...mChannelServiceConnection.onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            Log.v(TAG, "mChannelServiceConnection.onServiceDisconnected...");

            mChannelService = null;


            Log.v(TAG, "...mChannelServiceConnection.onServiceDisconnected");
        }
    };

    public void addNewChannel(AutoSensePlatform autoSensePlatform)
    {
        Log.v(TAG, "addNewChannel...");
        Log.d(TAG, "mChannelService=" + mChannelService);

        if(null != mChannelService)
        {
            ChannelInfo newChannelInfo;
            try
            {
                // Telling the ChannelService to add a new channel. This method
                // in ChannelService contains code required to acquire an ANT
                // channel from ANT Radio Service.
                newChannelInfo = mChannelService.addNewChannel(autoSensePlatform);
            } catch (ChannelNotAvailableException e)
            {
                // Occurs when a channel is not available. Printing out the
                // stack trace will show why no channels are available.
                Toast.makeText(this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                return;
            }

        }

        Log.v(TAG, "...addNewChannel");
    }

    private void clearAllChannels()
    {
        Log.v(TAG, "clearAllChannels...");
        if (null != mChannelService) {
            // Telling ChannelService to close all the channels
            mChannelService.clearAllChannels();
        }

        Log.v(TAG, "...clearAllChannels");
    }

    public ServiceAutoSenses() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
