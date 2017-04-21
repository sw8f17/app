#include <jni.h>
#include <string>
#include <mpg123.h>
#include <unistd.h>
#include <android/log.h>

typedef struct {
    mpg123_handle* handle;
    unsigned char* buffer;
    size_t totalRead;
    int channels;
    int encoding;
    long rate;
    size_t bufferSize;
    int fd;
} File;

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
const char *TAG = "JNI-NATIVE";

    /*
     * Get an int file descriptor from a java.io.FileDescriptor
     */
    int jniGetFDFromFileDescriptor(JNIEnv* env, jobject fileDescriptor) {
        jclass descriptorClass = env->FindClass("java/io/FileDescriptor");
        jfieldID descriptorID = env->GetFieldID(descriptorClass, "descriptor", "I");
        return env->GetIntField(fileDescriptor, descriptorID);
    }

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_static_1init(JNIEnv *env, jclass type) {
        mpg123_init();
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

    JNIEXPORT jlong JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_openFromDataSource(JNIEnv *env, jobject, jobject fileDescriptor, jlong offset, jlong length) {
        if (fileDescriptor == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "FUCK YOU");
            jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
            return 0;
        }

        int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);

        File *file = (File *) malloc(sizeof(File));
        int err;
        file->handle = mpg123_new(NULL, &err);
        if (err) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed creating mpg123: %s", mpg123_plain_strerror(err));
            return 0;
        }
        file->totalRead = 0;

        file->bufferSize = mpg123_outblock(file->handle);
        //I'm choosing to allocate a single buffer per song. This limits our options when it comes
        //to multithreading the playback of a single song, since the buffer can of course only be
        //used by one decode thread at a time. -JJ 21/04-2017
        file->buffer = (unsigned char *) malloc(file->bufferSize * sizeof(unsigned char));

        if (mpg123_open_fd(file->handle, fd)) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed calling open_fd: %s", mpg123_strerror(file->handle));
            return 0;
        }
        file->fd = fd;
        if (mpg123_getformat(file->handle, &file->rate, &file->channels, &file->encoding)) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed getting the format: %s", mpg123_strerror(file->handle));
            return 0;
        }
        return (jlong) file;
    }

    JNIEXPORT jlong JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_open(JNIEnv *env, jobject, jstring juri) {
        const char *uri = env->GetStringUTFChars(juri, 0);

        __android_log_print(ANDROID_LOG_WARN, TAG, "Opening file: %s", uri);

        File *file = (File *) malloc(sizeof(File));
        int err;
        file->handle = mpg123_new(NULL, &err);
        file->totalRead = 0;

        file->bufferSize = mpg123_outblock(file->handle);
        //I'm choosing to allocate a single buffer per song. This limits our options when it comes
        //to multithreading the playback of a single song, since the buffer can of course only be
        //used by one decode thread at a time. -JJ 21/04-2017
        file->buffer = (unsigned char *) malloc(file->bufferSize * sizeof(unsigned char));

        mpg123_open(file->handle, uri);
        file->fd = 0;
        mpg123_getformat(file->handle, &file->rate, &file->channels, &file->encoding);

        env->ReleaseStringUTFChars(juri, uri);
        return (jlong) file;
    }

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_close(JNIEnv *env, jobject, jlong handle) {
        File* file = (File *)handle;
        free(file->buffer);
        mpg123_close(file->handle);
        if(file->fd != 0)
            close(file->fd);
        mpg123_delete(file->handle);
    }


    JNIEXPORT jbyteArray JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_decodeFrame(JNIEnv* env, jobject, jlong handle) {
        File* file = (File *)handle;

        size_t done;
        if(mpg123_read(file->handle, file->buffer, file->bufferSize, &done) != MPG123_OK) {
            __android_log_print(ANDROID_LOG_WARN, TAG, "Done decoding");
            return env->NewByteArray(0);
        } else {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed decoding %s", mpg123_strerror(file->handle));
        }
        file->totalRead += done;
        __android_log_print(ANDROID_LOG_WARN, TAG, "Decoded %ld bytes", file->totalRead);

        jbyteArray arr = env->NewByteArray(done);
        env->SetByteArrayRegion(arr, 0, done, (const jbyte *) file->buffer);
        return arr;
    }
}
