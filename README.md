# Face detection Java Android application using OpenVino

### Hardware Requirements 

- Two Linux or Windows PCs
- USB flash drive
- Built-in laptop camera or usb webcam

### Build OpenVINO Java bindings for Android: 

These steps were done on Ubuntu 18.04, but in the general case, they can be done on Windows OS, without fundamental differences.

1. Install OpenJDK 8 on Linux computer:

 `sudo apt-get install -y openjdk-8-jdk`

2. Clone OpenVINO repositories to your computer. Let's assume that ~/Downloads is used as a working folder.

```
cd ~/Downloads
git clone https://github.com/openvinotoolkit/openvino.git
git clone https://github.com/openvinotoolkit/openvino_contrib.git
```

3. Download and unpack [Android NDK](https://dl.google.com/android/repository/android-ndk-r20-linux-x86_64.zip) to `~/Downloads` folder.

4. Now we are ready to build OpenVINO for Android:
```
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"

cd openvino
git submodule update --init --recursive

mkdir build & cd build

cmake -DANDROID_ABI=x86_64 \
-DANDROID_PLATFORM=21 \
-DANDROID_STL=c++_shared \
-DENABLE_VPU=OFF \
-DENABLE_GNA=OFF \
-DENABLE_CLDNN=OFF \
-DENABLE_OPENCV=OFF \
-DENABLE_SAMPLES=OFF \
-DIE_EXTRA_MODULES=~/Downloads/openvino_contrib/modules \
-DCMAKE_TOOLCHAIN_FILE=~/Downloads/android-ndk-r20/build/cmake/android.toolchain.cmake ..

make --jobs=$(nproc --all)

cd ../bin/intel64/Release/lib
~/Downloads/android-ndk-r20/toolchains/llvm/prebuilt/linux-x86_64/x86_64-linux-android/bin/strip *.so
```

<!-- ### To run Android on your PC

   ```wget https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F71931%2Fandroid-x86_64-9.0-r2.iso```
-->

### Create Android Studio project 

This application is an example of using OpenVINO Inference Engine on the Android OS. We used the core component of OpenVINO - Inference Engine, which manages the loading and compiling of the optimized neural network model, runs inference operations on input data, and outputs the results

The application reads an image from the camera, uses a neural network to detects faces and displays an image with the detection result (the face in the image is outlined in a rectangle).

1. Download Android Studio on your PC: https://developer.android.com/studio

2. Start a new project, choose "Empty Activity"
   ![image]()

3. To add face detection from the camera to the application, change the following files:
    - To work with camera we used [Doorbell](https://github.com/androidthings/doorbell) project sources. Add [app/java/com/example/myapplication/DoorbellCamera.java](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/java/com/example/myapplication/DoorbellCamera.java) file.

    - [```app/manifests/AndroidManifest.xml```](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/AndroidManifest.xml)

    - [```app/res/layout/activity_main.xml```](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/res/layout/activity_main.xml)
    - [app/java/com/example/myapplication/MainActivity.java](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/java/com/example/myapplication/MainActivity.java) file.

4. Add InferenceEngine dependency:

    * You need the following files from OpenVINO:

    ```
    cd ~/Downloads && mkdir openvino

    cp ~/Downloads/openvino/bin/intel64/Release/lib/plugins.xml ~/Downloads/openvino
    cp ~/Downloads/openvino/bin/intel64/Release/lib/inference_engine_java_api.jar ~/Downloads/openvino

    cp ~/Downloads/openvino/bin/intel64/Release/lib/*.so ~/Downloads/openvino

    cp ~/Downloads/openvino/inference-engine/temp/tbb/lib/libtbb.so ~/Downloads/openvino
    cp ~/Downloads/openvino/inference-engine/temp/tbb/lib/libtbbmalloc.so ~/Downloads/openvino
    cp ~/Downloads/openvino/inference-engine/temp/vpu/libusb/libs/x86_64/libusb1.0.so ~/Downloads/openvino
    ```

    * Also you will need `libc++_shared.so` file:

    `cp ~/Downloads/android-ndk/sources/cxx-stl/llvm-libc++/libs/x86_64/libc++_shared.so ~/Downloads/openvino`

    * Create `jniLibs/x86_64` directory in `app/src/main` folder
    ![image]()

    * Add all `.so` files from list above to `jniLibs/x86_64` folder:
    ```
    cp ~/Downloads/openvino/*.so /AndroidStudioProjects/MyApplication/app/src/main/jniLibs/x86_64
    ```

    * Add `inference_engine_java_api.jar` dependency.
        - Switch your folder structure from Android to Project.
        ![image]()
        - Search for the `libs` folder: `app/libs`. Paste your `.jar` file to this foldel.
        ![image]()
        - Right click on the `inference_engine_java_api.jar` file and choose `Add as library`. This will take care of adding compile files(`libs/inference_engine_java_api.jar`) in build.gradle.
        - Switch your folder structure from Project to Android.

5. Add OpenCV dependency:
    * Download [OpenCV SDK for Android](https://github.com/opencv/opencv/releases/download/4.5.0/opencv-4.5.0-android-sdk.zip) to `~/Downloads` folder.

    * `File -> New -> ImportModule`

    Specify a path to unpacked SDK: ~/Downloads/opencv-4.5.0-android-sdk/OpenCV-android-sdk/sdk
    ![image]()

    * Add module dependency: `File -> Project Structure`
    ![image]()
    ![image]()

    * Replace "minSdkVersion 21" to "minSdkVersion 15" in `Gradle Scripts -> build.gradle (Module: sdk)`
    ![image]()

6. Download model files: [face-detection-adas-0001.bin](https://download.01.org/opencv/2021/openvinotoolkit/2021.1/open_model_zoo/models_bin/2/face-detection-adas-0001/FP16/face-detection-adas-0001.bin) and [face-detection-adas-0001.xml](https://download.01.org/opencv/2021/openvinotoolkit/2021.1/open_model_zoo/models_bin/2/face-detection-adas-0001/FP16/face-detection-adas-0001.xml).

7. Use [Android Debug Bridge (adb)](https://developer.android.com/studio/command-line/adb) to transfer data files on Android:
    ```
    adb push ~/Downloads/openvino/plugins.xml /data
    adb push face-detection-adas-0001.xml face-detection-adas-0001.bin /data
    ```

8. Try to run the application 
![image]()
