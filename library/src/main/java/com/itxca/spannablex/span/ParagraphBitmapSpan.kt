/*
 * Copyright (C) 2022 TxcA, Inc.
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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import androidx.annotation.Px
import androidx.core.graphics.scale
import com.itxca.spannablex.utils.DrawableSize

class ParagraphBitmapSpan(
    val bitmap: Bitmap,
    val drawableSize: DrawableSize?,
    @Px val padding: Int
) : LeadingMarginSpan, LineHeightSpan {
    private val bitmapHeight: Int
        get() = drawableSize?.height ?: bitmap.height

    private val bitmapWidth: Int
        get() = drawableSize?.width ?: bitmap.width

    override fun getLeadingMargin(first: Boolean): Int {
        return bitmapWidth + padding
    }

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: Layout
    ) {
        val st = (text as Spanned).getSpanStart(this)
        val lineTop = layout.getLineTop(layout.getLineForOffset(st))
        val scaleBitmap = drawableSize?.let {
            bitmap.scale(bitmapWidth, bitmapHeight, true)
        } ?: bitmap

        c.drawBitmap(
            scaleBitmap,
            (if (dir < 0) bitmapWidth - x else x).toFloat(),
            lineTop.toFloat(),
            p
        )
    }

    override fun chooseHeight(
        text: CharSequence, start: Int, end: Int,
        istartv: Int, v: Int,
        fm: FontMetricsInt
    ) {
        if (end == (text as Spanned).getSpanEnd(this)) {
            val ht = bitmapHeight
            var need = ht - (v + fm.descent - fm.ascent - istartv)
            if (need > 0) {
                fm.descent += need
            }
            need = ht - (v + fm.bottom - fm.top - istartv)
            if (need > 0) {
                fm.bottom += need
            }
        }
    }
}
