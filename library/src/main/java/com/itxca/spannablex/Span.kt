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
@file:JvmName("Span")
@file:Suppress("unused")

package com.itxca.spannablex

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.MaskFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.*
import android.text.style.CharacterStyle
import android.text.style.ParagraphStyle
import android.text.style.SuggestionSpan
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.core.text.buildSpannedString
import com.bumptech.glide.request.RequestOptions
import com.drake.spannable.span.CenterImageSpan
import com.drake.spannable.span.GlideImageSpan
import com.itxca.spannablex.annotation.ConversionUnit
import com.itxca.spannablex.annotation.TextStyle
import com.itxca.spannablex.interfaces.OnSpanClickListener
import com.itxca.spannablex.interfaces.OnSpanReplacementMatch
import com.itxca.spannablex.span.SimpleClickableConfig
import com.itxca.spannablex.utils.*
import java.util.*

/**
 * <p>Chain Spannable</p>
 * sample:
 * <br/>
 * <br/>TextView.setText(Span.create()
 * <br/>                  .text("this is real text.")
 * <br/>                  .text("spannable").color(Color.BLUE).style(Typeface.BOLD)
 * <br/>                  .spannable())
 */
class Span private constructor() {

    private val spannableBuilder = SpannableStringBuilder()
    private var spannableCache: Spannable? = null

    private val Spannable?.isNotNullAndEmpty: Boolean
        get() = this != null && this.isNotEmpty()

    private fun runOnSelf(block: () -> Spannable?): Span = apply {
        block.invoke()?.let { spannableCache = it }
    }

    private fun checkImageSpan(autoPlaceholder: Boolean = false) {
        if (autoPlaceholder) {
            saveCache()
            spannableCache = SpannableString(" ")
        }
    }

    /**
     * 保存当前 [text] spannable(大部分情况不需要手动调用)
     */
    fun saveCache(): Span = apply {
        if (spannableCache.isNotNullAndEmpty) {
            spannableBuilder.append(spannableCache)
        }
    }

    /**
     * 插入待处理字符串
     * 在使用[style] [typeface] [color]... 等等之前，需调用该方法插入当前需要处理的字符串
     */
    fun text(text: CharSequence): Span = apply {
        saveCache()
        spannableCache = if (text is Spannable) {
            text
        } else SpannableString(text)
    }

    /**
     * 换行(可自行处理`\n`)
     */
    @JvmOverloads
    fun newline(@androidx.annotation.IntRange(from = 1L) lines: Int = 1): Span = apply {
        val newlines = if (lines > 1) {
            buildString {
                repeat(lines) { append("\n") }
            }
        } else "\n"
        when (val cache = spannableCache) {
            is SpannableStringBuilder -> cache.append(newlines)
            is Spanned -> spannableCache = SpannableStringBuilder(cache).append(newlines)
            is CharSequence -> spannableCache = SpannableString(cache + newlines)
            else -> spannableBuilder.append(newlines)
        }
    }

    /**
     * 构建Spannable
     */
    fun spannable(): CharSequence {
        saveCache()
        spannableCache = null
        return SpannedString(spannableBuilder)
    }

    /**
     * @see [CharSequence.spanStyle]
     */
    @JvmOverloads
    fun style(
        @TextStyle style: Int,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanStyle(style, replaceRule) }

    /**
     * @see [CharSequence.spanTypeface]
     */
    @JvmOverloads
    fun typeface(
        typeface: Typeface? = null,
        family: String? = null,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanTypeface(typeface, family, replaceRule) }

    /**
     * @see [CharSequence.spanTextAppearance]
     */
    @JvmOverloads
    fun textAppearance(
        @TextStyle style: Int = Typeface.NORMAL,
        size: Int = -1,
        @ColorInt color: Int? = null,
        family: String? = null,
        linkColor: ColorStateList? = null,
        replaceRule: Any? = null
    ): Span = runOnSelf {
        spannableCache?.spanTextAppearance(
            style,
            size,
            color,
            family,
            linkColor,
            replaceRule
        )
    }

    /**
     * @see [CharSequence.spanColor]
     */
    @JvmOverloads
    fun color(
        colorString: String,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanColor(colorString, replaceRule) }


    /**
     * @see [CharSequence.spanColor]
     */
    @JvmOverloads
    fun color(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanColor(color, replaceRule) }

    /**
     * @see [CharSequence.spanBackground]
     */
    @JvmOverloads
    fun background(
        colorString: String,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanBackground(colorString, replaceRule) }

    /**
     * @see [CharSequence.spanBackground]
     */
    @JvmOverloads
    fun background(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanBackground(color, replaceRule) }

    /**
     * @see [CharSequence.spanImage]
     */
    @JvmOverloads
    fun image(
        drawable: Drawable,
        source: String? = null,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        marginLeft: Int? = null,
        marginRight: Int? = null,
        align: CenterImageSpan.Align? = null,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        checkImageSpan(replaceRule == null)
        spannableCache?.spanImage(
            drawable,
            source,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align ?: CenterImageSpan.Align.CENTER,
            replaceRule
        )
    }

