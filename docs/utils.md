***SpannableX*** 除了提供强大的Span效果及替换规则外，也提供了一些为了更方便的使用Span而添加的辅助方法。

## CharSequence.removeSpans&lt;T&gt;()

> 删除指定Span效果
>
> 系统的removeSpan只能删除单个Span。而clearSpans则会导致光标等问题。

- Kotlin

  ``` kotlin
  // 可变Spannable (如EditText)，可直接调用
  EditText.text.removeSpans<BackgroundColorSpan>()
  
  // 不可变Spanned (如TextView)，需要重新赋值
  TextView.run {
  	text = text.removeSpans<BackgroundColorSpan>()
  }
  ```

- Java

  ``` java
  // 可变Spannable (如EditText)，可直接调用
  Span.removeSpans(EditText.getText(), BackgroundColorSpan.class);
  
  // 不可变Spanned (如TextView)，需要重新赋值
  TextView.setText(Span.removeSpans(TextView.getText(), BackgroundColorSpan.class));
  ```


## CharSequence.removeAllSpans()

> 删除全部Span效果

- Kotlin

  ``` kotlin
  // 可变Spannable (如EditText)，可直接调用
  EditText.text.removeAllSpans()
  
  // 不可变Spanned (如TextView)，需要重新赋值
  TextView.run {
  	text = text.removeAllSpans()
  }
  ```

- Java

  ``` java
  // 可变Spannable (如EditText)，可直接调用
  Span.removeAllSpans(EditText.getText());
  
  // 不可变Spanned (如TextView)，需要重新赋值
  TextView.setText(Span.removeAllSpans(TextView.getText());
  ```


## TextView.activateClick(Boolean)

> 参数为是否显示点击背景色
>
> TextView 激活点击，配合常用Span中的**[👉 clickable ](https://txca.github.io/SpannableX/spans//#span_1)**处理点击。

- Kotlin

  ``` kotlin
  TextView.activateClick().text = "Github: ".span + "https://github.com/TxcA/SpannableX/".spannable {
  		url("https://github.com/TxcA/SpannableX/")
  }
  ```

- Java

  ``` java
  Span.activateClick(TextView).setText(Span.create()
  		.text("Github: ")
  		.text("https://github.com/TxcA/SpannableX/").url("https://github.com/TxcA/SpannableX/")
  		.spannable());
  ```


>*** Tips: TextView autoLink知识点 ***
>
>常用的网址、电话等点击方式，系统自带了 *autoLinkMask* 的方法，无需使用 *activateClick* 和 *url*。
>
>系统会通过设置的 *autoLinkMask* 自动识别并处理点击。
>
>- 方式一、xml直接配置
>
>  ```xml
>  <TextView
>      android:id="@+id/tv_sample"
>      android:layout_width="match_parent"
>      android:layout_height="wrap_content"
>      android:layout_marginTop="8dp"
>      android:autoLink="web"
>      android:text="https://github.com/TxcA/SpannableX/"
>      android:textSize="14sp" />
>  ```
>
>- 方式二、代码配置
>
>  ``` kotlin
>  // Kotlin
>  TextView.apply {
>  	autoLinkMask = Linkify.WEB_URLS
>  	text = "https://github.com/TxcA/SpannableX/"
>  }
>  
>  // Java
>  TextView.setAutoLinkMask(Linkify.WEB_URLS);
>  TextView.setText("https://github.com/TxcA/SpannableX/");
>  ```
>
>  这些特性是系统自带的，可以更方便的处理常用点击。


## String.color

>StringColor 转 ColorInt
>
>Kotlin 扩展方法，其实也是调用了 *Color.parseColor()*

``` kotlin
val color: Int = "#aaaaaa".color
```


## 其它说明

**👉 替换规则(DSL、链式通用):** [替换规则](https://txca.github.io/SpannableX/replace/)

**👉 常用Span(DSL、链式通用):** [常用Span](https://txca.github.io/SpannableX/spans/)