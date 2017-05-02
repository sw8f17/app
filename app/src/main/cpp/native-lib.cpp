#include <jni.h>
#include <string>
#include <mpg123.h>
#include <android/log.h>

extern "C" {
    const char *TAG = "JNI-NATIVE";

    JNIEXPORT jstring JNICALL
    Java_rocks_stalin_android_app_MainActivity_stringFromJNI(
            JNIEnv *env,
            jobject /* this */) {
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
    }

    JNIEXPORT jbyteArray JNICALL
    Java_rocks_stalin_android_app_MP3Decoder_decode(JNIEnv* env, jobject, jstring juri) {
        const char* uri = env->GetStringUTFChars(juri, JNI_FALSE);
        __android_log_print(ANDROID_LOG_WARN, TAG, "Decoding file: %s", uri);

        mpg123_init();

        int err;
        mpg123_handle* mpgHandle = mpg123_new(NULL, &err);
        size_t bufferSize;
        size_t done;

        int channels;
        int encoding;
        long rate;
        bufferSize = mpg123_outblock(mpgHandle);
        unsigned char* buffer = (unsigned char *) malloc(bufferSize * sizeof(unsigned char));

        mpg123_open(mpgHandle, uri);
        mpg123_getformat(mpgHandle, &rate, &channels, &encoding);

        int totalBytes = 0;
        while(mpg123_read(mpgHandle, buffer, bufferSize, &done) == MPG123_OK) {
            __android_log_print(ANDROID_LOG_WARN, TAG, "Decoded %ld bytes", totalBytes);
            totalBytes += done;
        }

        free(buffer);
        mpg123_close(mpgHandle);
        mpg123_delete(mpgHandle);
        mpg123_exit();

        env->ReleaseStringUTFChars(juri, uri);
        return jbyteArray();
    }
}
