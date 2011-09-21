package eu.hansolo.fxgtools.main

import groovy.xml.Namespace
import groovy.transform.TupleConstructor

import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.Font
import java.awt.font.TextLayout
import java.awt.Graphics2D
import java.awt.Color
import java.awt.Paint
import java.awt.Shape
import java.awt.BasicStroke
import java.awt.LinearGradientPaint
import java.awt.RadialGradientPaint

import java.awt.geom.RoundRectangle2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.GeneralPath
import java.awt.image.BufferedImage

import java.util.regex.Pattern
import java.util.regex.Matcher

import eu.hansolo.fxgtools.fxg.FxgElement
import eu.hansolo.fxgtools.fxg.FxgRectangle
import eu.hansolo.fxgtools.fxg.FxgEllipse
import eu.hansolo.fxgtools.fxg.FxgShape
import eu.hansolo.fxgtools.fxg.FxgLine
import eu.hansolo.fxgtools.fxg.FxgPath
import eu.hansolo.fxgtools.fxg.FxgFill
import eu.hansolo.fxgtools.fxg.FxgColor
import eu.hansolo.fxgtools.fxg.FxgRichText
import eu.hansolo.fxgtools.fxg.FxgLinearGradient
import eu.hansolo.fxgtools.fxg.FxgRadialGradient
import java.awt.font.TextAttribute
import java.text.AttributedString
import eu.hansolo.fxgtools.fxg.FxgFilter

import eu.hansolo.fxgtools.fxg.FxgNoFill
import eu.hansolo.fxgtools.fxg.FxgShadow
import eu.hansolo.fxgtools.fxg.JavaShadow
import java.awt.Dimension
import java.awt.geom.AffineTransform
import java.awt.font.FontRenderContext
import java.awt.geom.Rectangle2D
import java.awt.font.GlyphVector

/**
 * User: han.solo at muenster.de
 * Date: 27.08.11
 * Time: 18:42
 */
class FxgParser {

    // Variable declarations
    private final Namespace D = new Namespace("http://ns.adobe.com/fxg/2008/dt")
    private final Namespace FXG = new Namespace("http://ns.adobe.com/fxg/2008")
    private final Pattern E_PATTERN = Pattern.compile("^(E_)(.)*", Pattern.CASE_INSENSITIVE)
    private final Pattern RR_PATTERN = Pattern.compile("^(RR)([0-9]+)(_){1}(([0-9]*)(_){1})?(.)*", Pattern.CASE_INSENSITIVE)
    private final Pattern VAR_PATTERN = Pattern.compile("[\\n\\r\\t\\.:;]*")
    private final Pattern SPACE_PATTERN = Pattern.compile("[\\s\\-]+")
    private final Matcher E_MATCHER = E_PATTERN.matcher("")
    private final Matcher RR_MATCHER = RR_PATTERN.matcher("")
    private String lastNodeType
    private String elementName
    String fxgVersion
    double originalWidth
    double originalHeight
    private double width
    private double height
    private double scaleFactorX = 1.0
    private double scaleFactorY = 1.0
    double aspectRatio
    private double offsetX
    private double offsetY
    private double groupOffsetX
    private double groupOffsetY
    private double lastShapeAlpha
    private AffineTransform oldTransform
    private AffineTransform groupTransform
    @TupleConstructor()
    private class FxgStroke {
        BasicStroke stroke
        Color color
    }
    @TupleConstructor()
    private class FxgText {
        float x
        float y
        double rotation
        double scaleX
        double scaleY
        float fontSize
        AttributedString string
        Font font
        String fontFamily
        boolean underline
        boolean italic
        boolean bold
        boolean lineThrough
        Color color
        String text
    }
    @TupleConstructor()
    private class FxgPaint {
        Point2D start
        Point2D stop
        Point2D center
        float radius
        float[] fractions
        Color[] colors
        Color color

        Paint getPaint() {
            if (start != null && stop != null && fractions != null && colors != null) {
                return new LinearGradientPaint(start, stop, fractions, colors)
            } else if (center != null && radius != null && fractions != null && colors != null) {
                return new RadialGradientPaint(center, radius, fractions, colors)
            } else if (color != null) {
                return color
            }

            return null
        }
    }
    private class FxgPathReader
    {
        protected List path
        protected double scaleFactorX
        protected double scaleFactorY

        FxgPathReader(List newPath, final SCALE_FACTOR_X, final SCALE_FACTOR_Y){
            path = newPath
            scaleFactorX = SCALE_FACTOR_X
            scaleFactorY = SCALE_FACTOR_Y
        }
        String read() {
            path.remove(0)
        }
        double nextX() {
            read().toDouble() * scaleFactorX
        }
        double nextY() {
            read().toDouble() * scaleFactorY
        }
    }


