/*
 * Copyright (c) 2024 Marco Marini, marco.marini@mmarini.org
 *
 *  Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 *    END OF TERMS AND CONDITIONS
 *
 */

package org.mmarini.imgproc.swing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

/**
 * Swing video box
 */
public class VideoViewer extends JComponent {
    private static final Logger logger = LoggerFactory.getLogger(VideoViewer.class);

    /*
     * Loads openCV native libraries
     */
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static BufferedImage matToBufferedImage(Mat original) {
        // init
        BufferedImage image;
        int width = original.width();
        int height = original.height();
        int channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    private final VideoCapture capture;
    private BufferedImage image;
    private UnaryOperator<Mat> frameProcessor;
    private boolean layout;

    /**
     *
     */
    public VideoViewer() {
        this(null);
    }

    /**
     * @param frameProcessor the frame processor
     */
    public VideoViewer(UnaryOperator<Mat> frameProcessor) {
        this.frameProcessor = frameProcessor;
        capture = new VideoCapture();
        setBackground(Color.BLACK);
    }

    /**
     * Returns the frame processor
     */
    public UnaryOperator<Mat> getFrameProcessor() {
        return frameProcessor;
    }

    /**
     * Sets the frame processor
     *
     * @param frameProcessor the frame processor
     */
    public void setFrameProcessor(UnaryOperator<Mat> frameProcessor) {
        this.frameProcessor = frameProcessor;
    }

    @Override
    public Dimension getPreferredSize() {
        Image image = this.image;
        return image != null ? new Dimension(image.getWidth(this), image.getHeight(this))
                : new Dimension();
    }

    /**
     *
     */
    private void grabFrame() {
        // init everything
        Mat frame = new Mat();

        // check if the capture is open
        if (capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty() && frameProcessor != null) {
                    frame = frameProcessor.apply(frame);
                    if (!layout) {
                        layout = true;
                        doLayout();
                    }
                }
            } catch (Exception e) {
                // log the error
                logger.atError().setCause(e).log("Exception during the image elaboration");
            }
        }
        this.image = matToBufferedImage(frame);
        repaint();
    }

    /**
     * Opens the file
     *
     * @param file the video file or url
     */
    public void open(String file) {
        capture.open(file);
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(this::grabFrame, 0, 33, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        Image image = this.image;
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
    }
}
