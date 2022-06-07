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

package com.itxca.spannablex.span.legacy

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import androidx.annotation.ColorInt
import androidx.annotation.IntRange

/** Copy @RequiresApi(Build.VERSION_CODES.P) Type from [android.text.style.BulletSpan] */
class LegacyBulletSpan(
    @ColorInt val color: Int,
    @IntRange(from = 0) val bulletRadius: Int,
    val gapWidth: Int
) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return 2 * bulletRadius + gapWidth
    }

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: Layout?
    ) {
        if ((text as Spanned).getSpanStart(this) == start) {
            val cacheStyle = paint.style
            val cacheColor = paint.color
            paint.color = color
            paint.style = Paint.Style.FILL
            val yPosition = (top + bottom) / 2f
            val xPosition = (x + dir * bulletRadius).toFloat()
            canvas.drawCircle(xPosition, yPosition, bulletRadius.toFloat(), paint)
            paint.color = cacheColor
            paint.style = cacheStyle
        }
    }
}
