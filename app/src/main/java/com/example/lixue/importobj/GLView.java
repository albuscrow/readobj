package com.example.lixue.importobj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ac on 11/22/17.
 */

public class GLView extends GLSurfaceView implements GLSurfaceView.Renderer {
    public static final String TAG = "GLView";


    private final String fileName;
    private int mNormalHandle;
    private int mTexCoordHandle;
    private ObjParser objParser;
    private int mMVPHandle;
    private float radio;
    private long start;

    public GLView(Context context, String fileName) {
        super(context);
        this.fileName = fileName;
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        requestRender();
    }


    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec3 vNormal;" +
                    "attribute vec2 vTexCoord;" +
                    "uniform mat4 mvp;" +
                    "varying vec4 pNormal;" +
                    "varying vec2 pTexCoord;" +
                    "void main() {" +
                    //"  vPosition[3] = 1;" +
                    "  gl_Position = mvp * vPosition;" +
                    "  pNormal = mvp * vec4(vNormal, 0);" +
                    "  pTexCoord = vTexCoord;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 pNormal;" +
                    "varying vec2 pTexCoord;" +
                    "uniform sampler2D texture1;" +
                    "void main() {" +
                    "  float temp = abs(pNormal[2]);" +
                    "  gl_FragColor = texture2D(texture1, pTexCoord) * temp;" +
                    "}";
    private int mProgram;
    private int mPositionHandle;

    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        start = System.currentTimeMillis();
        objParser = new ObjParser();
        objParser.readObj(fileName);
        Log.d(TAG, "read time is " + (System.currentTimeMillis() - start) + "ms");
//        System.out.println("MainActivity.display ac!!" + (System.currentTimeMillis() - start));
//        FloatBuffer fb = data.points.order(ByteOrder.nativeOrder()).asFloatBuffer();
//        System.out.println("MainActivity.display ac!!" + fb.capacity());
//        IntBuffer ib = data.indices.order(ByteOrder.nativeOrder()).asIntBuffer();
//        System.out.println("MainActivity.display ac!!" + ib.capacity());
//        System.out.println("GLView.onSurfaceCreated ac!!" + objParser.indices.position()
//                + " " + objParser.indices.remaining()
//                + " " + objParser.indices.capacity());
        int[] buffer = new int[2];
        GLES20.glGenBuffers(2, buffer, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                objParser.points.capacity(),
                objParser.points,
                GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffer[1]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                objParser.indices.capacity(),
                objParser.indices,
                GLES20.GL_STATIC_DRAW);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        int texture = loadTexture(getContext(), "texture.jpg");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);

    }

    public static int loadTexture(final Context context, final String textureName) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(context.getAssets().open(textureName), null, options);
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public void onDrawFrame(GL10 unused) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");
        mMVPHandle = GLES20.glGetUniformLocation(mProgram, "mvp");

        float[] p = new float[16];
        Matrix.frustumM(p, 0, -radio, radio, -1, 1, 1, 1000);

        float[] v = new float[16];
        Matrix.setLookAtM(v, 0, 0, 0, 300, 0, 0, 0, 0, 1, 0);

        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        //Matrix.rotateM(m, 0, -90, 0, 1, 0);

        float[] vm = new float[16];
        Matrix.multiplyMM(vm, 0, v, 0, m, 0);
        float[] pvm = new float[16];
        Matrix.multiplyMM(pvm, 0, p, 0, vm, 0);
        GLES20.glUniformMatrix4fv(mMVPHandle, 1, false, pvm, 0);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, true,
                8 * 4, 0);

        GLES20.glVertexAttribPointer(mTexCoordHandle, 2,
                GLES20.GL_FLOAT, true,
                8 * 4, 3 * 4);

        GLES20.glVertexAttribPointer(mNormalHandle, 3,
                GLES20.GL_FLOAT, true,
                8 * 4, 5 * 4);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                objParser.indices.capacity(),
                GLES20.GL_UNSIGNED_INT, 0);
        GLES20.glFinish();
        Log.d(TAG, "total time is " + (System.currentTimeMillis() - start) + "ms");
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        radio = (float) width / height;
        GLES20.glViewport(0, 0, width, height);
    }
}
