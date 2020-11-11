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
3. For ```openvino_contrib``` change ```openvino_contrib/modules/java_api/CMakeLists.txt``` file:   - CHECK THIS STEP
```diff
--- a/modules/java_api/CMakeLists.txt
+++ b/modules/java_api/CMakeLists.txt

 find_package(Java REQUIRED)
 include(UseJava)

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

 file(GLOB_RECURSE sources ${CMAKE_CURRENT_SOURCE_DIR}/cpp/*.cpp)
```
4. For ```openvino``` change ```openvino/inference-engine/thirdparty/movidius/mvnc/src/mvnc_api.c``` file:
```diff
--- a/openvino/inference-engine/thirdparty/movidius/mvnc/src/mvnc_api.c
+++ b/openvino/inference-engine/thirdparty/movidius/mvnc/src/mvnc_api.c

    rc = snprintf(full_path_to_firmware, MAX_PATH_LENGTH,
             "%s%s-%s%s", firmware_dir, fw_protocol_prefix, fw_device_name, fw_format);
    if (rc < 0)
        return NC_ERROR;

}

+char src[] = "/data/mvcmd/\0";
+memcpy(full_path_to_firmware, src, 13);

if (!isPathExists(full_path_to_firmware)) {
    mvLog(MVLOG_ERROR, "Firmware not found in: %s", full_path_to_firmware);
    FILE *fptr1;
```

5. Now we are ready to build OpenVINO for Android:
```
cd /path/to/openvino
mkdir build & cd build

cmake \
-DCMAKE_TOOLCHAIN_FILE=/path/to/Downloads/android-ndk/build/cmake/android.toolchain.cmake \
-DANDROID_ABI=armeabi-v7a \
-DANDROID_PLATFORM=21 \
-DANDROID_STL=c++_shared \
-DENABLE_OPENCV=OFF ..

make --jobs=$(nproc --all)
```
