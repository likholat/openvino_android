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

3. For `openvino_contrib` change `openvino_contrib/modules/java_api/CMakeLists.txt` file:
```diff
--- a/modules/java_api/CMakeLists.txt
+++ b/modules/java_api/CMakeLists.txt
@@ -9,7 +9,14 @@ include(UseJava)
 
 set(JAVA_AWT_INCLUDE_PATH NotNeeded)
 
-find_package(JNI REQUIRED)
+# find_package(JNI REQUIRED)
 
 # Build native part
```

* For `/openvino_contrib/modules/java_api/cpp/cnn_network.cpp` and `/openvino_contrib/modules/java_api/cpp/infer_request.cpp` files delete all `env->PopLocalFrame(hashMapObj);` method calls.

4. Download and unpack [Android NDK](https://dl.google.com/android/repository/android-ndk-r20-linux-x86_64.zip).

5. Now we are ready to build OpenVINO for Android:
```
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"

cd openvino
git submodule update --init --recursive

mkdir build & cd build

cmake -DANDROID_ABI=x86 \
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
```

<!-- ### To run Android on your PC

1. Download .iso file:

   ```wget https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F71931%2Fandroid-x86_64-9.0-r2.iso```

2. Use [BalenaEtcher](https://www.balena.io/etcher/) to flash Android OS to USB flash drive.
3. Reboot your PC to run Android OS. -->

### Create Android Studio project and run it on your PC with Android OS

This application is an example of using OpenVINO Inference Engine on the Android OS. We used the core component of OpenVINO - Inference Engine, which manages the loading and compiling of the optimized neural network model, runs inference operations on input data, and outputs the results

The application reads an image from the camera, uses a neural network to detects faces and displays an image with the detection result (the face in the image is outlined in a rectangle).

* You will need the following files from OpenVINO:

```
cd ~/Downloads & mkdir openvino

cp ~/Downloads/openvino/bin/intel64/Release/lib/plugins.xml ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/inference_engine_java_api.jar ~/Downloads/openvino

cd ~/Downloads/openvino/bin/intel64/Release/lib
~/Downloads/android-ndk/toolchains/llvm/prebuilt/linux-x86_64/x86_64-linux-android/bin/strip *.so

cp ~/Downloads/openvino/bin/intel64/Release/lib/*.so ~/Downloads/openvino

cp ~/Downloads/openvino/inference-engine/temp/tbb/lib/libtbb.so ~/Downloads/openvino
cp ~/Downloads/openvino/inference-engine/temp/tbb/lib/libtbbmalloc.so ~/Downloads/openvino
cp ~/Downloads/openvino/inference-engine/temp/vpu/libusb/libs/x86_64/libusb1.0.so ~/Downloads/openvino
```

* Also you will need `libc++_shared.so` file.

`cp ~/Downloads/android-ndk/sources/cxx-stl/llvm-libc++/libs/x86_64/libc++_shared.so ~/Downloads/openvino`

1. Create Android Studio project
   * Download Android Studio on your another PC: https://developer.android.com/studio
   * Start a new project
   * Choose "Empty Activity"
   ![image]()

2. Add `inference_engine_java_api.jar` dependency.
  - Switch your folder structure from Android to Project.
  ![image]()
  - Search for the `libs` folder: `MyApplication/app/libs`. Paste your `.jar` file to this foldel.
  ![image]()
  - Right click on the `inference_engine_java_api.jar` file and choose `Add as library`. This will take care of adding compile files(`libs/inference_engine_java_api.jar`) in build.gradle.

3. Create `jniLibs/x86_64` directory in `MyApplication/app/src/main` folder
![image]()

4. Add all `.so` files from list above to `jniLibs/x86_64` folder:
```
cp ~/Downloads/openvino/*.so /AndroidStudioProjects/MyApplication/app/src/main/jniLibs/x86_64
```

5. Switch your folder structure from Project to Android.

6. Download model files: [face-detection-adas-0001.bin](https://download.01.org/opencv/2021/openvinotoolkit/2021.1/open_model_zoo/models_bin/2/face-detection-adas-0001/FP16/face-detection-adas-0001.bin) and [face-detection-adas-0001.xml](https://download.01.org/opencv/2021/openvinotoolkit/2021.1/open_model_zoo/models_bin/2/face-detection-adas-0001/FP16/face-detection-adas-0001.xml).

7. Use [Android Debug Bridge (adb)](https://developer.android.com/studio/command-line/adb) to connect PC with Android OS to your PC:
    ```
    adb root

    adb push ~/Downloads/openvino/plugins.xml /data/openvino
    adb push ~/Downloads/model/face-detection-adas-0001.xml /data
    adb push ~/Downloads/model/face-detection-adas-0001 /data
    ```

8. To add face detection from the camera to the application, change the following files:
    - To work with camera we used [Doorbell](https://github.com/androidthings/doorbell) project sources. Just add [DoorbellCamera.java](https://github.com/androidthings/doorbell/blob/master/app/src/main/java/com/example/androidthings/doorbell/DoorbellCamera.java) file to ```app/java/com/example/myapplication``` foldel.

    - [```app/manifests/AndroidManifest.xml```](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/AndroidManifest.xml)

    - [```app/res/layout/activity_main.xml```](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/res/layout/activity_main.xml)
    - [app/java/com/example/myapplication/MainActivity.java](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/java/com/example/myapplication/MainActivity.java) file.

9. Try to run the application 
![image]()
