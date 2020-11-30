package com.example.aslrecognitiondemo;

import android.os.Bundle;
import android.util.Log;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import org.intel.openvino.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {

    private Blob MatToBlob(Mat img) {
        int[] dimsArr = {1, 3, 16, 224, 224};
        TensorDesc tDesc = new TensorDesc(Precision.U8, dimsArr, Layout.NDHWC);

        return new Blob(tDesc, img.dataAddr());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try{
            System.loadLibrary("opencv_java4");
            System.loadLibrary(IECore.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            Log.e("UnsatisfiedLinkError", "Failed to load native OpenVINO libraries\n" + e.toString());
            System.exit(1);
        }

        // Set up camera listener.
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraPermissionGranted();

        // Initialize model
        IECore core = new IECore("/data/plugins.xml");
        CNNNetwork net = core.ReadNetwork("/data/asl-recognition-0004.xml");
        Map<String, InputInfo> inputsInfo = net.getInputsInfo();
        inputName = new ArrayList<>(inputsInfo.keySet()).get(0);
        InputInfo inputInfo = inputsInfo.get(inputName);

        inputInfo.setLayout(Layout.NDHWC);
        inputInfo.setPrecision(Precision.U8);

        ExecutableNetwork executableNetwork = core.LoadNetwork(net, "CPU");
        inferRequest = executableNetwork.CreateInferRequest();

        Map<String, Data> outputsInfo = net.getOutputsInfo();
        outputName = new ArrayList<>(outputsInfo.keySet()).get(0);

        // Get list of ASL words
        classes = new ArrayList<>();
        try {
            InputStream is = this.getAssets().open("classes.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = reader.readLine()) != null)
                classes.add(line);

        } catch (IOException e) {
            Log.e("IOException", e.getMessage());
            System.exit(1);
        }

        images = new ArrayList<>();
        resClass = "";
    }

    @Override
    public void onResume() {
        super.onResume();
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        Mat frameBGR = new Mat();

        // Input image preprocessing
        Imgproc.cvtColor(frame, frameBGR, Imgproc.COLOR_RGBA2BGR);
        Imgproc.resize(frameBGR, frameBGR, new Size(224, 224));
        images.add(frameBGR);

        if (images.size() == 16) {
            // Input data preprocessing
            Mat blobMat = new Mat();
            Core.vconcat(images, blobMat);
            Blob imgBlob = MatToBlob(blobMat);

            inferRequest.SetBlob(inputName, imgBlob);
            inferRequest.Infer();

            // Postprocessing
            Blob outputBlob = inferRequest.GetBlob(outputName);
            float[] scores = new float[outputBlob.size()];
            outputBlob.rmap().get(scores);

            // Find a class with highest probability
            int classIdx = 0;
            for (int i = 1; i < scores.length; i++) {
                if (scores[classIdx] < scores[i]) {
                    classIdx = i;
                }
            }
            resClass = classes.get(classIdx);

            images.clear();
        }
        try {
            Imgproc.putText(frame, resClass, new Point(10, 40),
                    Imgproc.FONT_HERSHEY_COMPLEX, 0.8, new Scalar(0, 255, 0), 2);
        } catch (CvException e) {
            Log.e("CvException", Objects.requireNonNull(e.getMessage()));
        }
        return frame;
    }

    private CameraBridgeViewBase mOpenCvCameraView;

    private InferRequest inferRequest;
    private String inputName;
    private String outputName;

    private List<Mat> images;
    private List<String> classes;
    private String resClass;
}
