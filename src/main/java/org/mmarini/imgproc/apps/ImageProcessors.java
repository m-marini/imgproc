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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.lang.Math.*;

/**
 * Image processor
 */
public interface ImageProcessors {

    float[][] zeros = new float[3][3];
    float[][] eyes = eyes(new float[3][3], 1);

    static UnaryOperator<BufferedImage> convolution(int ww, int wh, Function<Convolution, float[][]> matrixSupplier) {
        return source -> {
            int w = source.getWidth();
            int h = source.getHeight();
            Raster in = source.getRaster();
            float[] rgb = new float[3];
            float[] acc = new float[3];
            Convolution conv = new Convolution();
            conv.width = w;
            conv.height = h;
            conv.ww = ww;
            conv.wh = wh;
            int ow = w - ww + 1;
            int oh = h - wh + 1;
            BufferedImage img = new BufferedImage(ow, oh, BufferedImage.TYPE_INT_RGB);
            WritableRaster out = img.getRaster();
            for (int ty = 0; ty < oh; ty++) {
                conv.target.y = ty + wh / 2;
                for (int tx = 0; tx < ow; tx++) {
                    conv.target.x = tx + ww / 2;
                    Arrays.fill(acc, 0);
                    for (int sy = ty; sy < ty + wh; sy++) {
                        conv.source.y = sy;
                        for (int sx = tx; sx < tx + ww; sx++) {
                            conv.source.x = sx;
                            rgb = in.getPixel(sx, sy, rgb);
                            float[][] m = matrixSupplier.apply(conv);
                            for (int i = 0; i < 3; i++) {
                                for (int j = 0; j < 3; j++) {
                                    acc[i] += rgb[j] * m[j][i];
                                }
                            }
                        }
                    }
                    out.setPixel(tx, ty, acc);
                }
            }
            return img;
        };
    }

    static float[][] eyes(float[][] mx, float value) {
        for (int i = 0; i < mx.length; i++) {
            for (int j = 0; j < mx[i].length; j++) {
                mx[i][j] = i == j ? value : 0;
            }
        }
        return mx;
    }

    static Function<Convolution, float[][]> gray() {
        float[][] mx = new float[][]{
                {1f / 3, 1f / 3, 1f / 3},
                {1f / 3, 1f / 3, 1f / 3},
                {1f / 3, 1f / 3, 1f / 3}
        };
        return convs -> mx;
    }

    /**
     * Returns the processor the process the hsb pixels
     *
     * @param pixelProcessor the pixel processor
     */
    static UnaryOperator<BufferedImage> hsbProcessor(UnaryOperator<float[]> pixelProcessor) {
        return source -> {
            int w = source.getWidth();
            int h = source.getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            WritableRaster in = source.getRaster();
            WritableRaster out = img.getRaster();
            int[] rgb = new int[3];
            float[] hsb = new float[3];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    in.getPixel(x, y, rgb);
                    Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsb);
                    float[] hsb1 = pixelProcessor.apply(hsb);
                    int rgbpx = Color.HSBtoRGB(hsb1[0], hsb1[1], hsb1[2]);
                    rgb[0] = (rgbpx >> 16) & 0xff;
                    rgb[1] = (rgbpx >> 8) & 0xff;
                    rgb[2] = rgbpx & 0xff;
                    out.setPixel(x, y, rgb);
                }
            }
            return img;
        };
    }

    static Function<Convolution, float[][]> identity() {
        return convs -> eyes;
    }

    static Function<Convolution, float[][]> lucri(BufferedImage img, double alphaRadius, double minAcuity, double maxAcuity, double minSensitivity, double maxSensitivity) {
        float[][] mx = new float[3][3];
        int w = img.getWidth();
        int h = img.getHeight();
        Point center = new Point(w / 2, h / 2);
        double radius = (double) (max(w, h) / 2) * alphaRadius / 2;
        double radius2 = radius * radius * 2;
        DoubleUnaryOperator mapper = PixelProcessors.map(1, 0, minAcuity, maxAcuity);
        DoubleUnaryOperator mapper1 = PixelProcessors.map(1, 0, minSensitivity, maxSensitivity);
        double k = 1d / 2 / PI;
        return conv -> {
            double radial2 = conv.source.distanceSq(center);
            double rad = exp(-radial2 / radius2);
            double acuity = mapper.applyAsDouble(rad);
            double sensitivity = mapper1.applyAsDouble(rad);
            if (acuity >= 1) {
                return conv.target.distanceSq(conv.source) <= 0.5 ? eyes(mx, (float) sensitivity) : zeros;
            } else {
                double radiusSens = 1 / acuity / 2;
                double radiusSens2 = radiusSens * radiusSens;
                double alpha = exp(-conv.target.distanceSq(conv.source) / radiusSens2 / 2) * k / radiusSens2;
                return eyes(mx, (float) (alpha * sensitivity));
            }
        };
    }

    static UnaryOperator<BufferedImage> lucriView(BufferedImage img, double alphaRadius, double minAcuity, double maxAcuity, double minSensitivity, double maxSensitivity) {
        int size = (int) (round(1 / minAcuity / 2) * 2 + 1);
        return convolution(size, size, lucri(img, alphaRadius, minAcuity, maxAcuity, minSensitivity, maxSensitivity));
    }

    static Function<Convolution, float[][]> smooth(float alpha) {
        float[][] mx = new float[3][3];
        return conv -> eyes(mx, alpha);
    }

    static UnaryOperator<BufferedImage> smoothImage(int numPixels, float alpha) {
        return convolution(numPixels, numPixels, smooth(1f / numPixels / numPixels * alpha));
    }

    /**
     * Returns the buffered image
     *
     * @param source   the source image
     * @param observer the observer
     */
    static BufferedImage toBuffered(Image source, ImageObserver observer) {
        int w = source.getWidth(observer);
        int h = source.getHeight(observer);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = img.createGraphics();
        gr.drawImage(source, 0, 0, observer);
        return img;
    }

    static float[][] zeros(float[][] mx) {
        for (float[] floats : mx) {
            Arrays.fill(floats, 0);
        }
        return mx;
    }

    class Convolution {
        public final Point source;
        public final Point target;
        public int ww;
        public int wh;
        public int width;
        public int height;

        public Convolution() {
            this.target = new Point();
            this.source = new Point();
        }
    }
}
