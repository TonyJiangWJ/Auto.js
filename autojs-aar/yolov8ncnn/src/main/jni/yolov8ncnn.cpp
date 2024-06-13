//
// Created by tonyjiang on 2024/6/4.
//

#include "yolov8ncnn.h"

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yolo.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#define TAG "yolov8ncnn"
#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static Yolo *m_yolo = nullptr;
static ncnn::Mutex lock;

jobject createResult(Object object, JNIEnv *env) {
    cv::Rect_<float> rect = object.rect;
    int label = object.label;
    float prob = object.prob;
    // 获取Rect类的构造函数和字段ID
    jclass rectClass = env->FindClass("org/opencv/core/Rect");
    jmethodID rectConstructor = env->GetMethodID(rectClass, "<init>", "(IIII)V");
    // 创建Rect对象并设置属性
    jobject javaRect = env->NewObject(rectClass, rectConstructor,
                                      (jint)rect.x, (jint)rect.y, (jint)rect.width, (jint)rect.height);
    // 获取Result类的构造函数和字段ID
    jclass resultClass = env->FindClass("com/tony/yolov8ncnn/PredictResult");
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "(Lorg/opencv/core/Rect;IF)V");
    // 创建Result对象并设置属性
    jobject result = env->NewObject(resultClass, resultConstructor, javaRect, (jint)label, (jfloat)prob);
    return result;
}

jobject convertToPredictResult(const std::vector<Object>& objects, JNIEnv *env) {
    if (objects.empty()) {
        return nullptr;
    }
    // 获取 ArrayList 类的引用
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    if (arrayListClass == nullptr) {
        return nullptr; // 处理错误
    }

    // 获取 ArrayList 的构造函数
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    if (arrayListConstructor == nullptr) {
        return nullptr; // 处理错误
    }

    // 创建 ArrayList 实例
    jobject arrayListInstance = env->NewObject(arrayListClass, arrayListConstructor);
    if (arrayListInstance == nullptr) {
        return nullptr; // 处理错误
    }

    // 获取 ArrayList 的 add 方法
    jmethodID arrayListAddMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    if (arrayListAddMethod == nullptr) {
        return nullptr; // 处理错误
    }

    for (const auto & obj : objects)
    {
        jobject javaObjectInstance = createResult(obj, env);
        if (javaObjectInstance == nullptr) {
            return nullptr; // 处理错误
        }
        env->CallBooleanMethod(arrayListInstance, arrayListAddMethod, javaObjectInstance);
        env->DeleteLocalRef(javaObjectInstance); // 释放局部引用
    }
    return arrayListInstance;
}
extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnLoad");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);
        if (m_yolo != nullptr) {
            delete m_yolo;
            m_yolo = nullptr;
        }
    }
}

JNIEXPORT jboolean JNICALL
Java_com_tony_yolov8ncnn_NcnnPredictorNative_loadModel(JNIEnv *env, jobject thiz, jstring param_path,
                                                       jstring bin_path, jint image_size, jint num_class,
                                                       jint cpugpu) {
    // TODO: implement loadModel()
    if (cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }
    const int target_sizes[] = {image_size, image_size};


    const float mean_vals[] = {103.53f, 116.28f, 123.675f};

    const float norm_vals[] = {1 / 255.f, 1 / 255.f, 1 / 255.f};

    bool use_gpu = (int)cpugpu == 1;

    const char* _model_path = env->GetStringUTFChars(param_path, 0);
    const char* _bin_path = env->GetStringUTFChars(bin_path, 0);
    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "model path %s", _model_path);
    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "bin path %s", _bin_path);
    FILE* file = fopen(_model_path, "r");
    if (file == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "yolov8ncnn", "model path can not open %s", _model_path);
        return JNI_FALSE;
    } else {
        fclose(file);
    }
    file = fopen(_bin_path, "r");
    if (file == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "yolov8ncnn", "bin path can not open %s", _model_path);
        return JNI_FALSE;
    } else {
        fclose(file);
    }
    // reload
    {
        __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "get lock and load model");
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            if (m_yolo != nullptr) {
                delete m_yolo;
                m_yolo = nullptr;
            }
            return JNI_FALSE;
        }
        else
        {
            if (!m_yolo)
                m_yolo = new Yolo;
            m_yolo->num_class = num_class;

            __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "start load model");
            m_yolo->load(_model_path, _bin_path, image_size, mean_vals, norm_vals, use_gpu);
            __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "load model done");
        }
    }

    return JNI_TRUE;
}

