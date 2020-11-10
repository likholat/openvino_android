package com.example.test_openvino;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import 	android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;

import org.intel.openvino.CNNNetwork;
import org.intel.openvino.ExecutableNetwork;
import org.intel.openvino.IECore;

import java.util.HashMap;
import java.util.Iterator;

import java.lang.Integer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

//        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
//        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
//        while(deviceIterator.hasNext()){
//            UsbDevice device = deviceIterator.next();
//            Log.d("MainActivity", device.toString());
////            Log.i("MainActivity",  Integer.valueOf(device.getDeviceProtocol()).toString());
//        }

        try {
            System.loadLibrary(IECore.NATIVE_LIBRARY_NAME);
            Log.d("MainActivity", "IMPORTED IE");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load Inference Engine library\n" + e);
            System.exit(1);
        }

        IECore core = new IECore("/data/plugins.xml");
        Log.d("MainActivity", "CORE CREATED");

        CNNNetwork net = core.ReadNetwork("/data/face-detection-0200.xml");
        Log.d("MainActivity", "ReadNetwork DONE");

        try {
            ExecutableNetwork executableNetwork = core.LoadNetwork(net, "MYRIAD");
            Log.d("MainActivity", "LoadNetwork DONE");
        }catch (Exception e) {
            Log.e("MainActivity", e.getMessage());
        }


    }
}