    // ********************   P U B L I C   M E T H O D S   ************************************************************
    Map<String, BufferedImage> parse(final Node FXG, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        Map<String, BufferedImage> images = [:]
        prepareParameters(FXG, WIDTH, HEIGHT, KEEP_ASPECT)

        def layers
        if (FXG.Group[0].attribute(D.layerType) && FXG.Group[0].attribute(D.userLabel)) { // fxg contains page attribute
            layers = FXG.Group[0].findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        } else {                                                                          // fxg does not contain page (Fireworks standard)
            layers = FXG.Group.findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        }

        String layerName
        layers.eachWithIndex {def layer, int i ->
            layerName = images.keySet().contains(layer.attribute(D.userLabel)) ? layer.attribute(D.userLabel) : layer.attribute(D.userLabel) + "_$i"
            layerName = layerName.replaceAll(VAR_PATTERN, "")
            layerName = layerName.replaceAll(SPACE_PATTERN, "_")
            images[layerName] = createImage((int)(originalWidth * scaleFactorX), (int) (originalHeight * scaleFactorY), Transparency.TRANSLUCENT)

            final Graphics2D G2 = images[layerName].createGraphics()
            addRenderingHints(G2)
            oldTransform = G2.getTransform()
            convertLayer(layer, G2)
            G2.dispose()
        }
        return images
    }

    Map<String, BufferedImage> parse(final String FILE_NAME, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        return parse(new XmlParser().parse(new File(FILE_NAME)), WIDTH, HEIGHT, KEEP_ASPECT)
    }

    BufferedImage parseLayer(final Node FXG, final String LAYER_NAME, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        prepareParameters(FXG, WIDTH, HEIGHT, KEEP_ASPECT)

        def layer = FXG.Group.find {('layer' == it.attribute(D.type)) && (LAYER_NAME == it.attribute(D.userLabel))}

        final BufferedImage IMAGE = createImage((int) WIDTH, (int) HEIGHT, Transparency.TRANSLUCENT)
        final Graphics2D G2 = IMAGE.createGraphics()
        addRenderingHints(G2)
        oldTransform = G2.getTransform()
        convertLayer(layer, G2)

        G2.dispose()

        return IMAGE
    }

    BufferedImage parseLayer(final String FILE_NAME, final String LAYER_NAME, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        return parseLayer(new XmlParser().parse(new File(FILE_NAME)), LAYER_NAME, WIDTH, HEIGHT, KEEP_ASPECT)
    }

    Map<String, List<FxgElement>> getElements(final Node FXG) {
        Map<String, List<FxgElement>> elements = [:]

        originalWidth = (FXG.@viewWidth ?: 100).toInteger()
        originalHeight = (FXG.@viewHeight ?: 100).toInteger()

        width = originalWidth
        height = originalHeight

        scaleFactorX = 1.0
        scaleFactorY = 1.0

        aspectRatio = originalHeight / originalWidth

        def layers
        if (FXG.Group[0].attribute(D.layerType) && FXG.Group[0].attribute(D.userLabel)) { // fxg contains page attribute
            layers = FXG.Group[0].findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        } else {                                                                          // fxg does not contain page (Fireworks standard)
            layers = FXG.Group.findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        }
        String layerName
        int shapeIndex = 0
        layers.eachWithIndex {def layer, int i ->
            layerName = elements.keySet().contains(layer.attribute(D.userLabel)) ? layer.attribute(D.userLabel) : layer.attribute(D.userLabel) + "_$i"
            layerName = layerName.replaceAll(VAR_PATTERN, "")
            layerName = layerName.replaceAll(SPACE_PATTERN, "_")
            List shapes = []
            shapeIndex = convertLayer(layerName, layer, elements, shapes, shapeIndex)
        }

        return elements
    }

    Map<String, List<FxgElement>> getElements(final String FILE_NAME) {
        return getElements(new XmlParser().parse(new File(FILE_NAME)))
    }

    Dimension getDimension(final Node FXG) {
        originalWidth = (int)(FXG.@viewWidth ?: 100).toDouble()
        originalHeight = (int)(FXG.@viewHeight ?: 100).toDouble()
        fxgVersion = FXG.@version
        return new Dimension((int) originalWidth, (int) originalHeight)
    }

    Dimension getDimension(final String FILE_NAME) {
        return getDimension(new XmlParser().parse(new File(FILE_NAME)))
    }

