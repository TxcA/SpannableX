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

package com.itxca.spannablex.interfaces;

import android.view.View;

import com.itxca.spannablex.SpanInternal;
import com.itxca.spannablex.span.SimpleClickableSpan;

/**
 * {@link SimpleClickableSpan} 点击回调
 * <p>
 * {@link SpanInternal#spanClickable}
 */
public interface OnSpanClickListener {
    /**
     * {@link SimpleClickableSpan}被点击时回调
     *
     * @param v         点击的当前View
     * @param matchText 点击时匹配上的文本
     */
    void onClick(View v, String matchText);
}
