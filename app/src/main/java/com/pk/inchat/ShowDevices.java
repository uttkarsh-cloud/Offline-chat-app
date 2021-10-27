package com.pk.inchat;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class ShowDevices extends AppCompatActivity {
    private BluetoothAdapter BtAdapter;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private ArrayAdapter<String> mNewDeviceArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_devices);
        BtAdapter = BluetoothAdapter.getDefaultAdapter();
        discoverDevices();
        mNewDeviceArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        ListView newDeviceListView = (ListView) findViewById(R.id.new_devices);
        newDeviceListView.setAdapter(mNewDeviceArrayAdapter);
        newDeviceListView.setOnItemClickListener(mDeviceClickListener);

        IntentFilter AvailableFound= new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, AvailableFound);

        Set<BluetoothDevice> pairedDevices = BtAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mNewDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

    }
    private void discoverDevices() {
        setTitle("Scanning for devices");

        // If we are already discovering bluetooth devices stop it
        if (BtAdapter.isDiscovering()) {
            BtAdapter.cancelDiscovery();
        }
        // Request device discovery from bluetooth adapter
        BtAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setTitle(R.string.select_device);
                if (mNewDeviceArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDeviceArrayAdapter.add(noDevices);
                }
            }
        }
    };


    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // Cancel the discovery before we reconnect
            BtAdapter.cancelDiscovery();

            // Get the MAC address of the device
            String info = ((TextView)view).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create result intent to include the mac address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS,address);

            // Set the result and finish this activity
            setResult(Activity.RESULT_OK,intent);
            finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (BtAdapter != null) {
            BtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }
}