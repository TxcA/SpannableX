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

package com.itxca.spannablex

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.MaskFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.text.style.*
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import com.itxca.spannablex.annotation.TextStyle
import com.itxca.spannablex.interfaces.OnSpanClickListener
import com.itxca.spannablex.span.SimpleClickableConfig
import com.itxca.spannablex.span.SimpleClickableSpan
import com.itxca.spannablex.span.SizeImageSpan
import com.itxca.spannablex.utils.DrawableSize
import java.util.*

/**
 * DSL Spannable
 *
 * sample: ```kotlin
 * TextView.text = spannable {
 *                   "this is real text.".text()
 *                   "spannable".color(Color.BLUE).style(Typeface.BOLD)
 *                 }
 * ```
 */
class SpanDsl private constructor(
    private val text: CharSequence?,
    private val globalReplaceRule: Any?
) {

    /**
     * [text] SpannedString
     */
    var textSpannable: Spanned? = text?.let {
        when (text) {
            is Spanned -> text
            else -> SpannedString(text)
        }
    }
        private set

    private val spannableBuilder = SpannableStringBuilder()

    internal fun spannable(): SpannableStringBuilder =
        spannableBuilder.apply { textSpannable?.let { ts -> insert(0, ts) } }

    /**
     * 设置单个Span
     */
    private fun Any?.singleSpan(
        autoPlaceholder: Boolean = false,
        span: CharSequence.() -> Spanned
    ) {
        when {
            this is CharSequence -> {
                spannableBuilder.append(span.invoke(this))
            }
            autoPlaceholder -> {
                spannableBuilder.append(span.invoke(IMAGE_SPAN_TAG))
            }
            textSpannable != null -> {
                textSpannable = span.invoke(textSpannable!!)
            }
        }
    }

    /**
     * 添加文本(无Spannable效果)
     */
    fun CharSequence?.text() {
        this?.let(spannableBuilder::append)
    }

    /**
     * 为 @receiver 设置多个Span
     */
    fun CharSequence?.span(replaceRule: Any? = null, span: SpanDsl.() -> Unit = {}) {
        spannableBuilder.append(
            create(this, replaceRule ?: globalReplaceRule).apply(span).spannable()
        )
    }

    /**
     * 换行(可自行处理`\n`)
     */
    fun <T> T?.newline(@androidx.annotation.IntRange(from = 1L) lines: Int = 1): CharSequence? = kotlin.run {
        val newlines = if (lines > 1){
            buildString {
                repeat(lines){ append("\n") }
            }
        } else "\n"
        when (this) {
            is SpannableStringBuilder -> append(newlines)
            is Spanned -> SpannableStringBuilder(this).append(newlines)
            is String -> "${this}$newlines"
            is CharSequence -> "${this}$newlines"
            else -> {
                spannableBuilder.append(newlines)
                null
            }
        }
    }

    /**
     * [StyleSpan] 设置文本样式
     * @see [CharSequence.spanStyle]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.style(
        @TextStyle style: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanStyle(style, replaceRule ?: globalReplaceRule) }

    /**
     * [TypefaceSpan] 设置字体样式
     * @see [CharSequence.spanTypeface]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.typeface(
        typeface: Typeface? = null,
        family: String? = null,
        replaceRule: Any? = null
    ) = singleSpan { spanTypeface(typeface, family, replaceRule ?: globalReplaceRule) }

    /**
     * [TextAppearanceSpan] 设置字体效果
     * @see [CharSequence.spanTextAppearance]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.textAppearance(
        @TextStyle style: Int = Typeface.NORMAL,
        size: Int = -1,
        @ColorInt color: Int? = null,
        family: String? = null,
        linkColor: ColorStateList? = null,
        replaceRule: Any? = null
    ) = singleSpan {
        spanTextAppearance(
            style,
            size,
            color,
            family,
            linkColor,
            replaceRule ?: globalReplaceRule
        )
    }

    /**
     * [ForegroundColorSpan] 文本颜色
     * @see [CharSequence.spanColor]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.color(
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan { spanColor(colorString, replaceRule ?: globalReplaceRule) }

    /**
     * [ForegroundColorSpan] 文本颜色
     * @see [CharSequence.spanColor]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.color(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanColor(color, replaceRule ?: globalReplaceRule) }

    /**
     * [BackgroundColorSpan] 背景颜色
     * @see [CharSequence.spanBackground]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.background(
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan { spanBackground(colorString, replaceRule ?: globalReplaceRule) }

    /**
     * [BackgroundColorSpan] 背景颜色
     * @see [CharSequence.spanBackground]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.background(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanBackground(color, replaceRule ?: globalReplaceRule) }

    /**
     * [SizeImageSpan] 图片
     * @see [CharSequence.image]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.image(
        drawable: Drawable,
        source: String? = null,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(drawable, source, useTextViewSize, size, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SizeImageSpan] 图片
     * @see [CharSequence.image]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.image(
        context: Context,
        uri: Uri,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(context, uri, useTextViewSize, size, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SizeImageSpan] 图片
     * @see [CharSequence.image]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.image(
        context: Context,
        @DrawableRes resourceId: Int,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(context, resourceId, useTextViewSize, size, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SizeImageSpan] 图片
     * @see [CharSequence.image]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.image(
        context: Context,
        bitmap: Bitmap,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(context, bitmap, useTextViewSize, size, replaceRule ?: globalReplaceRule)
    }

    /**
     * [ScaleXSpan] X轴文本缩放
     * @see [CharSequence.spanScaleX]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.scaleX(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ) = singleSpan {
        spanScaleX(proportion, replaceRule ?: globalReplaceRule)
    }

    /**
     * [MaskFilterSpan] 文本蒙版效果
     * @see [CharSequence.spanMaskFilter]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.maskFilter(
        filter: MaskFilter,
        replaceRule: Any? = null
    ) = singleSpan {
        spanMaskFilter(filter, replaceRule ?: globalReplaceRule)
    }

    /**
     * [BlurMaskFilter] 文本模糊滤镜蒙版效果
     * @see [CharSequence.spanBlurMask]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.blurMask(
        @FloatRange(from = 0.0) radius: Float,
        style: BlurMaskFilter.Blur = BlurMaskFilter.Blur.NORMAL,
        replaceRule: Any? = null
    ) = singleSpan {
        spanBlurMask(radius, style, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SuperscriptSpan] 文本上标
     * @see [CharSequence.spanSuperscript]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.superscript(replaceRule: Any? = null) = singleSpan {
        spanSuperscript(replaceRule ?: globalReplaceRule)
    }

    /**
     * [SubscriptSpan] 文本下标
     * @see [CharSequence.spanSubscript]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.subscript(replaceRule: Any? = null) = singleSpan {
        spanSubscript(replaceRule ?: globalReplaceRule)
    }

    /**
     * [AbsoluteSizeSpan] 文本绝对大小
     * @see [CharSequence.spanAbsoluteSize]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.absoluteSize(
        size: Int, dip:
        Boolean = true,
        replaceRule: Any? = null
    ) =
        singleSpan {
            spanAbsoluteSize(size, dip, replaceRule ?: globalReplaceRule)
        }

    /**
     * [RelativeSizeSpan] 文本相对大小
     * @see [CharSequence.spanRelativeSize]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.relativeSize(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ) = singleSpan {
        spanRelativeSize(proportion, replaceRule ?: globalReplaceRule)
    }

    /**
     * [StrikethroughSpan] 删除线
     * @see [CharSequence.spanStrikethrough]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.strikethrough(replaceRule: Any? = null) = singleSpan {
        spanStrikethrough(replaceRule ?: globalReplaceRule)
    }

    /**
     * [UnderlineSpan] 下划线
     * @see [CharSequence.spanUnderline]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.underline(replaceRule: Any? = null) = singleSpan {
        spanUnderline(replaceRule ?: globalReplaceRule)
    }

    /**
     * [URLSpan] 超链接
     * @see [CharSequence.spanURL]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.url(url: String, replaceRule: Any? = null) = singleSpan {
        spanURL(url, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SuggestionSpan] 文本输入提示
     * @see [CharSequence.spanSuggestion]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.suggestion(
        context: Context,
        suggestions: Array<String>,
        flags: Int = SuggestionSpan.SUGGESTIONS_MAX_SIZE,
        locale: Locale? = null,
        notificationTargetClass: Class<*>? = null,
        replaceRule: Any? = null
    ) = singleSpan {
        spanSuggestion(
            context,
            suggestions,
            flags,
            locale,
            notificationTargetClass,
            replaceRule ?: globalReplaceRule
        )
    }

    /**
     * [SimpleClickableSpan] 可点击文本
     * @see [CharSequence.spanClickable]
     *
     * @param replaceRule [ReplaceRule] 替换规则
     */
    fun Any?.clickable(
        @ColorInt color: Int? = null,
        @ColorInt backgroundColor: Int? = null,
        @TextStyle typeStyle: Int? = null,
        config: SimpleClickableConfig? = null,
        replaceRule: Any? = null,
        onClick: OnSpanClickListener? = null
    ) = singleSpan {
        spanClickable(
            color,
            backgroundColor,
            typeStyle,
            config,
            replaceRule ?: globalReplaceRule,
            onClick
        )
    }

    companion object {
        /**
         * @see [SpanDsl]
         */
        fun create(text: CharSequence?, replaceRule: Any?): SpanDsl =
            SpanDsl(text, replaceRule)
    }
}

