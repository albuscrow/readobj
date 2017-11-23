package com.example.lixue.importobj;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ac on 11/22/17.
 */

public class GLView extends GLSurfaceView implements GLSurfaceView.Renderer {


    private int mNormalHandle;
    private int mTexCoordHandle;
    private RendererData model;

    public GLView(Context context) {
        this(context, null);
    }

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec4 vNormal;" +
                    "attribute vec4 vTexCoord;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
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
        long start = System.currentTimeMillis();
        model = (RendererData) new ObjParser().readObj("/sdcard/test.obj");
        int[] buffer = new int[2];
        GLES20.glGenBuffers(2, buffer, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, model.points.capacity() * 4, model.points, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffer[1]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, model.indices.capacity() * 4, model.indices, GLES20.GL_STATIC_DRAW);

//        System.out.println("MainActivity.display ac!!" + (System.currentTimeMillis() - start));
//        FloatBuffer fb = data.points.order(ByteOrder.nativeOrder()).asFloatBuffer();
//        System.out.println("MainActivity.display ac!!" + fb.capacity());
//        IntBuffer ib = data.indices.order(ByteOrder.nativeOrder()).asIntBuffer();
//        System.out.println("MainActivity.display ac!!" + ib.capacity());

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);


        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoord");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, true,
                8, 0);

        GLES20.glVertexAttribPointer(mTexCoordHandle, 3,
                GLES20.GL_FLOAT, true,
                8, 2);

        GLES20.glVertexAttribPointer(mNormalHandle, 3,
                GLES20.GL_FLOAT, true,
                8, 5);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, model.indices.capacity(), GLES20.GL_UNSIGNED_SHORT, 0);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }
}
