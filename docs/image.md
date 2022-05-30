***SpnnableX*** 通过 ***[@liangjingkanji/spannable](https://github.com/liangjingkanji/spannable)*** 的 `CenterImageSpan` 和 `GlideImageSpan` 驱动，可以快便捷的实现图片加载。

## 图片加载通用参数

```Kotlin
/** 设置图片大小与附加的TextView(EditText)字体大小相同 */
useTextViewSize: TextView? = null,

/** 设置图片大小。与useTextViewSize参数冲突，两者都赋值时优先使用useTextViewSize */
size: DrawableSize? = null,

/** 设置图片左边距 */
marginLeft: Int? = null,

/** 设置图片右边距 */
marginRight: Int? = null,

/** 设置图片垂直对其方式，图片默认垂直居中对齐文字: [Align.CENTER] */
align: CenterImageSpan.Align = CenterImageSpan.Align.CENTER,

/** 替换规则（详见替换规则说明） */
replaceRule: Any? = null,
```

## image
> 基于 `CenterImageSpan` 驱动

> 支持图片垂直对齐方式、图片宽高设置、图片水平间距设置

> 若需应对更复杂的图片加载需求请使用glide

关联方法:
``` kotlin
image(drawable: Drawable, source: String? = null, ...图片加载通用参数)
image(context: Context, uri: Uri, ...图片加载通用参数)
image(context: Context, @DrawableRes resourceId: Int, ...图片加载通用参数)
image(context: Context, bitmap: Bitmap, ...图片加载通用参数)
```

代码示例:
```kotlin
spannable {
    image(requireContext(), R.mipmap.ic_launcher, size = 18.sp.drawableSize, marginLeft = 20.dp, marginRight = 10.dp, align = CenterImageSpan.Align.BOTTOM)
    ...
}
```

## glide
> 基于 `GlideImageSpan` 驱动 ***使用Glide加载图片资源, 请先依赖Glide ***

> 支持图片垂直对齐方式、图片宽高设置、图片水平间距设置、Gif动画播放

关联方法:
``` kotlin
/**
 *
 * @param view 当前Span所在的TextView
 * @param url 图片地址(与Glide.load功能相同，可以使用本地资源，也可以使用网络Url)
 * @param loopCount GIF动画播放循环次数, 默认无限循环
 * @param requestOption 配置Glide请求选项, 例如占位图、加载失败图等
 *                      如果使用[RequestOptions.placeholder]占位图会导致默认使用占位图宽高, 除非你使用[setDrawableSize]覆盖默认值
 *                      默认会使用[RequestOptions.fitCenterTransform]保持图片纵横比例不变, 当然你可以覆盖该配置
 */
glide(view: TextView, url: Any, loopCount: Int?, requestOption: RequestOptions?, ...图片加载通用参数)

```

代码示例:
```kotlin
spannable {
    glide(viewBinding.tvSample, "https://www.baidu.com/img/flexible/logo/pc/result.png", size = 24.sp.drawableSize, marginLeft = 20.dp, marginRight = 10.dp, align = CenterImageSpan.Align.BOTTOM)
    ...
}
```

## 其它说明

**👉 常用Span(DSL、链式通用):** [常用Span](https://txca.github.io/SpannableX/spans/)

**👉 Kotlin|Java 链式 Sample:**  [JavaSample](https://github.com/TxcA/SpannableX/blob/master/app/src/main/java/com/itxca/sample/spannable/JavaFragment.kt)

**👉 Kotlin DSL Sample:**  [KotlinSample](https://github.com/TxcA/SpannableX/blob/master/app/src/main/java/com/itxca/sample/spannable/KotlinFragment.kt)