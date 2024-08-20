/*
 * MIT License
 *
 * Copyright (c) 2024 Marco Marini
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.mmarini.imgproc.apps;

import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

/**
 *
 */
public interface PixelProcessors {

    /**
     * Returns the difference module 0.5 function
     *
     * @param x0 the reference value
     */
    static DoubleUnaryOperator diff(double x0) {
        return x -> {
            double d = x - x0;
            return d > 0.5f
                    ? d - 1
                    : d < -0.5f
                    ? 1 + d
                    : d;
        };
    }

    static UnaryOperator<float[]> hueFilter(double h0, double dh1, double dh0, double b0) {
        DoubleUnaryOperator diff = diff(h0);
        DoubleUnaryOperator hyst = hysteresis(dh1, dh0);
        DoubleUnaryOperator bright = map(0, 1, b0, 1);
        return hsb -> {
            double dh = diff.applyAsDouble(hsb[0]);
            double hSens = hyst.applyAsDouble(dh);
            double absHSens = Math.abs(hSens);
            double absSens = 1 - (1 - absHSens) * hsb[1];

            double hShift = dh * (1 - absSens);
            hsb[0] -= (float) hShift;
            hsb[1] *= (float) absSens;
            hsb[2] *= (float) bright.applyAsDouble(absSens);
            return hsb;
        };
    }

    /**
     * Returns the hysteresis function
     *
     * <pre>
     *  y                  y ^
     *                       |
     *                     1 +------
     *                       |     .\
     *                       |     . \
     *            -x0 -x1   |     .  \
     *          ---+---+-----0-----+---+----->
     *              \  .     |     x1  x0    x
     *               \ .     |
     *                \.     |
     *                 ------+ -1
     * </pre>
     *
     * @param x1 the value for unary result
     * @param x0 the value for zero result
     */
    static DoubleUnaryOperator hysteresis(double x1, double x0) {
        DoubleUnaryOperator pos = map(x1, x0, 1, 0);
        DoubleUnaryOperator neg = map(-x1, -x0, -1, 0);
        return x ->
                x >= x0 || x <= -x0 ?
                        0 :
                        x >= 0 ?
                                x <= x1 ?
                                        1 : pos.applyAsDouble(x) :
                                x >= -x1 ?
                                        -1 : neg.applyAsDouble(x);
    }

    /**
     * Returns the linear mapper
     *
     * @param x0 the x0 value
     * @param x1 the x1 value
     * @param y0 the y0 value
     * @param y1 the y1 value
     */
    static DoubleUnaryOperator map(double x0, double x1, double y0, double y1) {
        //  x -> (x - x0) / (x1 - x0) * (y1 - y0) + y0;
        double a = (y1 - y0) / (x1 - x0);
        double b = y0 - (y1 - y0) * x0 / (x1 - x0);
        return x -> a * x + b;
    }
}
