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

    bool operator==(const Face &other) const {
        return (v == other.v
                && vn == other.vn
                && vt == other.vt);
    }

};

struct hasher {
    std::size_t operator()(const Face &k) const {
        using std::size_t;
        using std::hash;

        // Compute individual hash values for first,
        // second and third and combine them using XOR
        // and bit shifting:

        return ((hash<int>()(k.vt)
                 ^ (hash<int>()(k.vn) << 1)) >> 1)
               ^ (hash<int>()(k.vt) << 1);
    }
};

unsigned int parseFace(unordered_map<Face, unsigned int, hasher> &aux, vector<float> *points,
                       const vector<float> &vs, const vector<float> &vts, const vector<float> &vns,
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
        unsigned int res = (unsigned int) (points->size() / 8);
        points->push_back(vs[face.v * 3]);
        points->push_back(vs[face.v * 3 + 1]);
        points->push_back(vs[face.v * 3 + 2]);

        points->push_back(vts[face.vt * 2]);
        points->push_back(vts[face.vt * 2 + 1]);

        points->push_back(vns[face.vn * 3]);
        points->push_back(vns[face.vn * 3 + 1]);
        points->push_back(vns[face.vn * 3 + 2]);
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

    size_t estimateVerticesNumber = 35000;

    char type[32];
    vector<float> vs;
    vs.reserve(estimateVerticesNumber * 3);
    vector<float> vts;
    vts.reserve(estimateVerticesNumber * 2);
    vector<float> vns;
    vns.reserve(estimateVerticesNumber * 3);
    char ignore[512];
    float if1, if2, if3;
    vector<jfloat> *points = new vector<jfloat>;
    vector<jint> *faces = new vector<jint>;
    points->reserve(2048);
    unordered_map<Face, unsigned int, hasher> aux;
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
