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

extern "C" {
static const char *TAG = "JNI-NATIVE";

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_decoding_MP3Decoder_staticInit(JNIEnv *env, jclass type) {
        fields.mp3fileClazz = (jclass) env->NewGlobalRef(env->FindClass("rocks/stalin/android/app/decoding/MP3File"));
        if(fields.mp3fileClazz == NULL)
            return;

        fields.mp3fileCons = env->GetMethodID(fields.mp3fileClazz, "<init>", "(JJJILrocks/stalin/android/app/decoding/MediaInfo;)V");
        if(fields.mp3fileCons == NULL)
            return;

        fields.mp3encodingClazz = (jclass) env->NewGlobalRef(env->FindClass("rocks/stalin/android/app/decoding/MP3Encoding"));
        if(fields.mp3encodingClazz == NULL)
            return;

        fields.mp3encodingValueof = env->GetStaticMethodID(fields.mp3encodingClazz, "valueOf", "(I)Lrocks/stalin/android/app/decoding/MP3Encoding;");
        if(fields.mp3encodingValueof == NULL)
            return;

        fields.mp3mediainfoClazz = (jclass) env->NewGlobalRef(env->FindClass("rocks/stalin/android/app/decoding/MediaInfo"));
        if(fields.mp3mediainfoClazz == NULL)
            return;

        fields.mp3mediainfoCons = env->GetMethodID(fields.mp3mediainfoClazz, "<init>", "(JIJLrocks/stalin/android/app/decoding/MP3Encoding;)V");
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

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_decoding_MP3Decoder_init(JNIEnv *env, jobject) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Starting mpg123");
        mpg123_init();
    }

    JNIEXPORT void JNICALL
    Java_rocks_stalin_android_app_decoding_MP3Decoder_exit(JNIEnv *env, jobject) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Stopping mpg123");
        mpg123_exit();
    }

    JNIEXPORT jobject JNICALL
    Java_rocks_stalin_android_app_decoding_MP3Decoder_openFromDataSource(JNIEnv *env, jobject, jint fd, jlong offset, jlong length) {
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
