package jp.co.cyberagent.android.gpuimage.filter;

/**
 * A filter that renders a FULLY transparent texture.
 * Used to have a transparent background when displaying a GPUImage before loading a texture
 */
public class GPUImageTransparentFilter extends GPUImageFilter {

    public static final String TRANSPARENT_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     gl_FragColor = vec4(textureColor.rgb, 0.0);\n" +
            "}";


    private float intensity = 1f;
    private int intensityLocation;

    public GPUImageTransparentFilter() {
        super(NO_FILTER_VERTEX_SHADER, TRANSPARENT_FILTER_FRAGMENT_SHADER);
    }
}
