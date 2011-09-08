package eu.hansolo.fxgtools.fxg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/** Author: hansolo Date  : 08.09.11 Time  : 12:42 */
public enum JavaShadow {
    INSTANCE;

    public BufferedImage createInnerShadow(final Shape SHAPE, final Paint SHAPE_PAINT, final int DISTANCE, final float ALPHA, final Color SHADOW_COLOR, final int BLUR, final int ANGLE) {
        final float COLOR_CONSTANT = 1f / 255f;
        final float RED = COLOR_CONSTANT * SHADOW_COLOR.getRed();
        final float GREEN = COLOR_CONSTANT * SHADOW_COLOR.getGreen();
        final float BLUE = COLOR_CONSTANT * SHADOW_COLOR.getBlue();
        final float MAX_STROKE_WIDTH = BLUR * 2;
        final float ALPHA_STEP = 1f / (2 * BLUR + 2) * ALPHA;
        final float TRANSLATE_X = (float) (DISTANCE * Math.cos(Math.toRadians(ANGLE)));
        final float TRANSLATE_Y = (float) (DISTANCE * Math.sin(Math.toRadians(ANGLE)));
        final BufferedImage SOFTCLIP_IMAGE = createSoftClipImage(SHAPE, SHAPE_PAINT);
        final Graphics2D G2 = SOFTCLIP_IMAGE.createGraphics();
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G2.translate(-SHAPE.getBounds2D().getX(), -SHAPE.getBounds2D().getY());
        G2.setColor(new Color(RED, GREEN, BLUE, ALPHA_STEP));
        G2.setComposite(AlphaComposite.SrcAtop);
        G2.translate(TRANSLATE_X, -TRANSLATE_Y);
        for (float strokeWidth = BLUR; Float.compare(strokeWidth, 1) >= 0; strokeWidth -= 1) {
            G2.setStroke(new BasicStroke((float)(MAX_STROKE_WIDTH * Math.pow(0.85, strokeWidth))));
            G2.draw(SHAPE);
        }
        G2.dispose();
        return SOFTCLIP_IMAGE;
    }

    public BufferedImage createDropShadow(final Shape SHAPE, final Paint SHAPE_PAINT, final int DISTANCE, final float ALPHA, final Color SHADOW_COLOR, final int BLUR, final int ANGLE) {
        //final float TRANSLATE_X = (float) (DISTANCE * Math.cos(Math.toRadians(360 - ANGLE)));
        //final float TRANSLATE_Y = (float) (DISTANCE * Math.sin(Math.toRadians(360 - ANGLE)));
        final float TRANSLATE_X = (float) (DISTANCE * Math.cos(Math.toRadians(ANGLE)));
        final float TRANSLATE_Y = (float) (DISTANCE * Math.sin(Math.toRadians(ANGLE)));
        final BufferedImage SOFTCLIP_IMAGE = createSoftClipImage(SHAPE, SHAPE_PAINT);
        final BufferedImage SHADOW_IMAGE = renderDropShadow(SOFTCLIP_IMAGE, BLUR, ALPHA, SHADOW_COLOR);
        final BufferedImage RESULT = new BufferedImage(SHADOW_IMAGE.getWidth(), SHADOW_IMAGE.getHeight(), BufferedImage.TYPE_INT_ARGB);

        final Graphics2D G2 = RESULT.createGraphics();
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G2.translate(TRANSLATE_X, TRANSLATE_Y);
        G2.drawImage(SHADOW_IMAGE, 0, 0, null);
        G2.translate(-TRANSLATE_X, -TRANSLATE_Y);
        G2.translate(BLUR , BLUR);
        G2.drawImage(SOFTCLIP_IMAGE, 0, 0, null);
        G2.dispose();

        return RESULT;
    }

