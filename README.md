<p align="center"><img src="./docs/res/logo.png" width="25%"/></p>

<p align="center"><b>简单易用的Spannable扩展</b></p>

<p align="center">
<a href="https://github.com/TxcA/SpannableX/actions"><img src="https://github.com/TxcA/SpannableX/workflows/CI/badge.svg?branch=master&event=push"/></a>
<a href="https://search.maven.org/artifact/com.itxca.spannablex/spannablex"><img src="https://img.shields.io/maven-central/v/com.itxca.spannablex/spannablex"/></a>
<img src="https://img.shields.io/badge/language-Kotlin-blue.svg"/>
<img src="https://img.shields.io/badge/license-Apache2.0-blue.svg"/>
</p>


|                Code Sample                |              Kotlin               | Java                            |
| :---------------------------------------: | :-------------------------------: | ------------------------------- |
| ![Gif 1.43MB](./docs/res/sample_code.gif) | ![](./docs/res/sample_kotlin.png) | ![](./docs/res/sample_java.png) |

✨ 本框架基于 ***[@liangjingkanji/spannable](https://github.com/liangjingkanji/spannable)*** 驱动，此外 **新增以下特性** : 

- **封装常用Span**

  [🛠 查看常用Spans文档](https://txca.github.io/SpannableX/spans/)

- **Kotlin DSL**

  ``` kotlin
  TextView.text = spannable {
                    "this is real text.".text()
                    "spannable".span {
                        color(Color.BLUE)
                        style(Typeface.BOLD)
                    }
                  }
  ```

- **Kotlin|Java 链式**

  ``` java
  TextView.setText(Span.create()
                    .text("this is real text.")
                    .text("spannable").color(Color.BLUE).style(Typeface.BOLD)
                    .spannable());
  ```

- **更方便的替换规则**

  支持String、正则、[ReplaceRule](https://txca.github.io/SpannableX/replace/#replacerule) 及相应的Array|List 替换规则

***更多详情参考:***

**[ ⭐ 使用文档 ](https://txca.github.io/SpannableX/)**  **[ ⚙ API文档 ](https://txca.github.io/SpannableX/api/)**

---

### 使用

框架存储在`mavenCentral`，大部分情况不需要手动配置，添加远程仓库根据创建项目的 Android Studio 版本有所不同。

``` groovy
// 项目根目录build.gradle
allprojects {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

``` groovy
// 项目根目录settings.gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

然后在 module 的 build.gradle 添加依赖框架

``` groovy
dependencies {
    // https://github.com/TxcA/SpannableX
    implementation 'com.itxca.spannablex:spannablex:1.0.3'

    // 若需使用glide()方法加载网络图片或Gif, 需同时引入Glide
    implementation com.github.bumptech.glide:glide:4.13.1
}
```

### 鸣谢

[@liangjingkanji/spannable](https://github.com/liangjingkanji/spannable)

### License

```
Apache-2.0 Copyright 2022 TxcA

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
