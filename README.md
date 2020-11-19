# Face detection Java Android application using OpenVino

### Hardware Requirements 

- Two Linux or Windows PCs
- USB flash drive
- Built-in laptop camera or usb webcam

### Build OpenVINO Java bindings for Android: 

1. Install OpenJDK 8 on Linux computer:

 ```sudo apt-get install -y openjdk-8-jdk```

2. Clone OpenVINO repositories to your computer

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

4. Download and unpack [Android NDK](https://developer.android.com/ndk/downloads). Let's assume that ~/Downloads is used as a working folder.
```
cd ~/Downloads
wget https://dl.google.com/android/repository/android-ndk-r20-linux-x86_64.zip
unzip android-ndk-r20-linux-x86_64.zip
mv android-ndk-r20 android-ndk
```

5. Now we are ready to build OpenVINO for Android:
```
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"

cd openvino
git submodule update --init --recursive

mkdir build & cd build

cmake -DANDROID_ABI=x86 \
-DANDROID_PLATFORM=21 \
-DANDROID_STL=c++_shared \
-DENABLE_OPENCV=OFF \
-DENABLE_SAMPLES=OFF \
-DIE_EXTRA_MODULES=~/Downloads/openvino_contrib/modules \
-DCMAKE_TOOLCHAIN_FILE=~/Downloads/android-ndk/build/cmake/android.toolchain.cmake ..

make --jobs=$(nproc --all)
```

### To run Android on your PC

1. Download .iso file:

   ```wget https://osdn.net/frs/redir.php?m=dotsrc&f=android-x86%2F71931%2Fandroid-x86_64-9.0-r2.iso```

2. Use [BalenaEtcher](https://www.balena.io/etcher/) to flash Android OS to USB flash drive.
3. Reboot your PC to run Android OS.

### Create Android Studio project and run it on your PC with Android OS

1. Create Android Studio project
   * Download Android Studio on your another PC: https://developer.android.com/studio
   * Start a new project
   * Choose "Empty Activity"
   ![image]()

2. Connect PC with Android OS to your PC:
   * On PC with Android OS open `Settings -> System -> About Tablet` and find it's IP address: `192.168.0.XXX`.
   * On PC with Android Studio open Command Prompt:
     ```
     cd /AppData/Local/Android/Sdk/platform-tools
     adb connect *ip_address*
     ```
     ![image]()

3. Open your Android Studio project. After `adb connect *ip_address*` command you can choose you PC as target device in Android Studio.
![image]()

4. Try to run default application
![image]()

5. Add the image output from usb webcam to the application

   * To work with camera we used [Doorbell](https://github.com/androidthings/doorbell) project sources. Just add [DoorbellCamera.java](https://github.com/androidthings/doorbell/blob/master/app/src/main/java/com/example/androidthings/doorbell/DoorbellCamera.java) file to ```app/java/com/example/myapplication``` foldel.

   * [```app/manifests/AndroidManifest.xml```](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/AndroidManifest.xml)
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

   * [```app/res/layout/activity_main.xml```](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/res/layout/activity_main.xml)
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

   * [```app/java/com/example/myapplication/MainActivity.java```]()
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

* Try to run the application 
![image]()

### Add OpenVINO to your project

* You will need the following files from OpenVINO:

```
cd ~/Downloads & mkdir openvino

cp ~/Downloads/openvino/bin/intel64/Release/lib/plugins.xml ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/inference_engine_java_api.jar ~/Downloads/openvino

cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine_java_api.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libformat_reader.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine_c_api.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine_legacy.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine_ir_reader.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine_transformations.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine_lp_transformations.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libngraph.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libMKLDNNPlugin.so ~/Downloads/openvino
cp ~/Downloads/openvino/bin/intel64/Release/lib/libinference_engine_preproc.so ~/Downloads/openvino

cp ~/Downloads/openvino/inference-engine/temp/tbb/lib/libtbb.so ~/Downloads/openvino
cp ~/Downloads/openvino/inference-engine/temp/tbb/lib/libtbbmalloc.so ~/Downloads/openvino
cp ~/Downloads/openvino/inference-engine/temp/vpu/libusb/libs/x86_64/libusb1.0.so ~/Downloads/openvino
```

* Also you will need `libc++_shared.so` file.

```cp ~/Downloads/android-ndk/sources/cxx-stl/llvm-libc++/libs/x86_64/libc++_shared.so ~/Downloads/openvino```

* Add `inference_engine_java_api.jar` dependency.
  - Switch your folder structure from Android to Project.
  ![image]()
  - Search for the `libs` folder: `MyApplication/app/libs`. Paste your `.jar` file to this foldel.
  ![image]()
  - Right click on the `inference_engine_java_api.jar` file and choose `Add as library`. This will take care of adding compile files(`libs/inference_engine_java_api.jar`) in build.gradle.

* Create `jniLibs/x86_64` directory in `MyApplication/app/src/main` folder
![image]()

* Add all `.so` files from list above to `jniLibs/x86_64` folder:
```
cp ~/Downloads/openvino/*.so /AndroidStudioProjects/MyApplication/app/src/main/jniLibs/x86_64
```

* Switch your folder structure from Project to Android.

* Download `face-detection-0200` model files (`.xml` and `.bin`) to `~/Downloads/model` foldel:
```
cd ~/Downloads & mkdir model
wget https://download.01.org/opencv/2021/openvinotoolkit/2021.1/open_model_zoo/models_bin/2/face-detection-adas-0001/FP16/face-detection-adas-0001.xml -P ~/Downloads/model
wget https://download.01.org/opencv/2021/openvinotoolkit/2021.1/open_model_zoo/models_bin/2/face-detection-adas-0001/FP16/face-detection-adas-0001.bin -P ~/Downloads/model
```

* Open Command Prompt:
     ```
     cd \AppData\Local\Android\Sdk\platform-tools
     adb connect *raspberry_pi_ip*
     adb root
     adb shell
     rpi3:/ $ chmod 777 data
     rpi3:/ $ cd data & mkdir openvino
     rpi3:/ $ cd data/openvino & mkdir model
     Ctrl + D
     ```

     Push OpenVINO files to Raspberry PI:
     ```
     adb push ~/Downloads/openvino/plugins.xml /data/openvino
     adb push ~/Downloads/model/face-detection-adas-0001.xml /data/openvino/model
     adb push ~/Downloads/model/face-detection-adas-0001 /data/openvino/model
     ```

  * Add OpenVINO:
    - Change [app/java/com/example/myapplication/MainActivity.java](https://github.com/likholat/openvino_android/blob/tutorial/app/src/main/java/com/example/myapplication/MainActivity.java) file.

* Try to run the application 
![image]()
