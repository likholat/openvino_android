# Face detection Java Android application using OpenVino

### Hardware Requirements 

- Linux and Windows computers
- Raspberry PI 3 model B
- Intel Neural Computer Stick 2
- Webcam

### To Install Android Things to Raspberry PI use this tutorial
https://developer.android.com/things/hardware/raspberrypi

### Build OpenVINO Java bindings for Android: 

1. Install OpenJDK 8 on Linux computer: ```sudo apt-get install -y openjdk-8-jdk```
2.Clone OpenVINO repositories to your computer
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
+
+set(JAVA_JVM_LIBRARY /usr/lib/jvm/default-java/lib/server/libjvm.so)
+set(JAVA_AWT_LIBRARY /usr/lib/jvm/default-java/lib/libjawt.so)
+set(JAVA_INCLUDE_PATH /usr/lib/jvm/default-java/include/jni.h)
+set(JAVA_INCLUDE_PATH2 /usr/lib/jvm/default-java/include/linux)
+set(JNI_INCLUDE_DIRS /usr/lib/jvm/default-java/include;/usr/lib/jvm/default-java/include/linux;NotNeeded)
+set(JNI_LIBRARIES /usr/lib/jvm/default-java/lib/libjawt.so;/usr/lib/jvm/default-java/lib/server/libjvm.so)
 
 # Build native part
```

4. For ```openvino``` change ```openvino/inference-engine/thirdparty/movidius/mvnc/src/mvnc_api.c``` file:
```diff
--- a/inference-engine/thirdparty/movidius/mvnc/src/mvnc_api.c
+++ b/inference-engine/thirdparty/movidius/mvnc/src/mvnc_api.c
@@ -617,6 +617,9 @@ ncStatus_t getFirmwarePath(char *firmware_file_path, const int firmware_file_len
         return NC_ERROR;
     }
 
