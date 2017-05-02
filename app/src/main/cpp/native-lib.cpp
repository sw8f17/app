#include <jni.h>
#include <string>
#include <mpg123.h>
#include <unistd.h>
#include <android/log.h>

struct fields_t {
    jclass mp3fileClazz;
    jmethodID mp3fileCons;

    jclass mp3encodingClazz;
    jmethodID mp3encodingValueof;

    jclass mp3mediainfoClazz;
    jmethodID mp3mediainfoCons;
};
static fields_t fields;

/*
 * Get a human-readable summary of an exception object.  The buffer will
 * be populated with the "binary" class name and, if present, the
 * exception message.
 */
static void getExceptionSummary(JNIEnv* env, jthrowable exception, char* buf, size_t bufLen)
{
    int success = 0;
    /* get the name of the exception's class */
    jclass exceptionClazz = env->GetObjectClass(exception); // can't fail
    jclass classClazz = env->GetObjectClass(exceptionClazz); // java.lang.Class, can't fail
    jmethodID classGetNameMethod = env->GetMethodID(
            classClazz, "getName", "()Ljava/lang/String;");
    jstring classNameStr = (jstring) env->CallObjectMethod(exceptionClazz, classGetNameMethod);
    if (classNameStr != NULL) {
        /* get printable string */
        const char* classNameChars = env->GetStringUTFChars(classNameStr, NULL);
        if (classNameChars != NULL) {
            /* if the exception has a message string, get that */
            jmethodID throwableGetMessageMethod = env->GetMethodID(
                    exceptionClazz, "getMessage", "()Ljava/lang/String;");
            jstring messageStr = (jstring) env->CallObjectMethod(
                                    exception, throwableGetMessageMethod);
            if (messageStr != NULL) {
                const char* messageChars = env->GetStringUTFChars(messageStr, NULL);
                if (messageChars != NULL) {
                    snprintf(buf, bufLen, "%s: %s", classNameChars, messageChars);
                    env->ReleaseStringUTFChars(messageStr, messageChars);
                } else {
                    env->ExceptionClear(); // clear OOM
                    snprintf(buf, bufLen, "%s: <error getting message>", classNameChars);
                }
                env->DeleteLocalRef(messageStr);
            } else {
                strncpy(buf, classNameChars, bufLen);
                buf[bufLen - 1] = '\0';
            }
            env->ReleaseStringUTFChars(classNameStr, classNameChars);
            success = 1;
        }
        env->DeleteLocalRef(classNameStr);
    }
    env->DeleteLocalRef(classClazz);
    env->DeleteLocalRef(exceptionClazz);
    if (! success) {
        env->ExceptionClear();
        snprintf(buf, bufLen, "%s", "<error getting class name>");
    }
}

