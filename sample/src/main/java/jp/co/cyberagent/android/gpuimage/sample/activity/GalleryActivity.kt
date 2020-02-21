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

package jp.co.cyberagent.android.gpuimage.sample.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImage3DSamplerInputFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageTwoInputFilter
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.FilterAdjuster
import jp.co.cyberagent.android.gpuimage.sample.R
import kotlinx.android.synthetic.main.activity_gallery.*

class GalleryActivity : AppCompatActivity() {

    private var filterAdjuster: FilterAdjuster? = null
    private val gpuImageView: GPUImageView by lazy { findViewById<GPUImageView>(R.id.gpuimage) }
    private val gpuImageContainerView: FrameLayout by lazy { findViewById<FrameLayout>(R.id.gpuimage_container) }
    private val seekBar: SeekBar by lazy { findViewById<SeekBar>(R.id.seekBar) }
    private val lutTableImage: ImageView by lazy { findViewById<ImageView>(R.id.lut_table) }
    private lateinit var imageUri: Uri

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        gpuimage.setBackgroundColor(0f,0f,0f,0.5f)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                filterAdjuster?.adjust(progress)
                gpuImageView.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<View>(R.id.button_choose_filter).setOnClickListener {
            GPUImageFilterTools.showDialog(this) { filter ->
                switchFilterTo(filter)
                gpuImageView.requestRender()
            }
        }
        findViewById<View>(R.id.button_save).setOnClickListener { saveImage() }

        gpuImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)

        startPhotoPicker()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PICK_IMAGE -> if (resultCode == RESULT_OK) {
                imageUri = data!!.data
                gpuImageView.setImage(data!!.data)
                gpuImageView.postInvalidate()
            } else {
                finish()
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startPhotoPicker() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE)
    }

    private fun saveImage() {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        gpuImageView.saveToPictures("GPUImage", fileName) { uri ->
            Toast.makeText(this, "Saved: $uri", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchFilterTo(filter: GPUImageFilter) {
        gpuImageView.filter = filter
        filterAdjuster = FilterAdjuster(filter)
        if (filterAdjuster!!.canAdjust()) {
            seekBar.visibility = View.VISIBLE
            filterAdjuster!!.adjust(seekBar.progress)
        } else {
            seekBar.visibility = View.GONE
        }
        when (filter) {
            is GPUImage3DSamplerInputFilter -> lutTableImage.setImageBitmap(filter.texture)
            is GPUImageTwoInputFilter -> lutTableImage.setImageBitmap(filter.bitmap)
        }
//        if(gpuImageView.parent == null) {
//            gpuImageContainerView.addView(gpuImageView)
//            gpuImageView.setImage(imageUri)
//        } else {
//            gpuImageContainerView.removeView(gpuImageView)
//        }
    }

    companion object {
        private const val REQUEST_PICK_IMAGE = 1
    }
}
