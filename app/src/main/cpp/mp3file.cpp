#include <jni.h>
#include <cwchar>
#include <android/log.h>
#include <unistd.h>
#include <mpg123.h>

struct fields_t {
    jfieldID    context;
};
static fields_t fields;

typedef struct {
    mpg123_handle* handle;
    unsigned char* buffer;
    size_t totalRead;
    size_t bufferSize;
    int fd;
} File;

static const char *TAG = "MP3File-NATIVE";

File* getFile(JNIEnv* env, jobject thiz) {
    return (File *) env->GetLongField(thiz, fields.context);
}

void setFile(JNIEnv* env, jobject thiz, File* file) {
    env->SetLongField(thiz, fields.context, (jlong) file);
}

extern "C" {
JNIEXPORT void JNICALL
Java_rocks_stalin_android_app_decoding_MP3File_nativeCons(JNIEnv *env, jobject thiz, jlong handle,
                                                       jlong buffer,
                                                       jlong bufferSize, jint fd) {
    File *file = (File *) malloc(sizeof(File));

    file->handle = (mpg123_handle *) handle;
    file->totalRead = 0;
    file->buffer = (unsigned char *) buffer;
    file->bufferSize = (size_t) bufferSize;
    file->fd = fd;

    setFile(env, thiz, file);
}

JNIEXPORT void JNICALL
Java_rocks_stalin_android_app_decoding_MP3File_staticInit(JNIEnv *env, jclass type) {
    fields.context = env->GetFieldID(type, "context", "J");
    if (fields.context == NULL) {
        return;
    }
}

JNIEXPORT void JNICALL
Java_rocks_stalin_android_app_decoding_MP3File_close(JNIEnv *env, jobject thiz) {
    File *file = getFile(env, thiz);

    __android_log_print(ANDROID_LOG_INFO, TAG, "Closing file");

    free(file->buffer);
    mpg123_close(file->handle);
    if (file->fd != 0)
        close(file->fd);
    mpg123_delete(file->handle);
}


JNIEXPORT jbyteArray JNICALL
Java_rocks_stalin_android_app_decoding_MP3File_decodeFrameNative(JNIEnv *env, jobject thiz) {
    File *file = getFile(env, thiz);

    size_t done;
    if (mpg123_read(file->handle, file->buffer, file->bufferSize, &done) != MPG123_OK) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "Done decoding: %s",
                            mpg123_strerror(file->handle));
        return env->NewByteArray(0);
    }
    file->totalRead += done;
    __android_log_print(ANDROID_LOG_WARN, TAG, "Decoded %ld bytes", file->totalRead);

    jbyteArray arr = env->NewByteArray(done);
    /*
    memset(file->buffer, 0, file->bufferSize);
    double amplitude = 0.25 * SHRT_MAX;
    double freq = 1000;
    unsigned short *sBuffer = (unsigned short *)file->buffer;
    for(int i = 0; i < file->bufferSize/2; i++) {
        sBuffer[i] = (unsigned short) (amplitude * sin((2 * 3.14 * i * freq) / 44100));
    }
     */
    env->SetByteArrayRegion(arr, 0, done, (const jbyte *) file->buffer);
    return arr;
}

JNIEXPORT void JNICALL
Java_rocks_stalin_android_app_decoding_MP3File_seek(JNIEnv *env, jobject thiz, jint sample) {
    File *file = getFile(env, thiz);
    mpg123_seek(file->handle, sample, SEEK_SET);
}

JNIEXPORT jlong JNICALL
Java_rocks_stalin_android_app_decoding_MP3File_tell(JNIEnv *env, jobject thiz) {
    File *file = getFile(env, thiz);
    return mpg123_tell(file->handle);
}

JNIEXPORT jlong JNICALL
Java_rocks_stalin_android_app_decoding_MP3File_tellframe(JNIEnv *env, jobject thiz) {
    File *file = getFile(env, thiz);
    return mpg123_tellframe(file->handle);
}
}
