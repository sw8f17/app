#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_rocks_stalin_android_app_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++ aka. the deep depths of hell.";
    return env->NewStringUTF(hello.c_str());
}
