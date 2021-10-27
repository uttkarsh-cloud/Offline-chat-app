package com.pk.inchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import static com.pk.inchat.MessageInstance.*;



public class EstablishCommunication
{

    
    private RecieveMsg mRecieveMsg;
    private SendMsg mSendMsg;
    private StartCommunication mStartCommunication;

    private static final String TAG = "BluetoothChatService";
    private static final String NAME = "MyBluetoothChat";
    private static final UUID MY_UUID = UUID.fromString("5a7d131a-6d10-491b-b9bc-8bf772bf988f");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    public static final int UPDATE_DISCONNECT = 0;
    public static final int UPDATE_RECEIVING_REQUEST = 1;
    public static final int UPDATE_CONNECTING = 2;
    public static final int UPDATE_CONNECTED = 3;
    public static final String TOAST = "toast";

    public static final int UPDATE_STATUS = 1;
    public static final int MESSAGE_TOAST = 5;

    public static final int IMAGE_RECEIVE = 6;
    public static final int TEXT_RECEIVE = 8;

    public static final int IMAGE_SEND = 9;
    public static final int TEXT_SEND = 11;

    private int mState;
    private int mNewState;


    public EstablishCommunication(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = UPDATE_DISCONNECT;
        mNewState = mState;
        mHandler = handler;

    }


    /*******************************************/



    public synchronized void connect(BluetoothDevice device) {

        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == UPDATE_CONNECTING) {
            if (mSendMsg != null) {
                mSendMsg.cancel();
                mSendMsg = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mStartCommunication != null) {
            mStartCommunication.cancel();
            mStartCommunication = null;
        }

        // Start the thread to connect with the given device
        mSendMsg = new SendMsg(device);
        mSendMsg.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    public synchronized void connected(BluetoothSocket socket) {

        // Cancel the thread once connection is completed
        if (mSendMsg != null) {
            mSendMsg.cancel();
            mSendMsg = null;
        }

        // Cancel any thread currently running a connection
        if (mStartCommunication != null) {
            mStartCommunication.cancel();
            mStartCommunication = null;
        }

        // Cancel the Recieve thread because we only want to connect to one device

        if (mRecieveMsg != null) {
            mRecieveMsg.cancel();
            mRecieveMsg = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mStartCommunication = new StartCommunication(socket);
        mStartCommunication.start();

        // Send the name of the connected device back to the UI Activity
        updateUserInterfaceTitle();
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mSendMsg != null) {
            mSendMsg.cancel();
            mSendMsg = null;
        }

        // Cancel any thread currently running a connection
        if (mStartCommunication != null) {
            mStartCommunication.cancel();
            mStartCommunication = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mRecieveMsg == null) {
            mRecieveMsg = new RecieveMsg();
            mRecieveMsg.start();
        }

        // Update UI title
        updateUserInterfaceTitle();
    }


    private class SendMsg extends Thread {
        private final BluetoothSocket SendSocket;
        private final BluetoothDevice mDevice;

        public SendMsg(BluetoothDevice device) {
            mDevice = device;
            BluetoothSocket mysocket = null;
            try {
                mysocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "connect thread creation failed", e);
            }
            SendSocket = mysocket;
            mState = UPDATE_CONNECTING;
        }

        public void run() {
            setName("SendMsg");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                SendSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    SendSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close socket during connection failure", e2);
                }
                connectionDisconnect();
                return;
            }

           // Reset the SendMsg because we're done
            synchronized (EstablishCommunication.this) {
                mSendMsg = null;
            }

