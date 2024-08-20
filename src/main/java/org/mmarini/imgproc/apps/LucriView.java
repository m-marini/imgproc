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

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.mmarini.imgproc.swing.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import static java.lang.Math.min;

/**
 * Main application
 */
public class LucriView {
    public static final Dimension DEFAULT_SIZE = new Dimension(800, 600);
    private static final Logger logger = LoggerFactory.getLogger(LucriView.class);

    /**
     * Returns the canvas
     */
    private static JComponent createCanvas(Image image) {
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return image != null ? new Dimension(image.getWidth(this), image.getHeight(this))
                        : new Dimension();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, this);
                }
            }
        };
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
            new LucriView(parser.parseArgs(args)).run();
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
    private final JSplitPane split;

    /**
     * Creates the application
     *
     * @param args the namespace of command line arguments
     */
    protected LucriView(Namespace args) {
        this.args = args;
        this.frame = new JFrame();
        this.split = new JSplitPane();
        init();
    }

    /**
     * Initializes application
     */
    private void init() {
        Container content = frame.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(split, BorderLayout.CENTER);

        frame.setTitle(Messages.getString("Imgproc.title"));
        frame.setSize(DEFAULT_SIZE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    /**
     * Runs the application
     */
    private void run() {
        logger.atInfo().log("Started {}.", Messages.getString("Imgproc.title"));
        String file = args.getString("file");
        BufferedImage source = ImageProcessors.toBuffered(new ImageIcon(file).getImage(), frame);
        Image image = ImageProcessors.lucriView(source, 1, 1d / 20, 0.2, 0.4, 1)
                .apply(source);
        split.setLeftComponent(new JScrollPane(createCanvas(source)));
        split.setRightComponent(new JScrollPane(createCanvas(image)));
        split.setResizeWeight(0.5);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                split.setDividerLocation(0.5);
            }
        });
        frame.setVisible(true);
    }
}