+    char src[] = "/data/openvino/mvcmd/\0";
+    memcpy(full_path_to_firmware, src, 23);
+
     // If there is no universal firmware available, use a special one
     if (deviceDesc.protocol == X_LINK_USB_VSC && deviceDesc.platform == X_LINK_MYRIAD_X
                                                 && !isPathExists(full_path_to_firmware)) {
```

5. Download and unpack [Android NDK](https://developer.android.com/ndk/downloads). Let's assume that ~/Downloads is used as a working folder.
```
cd ~/Downloads
wget https://dl.google.com/android/repository/android-ndk-r20-linux-x86_64.zip

unzip android-ndk-r20-linux-x86_64.zip
mv android-ndk-r20 android-ndk
```

6. Now we are ready to build OpenVINO for Android:
```
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"

cd openvino
git submodule update --init --recursive

mkdir build & cd build

cmake -DANDROID_ABI=armeabi-v7a \
-DANDROID_PLATFORM=21 \
-DANDROID_STL=c++_shared \
-DENABLE_OPENCV=OFF \
-DENABLE_SAMPLES=OFF \
-DIE_EXTRA_MODULES=~/Downloads/openvino_contrib/modules \
-DCMAKE_TOOLCHAIN_FILE=~/Downloads/android-ndk/build/cmake/android.toolchain.cmake ..

make --jobs=$(nproc --all)
```
7. Install Android Things on Raspberry PI

   To install Android Things on Raspberry PI device use this tutorial: https://developer.android.com/things/hardware/raspberrypi

8. Create Android Studio project and run it on Raspberry PI

Create Android Studio project
* Download Android Studio on your Windows computer: https://developer.android.com/studio
* Start a new project
* Choose "Empty Activity"
![image]()

Connect Raspberry PI device to Windows computer:
* Connect Raspberry PI device to monitor and Windows computer via USB, wait until it turns on.
* Connect your Raspberry PI to the same Wi-Fi network as your Windows computer. Find Raspberry PI IP-address: in the Networks tab under the SSID.
* Open Command Prompt:
  - `cd \AppData\Local\Android\Sdk\platform-tools`
  - `adb connect *raspberry_pi_ip*`
  ![image]()
* In future, you don't have to connect the Raspberry PI device to the Windows computer via USB, just use the connection via Wi-Fi (`adb connect *raspberry_pi_ip*` command).

* Open your Android Studio project. After `adb connect *raspberry_pi_ip*` command you can choose you Rasbperry PI as target device in Android Studio.
![image]()

* Try to run default application
![image]()

9. Add the image output from usb webcam to the application

* Connect webcam to Raspberry PI via USB

* To work with camera we used [Doorbell](https://github.com/androidthings/doorbell) project sources. Just add [DoorbellCamera.java](https://github.com/androidthings/doorbell/blob/master/app/src/main/java/com/example/androidthings/doorbell/DoorbellCamera.java) file to ```app/java/com/example/myapplication``` foldel.

* ```app/manifests/AndroidManifest.xml```
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.myapplication">

    <!--Allow to use a camera-->
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-feature android:name="android.hardware.camera" android:required="false"/>
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false"/>
    <uses-feature android:name="android.hardware.camera.front" android:required="false"/>
    <uses-feature android:name="android.hardware.camera.front.autofocus" android:required="false"/>

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.NoActionBar">  <!--Full screen mode-->

        <activity android:name=".MainActivity"
            android:screenOrientation="fullSensor">  <!--Screen orientation-->
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>

                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>

</manifest>
```

* ```app/res/layout/activity_main.xml```
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <ImageView
        android:id="@+id/imageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_alignParentTop="true" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

* ```app/java/com/example/myapplication/MainActivity.java```
```java
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
import android.widget.ImageView;

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
```

* Try to run application 
![image]()

10. Add OpenVINO to your project

* You will need the following files (copy all this files to your Windows computer):
  * Files from sources
    - `/openvino/inference-engine/temp/vpu/libusb/libs/armeabi-v7a/libusb1.0.so`
    - `/openvino/inference-engine/thirdparty/movidius/mvnc/src/97-myriad-usbboot.rules`
  * Files from `openvino/bin/armv7-a/Release/lib`
    - ```plugins.xml```
    - `inference_engine_java_api.jar`
    - `libinference_engine_java_api.so`, `libformat_reader.so`, `libinference_engine_c_api.so`, `libinference_engine_legacy.so`, `libmyriadPlugin.so`,  `libinference_engine.so`, `libinference_engine_ir_reader.so`, `libinference_engine_transformations.so`, `libngraph.so`
    - `pcie-ma248x.mvcmd`, `usb-ma2x8x.mvcmd` (copy to new `mvcmd` folder)
  * `/openvino/inference-engine/temp/vpu/libusb/libs/armeabi-v7a/libusb1.0.so`
  * Also you will need ```~/Downloads/android-ndk/sources/cxx-stl/llvm-libc++/libs/armeabi-v7a/libc++_shared.so``` file.

* Add `inference_engine_java_api.jar` dependency.
  - Switch your folder structure from Android to Project.
  ![image]()
  - Search for the `libs` folder: `MyApplication\app\libs`. Paste your `.jar` file to this foldel.
  ![image]()
  - Right click on the `inference_engine_java_api.jar` file and choose `Add as library`. This will take care of adding compile files(`libs/inference_engine_java_api.jar`) in build.gradle.

* Create `jniLibs/armeabi-v7a` directory in `/app/src/main` folder
![image]()

* Add all `.so` files from list above to `jniLibs/armeabi-v7a` folder.

* Switch your folder structure from Project to Android.

* Download `face-detection-0200` model files (`.xml` and `.bin`) to `~\Downloads\model` foldel:
https://download.01.org/opencv/2021/openvinotoolkit/2021.1/open_model_zoo/models_bin/2/face-detection-0200/FP16/

* Open Command Prompt:
     ```
     cd \AppData\Local\Android\Sdk\platform-tools
     adb connect *raspberry_pi_ip*
     adb root & adb remount
     adb shell
     rpi3:/ $ chmod 777 data
     rpi3:/ $ cd data & mkdir openvino
     rpi3:/ $ cd data/openvino & mkdir model
     Ctrl + D
     ```

     Push OpenVINO files to Raspberry PI:
     ```
     adb push path\to\97-myriad-usbboot.rules /etc/udev/rules.d`
     adb push path\to\plugins.xml /data/openvino`
     adb push path\to\model /data/openvino`
     adb push path\to\mvcmd /data/openvino`
     ```

  * Add OpenVINO:
    - Open [app/java/com/example/myapplication/MainActivity.java](https://github.com/likholat/openvino_android/blob/tutorial/app/java/com/example/myapplication/MainActivity.java) аnd change `onResume()` method
     ```java
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
                    Log.d("MainActivity", "IMPORTED IE");
               } catch (UnsatisfiedLinkError e) {
                    System.err.println("Failed to load Inference Engine library\n" + e);
                    System.exit(1);
               }

               IECore core = new IECore("/data/openvino/plugins.xml");
               Log.d("MainActivity", "IECore object was created");

               CNNNetwork net = core.ReadNetwork("/data/openvino/model/face-detection-0200.xml");
               Log.d("MainActivity", "CNNNetwork object was created");
          }

          ...
     }
     ```
* Try to run 
![image]()