void pretty_print(const ncnn::Mat& m)
{
    __android_log_print(ANDROID_LOG_DEBUG, "yolo", "pretty_print c: %d", m.c);
    __android_log_print(ANDROID_LOG_DEBUG, "yolo","pretty print d: %d h: %d w: %d", m.d, m.h, m.w);
    __android_log_print(ANDROID_LOG_DEBUG, "yolo","pretty_print_mat");
    std::string ss;
    char format_string[100];
    for (int q=0; q<m.c; q++)
    {
        const float* ptr = m.channel(q);
        for (int z=0; z<m.d; z++)
        {
            for (int y=0; y<m.h; y++)
            {
                for (int x=0; x<m.w; x++)
                {
                    sprintf(format_string, "%f ", ptr[x]);
                    ss.append(format_string);
//                    log_no_newline(ANDROID_LOG_DEBUG, "yolo", "%f ", ptr[x]);
                }
                ptr += m.w;
                ss.append("\n；");
                __android_log_print(ANDROID_LOG_DEBUG, "yolo","%s", ss.c_str());
                ss.clear();
//                log_no_newline(ANDROID_LOG_DEBUG, "yolo","\n");
            }
            ss.append("\n；");
            __android_log_print(ANDROID_LOG_DEBUG, "yolo","append \\n");
//            log_no_newline(ANDROID_LOG_DEBUG, "yolo","\n");
        }
        __android_log_print(ANDROID_LOG_DEBUG, "yolo","------------------------");
    }
    __android_log_print(ANDROID_LOG_DEBUG, "yolo","mat result: %s", ss.c_str());
}
JNIEXPORT jobject JNICALL
Java_com_tony_yolov8ncnn_NcnnPredictorNative_predictYolo(JNIEnv *env, jobject thiz,
                                                         jlong address, jfloat prob_threshold, jfloat nms_threshold) {
    // TODO: implement predictYolo()
    {
        ncnn::MutexLockGuard g(lock);
        // 从Java传递的long类型地址获取Mat对象
        cv::Mat& rgb = *(cv::Mat*)address;
        if (rgb.channels() == 4) {
            __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "input image has 4 channel");

            // 转换4通道为三通道
            cv::cvtColor(rgb, rgb, cv::COLOR_RGBA2RGB);
            __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "input image channel converted: %d", rgb.channels());
        }
        __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "Mat size: %d, %d", rgb.rows, rgb.cols);
//        pretty_print(ncnn::Mat(mat->rows, mat->cols, mat->data, (size_t)mat->step));
        if (m_yolo)
        {
            std::vector<Object> objects;
            m_yolo->detect(rgb, objects, prob_threshold, nms_threshold);
            return convertToPredictResult(objects, env);
        }
        else
        {
            return nullptr;
        }
    }
}


JNIEXPORT jobject JNICALL
Java_com_tony_yolov8ncnn_NcnnPredictorNative_predictYoloByPath(JNIEnv *env, jobject thiz,
                                                               jstring image_path,
                                                               jfloat prob_threshold, jfloat nms_threshold) {

    // 将Java字符串转换为C++字符串
    const char *nativeImagePath = env->GetStringUTFChars(image_path, nullptr);

    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "image path: %s", nativeImagePath);
    // 使用OpenCV读取图片
    cv::Mat image = cv::imread(nativeImagePath, cv::IMREAD_COLOR);

    // 释放Java字符串
    env->ReleaseStringUTFChars(image_path, nativeImagePath);

    // 检查图片是否成功读取
    if (image.empty()) {
        // 处理错误情况，例如抛出异常
        jclass exClass = env->FindClass("java/lang/Exception");
        env->ThrowNew(exClass, "Failed to load image");
        return nullptr;
    }

    cv::Mat *mat = &image;
    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "Mat size: %d, %d", mat->rows, mat->cols);
    {
        ncnn::MutexLockGuard g(lock);

        if (m_yolo)
        {
            std::vector<Object> objects;
            m_yolo->detect(image, objects, prob_threshold, nms_threshold);
            return convertToPredictResult(objects, env);
        }
        else
        {
            return nullptr;
        }
    }

}

}
extern "C"
JNIEXPORT void JNICALL
Java_com_tony_yolov8ncnn_NcnnPredictorNative_release(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "release");
    {
        ncnn::MutexLockGuard g(lock);
        if (m_yolo != nullptr) {
            delete m_yolo;
            m_yolo = nullptr;
        }
    }
}