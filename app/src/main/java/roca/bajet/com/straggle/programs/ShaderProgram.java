package roca.bajet.com.straggle.programs;

/**
 * Created by Arnold on 2/8/2017.
 */

import android.content.Context;

import roca.bajet.com.straggle.util.ShaderHelper;
import roca.bajet.com.straggle.util.TextResourceReader;

import static android.opengl.GLES20.glUseProgram;

abstract class ShaderProgram {
    // Uniform constants
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_COLOR = "u_Color";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    protected static final String U_ALPHA = "u_Alpha";

    // Attribute constants
    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    public final static String LOG_TAG = "ShaderProgram";

    // Shader program
    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId,
                            int fragmentShaderResourceId) {


        String vertexShader = TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId);
        String fragmentShader = TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId);

        // Compile the shaders and link the program.
        program = ShaderHelper.buildProgram(vertexShader, fragmentShader);

    }

    public void useProgram() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program);
    }
}
