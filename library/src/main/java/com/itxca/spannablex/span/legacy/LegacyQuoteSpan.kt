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
import android.text.style.LeadingMarginSpan
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.Px

/** Copy @RequiresApi(Build.VERSION_CODES.P) Type from [android.text.style.QuoteSpan] */
class LegacyQuoteSpan(
    @ColorInt val color: Int,
    @Px @IntRange(from = 0) val stripeWidth: Int,
    @Px @IntRange(from = 0) val gapWidth: Int
) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return stripeWidth + gapWidth
    }

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: Layout
    ) {
        val cacheStyle = p.style
        val cacheColor = p.color

        p.style = Paint.Style.FILL
        p.color = color
        c.drawRect(
            x.toFloat(),
            top.toFloat(),
            (x + dir * stripeWidth).toFloat(),
            bottom.toFloat(),
            p
        )

        p.style = cacheStyle
        p.color = cacheColor
    }
}