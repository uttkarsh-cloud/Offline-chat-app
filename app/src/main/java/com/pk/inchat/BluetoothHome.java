package com.pk.inchat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import static com.pk.inchat.MessageInstance.*;
import static com.pk.inchat.EstablishCommunication.*;



public class BluetoothHome extends AppCompatActivity {


    private static final int CONNECT_DEVICE = 3;
    private EstablishCommunication mChatService;
    public static BluetoothAdapter BtAdapter;
    public static BluetoothDevice device;

    private static final int SELECT_IMAGE = 11;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;

    public static final String DEVICE_NAME = "device_name";
    private StringBuffer mOutStringBuffer;
    private ListView mConversationView;
    private EditText mEditText;
    private ImageButton mButtonSend;
    private TextView connectionStatus;
    ChatMessageAdapter chatMessageAdapter;
    private Bitmap imageBitmap;
    static final SimpleDateFormat sdf=new SimpleDateFormat("HH:mm");
    private static final int CAMERA_REQUEST = 2000;

 //   private ImageView fullscreen;

    private final static String TAG = "BluetoothHome";
    private final static int MAX_IMAGE_SIZE = 200000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_home);
        BtAdapter=BluetoothAdapter.getDefaultAdapter();
        mEditText=findViewById(R.id.edit_text_text_message);
        mButtonSend=findViewById(R.id.btn_send);
        connectionStatus=findViewById(R.id.connection_status);
        mConversationView=findViewById(R.id.message_history);

        chatMessageAdapter= new ChatMessageAdapter(BluetoothHome.this, R.layout.chat_message);
        mConversationView.setAdapter(chatMessageAdapter);
        //fullscreen = (ImageView) findViewById(R.id.fullscreen_image);
        if(mChatService==null)
        {
            setupChat();
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_make_discoverable:
                ensureDiscoverable();
                return true;

            case R.id.menu_search_devices:

                Intent bluetoothIntent = new Intent(getApplicationContext(),
                        ShowDevices.class);
                startActivityForResult(bluetoothIntent, CONNECT_DEVICE);
                break;

        }
        return super.onOptionsItemSelected(item);
    }
    /********/

    public void ensureDiscoverable()
    {
        Toast.makeText(getApplicationContext(),"Start Discovering",Toast.LENGTH_LONG).show();
        if (BtAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    /********/


    public void connectDevice(String macAddress)
    {
        device =BtAdapter.getRemoteDevice(macAddress);
        String mConnectedDeviceAddress = macAddress;
        Toast.makeText(getApplicationContext(),"Connect Request",Toast.LENGTH_LONG).show();
        mChatService.connect(device);
    }


    /********/

    private void setupChat() {
        // Initialize the compose field with a listener for the return key
        mEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                String message = mEditText.getText().toString();

                sendMessage(message);

            }
        });


        mChatService = new EstablishCommunication(handler);
        mOutStringBuffer = new StringBuffer("");
    }


    /********/

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != EstablishCommunication.UPDATE_CONNECTED)
        {
            Toast.makeText(getApplicationContext(), R.string.not_connected,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            System.out.println("Message Length = " + message.length());

            Calendar calendar = Calendar.getInstance();
            String timeSent = sdf.format(calendar.getTime());
            mChatService.write(message.getBytes(), DATA_TEXT, timeSent);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mEditText.setText(mOutStringBuffer);
        }
    }


    /********/


    public void PhotoMessage(View view) {

        AccessPermission();
    }

    /********/

    public void AccessPermission()
    {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {

            // Should we show explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Add your explanation for the user here.
                Toast.makeText(this, "You have declined the permissions. " + "Please allow them first to proceed.", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            requestImageFromGallery();
        }

    }


    /********/



    public void requestImageFromGallery() {
//        Toast.makeText(getApplicationContext(),"Hello ",Toast.LENGTH_LONG).show();
        Intent attachImageIntent = new Intent();
        attachImageIntent.setType("image/*");
        attachImageIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(attachImageIntent, "Select Picture"),
                SELECT_IMAGE);
    }

    /********/

    public void CameraPhoto(View view) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    /*******when application close****/
    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy called");
        super.onDestroy();

        if (mChatService != null) {
            mChatService.CloseConnection();
        }
    }

    /***********/

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back pressed");

        if (mChatService != null) {
            mChatService.CloseConnection();
        }
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        switch (requestCode) {
            case CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String macAddress = data.getExtras().getString(ShowDevices.EXTRA_DEVICE_ADDRESS);
                    connectDevice(macAddress);
                }
                break;

            case SELECT_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

                            // If you can't compress the image, then do not try sending.
                            byte[] imageSend;
                            try {
                                imageSend = compressBitmap(bitmap, true);
                            } catch (NullPointerException e) {
                                Log.d(TAG, "Image cannot be compressed");
                                Toast.makeText(getApplicationContext(), "Image can not be found" + " or is too large to be sent", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Calendar calendar = Calendar.getInstance();
                            String timeSent = sdf.format(calendar.getTime());

                            if (imageSend.length > MAX_IMAGE_SIZE) {
                                Toast.makeText(getApplicationContext(), "Image is too large",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            mChatService.write(imageSend, DATA_IMAGE, timeSent);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;

            case CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");

                        byte[] cameraSend;
                        try {
                            cameraSend = compressBitmap(bitmap, true);
                        } catch (Exception e) {
                            Log.d(TAG, "Could not find the image");
                            Toast.makeText(getApplicationContext(), "Image could not be sent",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (cameraSend.length > MAX_IMAGE_SIZE) {
                            Toast.makeText(getApplicationContext(), "Image is too large to be sent",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Calendar calendar = Calendar.getInstance();
                        String timeSent = sdf.format(calendar.getTime());
                        mChatService.write(cameraSend, DATA_IMAGE, timeSent);

                    }
                }
                break;


        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /********/


    static byte[] compressBitmap(Bitmap image, boolean isBeforeSocketSend) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        image.compress(Bitmap.CompressFormat.JPEG, 50, bos);
        String encodedImage = Base64.encodeToString(bos.toByteArray(),Base64.DEFAULT);

        byte[] compressed = isBeforeSocketSend ? encodedImage.getBytes() : Base64.decode(encodedImage, Base64.DEFAULT);

        return compressed;
    }


    /********/



    Handler handler= new Handler()
    {

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case UPDATE_STATUS:
                    switch (msg.arg1)
                    {
                        case EstablishCommunication.UPDATE_CONNECTED:
                            connectionStatus.setText(getResources().getString(R.string.connected)+" to "+device.getName());
                            break;
                        case EstablishCommunication.UPDATE_CONNECTING:
                            connectionStatus.setText("Sending Request");
                            break;
                        case EstablishCommunication.UPDATE_RECEIVING_REQUEST:
                            connectionStatus.setText("Receiving Request");

                        case EstablishCommunication.UPDATE_DISCONNECT:
                            connectionStatus.setText(getResources().getString(R.string.disconnected));
                            break;

                    }
                    break;
                case TEXT_SEND:
                    MessageInstance textWriteInstance = (MessageInstance) msg.obj;
                    byte[] writeBuf = (byte[]) textWriteInstance.getData();
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Calendar calendar = Calendar.getInstance();
                    String txtWriteTime = sdf.format(calendar.getTime());

                    String writeDisplayMessage = "Me: " + writeMessage + "\n" + "(" + txtWriteTime + ")";

                    chatMessageAdapter.add(new MessageInstance(true, writeDisplayMessage));
                    chatMessageAdapter.notifyDataSetChanged();
                    break;

                case IMAGE_SEND:
                    Log.d(TAG, "Writing image");
                    MessageInstance imageWriteInstance = (MessageInstance) msg.obj;
                    String userMacAddress = imageWriteInstance.getMacAddress();

                    Calendar ImageCalendar = Calendar.getInstance();
                    String imageWriteTime = sdf.format(ImageCalendar.getTime());

                    imageBitmap = (Bitmap) imageWriteInstance.getData();
                    byte[] writeDecodedStringArray = compressBitmap(imageBitmap, false);

                    if (imageBitmap != null) {
                        chatMessageAdapter.add(new MessageInstance(true, imageBitmap));
                        chatMessageAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Fatal: Image bitmap is null");
                    }
                    break;


                case TEXT_RECEIVE:
                    MessageInstance msgTextData = (MessageInstance) msg.obj;
                    byte[] readBuf = (byte[]) msgTextData.getData();

                    Calendar cal = Calendar.getInstance();
                    String readTime = sdf.format(cal.getTime());

                    String message = new String(readBuf);
                    String connectedMacAddress = msgTextData.getMacAddress();



                    String displayMessage = msgTextData.getUserName() + ": " + message + "\n" + "(" + readTime + ")";

                    chatMessageAdapter.add(new MessageInstance(false, displayMessage));
                    chatMessageAdapter.notifyDataSetChanged();

                    Log.d(TAG, "Text was read from " + msgTextData.getUserName() + ": " + msgTextData.getMacAddress());
                    break;

                case IMAGE_RECEIVE:
                    MessageInstance msgImgData = (MessageInstance) msg.obj;
                    Calendar calTest = Calendar.getInstance();
                    String readImageTime = sdf.format(calTest.getTime());

                    if (msgImgData.getDataType() == DATA_IMAGE)
                    {
                        imageBitmap = (Bitmap) msgImgData.getData();
                        //  byte[] decodedStringArray = compressBitmap(imageBitmap, false);
                        if (imageBitmap != null) {
                            chatMessageAdapter.add(new MessageInstance(false, imageBitmap));
                            chatMessageAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Fatal: Image bitmap is null");
                        }
                    }
                    break;


            }

        }

    };


    /********/


    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();

                sendMessage(message);

            }
            return true;
        }
    };



    // --------> Rohit Pandey



}
