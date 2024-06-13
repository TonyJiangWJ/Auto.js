project(yolov8ncnn)

cmake_minimum_required(VERSION 3.10)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/OpenCV-android-sdk/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc imgcodecs)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20240410-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(yolov8ncnn SHARED yolov8ncnn.cpp yolo.cpp)

target_link_libraries(yolov8ncnn ncnn ${OpenCV_LIBS} mediandk)