    /**
     * @see [CharSequence.spanImage]
     */
    @JvmOverloads
    fun image(
        context: Context,
        uri: Uri,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        marginLeft: Int? = null,
        marginRight: Int? = null,
        align: CenterImageSpan.Align? = null,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        checkImageSpan(replaceRule == null)
        spannableCache?.spanImage(
            context,
            uri,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align ?: CenterImageSpan.Align.CENTER,
            replaceRule
        )
    }

    /**
     * @see [CharSequence.spanImage]
     */
    @JvmOverloads
    fun image(
        context: Context,
        @DrawableRes resourceId: Int,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        marginLeft: Int? = null,
        marginRight: Int? = null,
        align: CenterImageSpan.Align? = null,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        checkImageSpan(replaceRule == null)
        spannableCache?.spanImage(
            context,
            resourceId,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align ?: CenterImageSpan.Align.CENTER,
            replaceRule
        )
    }

    /**
     * @see [CharSequence.spanImage]
     */
    @JvmOverloads
    fun image(
        context: Context,
        bitmap: Bitmap,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        marginLeft: Int? = null,
        marginRight: Int? = null,
        align: CenterImageSpan.Align? = null,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        checkImageSpan(replaceRule == null)
        spannableCache?.spanImage(
            context,
            bitmap,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align ?: CenterImageSpan.Align.CENTER,
            replaceRule
        )
    }

    /**
     * @see [CharSequence.spanGlide]
     */
    @JvmOverloads
    fun glide(
        view: TextView,
        url: Any,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        marginLeft: Int? = null,
        marginRight: Int? = null,
        align: GlideImageSpan.Align? = null,
        loopCount: Int? = null,
        requestOption: RequestOptions? = null,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        checkImageSpan(replaceRule == null)
        spannableCache?.spanGlide(
            view,
            url,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align ?: GlideImageSpan.Align.CENTER,
            loopCount,
            requestOption,
            replaceRule
        )
    }

    /**
     * @see [CharSequence.spanScaleX]
     */
    @JvmOverloads
    fun scaleX(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanScaleX(proportion, replaceRule) }


    /**
     * @see [CharSequence.spanMaskFilter]
     */
    @JvmOverloads
    fun maskFilter(
        filter: MaskFilter,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanMaskFilter(filter, replaceRule) }

    /**
     * @see [CharSequence.spanBlurMask]
     */
    @JvmOverloads
    fun blurMask(
        @FloatRange(from = 0.0) radius: Float,
        style: BlurMaskFilter.Blur? = null,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanBlurMask(radius, style, replaceRule) }

    /**
     * @see [CharSequence.spanSuperscript]
     */
    @JvmOverloads
    fun superscript(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanSuperscript(replaceRule) }

    /**
     * @see [CharSequence.spanSubscript]
     */
    @JvmOverloads
    fun subscript(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanSubscript(replaceRule) }

    /**
     * @see [CharSequence.spanAbsoluteSize]
     */
    @JvmOverloads
    fun absoluteSize(
        size: Int,
        dip: Boolean = true,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanAbsoluteSize(size, dip, replaceRule) }

    /**
     * @see [CharSequence.spanRelativeSize]
     */
    @JvmOverloads
    fun relativeSize(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanRelativeSize(proportion, replaceRule) }

    /**
     * @see [CharSequence.spanStrikethrough]
     */
    @JvmOverloads
    fun strikethrough(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanStrikethrough(replaceRule) }

    /**
     * @see [CharSequence.spanUnderline]
     */
    @JvmOverloads
    fun underline(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanUnderline(replaceRule) }

    /**
     * @see [CharSequence.spanURL]
     */
    @JvmOverloads
    fun url(
        url: String,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanURL(url, replaceRule) }

    /**
     * @see [CharSequence.spanSuggestion]
     */
    @JvmOverloads
    fun suggestion(
        context: Context,
        suggestions: Array<String>,
        flags: Int = SuggestionSpan.SUGGESTIONS_MAX_SIZE,
        locale: Locale? = null,
        notificationTargetClass: Class<*>? = null,
        replaceRule: Any? = null
    ): Span = runOnSelf {
        spannableCache?.spanSuggestion(
            context,
            suggestions = suggestions,
            flags,
            locale,
            notificationTargetClass,
            replaceRule
        )
    }

    /**
     * @see [CharSequence.spanClickable]
     */
    @JvmOverloads
    fun clickable(
        @ColorInt color: Int? = null,
        @ColorInt backgroundColor: Int? = null,
        @TextStyle typeStyle: Int? = null,
        config: SimpleClickableConfig? = null,
        replaceRule: Any? = null,
        onClick: OnSpanClickListener? = null
    ): Span = runOnSelf {
        spannableCache?.spanClickable(
            color,
            backgroundColor,
            typeStyle,
            config,
            replaceRule,
            onClick
        )
    }

