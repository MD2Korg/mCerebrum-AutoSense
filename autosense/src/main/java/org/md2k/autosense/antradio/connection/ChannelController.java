package org.md2k.autosense.antradio.connection;

import android.os.RemoteException;
import android.util.Log;

import com.dsi.ant.channel.AntChannel;
import com.dsi.ant.channel.AntCommandFailedException;
import com.dsi.ant.channel.IAntChannelEventHandler;
import com.dsi.ant.message.ChannelId;
import com.dsi.ant.message.ChannelType;
import com.dsi.ant.message.fromant.AcknowledgedDataMessage;
import com.dsi.ant.message.fromant.BroadcastDataMessage;
import com.dsi.ant.message.fromant.ChannelEventMessage;
import com.dsi.ant.message.fromant.DataMessage;
import com.dsi.ant.message.fromant.MessageFromAntType;
import com.dsi.ant.message.ipc.AntMessageParcel;

import org.md2k.autosense.Constants;
import org.md2k.autosense.LoggerText;
import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.datakitapi.time.DateTime;

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

public class ChannelController {

    private static final String TAG = ChannelController.class.getSimpleName();

    private AntChannel mAntChannel;
    private ChannelBroadcastListener mChannelBroadcastListener;

    private ChannelEventCallback mChannelEventCallback = new ChannelEventCallback();
    private ChannelInfo mChannelInfo;

    private boolean mIsOpen;

    public ChannelController(AntChannel antChannel, AutoSensePlatform autoSensePlatform,
                             ChannelBroadcastListener broadcastListener) {
        mAntChannel = antChannel;
        mChannelInfo = new ChannelInfo(autoSensePlatform);
        mChannelBroadcastListener = broadcastListener;
        openChannel();
    }

    public boolean openChannel() {
        if (null != mAntChannel) {
            if (mIsOpen) {
                Log.w(TAG, "Channel was already open");
            } else {
                /*
                 * Although this reference code sets ChannelType to either a transmitting master or a receiving slave,
                 * the standard for ANT is that channels communication is bidirectional. The use of single-direction
                 * communication in this app is for ease of understanding as reference code. For more information and
                 * any additional features on ANT channel communication, refer to the ANT Protocol Doc found at:
                 * http://www.thisisant.com/resources/ant-message-protocol-and-usage/
                 */
                ChannelType channelType = ChannelType.BIDIRECTIONAL_SLAVE;

                // Channel ID message contains device number, type and transmission type. In
                // order for master (TX) channels and slave (RX) channels to connect, they
                // must have the same channel ID, or wildcard (0) is used.
                ChannelId channelId = new ChannelId(mChannelInfo.DEVICE_NUMBER,
                        mChannelInfo.CHANNEL_PROOF_DEVICE_TYPE, mChannelInfo.CHANNEL_PROOF_TRANSMISSION_TYPE);

                try {
                    // Setting the channel event handler so that we can receive messages from ANT
                    mAntChannel.setChannelEventHandler(mChannelEventCallback);

                    // Performs channel assignment by assigning the type to the channel. Additional
                    // features (such as, background scanning and frequency agility) can be enabled
                    // by passing an ExtendedAssignment object to assign(ChannelType, ExtendedAssignment).
                    mAntChannel.assign(channelType);

                    /*
                     * Configures the channel ID, messaging period and rf frequency after assigning,
                     * then opening the channel.
                     *
                     * For any additional ANT features such as proximity search or background scanning, refer to
                     * the ANT Protocol Doc found at:
                     * http://www.thisisant.com/resources/ant-message-protocol-and-usage/
                     */
                    mAntChannel.setChannelId(channelId);
                    mAntChannel.setPeriod(mChannelInfo.CHANNEL_PROOF_PERIOD);
                    mAntChannel.setRfFrequency(mChannelInfo.CHANNEL_PROOF_FREQUENCY);
                    mAntChannel.open();
                    mIsOpen = true;

                    Log.d(TAG, "Opened channel with device number: " + mChannelInfo.DEVICE_NUMBER);
                } catch (RemoteException e) {
                    close();
                    channelError(e);
                } catch (AntCommandFailedException e) {
                    // This will release, and therefore unassign if required
                    close();
                    channelError("Open failed", e);
                }
            }
        } else {
            Log.w(TAG, "No channel available");
        }

        return mIsOpen;
    }

    public ChannelInfo getCurrentInfo() {
        return mChannelInfo;
    }

