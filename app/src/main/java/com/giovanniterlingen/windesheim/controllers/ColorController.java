/**
 * Copyright (c) 2017 Giovanni Terlingen
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
package com.giovanniterlingen.windesheim.controllers;

import android.support.v4.util.LruCache;

import com.giovanniterlingen.windesheim.ApplicationLoader;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ColorController {

    private static final int[] colors = new int[]{
            0xff008dd7,
            0xff82189e,
            0xffe64310,
            0xff23a669,
            0xff545454
    };
    public final LruCache<Integer, Integer> cachedColors = new LruCache<>(colors.length);

    private static volatile ColorController Instance = null;

    public static ColorController getInstance() {
        ColorController localInstance = Instance;
        if (localInstance == null) {
            synchronized (ColorController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ColorController();
                }
            }
        }
        return localInstance;
    }

    private int getColorByPosition(int position) {
        return colors[position];
    }

    public int getColorById(int id) {
        if (cachedColors.get(id) == null) {
            int position = ApplicationLoader.databaseController.getPositionByScheduleId(id);
            int color = getColorByPosition(position);
            cachedColors.put(id, color);
        }
        return cachedColors.get(id);
    }

    public void invalidateColorCache() {
        this.cachedColors.evictAll();
    }
}