    @JvmOverloads
    fun quote(
        colorString: String,
        @IntRange(from = 0) stripeWidth: Int = 10,
        @IntRange(from = 0) gapWidth: Int = 0
    ): Span = runOnSelf {
        spannableCache?.spanQuote(colorString.color, stripeWidth, gapWidth)
    }

    @JvmOverloads
    fun quote(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) stripeWidth: Int = 10,
        @Px @IntRange(from = 0) gapWidth: Int = 0
    ): Span = runOnSelf {
        spannableCache?.spanQuote(color, stripeWidth, gapWidth)
    }

    @JvmOverloads
    fun bullet(
        colorString: String,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ): Span = runOnSelf {
        spannableCache?.spanBullet(colorString.color, bulletRadius, gapWidth)
    }

    @JvmOverloads
    fun bullet(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ): Span = runOnSelf {
        spannableCache?.spanBullet(color, bulletRadius, gapWidth)
    }

    fun alignment(
        align: Layout.Alignment
    ): Span = runOnSelf {
        spannableCache?.spanAlignment(align)
    }

    fun lineBackground(
        @ColorInt color: Int
    ): Span = runOnSelf {
        spannableCache?.spanLineBackground(color)
    }

    fun lineBackground(
        colorString: String
    ): Span = runOnSelf {
        spannableCache?.spanLineBackground(colorString.color)
    }

    @JvmOverloads
    fun leadingMargin(
        @IntRange(from = 1L) firstLines: Int,
        @Px firstMargin: Int,
        @Px restMargin: Int = 0
    ): Span = runOnSelf {
        spannableCache?.spanLeadingMargin(firstLines, firstMargin, restMargin)
    }

    fun lineHeight(
        @Px @IntRange(from = 1L) height: Int
    ): Span = runOnSelf {
        spannableCache?.spanLineHeight(height)
    }

    @JvmOverloads
    fun imageParagraph(
        bitmap: Bitmap,
        @Px padding: Int = 0,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null
    ): Span = runOnSelf {
        spannableCache?.spanImageParagraph(bitmap, padding, useTextViewSize, size)
    }

    @JvmOverloads
    fun imageParagraph(
        drawable: Drawable,
        @Px padding: Int = 0,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null
    ): Span = runOnSelf {
        spannableCache?.spanImageParagraph(drawable, padding, useTextViewSize, size)
    }

    @JvmOverloads
    fun <T : CharacterStyle> custom(
        style: T,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        spannableCache?.spanCustom(style, replaceRule)
    }

    @JvmOverloads
    fun <T : ParagraphStyle> custom(
        style: T,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        spannableCache?.spanCustom(style, replaceRule)
    }

    companion object {
        /**
         * 构建Span
         * @see [Span]
         */
        @JvmStatic
        fun create(): Span = Span()

        //<editor-fold desc="Java 适配">
        /**
         * 适配Java不支持CharSequence operator plus
         * eg. `SpanExtension.spannedString(spanImage(...),spanColor(..))`
         */
        @JvmStatic
        fun spannedString(vararg texts: CharSequence): SpannedString = buildSpannedString {
            texts.forEach(this::append)
        }

        /**
         * 兼容Java 适配
         * @see [toReplaceRule]
         */
        @JvmStatic
        @JvmOverloads
        fun toReplaceRule(
            replaceString: String,
            isRegex: Boolean = false,
            matchIndex: Int? = null,
            matchRange: kotlin.ranges.IntRange? = null,
            newString: CharSequence? = null,
            replacementMatch: OnSpanReplacementMatch? = null
        ): ReplaceRule =
            replaceString.toReplaceRule(
                isRegex,
                matchIndex,
                matchRange,
                newString,
                replacementMatch
            )

        @JvmStatic
        @JvmOverloads
        fun drawableSize(
            size: Int,
            @ConversionUnit unit: Int = ConversionUnit.NOT_CONVERT,
        ): DrawableSize =
            size.let {
                when (unit) {
                    ConversionUnit.SP -> it.sp
                    ConversionUnit.DP -> it.dp
                    else -> it
                }
            }.drawableSize

        @JvmStatic
        fun sp(value: Int): Int = value.sp

        @JvmStatic
        fun dp(value: Int): Int = value.dp

        @JvmStatic
        fun removeAllSpans(span: Spannable) {
            span.removeAllSpans()
        }

        @JvmStatic
        fun removeSpans(text: CharSequence, type: Class<*>): CharSequence =
            (if (text is Spannable) text else SpannableString(text)).apply {
                val allSpans = getSpans(0, length, type)
                for (span in allSpans) {
                    removeSpan(span)
                }
            }

        /**
         * 兼容Java 适配
         * @see [TextView.activateClick]
         */
        @JvmStatic
        @JvmOverloads
        fun activateClick(textView: TextView, background: Boolean = true): TextView =
            textView.activateClick(background)
        //</editor-fold>
    }
}