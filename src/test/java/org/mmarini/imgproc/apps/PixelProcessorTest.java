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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.DoubleUnaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

class PixelProcessorTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0",
            "0.25, 0, 0.25",
            "0.499, 0, 0.499",
            "0.501, 0, -0.499",
            "0.75, 0, -0.25",
            "1, 0, 0",

            "0, 0.5, -0.5",
            "0.001, 0.5, -0.499",
            "0.25, 0.5, -0.25",
            "0.5, 0.5, 0",
            "0.75, 0.5, 0.25",
            "0.999, 0.5, 0.499",
            "1, 0.5, 0.5",

            "0, 1, 0",
            "0.001, 1, 0.001",
            "0.25, 1, 0.25",
            "0.499, 1, 0.499",
            "0.501, 1, -0.499",
            "0.75, 1, -0.25",
            "1, 1, 0",
    })
    void diffTest(double x, double x0, double exp) {
        // Given ...
        DoubleUnaryOperator f = PixelProcessors.diff(x0);

        // When ...
        double y = f.applyAsDouble(x);

        // Then ...
        assertThat(y, closeTo(exp, 1e-3));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.5, 0.1,0.4, 0",
            "-0.4, 0.1,0.4, 0",
            "-0.3, 0.1,0.4, -0.333",
            "-0.2, 0.1,0.4, -0.667",
            "-0.1, 0.1,0.4, -1",
            "0, 0.1,0.4, 1",
            "0.1, 0.1,0.4, 1",
            "0.2, 0.1,0.4, 0.667",
            "0.3, 0.1,0.4, 0.333",
            "0.4, 0.1,0.4, 0",
            "0.5, 0.1,0.4, 0",
    })
    void hysteresisTest(double x, double dx1, double dx0, double exp) {
        // Given ...
        DoubleUnaryOperator f = PixelProcessors.hysteresis(dx1, dx0);

        // When ...
        double y = f.applyAsDouble(x);

        // Then ...
        assertThat(y, closeTo(exp, 1e-3));
    }
}