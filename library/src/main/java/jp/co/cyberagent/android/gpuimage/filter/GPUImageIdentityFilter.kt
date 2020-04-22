package jp.co.cyberagent.android.gpuimage.filter

import jp.co.cyberagent.android.gpuimage.R


class GPUImageIdentityFilter : BaseGPUImageFilter {

    constructor() : super(R.raw.shader_no_filter_vert, R.raw.shader_no_filter_frag)
}