    public BufferedImage renderDropShadow(final BufferedImage IMAGE, final int BLUR, final float ALPHA, final java.awt.Color SHADOW_COLOR) {
        // Written by Sesbastien Petrucci
        final int SHADOW_SIZE = BLUR * 2;

        final int SRC_WIDTH = IMAGE.getWidth();
        final int SRC_HEIGHT = IMAGE.getHeight();

        final int DST_WIDTH = SRC_WIDTH + SHADOW_SIZE;
        final int DST_HEIGHT = SRC_HEIGHT + SHADOW_SIZE;

        final int LEFT = BLUR;
        final int RIGHT = SHADOW_SIZE - LEFT;

        final int Y_STOP = DST_HEIGHT - RIGHT;

        final int SHADOW_RGB = SHADOW_COLOR.getRGB() & 0x00FFFFFF;
        int[] aHistory = new int[SHADOW_SIZE];
        int historyIdx;

        int aSum;

        final BufferedImage DST = new BufferedImage(DST_WIDTH, DST_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        int[] dstBuffer = new int[DST_WIDTH * DST_HEIGHT];
        int[] srcBuffer = new int[SRC_WIDTH * SRC_HEIGHT];

        getPixels(IMAGE, 0, 0, SRC_WIDTH, SRC_HEIGHT, srcBuffer);

        final int LAST_PIXEL_OFFSET = RIGHT * DST_WIDTH;
        final float H_SUM_DIVIDER = 1.0f / SHADOW_SIZE;
        final float V_SUM_DIVIDER = ALPHA / SHADOW_SIZE;

        int max;

        int[] hSumLookup = new int[256 * SHADOW_SIZE];
        max = hSumLookup.length;

        for (int i = 0; i < max; i++)
        {
            hSumLookup[i] = (int) (i * H_SUM_DIVIDER);
        }

        int[] vSumLookup = new int[256 * SHADOW_SIZE];
        max = vSumLookup.length;
        for (int i = 0; i < max; i++) {
            vSumLookup[i] = (int) (i * V_SUM_DIVIDER);
        }

        int srcOffset;

        // horizontal pass  extract the alpha mask from the source picture and
        // blur it into the destination picture
        for (int srcY = 0, dstOffset = LEFT * DST_WIDTH; srcY < SRC_HEIGHT; srcY++) {
            // first pixels are empty
            for (historyIdx = 0; historyIdx < SHADOW_SIZE;) {
                aHistory[historyIdx++] = 0;
            }

            aSum = 0;
            historyIdx = 0;
            srcOffset = srcY * SRC_WIDTH;

            // compute the blur average with pixels from the source image
            for (int srcX = 0; srcX < SRC_WIDTH; srcX++) {
                int a = hSumLookup[aSum];
                dstBuffer[dstOffset++] = a << 24; // store the alpha value only
                // the shadow color will be added in the next pass

                aSum -= aHistory[historyIdx]; // substract the oldest pixel from the sum

                // extract the new pixel ...
                a = srcBuffer[srcOffset + srcX] >>> 24;
                aHistory[historyIdx] = a; // ... and store its value into history
                aSum += a; // ... and add its value to the sum

                if (++historyIdx >= SHADOW_SIZE) {
                    historyIdx -= SHADOW_SIZE;
                }
            }

            // blur the end of the row - no new pixels to grab
            for (int i = 0; i < SHADOW_SIZE; i++) {
                final int A = hSumLookup[aSum];
                dstBuffer[dstOffset++] = A << 24;

                // substract the oldest pixel from the sum ... and nothing new to add !
                aSum -= aHistory[historyIdx];

                if (++historyIdx >= SHADOW_SIZE) {
                    historyIdx -= SHADOW_SIZE;
                }
            }
        }

        // vertical pass
        for (int x = 0, bufferOffset = 0; x < DST_WIDTH; x++, bufferOffset = x) {
            aSum = 0;

            // first pixels are empty
            for (historyIdx = 0; historyIdx < LEFT;) {
                aHistory[historyIdx++] = 0;
            }

            // and then they come from the dstBuffer
            for (int y = 0; y < RIGHT; y++, bufferOffset += DST_WIDTH) {
                final int A = dstBuffer[bufferOffset] >>> 24; // extract alpha
                aHistory[historyIdx++] = A; // store into history
                aSum += A; // and add to sum
            }

            bufferOffset = x;
            historyIdx = 0;

            // compute the blur avera`ge with pixels from the previous pass
            for (int y = 0; y < Y_STOP; y++, bufferOffset += DST_WIDTH) {

                int a = vSumLookup[aSum];
                dstBuffer[bufferOffset] = a << 24 | SHADOW_RGB; // store alpha value + shadow color

                aSum -= aHistory[historyIdx]; // substract the oldest pixel from the sum

                a = dstBuffer[bufferOffset + LAST_PIXEL_OFFSET] >>> 24; // extract the new pixel ...
                aHistory[historyIdx] = a; // ... and store its value into history
                aSum += a; // ... and add its value to the sum

                if (++historyIdx >= SHADOW_SIZE) {
                    historyIdx -= SHADOW_SIZE;
                }
            }

            // blur the end of the column - no pixels to grab anymore
            for (int y = Y_STOP; y < DST_HEIGHT; y++, bufferOffset += DST_WIDTH){
                final int A = vSumLookup[aSum];
                dstBuffer[bufferOffset] = A << 24 | SHADOW_RGB;

                aSum -= aHistory[historyIdx]; // substract the oldest pixel from the sum

                if (++historyIdx >= SHADOW_SIZE){
                    historyIdx -= SHADOW_SIZE;
                }
            }
        }

        setPixels(DST, 0, 0, DST_WIDTH, DST_HEIGHT, dstBuffer);

        return DST;
    }

    public int[] getPixels(final BufferedImage IMAGE, final int X, final int Y, final int W, final int H, int[] pixels) {
        if (W == 0 || H == 0) {
            return new int[0];
        }

        if (pixels == null) {
            pixels = new int[W * H];
        } else if (pixels.length < W * H) {
            throw new IllegalArgumentException("pixels array must have a length" + " >= w*h");
        }

        int imageType = IMAGE.getType();
        if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
            Raster raster = IMAGE.getRaster();
            return (int[]) raster.getDataElements(X, Y, W, H, pixels);
        }
        return IMAGE.getRGB(X, Y, W, H, pixels, 0, W);
    }

