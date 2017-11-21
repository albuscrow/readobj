#include <jni.h>
#include <string>
#include <syslog.h>
#include <cstdio>
#include <cstring>

using namespace std;

extern "C"
JNIEXPORT jstring
JNICALL
Java_com_example_lixue_importobj_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jobject
JNICALL
Java_com_example_lixue_importobj_MainActivity_testBuffer(
        JNIEnv *env,
        jobject /* this */, jstring input) {
    double *d = new double(1.0086);
    const char *c = env->GetStringUTFChars(input, nullptr);
    syslog(LOG_DEBUG, "%s", c);

    return env->NewDirectByteBuffer(d, sizeof(*d));
}


extern "C"
JNIEXPORT jobject
JNICALL
Java_com_example_lixue_importobj_MainActivity_readObj(
        JNIEnv *env,
        jobject /* this */, jstring fileName) {

    const char *fileNameC = env->GetStringUTFChars(fileName, nullptr);

    FILE *f = fopen(fileNameC, "r");
    char type[3];
    while (fscanf(f, "%s", type)) {
        if (strcmp("v", type)) {

        } else if (strcmp("vn", type)) {

        } else if (strcmp("vt", type)) {

        } else if (strcmp("f", type)) {

        }
    }

    env->ReleaseStringUTFChars(fileName, fileNameC);
}

