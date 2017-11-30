package com.example.lixue.importobj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ac on 11/22/17.
 */

public class GLView extends GLSurfaceView implements GLSurfaceView.Renderer {
    public static final String TAG = "GLView";


    private final String objFilePath;
    private final String textureFilePath;
    private int mNormalHandle;
    private int mTexCoordHandle;
    private ObjParser objParser;
    private int mMVPHandle;
    private float radio;
    private long startLoadObjTime;
    private float[] p;
    private float[] v;
    private float[] m;

    /**
     * @param context
     * @param objFilePath     obj 文件路径
     * @param textureFilePath 纹理图片路径
     */
    public GLView(Context context, String objFilePath, String textureFilePath) {
        super(context);
        this.objFilePath = objFilePath;
        this.textureFilePath = textureFilePath;
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
        objParser = new ObjParser();

        startLoadObjTime = System.currentTimeMillis();
        objParser.loadObj(objFilePath);
        Log.d(TAG, "read time is " + (System.currentTimeMillis() - startLoadObjTime) + "ms");

        initShader();
        checkGLError("init shader");
        initBuffer();
        checkGLError("init buffer");
        initTexture();
        checkGLError("init texture");


        //enable depth test
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);
    }

    private void initMVPMatrix() {
        p = new float[16];
        Matrix.frustumM(p, 0, -radio, radio, -1, 1, 1, 1000);
        v = new float[16];
        Matrix.setLookAtM(v, 0, 0, 0, 300, 0, 0, 0, 0, 1, 0);
        m = new float[16];
        Matrix.setIdentityM(m, 0);
    }

    public void checkGLError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
        }
    }

    private void initTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        int texture = loadTexture(getContext(), textureFilePath);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
    }

    private void initShader() {
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

        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mMVPHandle = GLES20.glGetUniformLocation(mProgram, "mvp");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");
    }

    private void initBuffer() {
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
    }

    public static int loadTexture(final Context context, final String textureFileName) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            Bitmap bitmap;
            bitmap = BitmapFactory.decodeFile(textureFileName, options);
            if (bitmap == null) {
                throw new RuntimeException(TAG + ": load texture file failed!");
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
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        updateMVP();

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                objParser.indices.capacity(),
                GLES20.GL_UNSIGNED_INT, 0);

        //wait gl draw
        GLES20.glFinish();
        checkGLError("draw");
        Log.d(TAG, "total time is " + (System.currentTimeMillis() - startLoadObjTime) + "ms");
    }

    private void updateMVP() {
        float[] vm = new float[16];
        Matrix.multiplyMM(vm, 0, v, 0, m, 0);
        float[] pvm = new float[16];
        Matrix.multiplyMM(pvm, 0, p, 0, vm, 0);
        GLES20.glUniformMatrix4fv(mMVPHandle, 1, false, pvm, 0);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        radio = (float) width / height;
        GLES20.glViewport(0, 0, width, height);
        initMVPMatrix();
    }


    private float lastX;
    private float lastY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX = (event.getX() - lastX);
                float deltaY = (event.getY() - lastY);
                if (deltaX == 0 && deltaY == 0) {
                    break;
                }
                float[] sTemp = new float[16];
                Matrix.setRotateM(sTemp, 0, 2f, deltaY, deltaX, 0);
                Matrix.multiplyMM(m, 0, sTemp, 0, m, 0);
                lastX = event.getX();
                lastY = event.getY();
                requestRender();
                break;
        }
        return true;
    }

}