            // Start the connected thread
            connected(SendSocket);
        }

        public void cancel() {
            try {
                SendSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "connect thread closing failed", e);
            }
        }
    }



    private class RecieveMsg extends Thread {

        private final BluetoothServerSocket ReceiveSocket;

        public RecieveMsg() {
            BluetoothServerSocket tmp = null;

            // Create new listening server socket
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen failed", e);
            }
            ReceiveSocket = tmp;
            mState = UPDATE_RECEIVING_REQUEST;
        }

        @Override
        public void run() {

            BluetoothSocket Socket = null;

            while (mState != UPDATE_CONNECTED) {
                try { Socket = ReceiveSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Not Received", e);
                    break;
                }

                if (Socket != null) {
                    synchronized (EstablishCommunication.this) {
                        switch (mState) {
                            case UPDATE_RECEIVING_REQUEST:
                            case UPDATE_CONNECTING:
                                connected(Socket);
                                break;
                            case UPDATE_DISCONNECT:
                            case UPDATE_CONNECTED:
                                try {
                                    Socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                ReceiveSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "closing of server failed", e);
            }
        }
    }


    private class StartCommunication extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public StartCommunication(BluetoothSocket socket) {
            mSocket = socket;
            InputStream Input = null;
            OutputStream Output = null;

            // Get the BluetoothSocket input and output streams
            try {
                Input = socket.getInputStream();
                Output = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mInStream = Input;
            mOutStream = Output;
            mState = UPDATE_CONNECTED;
        }


        public void run() {
            final int BUFFER_SIZE = 16384;
            byte[] bufferData = new byte[BUFFER_SIZE];
            int numOfPackets = 0;
            int datatype = 0;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            while (mState == UPDATE_CONNECTED) {
                try {
                    int numOfBytes = mInStream.read(bufferData);
                    byte[] trimmedBufferData = Arrays.copyOf(bufferData, numOfBytes);
                    bufferData = new byte[BUFFER_SIZE];
                    ByteBuffer tempBuffer = ByteBuffer.wrap(trimmedBufferData);

                    String macAddress = mSocket.getRemoteDevice().getAddress();
                    String userName = mSocket.getRemoteDevice().getName();

                    MessageInstance dataSent = new MessageInstance();
                    dataSent.setMacAddress(macAddress);
                    dataSent.setUserName(userName);


                    if (datatype == 0) {
                        datatype = tempBuffer.getInt();
                        Log.d(TAG, "Datatype: " + datatype);
                    }
                    if (numOfPackets == 0) {
                        numOfPackets = tempBuffer.getInt();
                        Log.d(TAG, "Packets size: " + numOfPackets);
                    }
                    byte[] dst = new byte[tempBuffer.remaining()];
                    tempBuffer.get(dst);
                    bos.write(dst);
                    //Following condition checks if we have received all necessary bytes to construct a message out of it.
                    if (bos.size() == numOfPackets) {

                        switch(datatype) {

                            case DATA_TEXT:
                                Log.d(TAG, "Reading text from socket");
                                dataSent.setData(bos.toByteArray());
                                dataSent.setDataType(DATA_TEXT);
                                Message textMsg = mHandler.obtainMessage(TEXT_RECEIVE, -1,
                                        datatype, dataSent);
                                textMsg.sendToTarget();
                                break;
                            case DATA_IMAGE:
                                Log.d(TAG, "Reading image from socket");
                                String decodedString = new String(bos.toByteArray(),
                                        Charset.defaultCharset());
                                byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                                Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray, 0, decodedStringArray.length);

                                dataSent.setDataType(DATA_IMAGE);
                                dataSent.setData(bp);
                                Message imgMsg = mHandler.obtainMessage(IMAGE_RECEIVE, -1, datatype, dataSent);
                                imgMsg.sendToTarget();
                                break;
                        }
                        //Re-initialize for the next message.
                        datatype = 0;
                        numOfPackets = 0;
                        bos = new ByteArrayOutputStream();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    DropConnection();
                    break;
                }
            }
        }

        public void write(byte[] bytes, int datatype, String timeSent) {
            try {
                Message writtenMsg = null;
                ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
                ByteBuffer tempBuffer = ByteBuffer.allocate(bytes.length + 8);
                MessageInstance dataSent = new MessageInstance();
                String macAddress = mSocket.getRemoteDevice().getAddress();
                String userName = mSocket.getRemoteDevice().getName();
                dataSent.setMacAddress(macAddress);
                dataSent.setUserName(userName);
                dataSent.setTime(timeSent);
                if (datatype == DATA_IMAGE) {

                    System.out.println("IMAGE WRITE");

                    tempBuffer.putInt(DATA_IMAGE);
                    ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                    imageStream.write(bytes);
                    String decodedString = new String(imageStream.toByteArray(),
                            Charset.defaultCharset());
                    byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                    Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray,
                            0, decodedStringArray.length);

                    dataSent.setData(bp);
                    dataSent.setDataType(DATA_IMAGE);

                    writtenMsg = mHandler.obtainMessage(IMAGE_SEND, -1, DATA_IMAGE,
                            dataSent);
                    imageStream.close();

                } else if (datatype == DATA_TEXT) {

                    tempBuffer.putInt(DATA_TEXT);
                    dataSent.setData(bytes);
                    dataSent.setDataType(DATA_TEXT);

                    writtenMsg = mHandler.obtainMessage(TEXT_SEND, -1, DATA_TEXT,
                            dataSent);

                }
                //Log.d(TAG, "Sending size: " + bytes.length);
                tempBuffer.putInt(bytes.length);
                //Log.d(TAG, "Sending data: " + new String(bytes, Charset.defaultCharset()));
                tempBuffer.put(bytes);
                tempOutputStream.write(tempBuffer.array());
                mOutStream.write(tempOutputStream.toByteArray());
                tempOutputStream.close();
                if (writtenMsg != null) {
                    writtenMsg.sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                Message writeErrorMsg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Device disconnected. " +
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private void connectionDisconnect() {

        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = UPDATE_DISCONNECT;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        EstablishCommunication.this.start();
    }

    private void DropConnection() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = UPDATE_DISCONNECT;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        EstablishCommunication.this.start();
    }





    public synchronized void CloseConnection() {
        Log.d(TAG, "CloseConnection");

        if (mSendMsg != null) {
            mSendMsg.cancel();
            mSendMsg = null;
        }

        if (mStartCommunication != null) {
            mStartCommunication.cancel();
            mStartCommunication = null;
        }

        if (mRecieveMsg != null) {
            mRecieveMsg.cancel();
            mRecieveMsg = null;
        }

        mState = UPDATE_DISCONNECT;
        // Update UI title
        updateUserInterfaceTitle();
    }


    public void write(byte[] out, int datatype, String timeSent) {
        // Create temporary object
        StartCommunication r;
        // Synchronize a copy of the StartCommunication
        synchronized (this) {
            if (mState != UPDATE_CONNECTED) {return;}
            r = mStartCommunication;
        }
        // Perform the write unsynchronized
        r.write(out, datatype, timeSent);
    }

    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(UPDATE_STATUS, mNewState, -1).sendToTarget();
    }

    public synchronized int getState()
    {
        return mState;
    }

}
