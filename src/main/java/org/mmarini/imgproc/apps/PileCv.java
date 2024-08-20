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

import ai.kognition.pilecv4j.ffmpeg.Ffmpeg;
import ai.kognition.pilecv4j.image.CvMat;
import ai.kognition.pilecv4j.image.display.ImageDisplay;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.mmarini.imgproc.swing.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PileCv {
    private static final Logger logger = LoggerFactory.getLogger(PileCv.class);

    private final Namespace args;

    public PileCv(Namespace args) {
        this.args = args;
    }

    /**
     * Returns the argument parser
     */
    private static ArgumentParser createParser() {
        ArgumentParser parser = ArgumentParsers.newFor(LucriView.class.getName()).build()
                .defaultHelp(true)
                .version(Messages.getString("Imgproc.title"))
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
            new PileCv(parser.parseArgs(args)).run();
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        } catch (Throwable e) {
            logger.atError().setCause(e).log("Error starting application");
            System.exit(1);
        }
    }

    private void run() {
        // Most components are java resources (AutoCloseables)
        final String file = args.getString("FILE");
        try (
                // We will create an ImageDisplay in order to show the frames from the video
                ImageDisplay window = new ImageDisplay.Builder()
                        .windowName("Tutorial 1")
                        .build();

                // create a StreamContext using Ffmpeg2. StreamContexts represent
                // a source of media data and a set of processing to be done on that data.
                final Ffmpeg.MediaContext sctx = Ffmpeg.createMediaContext(file)

                        // Tell the decoding that, if you need to convert the color anyway,
                        // you might as well convert it to BGR rather than RGB.
                        .preferBgr()

                        // We are simply going to pick the first video stream from the file.
                        .selectFirstVideoStream()

                        // Then we can add a processor. In this case we want the system to call us
                        // with each subsequent frame as an OpenCV Mat.
                        .processVideoFrames(videoFrame -> {

                            // we want to display each frame. PileCV4J extends the OpenCV Mat functionality
                            // for better native resource/memory management. So we can use a try-with-resource.
                            try (CvMat mat = videoFrame.bgr(false);) { // Note, we want to make sure the Mat is BGR
                                // Display the image.
                                window.update(mat);
                            }

                        })
                        // play the media stream.
                        .play();

        ) {
        }
    }
}
