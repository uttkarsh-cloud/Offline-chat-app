package com.pk.inchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class WifiHome extends AppCompatActivity {
    EditText e1,e2;
    Button b1;
    ListView l1;
    TextView t1;
    ArrayList<String> list=new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    //192.168.1.33
    //192.168.1.34
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_home);
        getSupportActionBar().hide();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        e1=findViewById(R.id.ip_et_wifihome);
        e2=findViewById(R.id.messege_et_wifihome);
        b1=findViewById(R.id.btn_send_wifi_home);
        l1=findViewById(R.id.lv_wifichat);
        t1=findViewById(R.id.wifi_ip_tv);
        getLocalIpAddress();
        arrayAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,list);
        l1.setAdapter(arrayAdapter);
        Thread thread=new Thread(new MyServer());
        thread.start();



        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(! e1.getText().toString().trim().equals("") && ! e2.getText().toString().trim().equals("")) {
                    BGTask bgTask = new BGTask();
                    String message = e2.getText().toString().trim();
                    String ip = e1.getText().toString().trim();
                    String messToSend = ip + "splitmymess" + message;
                    bgTask.execute(ip, messToSend);
                    list.add("Me:\n" + message);
                    arrayAdapter.notifyDataSetChanged();
                    e2.setText("");
                }
                else
                {
                    Toast.makeText(WifiHome.this, "Please fill the fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
        t1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", t1.getText().toString().substring(9));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(WifiHome.this, "IP copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }



    class BGTask extends AsyncTask<String,Void,String>
    {
        Socket socket;
        DataOutputStream dataOutputStream;
        String ip,message;

        @Override
        protected String doInBackground(String... strings) {
            ip=strings[0];
            message=strings[1];
            try {
                socket=new Socket(ip,9700);
                dataOutputStream=new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(message);
                dataOutputStream.close();
                socket.close();

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }
    }

    class MyServer implements Runnable{
        ServerSocket serverSocket;
        Socket mysocket;
        DataInputStream dataInputStream;
        String mess;
        Handler handler=new Handler();

        @Override
        public void run() {

            try {
                serverSocket=new ServerSocket(9700);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Waiting for client",Toast.LENGTH_SHORT).show();
                    }
                });
                while (true)
                {
                    mysocket=serverSocket.accept();
                    dataInputStream=new DataInputStream(mysocket.getInputStream());
                    mess=dataInputStream.readUTF();




                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"message recieved from client: "+mess,Toast.LENGTH_LONG).show();
                            String messArr[]=mess.split("splitmymess");
                            String fromIp,fromMess;
                            if(messArr.length>0) {
                                 fromIp = messArr[0];
                                 fromMess = messArr[1];
                                list.add(fromIp+" ->\n "+fromMess);
                                arrayAdapter.notifyDataSetChanged();
                            }
                            else
                            {
                                 fromIp="";
                                 fromMess="";
                            }

                            if(mess.equals("Open Camera"))
                            {
                                Toast.makeText(getApplicationContext(),"Opening Camera on other device",Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }
     void getLocalIpAddress() {
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        System.out.println(ip);
        t1.setText("Your IP : "+ip);
    }
}