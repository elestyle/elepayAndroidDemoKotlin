package jp.elestyle.androidapp.elepaydemoapp.utils

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

object ImageLoader {
    fun loadImage(urlString: String, context: Context, resultHandler: (Bitmap?) -> Unit) {
        Glide.with(context)
                .asBitmap()
                .load(urlString)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        resultHandler(resource)
                    }
                })
    }
}
