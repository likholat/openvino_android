# Face detection Java Android application using OpenVino

### Hardware Requirements 

- Linux computer
- Raspberry PI 3 model B
- Intel Neural Computer Stick 2

### To Install Android Things to Raspberry PI use this tutorial
https://developer.android.com/things/hardware/raspberrypi

### Build OpenVINO Java bindings for Android: 

1. Install OpenJDK 8 on Linux computer: ```sudo apt-get install openjdk-8-jdk```
2. Clone [openvino](https://github.com/openvinotoolkit/openvino.git) and [openvino_contrib](https://github.com/openvinotoolkit/openvino_contrib.git) repositories on your Linux computer. 
3. For ```openvino_contrib``` change ```openvino_contrib/modules/java_api/CMakeLists.txt``` file:
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
 
+    char src[] = "/data/mvcmd/\0";
+    memcpy(full_path_to_firmware, src, 13);
+
     // If there is no universal firmware available, use a special one
     if (deviceDesc.protocol == X_LINK_USB_VSC && deviceDesc.platform == X_LINK_MYRIAD_X
                                                 && !isPathExists(full_path_to_firmware)) {
```

5. Now we are ready to build OpenVINO for Android:
```
cd /path/to/openvino
mkdir build & cd build

cmake -DANDROID_ABI=armeabi-v7a \
-DANDROID_PLATFORM=21 \
-DANDROID_STL=c++_shared \
-DENABLE_OPENCV=OFF \
-DENABLE_SAMPLES=OFF \
-DIE_EXTRA_MODULES=/path/to/openvino_contrib/modules/java_api \
-DCMAKE_TOOLCHAIN_FILE=/path/to/android-ndk/build/cmake/android.toolchain.cmake ..

make --jobs=$(nproc --all)
```