extern "C" {
static const char *TAG = "JNI-NATIVE";

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_staticInit(JNIEnv *env, jclass type) {
        fields.mp3fileClazz = (jclass) env->NewGlobalRef(env->FindClass("rocks/stalin/android/app/utils/MP3File"));
        if(fields.mp3fileClazz == NULL)
            return;

        fields.mp3fileCons = env->GetMethodID(fields.mp3fileClazz, "<init>", "(JJJILrocks/stalin/android/app/MP3MediaInfo;)V");
        if(fields.mp3fileCons == NULL)
            return;

        fields.mp3encodingClazz = (jclass) env->NewGlobalRef(env->FindClass("rocks/stalin/android/app/MP3Encoding"));
        if(fields.mp3encodingClazz == NULL)
            return;

        fields.mp3encodingValueof = env->GetStaticMethodID(fields.mp3encodingClazz, "valueOf", "(I)Lrocks/stalin/android/app/MP3Encoding;");
        if(fields.mp3encodingValueof == NULL)
            return;

        fields.mp3mediainfoClazz = (jclass) env->NewGlobalRef(env->FindClass("rocks/stalin/android/app/MP3MediaInfo"));
        if(fields.mp3mediainfoClazz == NULL)
            return;

        fields.mp3mediainfoCons = env->GetMethodID(fields.mp3mediainfoClazz, "<init>", "(JIJLrocks/stalin/android/app/MP3Encoding;)V");
        if(fields.mp3mediainfoCons == NULL)
            return;
    }

    jobject
    MP3EncodingCreate(JNIEnv* env, int encoding) {
        return env->CallStaticObjectMethod(fields.mp3encodingClazz, fields.mp3encodingValueof, (jint) encoding);
    }

    jobject
    MP3MediaInfoCreate(JNIEnv* env, long sampleRate, int channels, long bufferSize, int encoding) {
        jobject encodingObj = MP3EncodingCreate(env, encoding);
        return env->NewObject(fields.mp3mediainfoClazz, fields.mp3mediainfoCons,
                              (jlong) sampleRate, (jint) channels, (jlong) bufferSize, encodingObj);
    }

    jobject
    MP3FileCreate(JNIEnv *env, mpg123_handle *handle, unsigned char *buffer, size_t bufferSize, int fd, jobject mediaInfo) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Creating new mp3file: %ld", bufferSize);
        jobject mp3file = env->NewObject(fields.mp3fileClazz, fields.mp3fileCons,
                                         (jlong)handle, (jlong)buffer, (jlong)bufferSize, fd,
                                         mediaInfo);
        return mp3file;
    }

    /*
     * Throw an exception with the specified class and an optional message.
     *
     * If an exception is currently pending, we log a warning message and
     * clear it.
     *
     * Returns 0 if the specified exception was successfully thrown.  (Some
     * sort of exception will always be pending when this returns.)
     */
    int jniThrowException(JNIEnv* env, const char* className, const char* msg)
    {
        jclass exceptionClass;
        if (env->ExceptionCheck()) {
            /* TODO: consider creating the new exception with this as "cause" */
            char buf[256];
            jthrowable exception = env->ExceptionOccurred();
            env->ExceptionClear();
            if (exception != NULL) {
                getExceptionSummary(env, exception, buf, sizeof(buf));
                __android_log_print(ANDROID_LOG_WARN, TAG, "Discarding pending exception (%s) to throw %s\n", buf, className);
                env->DeleteLocalRef(exception);
            }
        }
        exceptionClass = env->FindClass(className);
        if (exceptionClass == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Unable to find exception class %s\n", className);
            /* ClassNotFoundException now pending */
            return -1;
        }
        int result = 0;
        if (env->ThrowNew(exceptionClass, msg) != JNI_OK) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed throwing '%s' '%s'\n", className, msg);
            /* an exception, most likely OOM, will now be pending */
            result = -1;
        }
        env->DeleteLocalRef(exceptionClass);
        return result;
    }

    JNIEXPORT jstring JNICALL
    Java_rocks_stalin_android_app_MainActivity_stringFromJNI(JNIEnv *env, jobject) {
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
    }

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_init(JNIEnv *env, jobject) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Starting mpg123");
        mpg123_init();
    }

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_exit(JNIEnv *env, jobject) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Stopping mpg123");
        mpg123_exit();
    }

    JNIEXPORT jobject JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_openFromDataSource(JNIEnv *env, jobject, jint fd, jlong offset, jlong length) {
        int err;
        mpg123_handle* handle = mpg123_new(NULL, &err);
        if (err) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed creating mpg123: %s", mpg123_plain_strerror(err));
            return NULL;
        }
        size_t totalRead = 0;

        size_t bufferSize = mpg123_outblock(handle);
        //I'm choosing to allocate a single buffer per song. This limits our options when it comes
        //to multithreading the playback of a single song, since the buffer can of course only be
        //used by one decode thread at a time. -JJ 21/04-2017
        unsigned char* buffer = (unsigned char *) malloc(bufferSize * sizeof(unsigned char));

        if (mpg123_open_fd(handle, fd)) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed calling open_fd: %s", mpg123_strerror(handle));
            return NULL;
        }
        long rate;
        int channels;
        int encoding;
        if (mpg123_getformat(handle, &rate, &channels, &encoding)) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed getting the format: %s", mpg123_strerror(handle));
            return NULL;
        }
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Starting decoding of file with: %ld, %d, %d", rate, channels, encoding);
        return MP3FileCreate(env, handle, buffer, bufferSize, fd,
                             MP3MediaInfoCreate(env, rate, channels, bufferSize, encoding));
    }
}
