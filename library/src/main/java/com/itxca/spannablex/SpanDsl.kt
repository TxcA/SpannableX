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
@file:Suppress("unused")

package com.itxca.spannablex

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.text.style.*
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.IntRange
import com.bumptech.glide.request.RequestOptions
import com.drake.spannable.span.CenterImageSpan
import com.drake.spannable.span.GlideImageSpan
import com.drake.spannable.span.MarginSpan
import com.itxca.spannablex.annotation.TextStyle
import com.itxca.spannablex.interfaces.OnSpanClickListener
import com.itxca.spannablex.span.*
import com.itxca.spannablex.span.LeadingMarginSpan
import com.itxca.spannablex.utils.DrawableSize
import com.itxca.spannablex.utils.color
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
     * ????????????Span
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
     * ????????????(???Spannable??????)
     */
    fun CharSequence?.text() {
        this?.let(spannableBuilder::append)
    }

    /**
     * ??? @receiver ????????????Span
     */
    fun CharSequence?.span(replaceRule: Any? = null, span: SpanDsl.() -> Unit = {}) {
        mixed(replaceRule, span)
    }

    /**
     * ??? @receiver ????????????Span
     */
    fun CharSequence?.mixed(replaceRule: Any? = null, span: SpanDsl.() -> Unit = {}) {
        spannableBuilder.append(
            create(this, replaceRule ?: globalReplaceRule).apply(span).spannable()
        )
    }

    /**
     * ??????(???????????????`\n`)
     */
    fun <T> T?.newline(@androidx.annotation.IntRange(from = 1L) lines: Int = 1): CharSequence? =
        kotlin.run {
            val newlines = if (lines > 1) {
                buildString {
                    repeat(lines) { append("\n") }
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
     * [StyleSpan] ??????????????????
     *
     * @param style ???????????? [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.style(
        @TextStyle style: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanStyle(style, replaceRule ?: globalReplaceRule) }

    /**
     * [TypefaceSpan] ??????????????????
     *
     * @param typeface ??????(API>=28)
     * @param family ?????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.typeface(
        typeface: Typeface? = null,
        family: String? = null,
        replaceRule: Any? = null
    ) = singleSpan { spanTypeface(typeface, family, replaceRule ?: globalReplaceRule) }

    /**
     * [TextAppearanceSpan] ??????????????????spanTypeface
     *
     * @param style ???????????? [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param size ????????????
     * @param color ????????????
     * @param family ?????????
     * @param linkColor ????????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.textAppearance(
        @TextStyle style: Int = Typeface.NORMAL,
        @Px size: Int = -1,
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
     * [ForegroundColorSpan] ????????????
     *
     * @param color ????????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.color(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanColor(color, replaceRule ?: globalReplaceRule) }

    /**
     * [ForegroundColorSpan] ????????????
     *
     * @param colorString ???????????? #RRGGBB #AARRGGBB
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.color(
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan { spanColor(colorString.color, replaceRule ?: globalReplaceRule) }

    /**
     * [BackgroundColorSpan] ????????????
     *
     * @param color ????????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.background(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanBackground(color, replaceRule ?: globalReplaceRule) }

    /**
     * [BackgroundColorSpan] ????????????
     *
     * @param colorString ???????????? #RRGGBB #AARRGGBB
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.background(
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan { spanBackground(colorString.color, replaceRule ?: globalReplaceRule) }

    /**
     * [CenterImageSpan] ??????
     *
     * @param drawable [Drawable]
     * @param source [Drawable] Uri
     * @param useTextViewSize ?????????????????????[TextView]????????????????????????[size]?????????????????????[useTextViewSize]
     * @param size ???????????? [DrawableSize]
     * @param marginLeft ???????????????
     * @param marginRight ???????????????
     * @param align ?????????????????? [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.image(
        drawable: Drawable,
        source: String? = null,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
        align: CenterImageSpan.Align = CenterImageSpan.Align.CENTER,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(
            drawable,
            source,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align,
            replaceRule ?: globalReplaceRule
        )
    }

    /**
     * [CenterImageSpan] ??????
     *
     * @param context [Context]
     * @param uri ?????? Uri
     * @param useTextViewSize ?????????????????????[TextView]????????????????????????[size]?????????????????????[useTextViewSize]
     * @param size ???????????? [DrawableSize]
     * @param marginLeft ???????????????
     * @param marginRight ???????????????
     * @param align ?????????????????? [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.image(
        context: Context,
        uri: Uri,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
        align: CenterImageSpan.Align = CenterImageSpan.Align.CENTER,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(
            context,
            uri,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align,
            replaceRule ?: globalReplaceRule
        )
    }

    /**
     * [CenterImageSpan] ??????
     *
     * @param context [Context]
     * @param resourceId ??????Id
     * @param useTextViewSize ?????????????????????[TextView]????????????????????????[size]?????????????????????[useTextViewSize]
     * @param size ???????????? [DrawableSize]
     * @param marginLeft ???????????????
     * @param marginRight ???????????????
     * @param align ?????????????????? [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.image(
        context: Context,
        @DrawableRes resourceId: Int,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
        align: CenterImageSpan.Align = CenterImageSpan.Align.CENTER,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(
            context,
            resourceId,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align,
            replaceRule ?: globalReplaceRule
        )
    }

    /**
     * [CenterImageSpan] ??????
     *
     * @param context [Context]
     * @param bitmap [Bitmap]
     * @param useTextViewSize ?????????????????????[TextView]????????????????????????[size]?????????????????????[useTextViewSize]
     * @param size ???????????? [DrawableSize]
     * @param marginLeft ???????????????
     * @param marginRight ???????????????
     * @param align ?????????????????? [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.image(
        context: Context,
        bitmap: Bitmap,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
        align: CenterImageSpan.Align = CenterImageSpan.Align.CENTER,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanImage(
            context,
            bitmap,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align,
            replaceRule ?: globalReplaceRule
        )
    }

    /**
     * [GlideImageSpan] ??????
     *
     * @param view ??????Span?????????[TextView], ????????????????????????????????????[TextView]??????
     * @param url ?????????????????? [Glide.with(view).load(url)]
     * @param useTextViewSize ?????????????????????[TextView]????????????????????????[size]?????????????????????[useTextViewSize]
     * @param size ???????????? [DrawableSize]
     * @param marginLeft ???????????????
     * @param marginRight ???????????????
     * @param align ?????????????????? [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.glide(
        view: TextView,
        url: Any,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
        align: GlideImageSpan.Align = GlideImageSpan.Align.CENTER,
        loopCount: Int? = null,
        requestOption: RequestOptions? = null,
        replaceRule: Any? = null,
    ) = singleSpan(replaceRule == null) {
        spanGlide(
            view,
            url,
            useTextViewSize,
            size,
            marginLeft,
            marginRight,
            align,
            loopCount,
            requestOption,
            replaceRule ?: globalReplaceRule
        )
    }

    /**
     * [ScaleXSpan] X???????????????
     *
     * @param proportion ??????(X???)????????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.scaleX(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ) = singleSpan {
        spanScaleX(proportion, replaceRule ?: globalReplaceRule)
    }

    /**
     * [MaskFilterSpan] ????????????????????????
     *
     * @param filter ???????????? [MaskFilter]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.maskFilter(
        filter: MaskFilter,
        replaceRule: Any? = null
    ) = singleSpan {
        spanMaskFilter(filter, replaceRule ?: globalReplaceRule)
    }

    /**
     * [BlurMaskFilter] ????????????????????????????????????
     *
     * @param radius ????????????
     * @param style ???????????? [BlurMaskFilter.Blur]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.blurMask(
        @FloatRange(from = 0.0) radius: Float,
        style: BlurMaskFilter.Blur = BlurMaskFilter.Blur.NORMAL,
        replaceRule: Any? = null
    ) = singleSpan {
        spanBlurMask(radius, style, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SuperscriptSpan] ?????????????????????
     *
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.superscript(replaceRule: Any? = null) = singleSpan {
        spanSuperscript(replaceRule ?: globalReplaceRule)
    }

    /**
     * [SubscriptSpan] ?????????????????????
     *
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.subscript(replaceRule: Any? = null) = singleSpan {
        spanSubscript(replaceRule ?: globalReplaceRule)
    }

    /**
     * [AbsoluteSizeSpan] ????????????????????????
     *
     * @param size ????????????
     * @param dp true = [size] dp, false = [size] px
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.absoluteSize(
        size: Int,
        dp: Boolean = true,
        replaceRule: Any? = null
    ) =
        singleSpan {
            spanAbsoluteSize(size, dp, replaceRule ?: globalReplaceRule)
        }

    /**
     * [RelativeSizeSpan] ????????????????????????
     *
     * @param proportion ??????????????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.relativeSize(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ) = singleSpan {
        spanRelativeSize(proportion, replaceRule ?: globalReplaceRule)
    }

    /**
     * [StrikethroughSpan] ?????????????????????
     *
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.strikethrough(replaceRule: Any? = null) = singleSpan {
        spanStrikethrough(replaceRule ?: globalReplaceRule)
    }

    /**
     * [UnderlineSpan] ?????????????????????
     *
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.underline(replaceRule: Any? = null) = singleSpan {
        spanUnderline(replaceRule ?: globalReplaceRule)
    }

    /**
     * [URLSpan] ?????????????????????
     *
     * ?????????[TextView.activateClick]??????
     * @param url ???????????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.url(url: String, replaceRule: Any? = null) = singleSpan {
        spanURL(url, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SuggestionSpan] ????????????????????????
     *
     * @param context [Context]
     * @param suggestions ????????????????????????
     * @param flags ???????????? [SuggestionSpan.FLAG_EASY_CORRECT] [SuggestionSpan.FLAG_MISSPELLED] [SuggestionSpan.FLAG_AUTO_CORRECTION]
     * @param locale ??????????????????
     * @param notificationTargetClass ????????????. ???????????????, ??????API<29?????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.suggestion(
        context: Context,
        suggestions: Array<String>,
        flags: Int = SuggestionSpan.FLAG_EASY_CORRECT or SuggestionSpan.FLAG_AUTO_CORRECTION,
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
     * [SimpleClickableSpan] ????????????????????????
     *
     * @param color ????????????
     * @param backgroundColor ????????????
     * @param style ???????????? [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param config ???????????? [SimpleClickableConfig]
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     * @param onClick [OnSpanClickListener] ????????????
     */
    fun Any?.clickable(
        @ColorInt color: Int? = null,
        @ColorInt backgroundColor: Int? = null,
        @TextStyle style: Int? = null,
        config: SimpleClickableConfig? = null,
        replaceRule: Any? = null,
        onClick: OnSpanClickListener? = null
    ) = singleSpan {
        spanClickable(
            color,
            backgroundColor,
            style,
            config,
            replaceRule ?: globalReplaceRule,
            onClick
        )
    }

    /**
     * [MarginSpan] ??????????????????
     *
     * @param width ????????????
     * @param color ??????????????????
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.margin(
        @Px width: Int,
        @ColorInt color: Int = Color.TRANSPARENT,
        replaceRule: Any? = null
    ) = singleSpan(replaceRule == null) {
        spanMargin(width, color, replaceRule)
    }

    /**
     * [MarginSpan] ??????????????????
     *
     * @param width ????????????
     * @param colorString ?????????????????? #RRGGBB #AARRGGBB
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun Any?.margin(
        @Px width: Int,
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan(replaceRule == null) {
        spanMargin(width, colorString.color, replaceRule)
    }

    /**
     * [QuoteSpan] ????????????????????????(?????????????????????)
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param color ????????????
     * @param stripeWidth ????????????
     * @param gapWidth ?????????????????????????????????
     */
    fun Any?.quote(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) stripeWidth: Int = 10,
        @Px @IntRange(from = 0) gapWidth: Int = 0
    ) = singleSpan {
        spanQuote(color, stripeWidth, gapWidth)
    }

    /**
     * [QuoteSpan] ????????????????????????(?????????????????????)
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param colorString ???????????? #RRGGBB #AARRGGBB
     * @param stripeWidth ????????????
     * @param gapWidth ?????????????????????????????????
     */
    fun Any?.quote(
        colorString: String,
        @IntRange(from = 0) stripeWidth: Int = 10,
        @IntRange(from = 0) gapWidth: Int = 0
    ) = singleSpan {
        spanQuote(colorString.color, stripeWidth, gapWidth)
    }

    /**
     * [BulletSpan] ????????????????????????(?????????????????????)
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param color ????????????
     * @param bulletRadius ????????????
     * @param gapWidth ?????????????????????????????????
     */
    fun Any?.bullet(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ) = singleSpan {
        spanBullet(color, bulletRadius, gapWidth)
    }

    /**
     * [BulletSpan] ????????????????????????(?????????????????????)
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param colorString ???????????? #RRGGBB #AARRGGBB
     * @param bulletRadius ????????????
     * @param gapWidth ?????????????????????????????????
     */
    fun Any?.bullet(
        colorString: String,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ) = singleSpan {
        spanBullet(colorString.color, bulletRadius, gapWidth)
    }

    /**
     * [AlignmentSpan] ????????????????????????
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param align [Layout.Alignment.ALIGN_NORMAL] [Layout.Alignment.ALIGN_CENTER] [Layout.Alignment.ALIGN_OPPOSITE]
     */
    fun Any?.alignment(
        align: Layout.Alignment
    ) = singleSpan {
        spanAlignment(align)
    }

    /**
     * [LineBackgroundSpan] ????????????????????????
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param color ????????????
     */
    fun Any?.lineBackground(
        @ColorInt color: Int,
    ) = singleSpan {
        spanLineBackground(color)
    }

    /**
     * [LineBackgroundSpan] ????????????????????????
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param colorString ???????????? #RRGGBB #AARRGGBB
     */
    fun Any?.lineBackground(
        colorString: String,
    ) = singleSpan {
        spanLineBackground(colorString.color)
    }

    /**
     * [LeadingMarginSpan] ????????????????????????
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param firstLines ????????????. ???[firstMargin]??????
     * @param firstMargin ???????????????(??????)
     * @param restMargin ?????????(?????????)?????????(??????)
     */
    fun Any?.leadingMargin(
        @IntRange(from = 1L) firstLines: Int,
        @Px firstMargin: Int,
        @Px restMargin: Int = 0
    ) = singleSpan {
        spanLeadingMargin(firstLines, firstMargin, restMargin)
    }

    /**
     * [LineHeightSpan] ??????????????????
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param height ??????
     */
    fun Any?.lineHeight(
        @Px @IntRange(from = 1L) height: Int
    ) = singleSpan {
        spanLineHeight(height)
    }

    /**
     * [ParagraphBitmapSpan] ??????????????????
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param bitmap [Bitmap]
     * @param padding ????????????????????????
     * @param useTextViewSize ?????????????????????[TextView]????????????????????????[size]?????????????????????[useTextViewSize]
     * @param size ???????????? [DrawableSize]
     */
    fun Any?.imageParagraph(
        bitmap: Bitmap,
        @Px padding: Int = 0,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null
    ) = singleSpan {
        spanImageParagraph(bitmap, padding, useTextViewSize, size)
    }

    /**
     * [ParagraphDrawableSpan] ??????????????????
     *
     * [ParagraphStyle] ??????Style?????????????????????
     * @param drawable [Drawable]
     * @param padding ????????????????????????
     * @param useTextViewSize ?????????????????????[TextView]????????????????????????[size]?????????????????????[useTextViewSize]
     * @param size ???????????? [DrawableSize]
     */
    fun Any?.imageParagraph(
        drawable: Drawable,
        @Px padding: Int = 0,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null
    ) = singleSpan {
        spanImageParagraph(drawable, padding, useTextViewSize, size)
    }

    /**
     * ?????????????????????
     *
     * @param style ???????????????. eg. spanCustom(ForegroundColorSpan(Color.RED))
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     */
    fun <T : CharacterStyle> Any?.custom(
        style: T,
        replaceRule: Any? = null,
    ) = singleSpan {
        spanCustom(style, replaceRule)
    }

    /**
     * ?????????????????????
     *
     * @param style ???????????????. eg. spanCustom(LineBackgroundSpan.Standard(Color.Red))
     * @param replaceRule ?????????????????? [String] [Regex] [ReplaceRule]
     * ??????????????????????????????, [ParagraphStyle] ??????????????? [replaceRule] ??????????????????????????????
     */
    fun <T : ParagraphStyle> Any?.custom(
        style: T,
        replaceRule: Any? = null,
    ) = singleSpan {
        spanCustom(style, replaceRule)
    }

    companion object {
        /**
         * @see [SpanDsl]
         */
        fun create(text: CharSequence?, replaceRule: Any?): SpanDsl =
            SpanDsl(text, replaceRule)
    }
}

