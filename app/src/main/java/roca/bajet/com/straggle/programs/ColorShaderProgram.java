package roca.bajet.com.straggle.programs;

/**
 * Created by Arnold on 2/8/2017.
 */

import android.content.Context;

import roca.bajet.com.straggle.R;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;

public class ColorShaderProgram extends ShaderProgram{
    // Uniform locations
    private final int uMatrixLocation;
    private final int uColorLocation;
    public final static String LOG_TAG = "ColorShaderProgram";
    // Attribute locations
    private final int aPositionLocation;

    public ColorShaderProgram(Context context) {
        super(context, R.raw.simple_vertex_shader,
                R.raw.simple_fragment_shader);


        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uColorLocation = glGetUniformLocation(program, U_COLOR);

        // Retrieve attribute locations for the shader program.
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
    }

    public void setUniforms(float[] matrix, float r, float g, float b) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        glUniform4f(uColorLocation, r, g, b, 0.01f);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }
}
