precision mediump float; 
      	 				
uniform sampler2D u_TextureUnit;      	 							
varying vec2 v_TextureCoordinates;
uniform float u_Alpha;
  
void main()                    		
{                              	
    gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates);
    gl_FragColor.a *= u_Alpha;
}