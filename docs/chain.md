***Kotlin*** | ***Java*** 均能使用的链式调用方式。


## 核心方法

***Span.create().spannable()***

``` kotlin
// Span.create() 创建Span, 最后spannable() 构建Spannable并返回。
TextView.text = Span.create()
	// 给bold text附加粗体及红色
	.text("bold text").style(Typeface.BOLD).color(Color.RED)

	// 添加不带样式的real text
	.text("real text")

	// 构建span
	.spannable()
```


## Span方法

- ***create()***

  > 构建一个Span链

  ``` kotlin
  TextView.text = Span.create()
  
  ...
  ```

- ***spannable()***

  > 构建spannable

  ``` kotlin
  TextView.text = Span.create()
  	// 给bold text附加粗体
  	.text("bold text").style(Typeface.BOLD)
  	
  	// 链式构建方式最后需调用spannable()进行Span构建
  	.spannable()
  ```

- ***text(String)***

  > 添加一段文本

  ``` kotlin
  Span.create()
  	// 添加一段文本, 并附加红色
  	.text("message").color(Color.RED)
  
  	// 添加一段文本
  	.text("this is real text.")
  
  ...
  ```

- ***newline(Int)***

  > 若不传入参数，则换一行。若传入参数，则换指定数量行。
  >
  > 换行操作(也可自行使用 *\n* )
  
  ``` kotlin
  Span.create()
  	// 在文本末尾添加换行符
  	.text("message").newline().color(Color.RED)
  	
  	// 当前位置插入换行
  	.newline()
  ...
  ```


## 其它说明

**👉 替换规则(DSL、链式通用):** [替换规则](https://txca.github.io/SpannableX/replace/)

**👉 常用Span(DSL、链式通用):** [常用Span](https://txca.github.io/SpannableX/spans/)

**👉 Kotlin|Java 链式 Sample:**  [JavaSample](https://github.com/TxcA/SpannableX/blob/master/app/src/main/java/com/itxca/sample/spannable/JavaFragment.kt)

