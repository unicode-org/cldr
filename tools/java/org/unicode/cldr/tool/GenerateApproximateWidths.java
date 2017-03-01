package org.unicode.cldr.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JApplet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ICUUncheckedIOException;

public class GenerateApproximateWidths extends JApplet implements Runnable {
    private static final long serialVersionUID = 1L;

    private static final int IMAGE_HEIGHT = 360;
    private static final int IMAGE_WIDTH = 640;
    private static final BasicStroke BASIC_STROKE = new BasicStroke(0.5f);
    private static final Font FONT = new Font("TimesNewRoman", 0, 100);
    private static final Font FONT101 = new Font("TimesNewRoman", 0, 101);

    private BufferedImage bimg;
    private String string = "𝛛";

    public void paint(Graphics g) {
        Dimension d = getSize();
        if (bimg == null || bimg.getWidth() != d.width || bimg.getHeight() != d.height) {
            bimg = (BufferedImage) createImage(d.width, d.height);
        }
        final int w = bimg.getWidth();
        final int h = bimg.getHeight();
        drawDemo(bimg, w, h);
        g.drawImage(bimg, 0, 0, this);
    }

    public void drawDemo(BufferedImage bimg, int w, int h) {
        Graphics2D g = bimg.createGraphics();
        g.setBackground(Color.white);
        g.clearRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(BASIC_STROKE);

        drawString(g, FONT, string, 0.25f, w, 0.5f, h); // draw the 1st size character on the left
        drawString(g, FONT101, string, 0.75, w, 0.5f, h); // draw the 2st size character on the right
        showWidths(g);
        g.dispose();
    }