    // ********************   P R I V A T E   M E T H O D S   **********************************************************
    private RoundRectangle2D parseRectangle(final NODE) {
        double x = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width = (NODE.@width ?: 0).toDouble() * scaleFactorX
        double height = (NODE.@height ?: 0).toDouble() * scaleFactorY
        double scaleX = (NODE.@scaleX ?: 0).toDouble()
        double scaleY = (NODE.@scaleY ?: 0).toDouble()
        double rotation = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha = (NODE.@alpha ?: 1).toDouble()
        double radiusX = (NODE.@radiusX ?: 0).toDouble() * scaleFactorX
        double radiusY = (NODE.@radiusY ?: 0).toDouble() * scaleFactorY

        return new RoundRectangle2D.Double(x, y, width, height, radiusX, radiusY)
    }

    private Ellipse2D parseEllipse(final NODE) {
        double x = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width = (NODE.@width ?: 0).toDouble() * scaleFactorX
        double height = (NODE.@height ?: 0).toDouble() * scaleFactorY
        double scaleX = (NODE.@scaleX ?: 0).toDouble()
        double scaleY = (NODE.@scaleY ?: 0).toDouble()
        double rotation = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha = (NODE.@alpha ?: 1).toDouble()

        return new Ellipse2D.Double(x, y, width, height)
    }

