/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.filter;

/**
 * Resembles a filter that consists of multiple filters applied after each
 * other.
 */
public class GPUImageTrailFilter extends GPUImageFilterGroup {

    private GPUImageTransformFilter transformFilter = new GPUImageClearBufferTransformFilter();

    public GPUImageTrailFilter() {
        this.addFilter(transformFilter);
        this.addFilter(new GPUImageIdentityFilter());
    }

    public float[] getTransform3D() {
        return transformFilter.getTransform3D();
    }

    public void setTransform3D(float[] transform3D) {
        transformFilter.setTransform3D(transform3D);
    }

    public void setIgnoreAspectRatio(boolean ignoreAspectRatio) {
        transformFilter.setIgnoreAspectRatio(ignoreAspectRatio);
    }

    public boolean ignoreAspectRatio() {
        return transformFilter.ignoreAspectRatio();
    }

    public void setAnchorTopLeft(boolean anchorTopLeft) {
        transformFilter.setAnchorTopLeft(anchorTopLeft);
    }

    public boolean anchorTopLeft() {
        return transformFilter.anchorTopLeft();
    }

    @Override
    public GPUImageFilter addFilter(GPUImageFilter aFilter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFilter(GPUImageFilter filter) {
        throw new UnsupportedOperationException();
    }

}
