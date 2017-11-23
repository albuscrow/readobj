#include <jni.h>
#include <string>
#include <syslog.h>
#include <cstdio>
#include <cstring>
#include <unordered_map>
#include <map>
#include <vector>
#include <iostream>

using namespace std;

struct Face {
    int v;
    int vn;
    int vt;

    bool operator<(const Face &face) const {
        if (v == face.v) {
            if (vn == face.vn) {
                if (vt == face.vt) {
                    return false;
                } else {
                    return vt < face.vt;
                }
            } else {
                return vn < face.vn;
            }
        } else {
            return v < face.v;
        }
    }
};

short parseFace(map<Face, short> &aux, vector<float> *points,
              vector<float> &vs, vector<float> &vts, vector<float> &vns,
              FILE *f) {
    Face face;
    fscanf(f, "%d/%d/%d", &face.v, &face.vt, &face.vn);
    face.v -= 1;
    face.vt -= 1;
    face.vn -= 1;
    const auto &iterator = aux.find(face);
    if (iterator != aux.end()) {
        return iterator->second;
    } else {
        points->push_back(vs[face.v * 3]);
        points->push_back(vs[face.v * 3 + 1]);
        points->push_back(vs[face.v * 3 + 2]);

        points->push_back(vts[face.vt * 2]);
        points->push_back(vts[face.vt * 2 + 1]);

        points->push_back(vns[face.vn * 3]);
        points->push_back(vns[face.vn * 3 + 1]);
        points->push_back(vns[face.vn * 3 + 2]);
        short res = (short) (points->size() / 8 - 1);
        aux.insert({face, res});
        return res;
    }

}

extern "C"
JNIEXPORT jobject
JNICALL
Java_com_example_lixue_importobj_ObjParser_readObj(
        JNIEnv *env,
        jobject /* this */, jstring fileName) {

    const char *fileNameC = env->GetStringUTFChars(fileName, nullptr);

    FILE *f = fopen(fileNameC, "r");
    char type[32];
    vector<float> vs;
    vs.reserve(1024);
    vector<float> vts;
    vts.reserve(1024);
    vector<float> vns;
    vns.reserve(1024);
    char ignore[1024];
    float if1, if2, if3;
    vector<jfloat> *points = new vector<jfloat>;
    vector<jshort> *faces = new vector<jshort>;
    points->reserve(2014);
    map<Face, short> aux;
    while (fscanf(f, "%s", type) != EOF) {
        if (strcmp("v", type) == 0) {
            fscanf(f, "%f%f%f", &if1, &if2, &if3);
            vs.push_back(if1);
            vs.push_back(if2);
            vs.push_back(if3);
        } else if (strcmp("vt", type) == 0) {
            fscanf(f, "%f%f", &if1, &if2);
            vts.push_back(if1);
            vts.push_back(if2);
        } else if (strcmp("vn", type) == 0) {
            fscanf(f, "%f%f%f", &if1, &if2, &if3);
            vns.push_back(if1);
            vns.push_back(if2);
            vns.push_back(if3);
        } else if (strcmp("f", type) == 0) {
            faces->push_back(parseFace(aux, points, vs, vts, vns, f));
            faces->push_back(parseFace(aux, points, vs, vts, vns, f));
            faces->push_back(parseFace(aux, points, vs, vts, vns, f));
        } else {
            fgets(ignore, sizeof(ignore), f);
        }

    }

    env->ReleaseStringUTFChars(fileName, fileNameC);


//    public class RendererData {
//        IntBuffer indices;
//        FloatBuffer points;
//    }
    jclass resClass = env->FindClass("com/example/lixue/importobj/RendererData");
    jmethodID initId = env->GetMethodID(resClass, "<init>", "()V");
    jobject res = env->NewObject(resClass, initId);

    jfieldID pointsId = env->GetFieldID(resClass, "points", "Ljava/nio/ByteBuffer;");
    jobject pointBuffer = env->NewDirectByteBuffer(&((*points)[0]), points->size() * sizeof(jfloat));
    env->SetObjectField(res, pointsId, pointBuffer);

    jfieldID indicesId = env->GetFieldID(resClass, "indices", "Ljava/nio/ByteBuffer;");
    jobject indicesBuffer = env->NewDirectByteBuffer(&((*faces)[0]), faces->size() * sizeof(jint));
    env->SetObjectField(res, indicesId, indicesBuffer);

    return res;
}

