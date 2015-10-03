/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package ca.mlaflamme.clocktime;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Displays text with no padding at the top.
 */
public class FitRobotoTextView extends TextView {
    private static final float NORMAL_FONT_PADDING_TOP_RATIO = 0.2f;
    private static final float NORMAL_FONT_PADDING_BOTTOM_RATIO = 0.23f;

    public FitRobotoTextView(Context context) {
        this(context, null);
    }

    public FitRobotoTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FitRobotoTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setIncludeFontPadding(false);

        updatePadding();
    }

    private void updatePadding() {
        float paddingTopRatio, paddingBottomRatio;
        int paddingLeft, paddingRight, paddingTop, paddingBottom;

        paddingTopRatio = NORMAL_FONT_PADDING_TOP_RATIO;
        paddingBottomRatio = NORMAL_FONT_PADDING_BOTTOM_RATIO;

        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();
        paddingTop = (int) (-paddingTopRatio * getTextSize());
        paddingBottom = (int) (-paddingBottomRatio * getTextSize());

        // no need to scale by display density because getTextSize() already returns the font
        // height in px
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updatePadding();
    }

    @Override
    protected void onAttachedToWindow() {
        updatePadding();
        super.onAttachedToWindow();
    }
}