    private void showWidths(Graphics2D g) {
        try {
            PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "widths/", "ApproximateWidth.txt");
            // TODO Auto-generated method stub
            UnicodeMap<Integer> map = new UnicodeMap<Integer>();
            Widths widths = new Widths(g, new Font("Serif", 0, 100), new Font("SansSerif", 0, 100));

            UnicodeSet CHECK = new UnicodeSet("[[:^c:][:cc:][:cf:]]");
            int defaultWidth = widths.getMetrics(0xFFFD);

            // corrections
            CHECK.removeAll(addCorrections(map, "[:Cn:]", defaultWidth));
            CHECK.removeAll(addCorrections(map, "[\\u0000-\\u0008\\u000E-\\u001F\\u007F-\\u0084\\u0086-\\u009F]",
                defaultWidth));

            int cjkWidth = widths.getMetrics(0x4E00);
            CHECK.removeAll(addCorrections(map, "[:ideographic:]", cjkWidth));

            CHECK.removeAll(addCorrections(map, "[[:Cf:][:Mn:][:Me:]]", 0));

            int count = 0;
            for (UnicodeSetIterator it = new UnicodeSetIterator(CHECK); it.next();) {
                ++count;
                if ((count % 1000) == 0) {
                    System.out.println(count + "\t" + Utility.hex(it.codepoint));
                }
                int cpWidth = widths.getMetrics(it.codepoint);
                if (cpWidth != defaultWidth) {
                    map.put(it.codepoint, cpWidth);
                }
            }
            out.println("# ApproximateWidth\n" +
                "# @missing: 0000..10FFFF; " + defaultWidth);

            Set<Integer> values = new TreeSet<Integer>(map.values());
            for (Integer integer0 : values) {
                if (integer0 == null) {
                    continue;
                }
                int integer = integer0;
                if (integer == defaultWidth) {
                    continue;
                }
                UnicodeSet uset = map.getSet(integer);
                out.println("\n# width: " + integer + "\n");
                for (UnicodeSetIterator foo = new UnicodeSetIterator(uset); foo.nextRange();) {
                    if (foo.codepoint != foo.codepointEnd) {
                        out.println(Utility.hex(foo.codepoint) + ".." + Utility.hex(foo.codepointEnd) + "; "
                            + integer + "; # " + UCharacter.getExtendedName(foo.codepoint) + ".."
                            + UCharacter.getExtendedName(foo.codepointEnd));
                    } else {
                        out.println(Utility.hex(foo.codepoint) + "; "
                            + integer + "; # " + UCharacter.getExtendedName(foo.codepoint));
                    }
                }
                out.println("\n# codepoints: " + uset.size() + "\n");
            }
            out.close();
            System.out.println("Adjusted: " + widths.adjusted);
            System.out.println("DONE");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private UnicodeSet addCorrections(UnicodeMap<Integer> map, String usetString, int width) {
        UnicodeSet uset = new UnicodeSet(usetString);
        map.putAll(uset, width);
        return uset;
    }

    class Widths {
        UnicodeSet adjusted = new UnicodeSet();

        Graphics2D g;
        FontMetrics[] metrics;
        char[] buffer = new char[20];
        int bufferLen = 0;
        int baseChar = -1;

        UnicodeSet SPACING_COMBINING = new UnicodeSet("[:Mc:]").freeze();
        int[] SCRIPT2BASE = new int[UScript.CODE_LIMIT];
        {
            for (int i = 0; i < SCRIPT2BASE.length; ++i) {
                SCRIPT2BASE[i] = -1;
            }
            for (String s : new UnicodeSet("[क ক ਕ ક କ க క ಕ ക ක ཀ ၵ ក]")) {
                int cp = s.codePointAt(0);
                if (cp < 0x10000) {
                    int script = UScript.getScript(cp);
                    SCRIPT2BASE[script] = cp;
                }
            }
        }

        public Widths(Graphics2D g2, Font... fonts) {
            g = g2;
            metrics = new FontMetrics[fonts.length];
            for (int i = 0; i < fonts.length; ++i) {
                g.setFont(fonts[i]);
                metrics[i] = g.getFontMetrics();
            }
        }

        private int getBase(int cp) {
            if (SPACING_COMBINING.contains(cp)) {
                return SCRIPT2BASE[UScript.getScript(cp)];
            }
            return -1;
        }

        private int getMetrics(int cp) {
            fillBuffer(cp);
            double totalWidth = 0.0d;
            for (FontMetrics m : metrics) {
                double width = getTotalWidth(m);
                if (baseChar >= 0) {
                    fillBuffer(baseChar);
                    width -= getTotalWidth(m) / bufferLen;
                    fillBuffer(cp);
                } else {
                    width /= bufferLen;
                }
                totalWidth += width;
            }
            int result = (int) (totalWidth / metrics.length / 10.0d + 0.499999d);
            // if (result == 0 && totalWidth != 0.0d) {
            // result = 1;
            // adjusted.add(cp);
            // }
            if (result > 31 || result < -2) { // just to catch odd results
                throw new IllegalArgumentException("Value too large " + result);
            }
            return result;
        }

        private double getTotalWidth(FontMetrics fontMetrics) {
            Rectangle2D rect1 = fontMetrics.getStringBounds(buffer, 0, bufferLen, g);
            return rect1.getWidth();
            // Rectangle2D rect2 = metrics2.getStringBounds(buffer, 0, bufferLen, g);
            // double rwidth2 = rect2.getWidth();
            // if (DEBUG && rwidth1 != rwidth2) {
            // System.out.println(Utility.hex(cp) + ", " + rwidth1 + ", " + rwidth2);
            // }
            // return (rwidth1 + rwidth2) / (2.0d * bufferLen);
        }

        private void fillBuffer(int cp) {
            baseChar = -1;
            if (SPACING_COMBINING.contains(cp)) {
                baseChar = getBase(cp);
                if (baseChar != -1) {
                    buffer[0] = (char) baseChar;
                    buffer[1] = (char) cp;
                    bufferLen = 2;
                    return;
                }
            }
            if (cp < 0x10000) {
                buffer[0] = buffer[1] = buffer[2] = (char) cp;
                bufferLen = 3;
            } else {
                char[] temp = UCharacter.toChars(cp);
                buffer[0] = buffer[2] = buffer[4] = temp[0];
                buffer[1] = buffer[3] = buffer[5] = temp[1];
                bufferLen = 6;
            }
        }
    }

    public void drawString(Graphics2D g, Font font2, String mainString,
        double wPercent, double w, double hPercent, double h) {
        g.setFont(font2);
        FontMetrics metrics = g.getFontMetrics();
        int ascent = metrics.getAscent();
        Rectangle2D bounds = metrics.getStringBounds(mainString, g);
        double x = wPercent * (w - bounds.getWidth());
        double y = hPercent * (h - bounds.getHeight());
        bounds.setRect(x, y, bounds.getWidth(), bounds.getHeight());

        g.setColor(Color.blue);
        g.draw(bounds);
        g.drawLine((int) x, (int) y + ascent, (int) (x + bounds.getWidth()), (int) y + ascent);

        g.setColor(Color.black);
        g.drawString(mainString, (int) x, (int) y + ascent);

        System.out.println(font2.getSize() + "\t" + string + " " + Integer.toHexString(string.codePointAt(0)));
    }

    public static void main(String argv[]) throws IOException {

        final GenerateApproximateWidths demo = new GenerateApproximateWidths();
        demo.init();
        Frame f = new Frame("Frame");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            public void windowDeiconified(WindowEvent e) {
                demo.start();
            }

            public void windowIconified(WindowEvent e) {
                demo.stop();
            }
        });
        f.add(demo);
        f.pack();
        f.setSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT));
        f.show();
        demo.start();
    }

    @Override
    public void run() {
        repaint();
    }
}