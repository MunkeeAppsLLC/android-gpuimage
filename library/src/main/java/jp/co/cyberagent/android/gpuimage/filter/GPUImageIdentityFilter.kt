package jp.co.cyberagent.android.gpuimage.filter


class GPUImageIdentityFilter : BaseGPUImageFilter {

    constructor() : super()
    constructor(vertexShader: String?, fragmentShader: String?) : super(vertexShader, fragmentShader)
}