package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import org.intel.openvino.*;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Mat ImageToMat(Image image){
        ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
        Mat buf = new Mat(1, imageBuf.remaining(), CvType.CV_8UC1, imageBuf);

        return Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_COLOR);
    }

    private Blob MatToBlob(Mat img) {
        int[] dimsArr = {1, img.channels(), img.height(), img.width()};
        TensorDesc tDesc = new TensorDesc(Precision.U8, dimsArr, Layout.NHWC);

        return new Blob(tDesc, img.dataAddr());
    }

    @Override
    public void onResume() {
        super.onResume();

        try{
            System.loadLibrary("opencv_java4");
        } catch (UnsatisfiedLinkError e) {
            Log.e("MainActivity", "Failed to load OpenCV library\n" + e.toString());
            System.exit(1);
        }

        try {
            System.loadLibrary(IECore.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            Log.e("MainActivity", "Failed to load Inference Engine library\n" + e.toString());
            System.exit(1);
        }

        core = new IECore("/data/plugins.xml");
        net = core.ReadNetwork("/data/face-detection-adas-0001.xml");

        Map<String, InputInfo> inputsInfo = net.getInputsInfo();
        inputName = new ArrayList<String>(inputsInfo.keySet()).get(0);
        inputInfo = inputsInfo.get(inputName);

        inputInfo.getPreProcess().setResizeAlgorithm(ResizeAlgorithm.RESIZE_BILINEAR);
        inputInfo.setLayout(Layout.NHWC);
        inputInfo.setPrecision(Precision.U8);

        executableNetwork = core.LoadNetwork(net, "CPU");
        inferRequest = executableNetwork.CreateInferRequest();

        outputName = new ArrayList<String>(net.getOutputsInfo().keySet()).get(0);

        color = new Scalar(0, 255, 0);
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
                    Log.e("MYTAG", "IMAGE");
                    mCamera.takePicture();
                    image = reader.acquireNextImage();

                    Mat img = ImageToMat(image);
                    image.close();

                    Blob imgBlob = MatToBlob(img);

                    inferRequest.SetBlob(inputName, imgBlob);
                    inferRequest.Infer();

                    Blob output = inferRequest.GetBlob(outputName);

                    float detection[] = new float[output.size()];
                    output.rmap().get(detection);

                    int maxProposalCount = detection.length / 7;

                    for (int curProposal = 0; curProposal < maxProposalCount; curProposal++) {
                        int image_id = (int) detection[curProposal * 7];
                        if (image_id < 0) break;

                        float confidence = detection[curProposal * 7 + 2];

                        // Drawing only objects with >70% probability
                        if (confidence < 0.7) continue;

                        int xmin = (int) (detection[curProposal * 7 + 3] * img.cols());
                        int ymin = (int) (detection[curProposal * 7 + 4] * img.rows());
                        int xmax = (int) (detection[curProposal * 7 + 5] * img.cols());
                        int ymax = (int) (detection[curProposal * 7 + 6] * img.rows());

                        // Draw rectangle around detected object.
                        Point lt = new Point(xmin, ymin);
                        Point br = new Point(xmax, ymax);
                        Imgproc.rectangle(img, lt, br, color, 1);
                    }

                    Bitmap bmp = null;
                    try {
                        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
                        bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(img, bmp);
                    }
                    catch (CvException e){
                        Log.e("CvException", e.getMessage());
                    }

                    Message msg = new Message();
                    msg.obj = bmp;
                    handler.sendMessage(msg);
                }
            };

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private DoorbellCamera mCamera;

    private ImageView mImage;
    private Handler handler;

    private IECore core;
    private CNNNetwork net;
    private ExecutableNetwork executableNetwork;
    private InferRequest inferRequest;
    private InputInfo inputInfo;
    private String inputName;
    private String outputName;
    private Scalar color;
    private Image image;
}
