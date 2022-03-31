/*
 * Copyright (C) 2018 Drake, Inc.
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

package com.itxca.spannablex.span

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.style.ImageSpan
import java.lang.ref.WeakReference

/**
 * Clone modify : [spannable](https://github.com/liangjingkanji/spannable)
 * [CenterImageSpan](spannable/src/main/java/com/drake/spannable/span/CenterImageSpan.kt)
 *
 * 可设置图片大小的[ImageSpan]
 */
class SizeImageSpan : ImageSpan {

    /**
     * @see [android.text.style.DynamicDrawableSpan.getCachedDrawable]
     */
    @Suppress("KDocUnresolvedReference")
    private var drawableRef: WeakReference<Drawable>? = null
    private val drawableCache: Drawable
        get() = drawableRef?.get() ?: drawable.apply {
            setBounds(
                0, 0,
                if (drawableWidth == DRAWABLE_AUTO_SIZE) intrinsicWidth else drawableWidth,
                if (drawableHeight == DRAWABLE_AUTO_SIZE) intrinsicHeight else drawableHeight
            )
            drawableRef = WeakReference(this)
        }

    /**
     * @see [drawableSize]
     * [DRAWABLE_AUTO_SIZE] 等于 [Drawable.getIntrinsicWidth]
     */
    var drawableWidth: Int = DRAWABLE_AUTO_SIZE
        private set

    /**
     * @see [drawableSize]
     * [DRAWABLE_AUTO_SIZE] 等于 [Drawable.getIntrinsicHeight]
     */
    var drawableHeight: Int = DRAWABLE_AUTO_SIZE
        private set

    constructor(drawable: Drawable) : super(drawable)
    constructor(drawable: Drawable, source: String) : super(drawable, source)
    constructor(context: Context, uri: Uri) : super(context, uri)
    constructor(context: Context, resourceId: Int) : super(context, resourceId)
    constructor(context: Context, bitmap: Bitmap) : super(context, bitmap)

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        fm?.let {
            val fontHeight = paint.fontMetricsInt.descent - paint.fontMetricsInt.ascent
            val imageHeight = drawableCache.bounds.height()
            it.ascent = paint.fontMetricsInt.ascent - ((imageHeight - fontHeight) / 2.0f).toInt()
            it.top = it.ascent
            it.descent = it.ascent + imageHeight
            it.bottom = it.descent
        }
        return drawableCache.bounds.right
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val fontHeight = paint.fontMetricsInt.descent - paint.fontMetricsInt.ascent
        val imageAscent =
            paint.fontMetricsInt.ascent - ((drawableCache.bounds.height() - fontHeight) / 2.0f).toInt()
        canvas.save()
        canvas.translate(x, (y + imageAscent).toFloat())
        drawableCache.draw(canvas)
        canvas.restore()
    }

    /**
     * 设置图片大小
     */
    fun drawableSize(width: Int, height: Int = width): SizeImageSpan = also {
        this.drawableWidth = width
        this.drawableHeight = height
        clearDrawableCache()
    }

    /**
     * 清理 [drawableRef] 缓存
     */
    fun clearDrawableCache() {
        drawableRef?.clear()
    }

    companion object {
        private const val DRAWABLE_AUTO_SIZE = -1
    }
}