    void displayChannelError(String displayText) {
        mChannelInfo.die(displayText);
    }

    void channelError(RemoteException e) {
        String logString = "Remote service communication failed.";

        Log.e(TAG, logString);

        displayChannelError(logString);
    }

    void channelError(String error, AntCommandFailedException e) {
        close();
    }

    public void close() {
        // TODO kill all our resources
        if (null != mAntChannel) {
            mIsOpen = false;
            // Releasing the channel to make it available for others.
            // After releasing, the AntChannel instance cannot be reused.
            try {
                mAntChannel.clearChannelEventHandler();
                mAntChannel.unassign();
            } catch (RemoteException | AntCommandFailedException ignored) {
                Log.d(TAG,"error");
            }

            mAntChannel.release();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mAntChannel = null;
        }


        displayChannelError("Channel Closed");
    }

    static public abstract class ChannelBroadcastListener {
        public abstract void onBroadcastChanged(ChannelInfo newInfo);
    }

    /**
     * Implements the Channel Event Handler Interface so that messages can be
     * received and channel death events can be handled.
     */
    public class ChannelEventCallback implements IAntChannelEventHandler {
        private void updateData(DataMessage data) {
            if(!mIsOpen) return;
            mChannelInfo.status = 0;
            mChannelInfo.broadcastData = data.getMessageContent();
            mChannelInfo.timestamp= DateTime.getDateTime();
            mChannelBroadcastListener.onBroadcastChanged(mChannelInfo);
        }

        private void sendError(String msg) {
            mChannelInfo.broadcastData = msg.getBytes();
            mChannelInfo.status = 1;
            mChannelBroadcastListener.onBroadcastChanged(mChannelInfo);
        }

        @Override
        public void onChannelDeath() {
            // Display channel death message when channel dies
            displayChannelError("Channel Death");
        }

        @Override
        public void onReceiveMessage(MessageFromAntType messageType, AntMessageParcel antParcel) {
            // Switching on message type to handle different types of messages
            if(Constants.LOG_TEXT){
                String str=String.valueOf(DateTime.getDateTime())+","+antParcel+"\n";
                LoggerText.getInstance().saveDataToTextFile(str);
            }
            switch (messageType) {
                // If data message, construct from parcel and update channel data
                case BROADCAST_DATA:
                    // Rx Data
                    updateData(new BroadcastDataMessage(antParcel));
                    break;
                case ACKNOWLEDGED_DATA:
                    // Rx Data
                    updateData(new AcknowledgedDataMessage(antParcel));
                    break;
                case CHANNEL_EVENT:
                    // Constructing channel event message from parcel
                    ChannelEventMessage eventMessage = new ChannelEventMessage(antParcel);
                    // Switching on event code to handle the different types of channel events
                    switch (eventMessage.getEventCode()) {
                        case TX:
                            // Use old info as this is what remote device has just received
                            Log.d(TAG, "TX = " + mChannelInfo.broadcastData);
                            mChannelBroadcastListener.onBroadcastChanged(mChannelInfo);

                            mChannelInfo.broadcastData[0]++;

                            if (mIsOpen) {
                                try {
                                    // Setting the data to be broadcast on the next channel period
                                    mAntChannel.setBroadcastData(mChannelInfo.broadcastData);
                                } catch (RemoteException e) {
                                    channelError(e);
                                }
                            }
                            break;
                        case RX_SEARCH_TIMEOUT:
                            // TODO May want to keep searching
                            displayChannelError("No Device Found");
                            sendError("No Device Found");
                            break;
                        case CHANNEL_CLOSED:
                        case CHANNEL_COLLISION:
                        case RX_FAIL:
                        case RX_FAIL_GO_TO_SEARCH:
                        case TRANSFER_RX_FAILED:
                        case TRANSFER_TX_COMPLETED:
                        case TRANSFER_TX_FAILED:
                        case TRANSFER_TX_START:
                        case UNKNOWN:
                            // TODO More complex communication will need to handle these events
                            break;
                    }
                    break;
                case ANT_VERSION:
                case BURST_TRANSFER_DATA:
                case CAPABILITIES:
                case CHANNEL_ID:
                case CHANNEL_RESPONSE:
                case CHANNEL_STATUS:
                case SERIAL_NUMBER:
                case OTHER:
                    // TODO More complex communication will need to handle these message types
                    break;
            }
        }
    }
}
