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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import androidx.annotation.Px
import com.itxca.spannablex.utils.DrawableSize

class ParagraphDrawableSpan(
    val drawable: Drawable,
    val drawableSize: DrawableSize?,
    @Px val padding: Int
) : LeadingMarginSpan, LineHeightSpan {
    private val drawableHeight: Int
        get() = drawableSize?.height ?: drawable.intrinsicHeight

    private val drawableWidth: Int
        get() = drawableSize?.width ?: drawable.intrinsicWidth

    override fun getLeadingMargin(first: Boolean): Int {
        return drawableWidth + padding
    }

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: Layout
    ) {
        val st = (text as Spanned).getSpanStart(this)
        val lineTop = layout.getLineTop(layout.getLineForOffset(st))
        drawable.setBounds(x, lineTop, x + drawableWidth, lineTop + drawableHeight)
        drawable.draw(c)
    }

    override fun chooseHeight(
        text: CharSequence, start: Int, end: Int,
        istartv: Int, v: Int,
        fm: FontMetricsInt
    ) {
        if (end == (text as Spanned).getSpanEnd(this)) {
            val ht = drawableHeight
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
