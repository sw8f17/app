#include <jni.h>
#include <android/log.h>
#include <time.h>
struct fields_t {
    jclass timeClazz;
    jmethodID timeCons;
};
static fields_t fields;

static const char *TAG = "Clock-NATIVE";

extern "C" {
JNIEXPORT void JNICALL
Java_rocks_stalin_android_app_utils_time_Clock_staticInit(JNIEnv *env, jclass type) {
    fields.timeClazz = (jclass) env->NewGlobalRef(env->FindClass("rocks/stalin/android/app/utils/time/Clock$Instant"));
    if(fields.timeClazz == NULL)
        return;

    fields.timeCons = env->GetMethodID(fields.timeClazz, "<init>", "(JI)V");
    if(fields.timeCons == NULL)
        return;
}

jobject
TimeCreate(JNIEnv* env, int64_t millis, int32_t nanos) {
    return env->NewObject(fields.timeClazz, fields.timeCons,
                          (jlong) millis, (jint) nanos);
}

JNIEXPORT jobject JNICALL
Java_rocks_stalin_android_app_utils_time_Clock_getTime(JNIEnv *env, jclass type) {
    timespec time;
    clock_gettime(CLOCK_REALTIME, &time);
    int64_t ms = (((int64_t)time.tv_sec) * 1000L) + (((int64_t)time.tv_nsec) / 1000000L);
    int32_t ns = (int32_t) (time.tv_nsec % 1000000L);
    return TimeCreate(env, ms, ns);
}
}