    private Line2D parseLine(final NODE) {
        double xFrom = ((NODE.@xFrom ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yFrom = ((NODE.@yFrom ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double xTo = ((NODE.@xTo ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yTo = ((NODE.@yTo ?: 0).toDouble() + groupOffsetY) * scaleFactorX
        double scaleX = (NODE.@scaleX ?: 0).toDouble()
        double scaleY = (NODE.@scaleY ?: 0).toDouble()
        double rotation = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha = (NODE.@alpha ?: 1).toDouble()

        return new Line2D.Double(xFrom, yFrom, xTo, yTo)
    }

    private GeneralPath parsePath(final NODE) {
        String data = NODE.@data ?: ''
        double x = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double scaleX = (NODE.@scaleX ?: 0).toDouble()
        double scaleY = (NODE.@scaleY ?: 0).toDouble()
        double rotation = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha = (NODE.@alpha ?: 1).toDouble()
        String winding = (NODE.@winding ?: 'evenOdd')
        final GeneralPath PATH = new GeneralPath()

        if (winding == 'evenOdd') {
            PATH.setWindingRule(Path2D.WIND_EVEN_ODD)
        } else if (winding == 'nonZero') {
            PATH.setWindingRule(Path2D.WIND_NON_ZERO)
        }

        data = data.replaceAll(/([A-Za-z])/, / $1 /) // alle einzelnen Grossbuchstaben in blanks huellen
        def pathList = data.tokenize()
        def pathReader = new FxgPathReader(pathList, scaleFactorX, scaleFactorY)

        processPath(pathList, pathReader, PATH, x, y)

        return PATH
    }

    private FxgText parseRichText(final NODE) {
        FxgText fxgText = new FxgText()
        def fxgLabel = NODE.content[0].p[0]
        String text
        double fontSize
        String colorString
        if (fxgLabel.span) {
            // Adobe Illustrator
            text = NODE.content[0].p[0].span[0].text()
            fontSize = (NODE.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (NODE.@color ?: '#000000')
        } else {
            // Adobe Fireworks
            text = fxgLabel.text()
            fontSize = (fxgLabel.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (NODE.content.p.@color[0] ?: '#000000')
        }
        float x = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * (float) scaleFactorX
        float y = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * (float) scaleFactorY
        double rotation = ((NODE.@rotation ?: 0).toDouble())
        double scaleX = (NODE.@scaleX ?: 0).toDouble()
        double scaleY = (NODE.@scaleY ?: 0).toDouble()
        String fontFamily = (fxgLabel.@fontFamily ?: 'sans-serif')
        String fontStyle = (NODE.@fontStyle ?: 'normal')
        String textDecoration = (NODE.@textDecoration ?: 'none')
        String lineThrough = (NODE.@lineThrough ?: 'false')
        int alpha = (NODE.@alpha ?: 1).toDouble() * 255
        fxgText.x = x
        fxgText.y = (y + (float) fontSize)
        fxgText.rotation = rotation
        fxgText.scaleX = scaleX
        fxgText.scaleY = scaleY
        fxgText.fontSize = fontSize
        fxgText.fontFamily = fontFamily
        fxgText.color = parseColor(colorString, alpha)
        fxgText.bold = (fxgLabel.@fontWeight ?: 'normal') == 'bold'
        fxgText.italic = fontStyle == 'italic'
        fxgText.underline = textDecoration == 'underline'
        fxgText.lineThrough = lineThrough == 'true'
        int style = fxgText.italic ? Font.PLAIN | Font.ITALIC : Font.PLAIN
        fxgText.font = new Font(fontFamily, style, (float) fontSize)
        fxgText.text = text

        return fxgText
    }

    private FxgPaint parseFill(final NODE) {
        FxgPaint paint = new FxgPaint()
        if (NODE.fill) {
            def fill = NODE.fill[0]
            if (fill != null) {
                if (fill.SolidColor) {
                    convertSolidColor(paint, fill)
                }
                if (fill.LinearGradient) {
                    convertLinearGradient(paint, fill)
                }
                if (fill.RadialGradient) {
                    convertRadialGradient(paint, fill)
                }
            }
        }
        return paint
    }

    private FxgStroke parseStroke(final NODE) {
        FxgStroke fxgStroke = new FxgStroke()
        BasicStroke basicStroke = new BasicStroke(1f)
        Color color = Color.BLACK
        if (NODE.stroke) {
            def stroke = NODE.stroke
            if (stroke.SolidColorStroke) {
                def solidColorStroke = stroke[0].SolidColorStroke
                String colorString = (solidColorStroke[0].@color ?: '#000000')
                float weight = (solidColorStroke[0].@weight ?: 1f).toFloat()
                String caps = (solidColorStroke[0].@caps ?: 'round')
                String joints = (solidColorStroke[0].@joints ?: 'round')
                int alpha = ((solidColorStroke[0].@alpha ?: 1).toDouble() * lastShapeAlpha) * 255
                color =  parseColor(colorString, alpha)
                final CAP
                switch(caps){                    
                    case 'none':
                        CAP = BasicStroke.CAP_BUTT
                        break
                    case 'square':
                        CAP = BasicStroke.CAP_SQUARE
                        break
                    case 'round':
                        CAP = BasicStroke.CAP_ROUND
                        break
                }                                
                final int  JOIN
                switch(joints) {
                    case 'miter':
                        JOIN = BasicStroke.JOIN_MITER
                        break
                    case 'bevel':
                        JOIN = BasicStroke.JOIN_BEVEL
                        break
                    case 'round':
                        JOIN = BasicStroke.JOIN_ROUND
                    break
                }
                basicStroke = new BasicStroke(weight, CAP, JOIN)
            }
        }
        fxgStroke.stroke = basicStroke
        fxgStroke.color = color
        return fxgStroke
    }

    private AffineTransform parseTransform(final NODE) {
        AffineTransform transform = new AffineTransform()
        if (NODE.transform.Transform.matrix.Matrix) {
            def matrix = NODE.transform.Transform.matrix.Matrix
            double a = ((matrix.@a[0] ?: 0.0).toDouble()) // scaleX
            double b = ((matrix.@b[0] ?: 0.0).toDouble()) // shearY
            double c = ((matrix.@c[0] ?: 0.0).toDouble()) // shearX
            double d = ((matrix.@d[0] ?: 0.0).toDouble()) // scaleY
            double tx = ((matrix.@tx[0] ?: 0.0).toDouble() + groupOffsetX) * scaleFactorX // translateX
            double ty = ((matrix.@ty[0] ?: 0.0).toDouble() + groupOffsetY) * scaleFactorY // translateY
            transform.setTransform(a, b, c, d, tx, ty)
         }
        return transform
    }

    private void parseFilter(final NODE, final Graphics2D G2, final Shape SHAPE, final Paint SHAPE_PAINT) {
        if (NODE.DropShadowFilter) {
            BufferedImage innerShadowImage = null
            NODE.DropShadowFilter.each {def shadow->
                int angle = (shadow.@angle ?: 0).toInteger()
                String colorString = (shadow.@color ?: '#000000')
                int distance = (shadow.@distance ?: 0).toDouble() * scaleFactorX
                int alpha = ((shadow.@alpha ?: 1).toDouble() * lastShapeAlpha) * 255
                int blurX = (shadow.@blurX ?: 0).toDouble() * scaleFactorX
                //int blurY = (shadow.@blurY ?: 0).toDouble() * scaleFactorY
                boolean inner = (shadow.@inner ?: false)
                Color color = parseColor(colorString, alpha)

                if (inner) {  // inner shadow
                    innerShadowImage = JavaShadow.INSTANCE.createInnerShadow(SHAPE, SHAPE_PAINT, distance, (float)(alpha / 255), color, blurX, angle)
                    G2.drawImage(innerShadowImage, (int) SHAPE.bounds2D.x, (int) SHAPE.bounds2D.y, null)
                } else {  // drop shadow
                    G2.drawImage(JavaShadow.INSTANCE.createDropShadow(SHAPE, SHAPE_PAINT, distance, (float)(alpha / 255), color, blurX, angle), (int)SHAPE.bounds2D.x - blurX, (int)SHAPE.bounds2D.y - blurX, null)
                    if (innerShadowImage != null) {
                        G2.drawImage(innerShadowImage, (int) SHAPE.bounds2D.x, (int) SHAPE.bounds2D.y, null)
                        innerShadowImage = null
                    }
                }
            }
        }
    }

    private List<FxgFilter> parseFilter(final NODE) {
        if (NODE.DropShadowFilter) {
            List<FxgFilter> filters = []
            NODE.DropShadowFilter.each {def shadow->
                FxgShadow fxgFilter = new FxgShadow()
                fxgFilter.angle = (shadow.@angle ?: 0).toInteger()
                String colorString = (shadow.@color ?: '#000000')
                fxgFilter.distance = (shadow.@distance ?: 0).toDouble() * scaleFactorX
                fxgFilter.alpha = ((shadow.@alpha ?: 1).toDouble() * lastShapeAlpha) * 255
                fxgFilter.blurX = (shadow.@blurX ?: 0).toDouble() * scaleFactorX
                fxgFilter.blurY = (shadow.@blurY ?: 0).toDouble() * scaleFactorY
                fxgFilter.inner = (shadow.@inner ?: false)
                fxgFilter.color = parseColor(colorString, (int) fxgFilter.alpha)
                filters.add(fxgFilter)
            }
            return filters
        }
        return null
    }

    private Color parseColor(final NODE) {
        String color = (NODE.@color ?: '#000000')
        int alpha = ((NODE.@alpha ?: 1).toDouble() * lastShapeAlpha) * 255
        return parseColor(color, alpha)
    }

    private Color parseColor(final String COLOR, final int ALPHA) {
        assert COLOR.size() == 7
        int red = Integer.valueOf(COLOR[1..2], 16).intValue()
        int green = Integer.valueOf(COLOR[3..4], 16).intValue()
        int blue = Integer.valueOf(COLOR[5..6], 16).intValue()
        new Color(red, green, blue, ALPHA)
    }

    private processPath(final PATH_LIST, final FxgPathReader READER, final GeneralPath PATH, double x, double y) {
        while (PATH_LIST) {
            switch (READER.read()) {
                case "M":
                    PATH.moveTo(READER.nextX() + x, READER.nextY() + y)
                    break
                case "L":
                    PATH.lineTo(READER.nextX() + x, READER.nextY() + y)
                    break
                case "C":
                    PATH.curveTo(READER.nextX() + x, READER.nextY() + y, READER.nextX() + x, READER.nextY() + y, READER.nextX() + x, READER.nextY() + y)
                    break
                case "Q":
                    PATH.quadTo(READER.nextX() + x, READER.nextY() + y, READER.nextX() + x, READER.nextY() + y)
                    break
                case "Z":
                    PATH.closePath()
                    break
            }
        }
    }

    private convertSolidColor(paint, node) {
        paint.color = parseColor((node.SolidColor[0] ?: '#000000'))
    }

    private convertLinearGradient(paint, node) {
        def linearGradient = node.LinearGradient[0]
        double x1 = (linearGradient.@x ?: 0).toDouble() * scaleFactorX
        double y1 = (linearGradient.@y ?: 0).toDouble() * scaleFactorY
        double scaleX = (linearGradient.@scaleX ?: 0).toDouble()
        //double scaleY = (linearGradient.@scaleY ?: 1).toDouble()
        double rotation = Math.toRadians((linearGradient.@rotation ?: 0).toDouble())
        double x2 = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double y2 = Math.sin(rotation) * scaleX * scaleFactorY + y1

        Point2D start = new Point2D.Double(x1 + offsetX, y1 + offsetY)
        Point2D stop = new Point2D.Double(x2 + offsetX, y2 + offsetY)

        if (start == stop) {  // make sure that the gradient start point is different from the stop point
            stop.setLocation(stop.x, stop.y + 0.001)
        }

        def gradientEntries = linearGradient.GradientEntry
        float[] fractions = new float[gradientEntries.size()]
        Color[] colors = new Color[gradientEntries.size()]

        convertGradientEntries(gradientEntries, fractions, colors)

        paint.start = start
        paint.stop = stop
        paint.fractions = fractions
        paint.colors = colors
    }

    private convertRadialGradient(paint, node) {
        def radialGradient = node.RadialGradient[0]
        double x1 = (radialGradient.@x ?: 0).toDouble() * scaleFactorX + offsetX
        double y1 = (radialGradient.@y ?: 0).toDouble() * scaleFactorY + offsetY
        double scaleX = (radialGradient.@scaleX ?: 0).toDouble()
        //double scaleY = (radialGradient.@scaleY ?: 0).toDouble()
        double rotation = Math.toRadians((radialGradient.@rotation ?: 0).toDouble())
        double x2 = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double y2 = Math.sin(rotation) * scaleX * scaleFactorY + y1
        Point2D center = new Point2D.Double(x1, y1)
        Point2D stop = new Point2D.Double(x2, y2)
        float radius = (float) (center.distance(stop) / 2.0)
        def gradientEntries = radialGradient.GradientEntry
        float[] fractions = new float[gradientEntries.size()]
        Color[] colors = new Color[gradientEntries.size()]
        convertGradientEntries(gradientEntries, fractions, colors)
        paint.center = center
        paint.radius = radius ?: 0.001f
        paint.fractions = fractions
        paint.colors = colors
    }

    private convertGradientEntries(gradientEntries, float[] fractions, Color[] colors) {
        float fraction = 0
        float oldFraction = -1f
        Color color
        int alpha = 0
        def gradientMap = new HashMap<Float, java.awt.Color>(16)

        gradientEntries.each { def gradientEntry->
            fraction = (gradientEntry.@ratio ?: 0).toFloat()
            alpha = ((gradientEntry.@alpha ?: 1).toDouble() * lastShapeAlpha) * 255
            if (fraction.compareTo(oldFraction) == 0) { // make sure that the current fraction is different from the last
                fraction += 0.0001f
            }
            color = gradientEntry.@color == null ? Color.BLACK : parseColor(gradientEntry.@color, alpha)
            (gradientMap[fraction] = color)
            oldFraction = fraction
        }
        gradientMap = gradientMap.sort()

        gradientMap.keySet().eachWithIndex{ float frac, i->
            fractions[i] = frac
            colors[i] = gradientMap[frac]
            i.next()
        }
    }

    private void paintShape(final Graphics2D G2, final SHAPE, final NODE) {
        if (NODE.transform) {
            G2.setTransform(parseTransform(NODE))
        }
        if (NODE.fill) {
            G2.setPaint(parseFill(NODE).paint)
            G2.fill(SHAPE)
        }
        if (NODE.filters) {
            parseFilter(NODE.filters, G2, SHAPE, G2.paint)
        }
        if (NODE.stroke) {
            FxgStroke fxgStroke = parseStroke(NODE)
            G2.setColor(fxgStroke.color)
            G2.setStroke(fxgStroke.stroke)
            G2.draw(SHAPE)
        }
    }

    private void convertLayer(final LAYER, final Graphics2D G2) {
        String elementName
        LAYER.each {Node node->
            Shape shape
            Paint paint
            FxgStroke stroke
            switch(node.name()) {
                case FXG.Group:
                    groupOffsetX = (node.@x ?: 0).toDouble()
                    groupOffsetY = (node.@y ?: 0).toDouble()
                    elementName = node.attribute(D.userLabel)
                    G2.setTransform(oldTransform)
                    paintShape(G2, null, node)
                    convertLayer(node, G2)
                    break
                case FXG.Rect:
                    shape = parseRectangle(node)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    paintShape(G2, shape, node)
                    G2.setTransform(oldTransform)
                    break
                case FXG.Ellipse:
                    shape = parseEllipse(node)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    paintShape(G2, shape, node)
                    G2.setTransform(oldTransform)
                    break
                case FXG.Line:
                    shape = parseLine(node)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    paintShape(G2, shape, node)
                    G2.setTransform(oldTransform)
                    break
                case FXG.Path:
                    offsetX = groupOffsetX
                    offsetY = groupOffsetY
                    elementName = node.attribute(D.userLabel) ?: elementName
                    shape = parsePath(node)
                    if (elementName != null) {
                        E_MATCHER.reset(elementName)
                        if (E_MATCHER.matches()) {
                            shape = new Ellipse2D.Double(shape.bounds2D.x, shape.bounds2D.y, shape.bounds2D.width, shape.bounds2D.height)
                        }
                        RR_MATCHER.reset(elementName)
                        if (RR_MATCHER.matches()) {
                            double cornerRadius = RR_MATCHER.group(4) == null ? RR_MATCHER.group(2).toDouble() * scaleFactorX * 2 : (RR_MATCHER.group(2) + "." + RR_MATCHER.group(5)).toDouble() * scaleFactorX * 2
                            shape = new RoundRectangle2D.Double(shape.bounds2D.x, shape.bounds2D.y, shape.bounds2D.width, shape.bounds2D.height, cornerRadius, cornerRadius)
                        }
                    }
                    paintShape(G2, shape, node)
                    G2.setTransform(oldTransform)
                    break
                case FXG.RichText:
                    def fxgText = parseRichText(node)
                    final AttributedString STRING = new AttributedString(fxgText.text)
                    STRING.addAttribute(TextAttribute.FONT, fxgText.font)
                    if (fxgText.bold){
                        STRING.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)
                    }
                    if (fxgText.underline) {
                        STRING.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)
                    }
                    if (fxgText.lineThrough) {
                        STRING.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON)
                    }
                    G2.setFont(fxgText.font)
                    float offsetY = fxgText.y - (new TextLayout(fxgText.text, G2.font, G2.fontRenderContext)).descent
                    G2.setPaint(fxgText.color)
                    AffineTransform oldTransform = new AffineTransform()
                    boolean transformActive = false
                    if (node.transform) {
                        oldTransform = G2.getTransform()
                        G2.setTransform(parseTransform(node))
                        transformActive = true
                    }
                    if (fxgText.rotation != 0) {
                        G2.rotate(Math.toRadians(fxgText.rotation), fxgText.x, offsetY)
                    }

                    G2.drawString(STRING.iterator, fxgText.x, offsetY)

                    if (transformActive) {
                        G2.setTransform(oldTransform)
                    }
                    if (fxgText.rotation != 0) {
                        G2.rotate(Math.toRadians(-fxgText.rotation), fxgText.x, offsetY)
                    }
                    break
            }
        }
    }

    private int convertLayer(final String LAYER_NAME, final Node LAYER, Map<String, List<FxgElement>> elements, List shapes, int index) {
        LAYER.eachWithIndex {Node node, int i->
            Shape shape
            Paint paint
            FxgStroke stroke
            FxgShape fxgShape = null
            index += 1
            switch(node.name()) {
                case FXG.Group:
                    elementName = node.attribute(D.userLabel)?:"Group"
                    lastNodeType = "Group"
                    groupOffsetX = (node.@x ?: 0).toDouble()
                    groupOffsetY = (node.@y ?: 0).toDouble()
                    // Take group transforms into account
                    if (node.transform) {
                        groupTransform = parseTransform(node)
                    } else {
                        groupTransform = null
                    }
                    convertLayer(LAYER_NAME, node, elements, shapes, index)
                    break
                case FXG.Rect:
                    elementName = node.attribute(D.userLabel)?:"Rectangle"
                    elementName += "_${i}"
                    shape = parseRectangle(node)
                    fxgShape = new FxgRectangle(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height, radiusX: ((RoundRectangle2D) shape).arcWidth, radiusY: ((RoundRectangle2D) shape).arcHeight, alpha: lastShapeAlpha)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    lastNodeType = "Rect"
                    break
                case FXG.Ellipse:
                    elementName = node.attribute(D.userLabel)?:"Ellipse"
                    elementName += "_${i}"
                    shape = parseEllipse(node)
                    fxgShape = new FxgEllipse(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height, alpha: lastShapeAlpha)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    lastNodeType = "Ellipse"
                    break
                case FXG.Line:
                    elementName = node.attribute(D.userLabel)?:"Line"
                    elementName += "_${i}"
                    shape = parseLine(node)
                    fxgShape = new FxgLine(layerName: LAYER_NAME, shapeName: elementName, x1: ((Line2D)shape).p1.x, y1: ((Line2D)shape).p1.y, x2: ((Line2D)shape).p2.x, y2: ((Line2D)shape).p2.y, alpha: lastShapeAlpha)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    lastNodeType = "Line"
                    break
                case FXG.Path:
                    elementName = lastNodeType == "Group" ?  elementName : node.attribute(D.userLabel)?:"Path"
                    elementName += "_${i}"
                    shape = parsePath(node)
                    if (elementName != null) {
                        E_MATCHER.reset(elementName)
                        if (E_MATCHER.matches()) {
                            shape = new Ellipse2D.Double(shape.bounds2D.x, shape.bounds2D.y, shape.bounds2D.width, shape.bounds2D.height)
                            fxgShape = new FxgEllipse(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height, alpha: lastShapeAlpha)
                            break
                        }
                        RR_MATCHER.reset(elementName)
                        if (RR_MATCHER.matches()) {
                            double cornerRadius = RR_MATCHER.group(4) == null ? RR_MATCHER.group(2).toDouble() * scaleFactorX : (RR_MATCHER.group(2) + "." + RR_MATCHER.group(5)).toDouble() * scaleFactorX
                            shape = new RoundRectangle2D.Double(shape.bounds2D.x, shape.bounds2D.y, shape.bounds2D.width, shape.bounds2D.height, cornerRadius, cornerRadius)
                            fxgShape = new FxgRectangle(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height, radiusX: ((RoundRectangle2D) shape).arcWidth, radiusY: ((RoundRectangle2D) shape).arcHeight, alpha: lastShapeAlpha)
                            break
                        }
                        fxgShape = new FxgPath(layerName: LAYER_NAME, shapeName: elementName, path: shape)
                    }
                    offsetX = groupOffsetX
                    offsetY = groupOffsetY
                    lastNodeType = "Path"
                    break
                case FXG.RichText:
                    elementName = node.attribute(D.userLabel)?:"Text"
                    elementName += "_${i}"
                    def fxgText = parseRichText(node)
                    FxgFill fxgFill = new FxgColor(layerName: LAYER_NAME, shapeName: elementName, hexColor: Integer.toHexString((int)(fxgText.color.RGB) & 0x00ffffff), alpha: (float)(fxgText.color.alpha / 255), color: fxgText.color)
                    fxgShape = new FxgRichText(layerName: LAYER_NAME, shapeName: elementName, x: fxgText.x, y: fxgText.y, text: fxgText.text, fill: fxgFill, font: fxgText.font, italic: fxgText.italic, bold: fxgText.bold, underline: fxgText.underline, lineThrough: fxgText.lineThrough, fontFamily: fxgText.fontFamily, color: fxgText.color, rotation: fxgText.rotation)
                    lastNodeType = "RichText"
                    fxgShape.fill = fxgFill
                    break
            }
            if (fxgShape != null) {
                if (node.transform) {
                    fxgShape.transform = parseTransform(node)
                    fxgShape.transformed = true
                } else if (groupTransform != null) {
                    fxgShape.transform = groupTransform
                    fxgShape.transformed = true
                }
                if (node.fill) {
                    FxgFill fxgFill
                    paint = parseFill(node).paint
                    if (paint instanceof Color) {
                        fxgFill = new FxgColor(layerName: LAYER_NAME, shapeName: elementName, hexColor: Integer.toHexString((int) ((Color) paint).RGB & 0x00ffffff), alpha: (float)(((Color) paint).alpha / 255), color: (Color) paint)
                    } else if (paint instanceof LinearGradientPaint) {
                        fxgFill = new FxgLinearGradient(layerName: LAYER_NAME, shapeName: elementName, start: ((LinearGradientPaint) paint).startPoint, stop: ((LinearGradientPaint) paint).endPoint, fractions: ((LinearGradientPaint) paint).fractions, colors: ((LinearGradientPaint) paint).colors)
                    } else if (paint instanceof RadialGradientPaint) {
                        fxgFill = new FxgRadialGradient(layerName: LAYER_NAME, shapeName: elementName, center: ((RadialGradientPaint) paint).centerPoint, radius: ((RadialGradientPaint) paint).radius, fractions: ((RadialGradientPaint) paint).fractions, colors: ((RadialGradientPaint) paint).colors)
                    } else {
                        fxgFill = new FxgNoFill()
                    }
                    fxgShape.fill = fxgFill
                    fxgShape.filled = true
                } else {
                    if (node.name() != FXG.RichText) {
                        fxgShape.fill = new FxgNoFill()
                        fxgShape.filled = true
                    }
                }
                if (node.filters) {
                    fxgShape.filters = parseFilter(node.filters)
                }
                if (node.stroke) {
                    FxgStroke fxgStroke = parseStroke(node)
                    fxgShape.stroke = new eu.hansolo.fxgtools.fxg.FxgStroke(name: elementName, color: fxgStroke.color, stroke: fxgStroke.stroke)
                    fxgShape.stroked = true
                }
                fxgShape.referenceWidth = originalWidth
                fxgShape.referenceHeight = originalHeight
                shapes.add(new FxgElement(name: elementName, shape: fxgShape))

                groupTransform = null
            }
        }
        elements.put(LAYER_NAME, shapes)
        return index
    }

    private void prepareParameters(def fxg, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        offsetX = 0
        offsetY = 0
        groupOffsetX = 0
        groupOffsetY = 0

        fxgVersion = fxg.@version
        originalWidth = (int)(fxg.@viewWidth ?: 100).toDouble()
        originalHeight = (int)(fxg.@viewHeight ?: 100).toDouble()

        width = WIDTH
        height = KEEP_ASPECT ? WIDTH * (originalHeight / originalWidth) : HEIGHT

        aspectRatio = originalHeight / originalWidth

        scaleFactorX = width / originalWidth
        scaleFactorY = height / originalHeight
    }

    private BufferedImage createImage(final int WIDTH, final int HEIGHT, final int TRANSPARENCY) {
        final GraphicsConfiguration GFX_CONF = GraphicsEnvironment.localGraphicsEnvironment.defaultScreenDevice.defaultConfiguration
        if (WIDTH <= 0 || HEIGHT <= 0) {
            return GFX_CONF.createCompatibleImage(1, 1, TRANSPARENCY)
        }
        final BufferedImage IMAGE = GFX_CONF.createCompatibleImage(WIDTH, HEIGHT, TRANSPARENCY)
        return IMAGE
    }

    private void addRenderingHints(final Graphics2D G2) {
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }
}
