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

import android.graphics.Paint.FontMetricsInt
import android.text.style.LineHeightSpan
import androidx.annotation.IntRange
import androidx.annotation.Px
import kotlin.math.roundToInt

/** Copy @RequiresApi(Build.VERSION_CODES.Q) Type from [android.text.style.LineHeightSpan.Standard] */
class LegacyLineHeightSpan(@Px @IntRange(from = 1) val  height: Int) : LineHeightSpan {

    override fun chooseHeight(
        text: CharSequence, start: Int, end: Int,
        spanstartv: Int, lineHeight: Int,
        fm: FontMetricsInt
    ) {
        val originHeight = fm.descent - fm.ascent
        if (originHeight <= 0) {
            return
        }
        val ratio = height * 1.0f / originHeight
        fm.descent = (fm.descent * ratio).roundToInt()
        fm.ascent = fm.descent - height
    }
}