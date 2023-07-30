package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;

    private String DEVICE_ADDRESS = "";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private ProgressDialog progress;
    private ListView lvDevice;
    // views from activity
    TextView mTextTv;
    ImageButton mVoiceBtn;

    private ImageView imgStatus;

    private Button btnConnect;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothSocket socket;

    private ArrayAdapter arrayAdapter;
    private ArrayList list;
    String command;

    private OutputStream outputStream;
    boolean found = false;

    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // button click to show speech to text dialog
        mVoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speak();
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
                } else {
                    scanDevice();
                }
            }
        });

        progress = new ProgressDialog(MainActivity.this);
        progress.setMessage("Connecting...");
        progress.setIndeterminate(true);
        progress.setCancelable(false);

        lvDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                progress.show();
                DEVICE_ADDRESS = list.get(position).toString().substring(0, 17);
                scanDevice();
                //Toast.makeText(getApplicationContext(), DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                if(found) {
                    BTconnect();
                }

            }
        });
    }

    private void speak() {
        // intent to show speech to text dialog
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi, speak something");

        // start intent
        try {
            // in there was no error
            // show dialog
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            // if there was some error
            // get message of error and show
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // receive voice input and handle it
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    // get text array from voice intent
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    // set to text view
                    mTextTv.setText(result.get(0));
                    command = result.get(0);
                    try
                    {
                        outputStream.write(command.getBytes());
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    }

    private void init() {
        mTextTv = findViewById(R.id.textTv);
        mVoiceBtn = findViewById(R.id.voiceBtn);
        btnConnect = findViewById(R.id.btnConnect);
        lvDevice = findViewById(R.id.lvDevice);
        imgStatus = findViewById(R.id.imgStatus);
    }

    private void scanDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        list = new ArrayList();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice iterator : pairedDevices) {
                String deviceName = iterator.getName();
                String deviceHardwareAddress = iterator.getAddress(); // MAC address
                list.add(deviceHardwareAddress + "\n" + deviceName);
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device = iterator;
                    found = true;
                    break;
                }

            }
            arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
            lvDevice.setAdapter(arrayAdapter);
        } else {
            Toast.makeText(getApplicationContext(), "Hãy ghép đôi thiết bị với nhau", Toast.LENGTH_SHORT).show();
        }

    }

    public void BTconnect()
    {
        boolean connected = true;

        try
        {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID); //Creates a socket to handle the outgoing connection
            socket.connect();
            if(socket.isConnected()) {

                Toast.makeText(getApplicationContext(),
                        "Kết nối thành công", Toast.LENGTH_LONG).show();
                imgStatus.setImageResource(R.drawable.green_circle);
                progress.dismiss();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Chưa kết nối được", Toast.LENGTH_LONG).show();
                imgStatus.setImageResource(R.drawable.red_circle);
                progress.dismiss();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            connected = false;
            Toast.makeText(getApplicationContext(),
                    "Kết nối thất bại", Toast.LENGTH_LONG).show();
            imgStatus.setImageResource(R.drawable.red_circle);
            progress.dismiss();
        }

        if(connected)
        {
            try
            {
                outputStream = socket.getOutputStream(); //gets the output stream of the socket
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

    }

}