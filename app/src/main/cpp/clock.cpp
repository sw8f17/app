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

    fields.timeCons = env->GetMethodID(fields.timeClazz, "<init>", "(JJ)V");
    if(fields.timeCons == NULL)
        return;
}

jobject
TimeCreate(JNIEnv* env, int64_t millis, int64_t nanos) {
    return env->NewObject(fields.timeClazz, fields.timeCons,
                          (jlong) millis, (jlong) nanos);
}

JNIEXPORT jobject JNICALL
Java_rocks_stalin_android_app_utils_time_Clock_getTime(JNIEnv *env, jclass type) {
    timespec time;
    clock_gettime(CLOCK_REALTIME, &time);
    return TimeCreate(env, ((int64_t)time.tv_sec) * 1000, time.tv_nsec);
}
}
