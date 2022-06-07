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
        mixed(replaceRule, span)
    }

    /**
     * 为 @receiver 设置多个Span
     */
    fun CharSequence?.mixed(replaceRule: Any? = null, span: SpanDsl.() -> Unit = {}) {
        spannableBuilder.append(
            create(this, replaceRule ?: globalReplaceRule).apply(span).spannable()
        )
    }

    /**
     * 换行(可自行处理`\n`)
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
     * [StyleSpan] 设置文本样式
     *
     * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.style(
        @TextStyle style: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanStyle(style, replaceRule ?: globalReplaceRule) }

    /**
     * [TypefaceSpan] 设置字体样式
     *
     * @param typeface 字体(API>=28)
     * @param family 字体集
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.typeface(
        typeface: Typeface? = null,
        family: String? = null,
        replaceRule: Any? = null
    ) = singleSpan { spanTypeface(typeface, family, replaceRule ?: globalReplaceRule) }

    /**
     * [TextAppearanceSpan] 设置字体效果spanTypeface
     *
     * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param size 文本大小
     * @param color 文本颜色
     * @param family 字体集
     * @param linkColor 链接颜色
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [ForegroundColorSpan] 文本颜色
     *
     * @param color 文本颜色
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.color(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanColor(color, replaceRule ?: globalReplaceRule) }

    /**
     * [ForegroundColorSpan] 文本颜色
     *
     * @param colorString 文本颜色 #RRGGBB #AARRGGBB
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.color(
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan { spanColor(colorString.color, replaceRule ?: globalReplaceRule) }

    /**
     * [BackgroundColorSpan] 背景颜色
     *
     * @param color 背景颜色
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.background(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ) = singleSpan { spanBackground(color, replaceRule ?: globalReplaceRule) }

    /**
     * [BackgroundColorSpan] 背景颜色
     *
     * @param colorString 背景颜色 #RRGGBB #AARRGGBB
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.background(
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan { spanBackground(colorString.color, replaceRule ?: globalReplaceRule) }

    /**
     * [CenterImageSpan] 图片
     *
     * @param drawable [Drawable]
     * @param source [Drawable] Uri
     * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
     * @param size 图片大小 [DrawableSize]
     * @param marginLeft 图片左边距
     * @param marginRight 图片右边距
     * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [CenterImageSpan] 图片
     *
     * @param context [Context]
     * @param uri 图片 Uri
     * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
     * @param size 图片大小 [DrawableSize]
     * @param marginLeft 图片左边距
     * @param marginRight 图片右边距
     * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [CenterImageSpan] 图片
     *
     * @param context [Context]
     * @param resourceId 图片Id
     * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
     * @param size 图片大小 [DrawableSize]
     * @param marginLeft 图片左边距
     * @param marginRight 图片右边距
     * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [CenterImageSpan] 图片
     *
     * @param context [Context]
     * @param bitmap [Bitmap]
     * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
     * @param size 图片大小 [DrawableSize]
     * @param marginLeft 图片左边距
     * @param marginRight 图片右边距
     * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [GlideImageSpan] 图片
     *
     * @param view 当前Span所在的[TextView], 用于异步加载完图片后通知[TextView]刷新
     * @param url 图片地址参见 [Glide.with(view).load(url)]
     * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
     * @param size 图片大小 [DrawableSize]
     * @param marginLeft 图片左边距
     * @param marginRight 图片右边距
     * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [ScaleXSpan] X轴文本缩放
     *
     * @param proportion 水平(X轴)缩放比例
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.scaleX(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ) = singleSpan {
        spanScaleX(proportion, replaceRule ?: globalReplaceRule)
    }

    /**
     * [MaskFilterSpan] 设置文本蒙版效果
     *
     * @param filter 蒙版效果 [MaskFilter]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.maskFilter(
        filter: MaskFilter,
        replaceRule: Any? = null
    ) = singleSpan {
        spanMaskFilter(filter, replaceRule ?: globalReplaceRule)
    }

    /**
     * [BlurMaskFilter] 设置文本模糊滤镜蒙版效果
     *
     * @param radius 模糊半径
     * @param style 模糊效果 [BlurMaskFilter.Blur]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.blurMask(
        @FloatRange(from = 0.0) radius: Float,
        style: BlurMaskFilter.Blur = BlurMaskFilter.Blur.NORMAL,
        replaceRule: Any? = null
    ) = singleSpan {
        spanBlurMask(radius, style, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SuperscriptSpan] 设置文本为上标
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.superscript(replaceRule: Any? = null) = singleSpan {
        spanSuperscript(replaceRule ?: globalReplaceRule)
    }

    /**
     * [SubscriptSpan] 设置文本为下标
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.subscript(replaceRule: Any? = null) = singleSpan {
        spanSubscript(replaceRule ?: globalReplaceRule)
    }

    /**
     * [AbsoluteSizeSpan] 设置文本绝对大小
     *
     * @param size 文本大小
     * @param dp true = [size] dp, false = [size] px
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [RelativeSizeSpan] 设置文本相对大小
     *
     * @param proportion 文本缩放比例
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.relativeSize(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ) = singleSpan {
        spanRelativeSize(proportion, replaceRule ?: globalReplaceRule)
    }

    /**
     * [StrikethroughSpan] 设置文本删除线
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.strikethrough(replaceRule: Any? = null) = singleSpan {
        spanStrikethrough(replaceRule ?: globalReplaceRule)
    }

    /**
     * [UnderlineSpan] 设置文本下划线
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.underline(replaceRule: Any? = null) = singleSpan {
        spanUnderline(replaceRule ?: globalReplaceRule)
    }

    /**
     * [URLSpan] 设置文本超链接
     *
     * 需配合[TextView.activateClick]使用
     * @param url 超链接地址
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.url(url: String, replaceRule: Any? = null) = singleSpan {
        spanURL(url, replaceRule ?: globalReplaceRule)
    }

    /**
     * [SuggestionSpan] 设置文本输入提示
     *
     * @param context [Context]
     * @param suggestions 提示规则文本数组
     * @param flags 提示规则 [SuggestionSpan.FLAG_EASY_CORRECT] [SuggestionSpan.FLAG_MISSPELLED] [SuggestionSpan.FLAG_AUTO_CORRECTION]
     * @param locale 语言区域设置
     * @param notificationTargetClass 通知目标. 基本已废弃, 只在API<29时生效
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
     * [SimpleClickableSpan] 设置文本点击效果
     *
     * @param color 文本颜色
     * @param backgroundColor 背景颜色
     * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param config 附加配置 [SimpleClickableConfig]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     * @param onClick [OnSpanClickListener] 点击回调
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
     * [MarginSpan] 设置文本间距
     *
     * @param width 文本间距
     * @param color 间距填充颜色
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.margin(
        @Px width: Int,
        @ColorInt color: Int = Color.TRANSPARENT,
        replaceRule: Any? = null
    ) = singleSpan(replaceRule == null) {
        spanMargin(width, color, replaceRule)
    }

    /**
     * [MarginSpan] 设置文本间距
     *
     * @param width 文本间距
     * @param colorString 间距填充颜色 #RRGGBB #AARRGGBB
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun Any?.margin(
        @Px width: Int,
        colorString: String,
        replaceRule: Any? = null
    ) = singleSpan(replaceRule == null) {
        spanMargin(width, colorString.color, replaceRule)
    }

    /**
     * [QuoteSpan] 设置段落引用样式(段落前竖线标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param color 竖线颜色
     * @param stripeWidth 竖线宽度
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    fun Any?.quote(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) stripeWidth: Int = 10,
        @Px @IntRange(from = 0) gapWidth: Int = 0
    ) = singleSpan {
        spanQuote(color, stripeWidth, gapWidth)
    }

    /**
     * [QuoteSpan] 设置段落引用样式(段落前竖线标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param colorString 竖线颜色 #RRGGBB #AARRGGBB
     * @param stripeWidth 竖线宽度
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    fun Any?.quote(
        colorString: String,
        @IntRange(from = 0) stripeWidth: Int = 10,
        @IntRange(from = 0) gapWidth: Int = 0
    ) = singleSpan {
        spanQuote(colorString.color, stripeWidth, gapWidth)
    }

    /**
     * [BulletSpan] 设置段落项目符号(段落前圆形标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param color 圆形颜色
     * @param bulletRadius 圆形半径
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    fun Any?.bullet(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ) = singleSpan {
        spanBullet(color, bulletRadius, gapWidth)
    }

    /**
     * [BulletSpan] 设置段落项目符号(段落前圆形标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param colorString 圆形颜色 #RRGGBB #AARRGGBB
     * @param bulletRadius 圆形半径
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    fun Any?.bullet(
        colorString: String,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ) = singleSpan {
        spanBullet(colorString.color, bulletRadius, gapWidth)
    }

    /**
     * [AlignmentSpan] 设置段落对齐方式
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param align [Layout.Alignment.ALIGN_NORMAL] [Layout.Alignment.ALIGN_CENTER] [Layout.Alignment.ALIGN_OPPOSITE]
     */
    fun Any?.alignment(
        align: Layout.Alignment
    ) = singleSpan {
        spanAlignment(align)
    }

    /**
     * [LineBackgroundSpan] 设置段落背景颜色
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param color 背景颜色
     */
    fun Any?.lineBackground(
        @ColorInt color: Int,
    ) = singleSpan {
        spanLineBackground(color)
    }

    /**
     * [LineBackgroundSpan] 设置段落背景颜色
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param colorString 背景颜色 #RRGGBB #AARRGGBB
     */
    fun Any?.lineBackground(
        colorString: String,
    ) = singleSpan {
        spanLineBackground(colorString.color)
    }

    /**
     * [LeadingMarginSpan] 设置段落文本缩进
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param firstLines 首行行数. 与[firstMargin]关联
     * @param firstMargin 首行左边距(缩进)
     * @param restMargin 剩余行(非首行)左边距(缩进)
     */
    fun Any?.leadingMargin(
        @IntRange(from = 1L) firstLines: Int,
        @Px firstMargin: Int,
        @Px restMargin: Int = 0
    ) = singleSpan {
        spanLeadingMargin(firstLines, firstMargin, restMargin)
    }

    /**
     * [LineHeightSpan] 设置段落行高
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param height 行高
     */
    fun Any?.lineHeight(
        @Px @IntRange(from = 1L) height: Int
    ) = singleSpan {
        spanLineHeight(height)
    }

    /**
     * [ParagraphBitmapSpan] 设置段落图片
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param bitmap [Bitmap]
     * @param padding 图片与文本的间距
     * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
     * @param size 图片大小 [DrawableSize]
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
     * [ParagraphDrawableSpan] 设置段落图片
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param drawable [Drawable]
     * @param padding 图片与文本的间距
     * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
     * @param size 图片大小 [DrawableSize]
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
     * 自定义字符样式
     *
     * @param style 自定义样式. eg. spanCustom(ForegroundColorSpan(Color.RED))
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    fun <T : CharacterStyle> Any?.custom(
        style: T,
        replaceRule: Any? = null,
    ) = singleSpan {
        spanCustom(style, replaceRule)
    }

    /**
     * 自定义段落样式
     *
     * @param style 自定义样式. eg. spanCustom(LineBackgroundSpan.Standard(Color.Red))
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     * 由于段落样式的特殊性, [ParagraphStyle] 段落样式下 [replaceRule] 大部分情况并不会生效
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

