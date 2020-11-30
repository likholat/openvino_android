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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {

    private Blob MatToBlob(Mat img) {
        int[] dimsArr = {1, 3, 16, 224, 224};
        TensorDesc tDesc = new TensorDesc(Precision.U8, dimsArr, Layout.NDHWC);

        return new Blob(tDesc, img.dataAddr());
    }

    private int findResClass(float[] res) {
        float max = res[0];
        int maxId = 0;
        for (int i = 1; i < res.length; i++) {
            if(res[i] > max) {
                max = res[i];
                maxId = i;
            }
        }
        return maxId;
    }

    private String[] getClasses(String pathToJson) {
        String response;
        String[] classes = new String[100];
        File file = new File(pathToJson);
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            response = stringBuilder.toString();

            int class_num = 0;
            Matcher m = Pattern.compile("\"(.*?)\"").matcher(response);
            while(m.find()) {
                classes[class_num++] = m.group(1);
            }
        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException", Objects.requireNonNull(e.getMessage()));
            System.exit(1);
        } catch (IOException e) {
            Log.e("IOException", Objects.requireNonNull(e.getMessage()));
            System.exit(1);
        }
        return classes;
    }

    @Override
    public void onResume() {
        super.onResume();
        try{
            System.loadLibrary("opencv_java4");
            System.loadLibrary(IECore.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            Log.e("UnsatisfiedLinkError", "Failed to load native OpenVINO libraries\n" + e.toString());
            System.exit(1);
        }

        mOpenCvCameraView.enableView();

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

        classes = getClasses("/data/classes.json");
        color = new Scalar(0, 255, 0);
        images = new ArrayList<>();
        resClass = "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up camera listener.
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraPermissionGranted();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        Mat input_img = new Mat();

        // Input image preprocessing
        Imgproc.cvtColor(frame, input_img, Imgproc.COLOR_RGB2BGR);
        Imgproc.resize(input_img, input_img, new Size(224, 224));
        images.add(input_img);

        if (images.size() == 16) {
            // Input data preprocessing
            Mat blobMat = new Mat();
            Core.vconcat(images, blobMat);
            Blob imgBlob = MatToBlob(blobMat);

            inferRequest.SetBlob(inputName, imgBlob);
            inferRequest.Infer();

            // Postprocessing
            Blob outputBlob = inferRequest.GetBlob(outputName);
            float[] detection = new float[outputBlob.size()];
            outputBlob.rmap().get(detection);
            resClass = classes[findResClass(detection)];

            images.clear();
        }
        try {
            Imgproc.putText(frame, resClass, new Point(10, 40),
                            Imgproc.FONT_HERSHEY_COMPLEX, 0.8, color, 2);
        } catch (CvException e) {
            Log.e("CvException", Objects.requireNonNull(e.getMessage()));
        }
        return frame;
    }

    private CameraBridgeViewBase mOpenCvCameraView;

    private InferRequest inferRequest;
    private String inputName;
    private String outputName;
    private Scalar color;
    String resClass;

    private List<Mat> images;
    String[] classes;
}
