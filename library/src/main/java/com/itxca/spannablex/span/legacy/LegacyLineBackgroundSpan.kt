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
import android.text.style.LineBackgroundSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px

/** Copy @RequiresApi(Build.VERSION_CODES.Q) Type from [android.text.style.LineBackgroundSpan.Standard] */
class LegacyLineBackgroundSpan(@ColorInt val color: Int) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas, paint: Paint,
        @Px left: Int, @Px right: Int,
        @Px top: Int, @Px baseline: Int, @Px bottom: Int,
        text: CharSequence, start: Int, end: Int,
        lineNumber: Int
    ) {
        val originColor = paint.color
        paint.color = color
        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        paint.color = originColor
    }
}