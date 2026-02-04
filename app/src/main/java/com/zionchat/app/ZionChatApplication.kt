package com.zionchat.app

import android.app.Application
import coil3.ImageLoader
import coil3.ImageLoaderFactory
import coil3.decode.SvgDecoder

class ZionChatApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
