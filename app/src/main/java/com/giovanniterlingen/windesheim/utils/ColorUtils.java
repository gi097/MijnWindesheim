/**
 * Copyright (c) 2019 Giovanni Terlingen
 * <p/>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.giovanniterlingen.windesheim.utils;

import androidx.collection.LruCache;

import com.giovanniterlingen.windesheim.controllers.DatabaseController;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ColorUtils {

    public static final int[] colors = new int[]{
            0xFF9DCC25,
            0xFFFF7761,
            0xFF2056B3,
            0xFF8CB329,
            0xFF478BFF
    };
    private static final LruCache<String, Integer> cachedColors = new LruCache<>(colors.length);

    private static int getColorByPosition(int position) {
        return colors[position];
    }

    public static int getColorById(String id) {
        Integer color;
        if ((color = cachedColors.get(id)) == null) {
            int position = DatabaseController.getInstance().getPositionByScheduleId(id);
            color = getColorByPosition(position);
            cachedColors.put(id, color);
        }
        return color;
    }

    public static void invalidateColorCache() {
        cachedColors.evictAll();
    }
}
