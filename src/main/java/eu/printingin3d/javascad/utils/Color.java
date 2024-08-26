package eu.printingin3d.javascad.utils;

public class Color {
    /**
     * The color white.  In the default sRGB space.
     */
    public static final Color white = new Color(255, 255, 255);

    /**
     * The color white.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color WHITE = white;

    /**
     * The color light gray.  In the default sRGB space.
     */
    public static final Color lightGray = new Color(192, 192, 192);

    /**
     * The color light gray.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color LIGHT_GRAY = lightGray;

    /**
     * The color gray.  In the default sRGB space.
     */
    public static final Color gray = new Color(128, 128, 128);

    /**
     * The color gray.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color GRAY = gray;

    /**
     * The color dark gray.  In the default sRGB space.
     */
    public static final Color darkGray = new Color(64, 64, 64);

    /**
     * The color dark gray.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color DARK_GRAY = darkGray;

    /**
     * The color black.  In the default sRGB space.
     */
    public static final Color black = new Color(0, 0, 0);

    /**
     * The color black.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color BLACK = black;

    /**
     * The color red.  In the default sRGB space.
     */
    public static final Color red = new Color(255, 0, 0);

    /**
     * The color red.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color RED = red;

    /**
     * The color pink.  In the default sRGB space.
     */
    public static final Color pink = new Color(255, 175, 175);

    /**
     * The color pink.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color PINK = pink;

    /**
     * The color orange.  In the default sRGB space.
     */
    public static final Color orange = new Color(255, 200, 0);

    /**
     * The color orange.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color ORANGE = orange;

    /**
     * The color yellow.  In the default sRGB space.
     */
    public static final Color yellow = new Color(255, 255, 0);

    /**
     * The color yellow.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color YELLOW = yellow;

    /**
     * The color green.  In the default sRGB space.
     */
    public static final Color green = new Color(0, 255, 0);

    /**
     * The color green.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color GREEN = green;

    /**
     * The color magenta.  In the default sRGB space.
     */
    public static final Color magenta = new Color(255, 0, 255);

    /**
     * The color magenta.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color MAGENTA = magenta;

    /**
     * The color cyan.  In the default sRGB space.
     */
    public static final Color cyan = new Color(0, 255, 255);

    /**
     * The color cyan.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color CYAN = cyan;

    /**
     * The color blue.  In the default sRGB space.
     */
    public static final Color blue = new Color(0, 0, 255);

    /**
     * The color blue.  In the default sRGB space.
     *
     * @since 1.4
     */
    public static final Color BLUE = blue;

    /**
     * The color value.
     *
     * @serial
     * @see #getRGB
     */
    int value;

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF) << 0);
        testColorValueRange(r, g, b, a);
    }

    /**
     * Creates an opaque sRGB color with the specified combined RGB value
     * consisting of the red component in bits 16-23, the green component
     * in bits 8-15, and the blue component in bits 0-7.  The actual color
     * used in rendering depends on finding the best match given the
     * color space available for a particular output device.  Alpha is
     * defaulted to 255.
     *
     * @param rgb the combined RGB components
     * @see java.awt.image.ColorModel#getRGBdefault
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getRGB
     */
    public Color(int rgb) {
        value = 0xff000000 | rgb;
    }

    /**
     * Checks the color integer components supplied for validity.
     * Throws an {@link IllegalArgumentException} if the value is out of
     * range.
     *
     * @param r the Red component
     * @param g the Green component
     * @param b the Blue component
     **/
    private static void testColorValueRange(int r, int g, int b, int a) {
        boolean rangeError = false;
        String badComponentString = "";

        if (a < 0 || a > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Alpha";
        }
        if (r < 0 || r > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Red";
        }
        if (g < 0 || g > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Green";
        }
        if (b < 0 || b > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Blue";
        }
        if (rangeError == true) {
            throw new IllegalArgumentException("Color parameter outside of expected range:"
                    + badComponentString);
        }
    }

    /**
     * Returns the red component in the range 0-255 in the default sRGB
     * space.
     *
     * @return the red component.
     * @see #getRGB
     */
    public int getRed() {
        return (getRGB() >> 16) & 0xFF;
    }

    /**
     * Returns the green component in the range 0-255 in the default sRGB
     * space.
     *
     * @return the green component.
     * @see #getRGB
     */
    public int getGreen() {
        return (getRGB() >> 8) & 0xFF;
    }

    /**
     * Returns the blue component in the range 0-255 in the default sRGB
     * space.
     *
     * @return the blue component.
     * @see #getRGB
     */
    public int getBlue() {
        return (getRGB() >> 0) & 0xFF;
    }

    /**
     * Returns the alpha component in the range 0-255.
     *
     * @return the alpha component.
     * @see #getRGB
     */
    public int getAlpha() {
        return (getRGB() >> 24) & 0xff;
    }

    /**
     * Returns the RGB value representing the color in the default sRGB
     * (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are
     * blue).
     *
     * @return the RGB value of the color in the default sRGB
     * {@code ColorModel}.
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @since 1.0
     */
    public int getRGB() {
        return value;
    }

}
