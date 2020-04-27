package jp.co.cyberagent.android.gpuimage.filter

import android.opengl.GLES20
import androidx.annotation.RawRes
import jp.co.cyberagent.android.gpuimage.R
import java.nio.FloatBuffer


class GPUImageIdentityFilter : BaseGPUImageFilter {

    constructor(vertexShader: String, fragmentShader: String) : super(vertexShader, fragmentShader)

    constructor(
            @RawRes vertexShader: Int,
            @RawRes fragmentShader: Int
    ) : super(vertexShader, fragmentShader)

    constructor() : super(R.raw.shader_no_filter_vert, R.raw.shader_no_filter_frag)

    override fun onDraw(textureId: Int, cubeBuffer: FloatBuffer?, textureBuffer: FloatBuffer?) {
        super.onDraw(textureId, cubeBuffer, textureBuffer)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
    }

    override fun copy(): GPUImageFilter {
        return GPUImageIdentityFilter(vertexShader, fragmentShader)
    }
}