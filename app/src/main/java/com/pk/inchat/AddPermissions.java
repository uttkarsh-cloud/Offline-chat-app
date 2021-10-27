package com.pk.inchat;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AddPermissions extends AppCompatActivity {
    Button enable_btn,connect_btn;
    TextView notification;
    BluetoothAdapter bluetoothAdapter;
    int REQUEST_ENABLE_BLUETOOTH=10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_permissions);
        getSupportActionBar().hide();
        enable_btn=(Button)findViewById(R.id.enable);
        connect_btn=(Button)findViewById(R.id.connect);
        notification=findViewById(R.id.Notified);
        notification.setText("\tSteps to Connect \n\n 1.Start Discovery of an App. \n\n 2.Select Device from Device list. \n\n 3.Start Communication by selecting the device.");
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null)
        {
            Toast.makeText(getApplicationContext(),"No Bluetooth Found",Toast.LENGTH_LONG).show();
            finish();
        }
        else if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(),"Press Enable Button",Toast.LENGTH_LONG).show();}
        else{
            enable_btn.setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(),"Press Start Button",Toast.LENGTH_LONG).show();
        }

        enable_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!bluetoothAdapter.isEnabled())
                {

                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);

                }



            }

        });



        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(),"Please Enable Bluetooth",Toast.LENGTH_LONG).show();
                }
                else{
                    Intent intent = new Intent(AddPermissions.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        if(requestCode==REQUEST_ENABLE_BLUETOOTH)
        {
            enable_btn.setVisibility(View.INVISIBLE);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}