#include <jni.h>
#include <string>
#include <syslog.h>
#include <cstdio>
#include <cstring>
#include <unordered_map>
#include <map>
#include <vector>
#include <iostream>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

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

unsigned int parseFace(map<Face, unsigned int> &aux, vector<float> *points,
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
        unsigned int res = (unsigned int) (points->size() / 8 - 1);
        aux.insert({face, res});
        return res;
    }

}

extern "C"
JNIEXPORT void
JNICALL
Java_com_example_lixue_importobj_ObjParser_readObj(
        JNIEnv *env,
        jobject self, jstring fileName) {

    const char *fileNameC = env->GetStringUTFChars(fileName, nullptr);

    syslog(LOG_INFO, "obj file name is %s", fileNameC);

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
    vector<jint> *faces = new vector<jint>;
    points->reserve(2048);
    map<Face, unsigned int> aux;
    while (fscanf(f, "%s", type) != EOF) {
        if (strcmp("v", type) == 0) {
            fscanf(f, "%f%f%f", &if1, &if2, &if3);
            vs.push_back(if1);
            vs.push_back(if2);
            vs.push_back(if3);
        } else if (strcmp("vt", type) == 0) {
            fscanf(f, "%f%f", &if1, &if2);
            vts.push_back(if1);
            vts.push_back(1 - if2);
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


    jclass resClass = env->GetObjectClass(self);

    jfieldID pointsId = env->GetFieldID(resClass, "points", "Ljava/nio/ByteBuffer;");
    jobject pointBuffer = env->NewDirectByteBuffer(&((*points)[0]),
                                                   points->size() * sizeof(jfloat));
    env->SetObjectField(self, pointsId, pointBuffer);

    jfieldID indicesId = env->GetFieldID(resClass, "indices", "Ljava/nio/ByteBuffer;");
    jobject indicesBuffer = env->NewDirectByteBuffer(&((*faces)[0]),
                                                     faces->size() * sizeof(jint));
    env->SetObjectField(self, indicesId, indicesBuffer);

}
