package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import org.intel.openvino.CNNNetwork;
import org.intel.openvino.IECore;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private DoorbellCamera mCamera;

    private ImageView mImage;
    private Handler handler;

    @Override
    public void onResume() {
        super.onResume();
        try {
            System.loadLibrary(IECore.NATIVE_LIBRARY_NAME);
            Log.d("MainActivity", "Inference Engine library was loaded!");
        } catch (UnsatisfiedLinkError e) {
            Log.e("MainActivity", "Failed to load Inference Engine library\n" + e.toString());
            System.exit(1);
        }

        IECore core = new IECore("/data/openvino/plugins.xml");
        Log.d("MainActivity", "IECore object was created");

        CNNNetwork net = core.ReadNetwork("/data/openvino/model/face-detection-0200.xml");
        Log.d("MainActivity", "CNNNetwork object was created");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler() {
            @Override
            public void handleMessage(Message bitmap) {
                Bitmap src = (Bitmap) bitmap.obj;
                mImage = findViewById(R.id.imageView);
                mImage.setImageBitmap(src);
            }
        };

        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mCamera.takePicture();
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mCamera.takePicture();
                    Image image = reader.acquireNextImage();

                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[imageBuf.capacity()];
                    imageBuf.get(bytes);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                    Message msg = new Message();
                    msg.obj = bitmap;
                    handler.sendMessage(msg);

                    image.close();
                }
            };
}
