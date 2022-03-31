基于Kotlin优异的特性，使用DSL来构建一个Spannable。


## 核心方法

***spannable{ }***

> 创建一个Spannable DSL, 可以携带receiver。

``` kotlin
// 不携带receiver
TextView.text = spannable{
    // 给bold text附加粗体及红色
    "bold text".style(Typeface.BOLD).color(Color.RED)
    
    // 添加不带样式的real text
    "real text".text()
}

// 携带receiver
TextView.text = "receiver".spannable{
    // 对receiver进行spanned操作
    style(Typeface.ITALIC)
    style(Typeface.BOLD)
    color(Color.BLACK)
    
    // 对text进行spanned操作并附加在receiver后面
    "text".style(Typeface.BOLD).color(Color.RED)
}
```


## DSL内附加方法

以下方法为 *spannable* 内部的方法，只能在 *spannable DSL* 内部调用。

- ***CharSequence?.text()***

  > 添加一段不附加效果的文本

  ``` kotlin
  TextView.text = spannable{
  	"this is real text.".text()
  }
  ```

- ***CharSequence?.span{ }***

  > 添加一个需要混合span的DSL

  ``` kotlin
  TextView.text = spannable{
  	"color text.".color(Color.RED)
  	
  	// span 方法可以混合多个Span效果
  	"mix text.".span{
  		style(Typeface.ITALIC and Typeface.BOLD)
  		color(Color.RED)
  	}
  }
  ```

- ***T?.newline(Int)***

  > 若不传入参数，则换一行。若传入参数，则换指定数量行。换行操作(也可自行使用 *\n* )。
  >
  > 若有receiver，就对receiver末尾添加 *\n* ，并返回receiver。
  >
  > 若无receiver，就直接对当前DSL内进行换行。

  ``` kotlin
  TextView.text = spannable{
  	// 在文本末尾添加换行符，并返回文本
  	"this is real text.".newline().color(Color.RED)
      
      // 在当前spannable dsl内，添加换行
      newline()
  }
  ```


## 其它说明

**👉 替换规则(DSL、链式通用):** [替换规则](https://txca.github.io/SpannableX/replace/)

**👉 常用Span(DSL、链式通用):** [常用Span](https://txca.github.io/SpannableX/spans/)

**👉 Kotlin DSL Sample:**  [KotlinSample](https://github.com/TxcA/SpannableX/blob/master/app/src/main/java/com/itxca/sample/spannable/KotlinFragment.kt)