    public void setPixels(final BufferedImage IMAGE, final int X, final int Y, final int W, final int H, int[] pixels) {
        if (pixels == null || W == 0 || H == 0) {
            return;
        } else if (pixels.length < W * H) {
            throw new IllegalArgumentException("pixels array must have a length" + " >= w*h");
        }

        int imageType = IMAGE.getType();
        if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
            WritableRaster raster = IMAGE.getRaster();
            raster.setDataElements(X, Y, W, H, pixels);
        } else {
            IMAGE.setRGB(X, Y, W, H, pixels, 0, W);
        }
    }

    public BufferedImage createSoftClipImage(final Shape SHAPE, final Paint SHAPE_PAINT) {
        final GraphicsConfiguration GFX_CONF = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        final BufferedImage IMAGE = GFX_CONF.createCompatibleImage(SHAPE.getBounds().width, SHAPE.getBounds().height, Transparency.TRANSLUCENT);
        final Graphics2D G2 = IMAGE.createGraphics();
        G2.setComposite(AlphaComposite.Clear);
        G2.fillRect(0, 0, IMAGE.getWidth(), IMAGE.getHeight());
        G2.setComposite(AlphaComposite.Src);
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G2.setPaint(SHAPE_PAINT);
        G2.translate(-SHAPE.getBounds2D().getX(), -SHAPE.getBounds2D().getY());
        G2.fill(SHAPE);
        return IMAGE;
    }
}
