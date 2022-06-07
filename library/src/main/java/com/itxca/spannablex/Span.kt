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
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.core.text.buildSpannedString
import com.bumptech.glide.request.RequestOptions
import com.drake.spannable.movement.ClickableMovementMethod
import com.drake.spannable.span.CenterImageSpan
import com.drake.spannable.span.GlideImageSpan
import com.itxca.spannablex.annotation.ConversionUnit
import com.itxca.spannablex.annotation.TextStyle
import com.itxca.spannablex.interfaces.OnSpanClickListener
import com.itxca.spannablex.interfaces.OnSpanReplacementMatch
import com.itxca.spannablex.span.*
import com.itxca.spannablex.span.LeadingMarginSpan
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
     * [StyleSpan] 设置文本样式
     *
     * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun style(
        @TextStyle style: Int,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanStyle(style, replaceRule) }

    /**
     * [TypefaceSpan] 设置字体样式
     *
     * @param typeface 字体(API>=28)
     * @param family 字体集
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun typeface(
        typeface: Typeface? = null,
        family: String? = null,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanTypeface(typeface, family, replaceRule) }

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
    @JvmOverloads
    fun textAppearance(
        @TextStyle style: Int = Typeface.NORMAL,
        @Px size: Int = -1,
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
     * [ForegroundColorSpan] 文本颜色
     *
     * @param color 文本颜色
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun color(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanColor(color, replaceRule) }

    /**
     * [ForegroundColorSpan] 文本颜色
     *
     * @param colorString 文本颜色 #RRGGBB #AARRGGBB
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun color(
        colorString: String,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanColor(Companion.color(colorString), replaceRule) }

    /**
     * [BackgroundColorSpan] 背景颜色
     *
     * @param color 背景颜色
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun background(
        @ColorInt color: Int,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanBackground(color, replaceRule) }

    /**
     * [BackgroundColorSpan] 背景颜色
     *
     * @param colorString 背景颜色 #RRGGBB #AARRGGBB
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun background(
        colorString: String,
        replaceRule: Any? = null
    ): Span =
        runOnSelf { spannableCache?.spanBackground(Companion.color(colorString), replaceRule) }

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
    @JvmOverloads
    fun image(
        drawable: Drawable,
        source: String? = null,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
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
    @JvmOverloads
    fun image(
        context: Context,
        uri: Uri,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
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
    @JvmOverloads
    fun image(
        context: Context,
        @DrawableRes resourceId: Int,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
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
    @JvmOverloads
    fun image(
        context: Context,
        bitmap: Bitmap,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
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
    @JvmOverloads
    fun glide(
        view: TextView,
        url: Any,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null,
        @Px marginLeft: Int? = null,
        @Px marginRight: Int? = null,
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
     * [ScaleXSpan] X轴文本缩放
     *
     * @param proportion 水平(X轴)缩放比例
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun scaleX(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanScaleX(proportion, replaceRule) }


    /**
     * [MaskFilterSpan] 设置文本蒙版效果
     *
     * @param filter 蒙版效果 [MaskFilter]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun maskFilter(
        filter: MaskFilter,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanMaskFilter(filter, replaceRule) }

    /**
     * [BlurMaskFilter] 设置文本模糊滤镜蒙版效果
     *
     * @param radius 模糊半径
     * @param style 模糊效果 [BlurMaskFilter.Blur]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun blurMask(
        @FloatRange(from = 0.0) radius: Float,
        style: BlurMaskFilter.Blur? = null,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanBlurMask(radius, style, replaceRule) }

    /**
     * [SuperscriptSpan] 设置文本为上标
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun superscript(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanSuperscript(replaceRule) }

    /**
     * [SubscriptSpan] 设置文本为下标
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun subscript(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanSubscript(replaceRule) }

    /**
     * [AbsoluteSizeSpan] 设置文本绝对大小
     *
     * @param size 文本大小
     * @param dp true = [size] dp, false = [size] px
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun absoluteSize(
        size: Int,
        dp: Boolean = true,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanAbsoluteSize(size, dp, replaceRule) }

    /**
     * [RelativeSizeSpan] 设置文本相对大小
     *
     * @param proportion 文本缩放比例
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun relativeSize(
        @FloatRange(from = 0.0) proportion: Float,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanRelativeSize(proportion, replaceRule) }

    /**
     * [StrikethroughSpan] 设置文本删除线
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun strikethrough(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanStrikethrough(replaceRule) }

    /**
     * [UnderlineSpan] 设置文本下划线
     *
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun underline(
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanUnderline(replaceRule) }

    /**
     * [URLSpan] 设置文本超链接
     *
     * 需配合[TextView.activateClick]使用
     * @param url 超链接地址
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun url(
        url: String,
        replaceRule: Any? = null
    ): Span = runOnSelf { spannableCache?.spanURL(url, replaceRule) }

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
    @JvmOverloads
    fun suggestion(
        context: Context,
        suggestions: Array<String>,
        flags: Int = SuggestionSpan.FLAG_EASY_CORRECT or SuggestionSpan.FLAG_AUTO_CORRECTION,
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
     * [SimpleClickableSpan] 设置文本点击效果
     *
     * @param color 文本颜色
     * @param backgroundColor 背景颜色
     * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
     * @param config 附加配置 [SimpleClickableConfig]
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     * @param onClick [OnSpanClickListener] 点击回调
     */
    @JvmOverloads
    fun clickable(
        @ColorInt color: Int? = null,
        @ColorInt backgroundColor: Int? = null,
        @TextStyle style: Int? = null,
        config: SimpleClickableConfig? = null,
        replaceRule: Any? = null,
        onClick: OnSpanClickListener? = null
    ): Span = runOnSelf {
        spannableCache?.spanClickable(
            color,
            backgroundColor,
            style,
            config,
            replaceRule,
            onClick
        )
    }

    /**
     * [QuoteSpan] 设置段落引用样式(段落前竖线标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param color 竖线颜色
     * @param stripeWidth 竖线宽度
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    @JvmOverloads
    fun quote(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) stripeWidth: Int = 10,
        @Px @IntRange(from = 0) gapWidth: Int = 0
    ): Span = runOnSelf {
        spannableCache?.spanQuote(color, stripeWidth, gapWidth)
    }

    /**
     * [QuoteSpan] 设置段落引用样式(段落前竖线标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param colorString 竖线颜色 #RRGGBB #AARRGGBB
     * @param stripeWidth 竖线宽度
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    @JvmOverloads
    fun quote(
        colorString: String,
        @IntRange(from = 0) stripeWidth: Int = 10,
        @IntRange(from = 0) gapWidth: Int = 0
    ): Span = runOnSelf {
        spannableCache?.spanQuote(colorString.color, stripeWidth, gapWidth)
    }

    /**
     * [BulletSpan] 设置段落项目符号(段落前圆形标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param color 圆形颜色
     * @param bulletRadius 圆形半径
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    @JvmOverloads
    fun bullet(
        @ColorInt color: Int,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ): Span = runOnSelf {
        spannableCache?.spanBullet(color, bulletRadius, gapWidth)
    }

    /**
     * [BulletSpan] 设置段落项目符号(段落前圆形标识)
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param colorString 圆形颜色 #RRGGBB #AARRGGBB
     * @param bulletRadius 圆形半径
     * @param gapWidth 竖线与文本之间间隔宽度
     */
    @JvmOverloads
    fun bullet(
        colorString: String,
        @Px @IntRange(from = 0) bulletRadius: Int,
        @Px gapWidth: Int = 0,
    ): Span = runOnSelf {
        spannableCache?.spanBullet(colorString.color, bulletRadius, gapWidth)
    }

    /**
     * [AlignmentSpan] 设置段落对齐方式
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param align [Layout.Alignment.ALIGN_NORMAL] [Layout.Alignment.ALIGN_CENTER] [Layout.Alignment.ALIGN_OPPOSITE]
     */
    fun alignment(
        align: Layout.Alignment
    ): Span = runOnSelf {
        spannableCache?.spanAlignment(align)
    }

    /**
     * [LineBackgroundSpan] 设置段落背景颜色
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param color 背景颜色
     */
    fun lineBackground(
        @ColorInt color: Int
    ): Span = runOnSelf {
        spannableCache?.spanLineBackground(color)
    }

    /**
     * [LineBackgroundSpan] 设置段落背景颜色
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param colorString 背景颜色 #RRGGBB #AARRGGBB
     */
    fun lineBackground(
        colorString: String
    ): Span = runOnSelf {
        spannableCache?.spanLineBackground(colorString.color)
    }

    /**
     * [LeadingMarginSpan] 设置段落文本缩进
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param firstLines 首行行数. 与[firstMargin]关联
     * @param firstMargin 首行左边距(缩进)
     * @param restMargin 剩余行(非首行)左边距(缩进)
     */
    @JvmOverloads
    fun leadingMargin(
        @IntRange(from = 1L) firstLines: Int,
        @Px firstMargin: Int,
        @Px restMargin: Int = 0
    ): Span = runOnSelf {
        spannableCache?.spanLeadingMargin(firstLines, firstMargin, restMargin)
    }

    /**
     * [LineHeightSpan] 设置段落行高
     *
     * [ParagraphStyle] 段落Style不支持文本替换
     * @param height 行高
     */
    fun lineHeight(
        @Px @IntRange(from = 1L) height: Int
    ): Span = runOnSelf {
        spannableCache?.spanLineHeight(height)
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
    @JvmOverloads
    fun imageParagraph(
        bitmap: Bitmap,
        @Px padding: Int = 0,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null
    ): Span = runOnSelf {
        spannableCache?.spanImageParagraph(bitmap, padding, useTextViewSize, size)
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
    @JvmOverloads
    fun imageParagraph(
        drawable: Drawable,
        @Px padding: Int = 0,
        useTextViewSize: TextView? = null,
        size: DrawableSize? = null
    ): Span = runOnSelf {
        spannableCache?.spanImageParagraph(drawable, padding, useTextViewSize, size)
    }

    /**
     * 自定义字符样式
     *
     * @param style 自定义样式. eg. spanCustom(ForegroundColorSpan(Color.RED))
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     */
    @JvmOverloads
    fun <T : CharacterStyle> custom(
        style: T,
        replaceRule: Any? = null,
    ): Span = runOnSelf {
        spannableCache?.spanCustom(style, replaceRule)
    }

    /**
     * 自定义段落样式
     *
     * @param style 自定义样式. eg. spanCustom(LineBackgroundSpan.Standard(Color.Red))
     * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
     * 由于段落样式的特殊性, [ParagraphStyle] 段落样式下 [replaceRule] 大部分情况并不会生效
     */
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
         * 兼容Java适配 @see [toReplaceRule]
         *
         * @param replaceString 查找的字符串或正则文本
         * @param isRegex [replaceString]是否为正则
         * @param matchIndex 单一匹配位置 ([matchRange]不为null时优先使用[matchRange])
         * @param matchRange 匹配范围
         * @param newString 替换文本(null 为不替换)
         * @param replacementMatch 匹配时回调
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

        /**
         * 快速构建 [DrawableSize]
         */
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


        /**
         * dp 2 px
         */
        @JvmStatic
        fun dp(value: Int): Int = value.dp

        /**
         * sp 2 px
         */
        @JvmStatic
        fun sp(value: Int): Int = value.sp

        @JvmStatic
        fun color(colorString: String): Int = colorString.color

        /**
         * 删除所有[CharacterStyle] Span
         */
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
         * 兼容Java适配 @see [TextView.activateClick]
         *
         * 配置 [LinkMovementMethod] 或 [ClickableMovementMethod]
         * @param textView 需要配置点击效果的[TextView]
         * @param background 是否显示点击背景
         */
        @JvmStatic
        @JvmOverloads
        fun activateClick(textView: TextView, background: Boolean = true): TextView =
            textView.activateClick(background)
        //</editor-fold>
    }
}