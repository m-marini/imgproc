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

package org.mmarini.imgproc.apps;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.mmarini.imgproc.swing.Messages;
import org.mmarini.imgproc.swing.VideoViewer;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class QRCode {
    static final Scalar color = new Scalar(255, 255, 255);
    private static final Logger logger = LoggerFactory.getLogger(QRCode.class);

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Returns the argument parser
     */
    private static ArgumentParser createParser() {
        ArgumentParser parser = ArgumentParsers.newFor(QRCode.class.getName()).build()
                .defaultHelp(true)
                .version(Messages.getString("QRCode.title"))
                .description("Run the test.");
        parser.addArgument("-f", "--file")
                .required(true)
                .help("specify the image file");
        parser.addArgument("-v", "--version")
                .action(Arguments.version())
                .help("show current version");
        return parser;
    }

    /**
     * Entry point
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ArgumentParser parser = createParser();
        try {
            new QRCode(parser.parseArgs(args)).run();
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        } catch (Throwable e) {
            logger.atError().setCause(e).log("Error starting application");
            System.exit(1);
        }
    }
    private final Namespace args;
    private final JFrame frame;
    private final VideoViewer videoBox;
    private final Mat points = new Mat();

    /**
     * @param args the argument
     */
    public QRCode(Namespace args) {
        this.args = args;
        this.frame = new JFrame("OpenCv");
        this.videoBox = new VideoViewer();
        Container content = frame.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(new JScrollPane(videoBox), BorderLayout.CENTER);
    }

    /**
     * @param frame the frame
     */
    private Mat processFrame(Mat frame) {
        String data = new QRCodeDetector().detectAndDecode(frame, points);
        if (!data.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                Point position = new Point(points.get(0, i));
                Imgproc.drawMarker(frame, position, color);
            }
            logger.atInfo().log("Qrcode = {}", data);
        }
        return frame;
    }

    /**
     *
     */
    private void run() {
        logger.atInfo().log("Running {}", QRCode.class.getName());

        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String file = args.getString("file");
        logger.atInfo().log("Opening {}", file);
        videoBox.setFrameProcessor(this::processFrame);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                videoBox.open(file);
            }
        });
        logger.atInfo().log("Completed");
    }
}
