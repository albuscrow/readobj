package com.example.lixue.importobj;

import java.nio.ByteBuffer;

/**
 * Created by lixue on 2017/11/22.
 */

public class ObjParser {
    ByteBuffer indices;
    ByteBuffer points;

    public native void loadObj(String fileName);
}
