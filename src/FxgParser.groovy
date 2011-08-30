import groovy.xml.Namespace
import groovy.transform.TupleConstructor
import java.awt.AlphaComposite
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
import java.awt.geom.AffineTransform

import java.awt.geom.RoundRectangle2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.GeneralPath
import java.awt.image.BufferedImage
import java.util.regex.Pattern
import java.util.regex.Matcher

import fxg.FxgElement
import fxg.FxgRectangle
import fxg.FxgEllipse
import fxg.FxgShape
import fxg.FxgLine
import fxg.FxgPath
import fxg.FxgFill
import fxg.FxgColor
import fxg.FxgRichText
import fxg.FxgLinearGradient
import fxg.FxgRadialGradient
import java.awt.font.TextAttribute
import java.text.AttributedString
import fxg.FxgFilter
import fxg.FxgDropShadow

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 27.08.11
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
class FxgParser {

    // Variable declarations
    private final Namespace D = new Namespace("http://ns.adobe.com/fxg/2008/dt")
    private final Namespace FXG = new Namespace("http://ns.adobe.com/fxg/2008")
    private final Pattern E_PATTERN = Pattern.compile("^(E_)(.)*", Pattern.CASE_INSENSITIVE)
    private final Pattern RR_PATTERN = Pattern.compile("^(RR)([0-9]+)(_){1}(([0-9]*)(_){1})?(.)*", Pattern.CASE_INSENSITIVE)
    private final Matcher E_MATCHER = E_PATTERN.matcher("")
    private final Matcher RR_MATCHER = RR_PATTERN.matcher("")
    private int shapeIndex
    private String elementName
    private String lastNodeType
    double originalWidth
    double originalHeight
    private double width
    private double height
    private double scaleFactorX = 1.0
    private double scaleFactorY = 1.0
    double aspectRatio
    private double offsetX;
    private double offsetY;
    @TupleConstructor()
    private class FxgStroke {
        BasicStroke stroke
        Color color
    }
    @TupleConstructor()
    private class FxgText {
        float x
        float y
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
            } else {
                return null
            }
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
    public Map<String, BufferedImage> parse(final Node FXG, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        Map<String, BufferedImage> images = [:]
        prepareParameters(FXG, WIDTH, HEIGHT, KEEP_ASPECT)

        def layers
        if (FXG.Group[0].attribute(D.layerType) && FXG.Group[0].attribute(D.userLabel)) { // fxg contains page attribute
            layers = FXG.Group[0].findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        } else {                                                                          // fxg does not contain page (Fireworks standard)
            layers = FXG.Group.findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        }

        String layerName
        layers.each {def layer ->
            layerName = images.keySet().contains(layer.attribute(D.userLabel)) ? layer.attribute(D.userLabel) : layer.attribute(D.userLabel) + "_1"
            images[layerName] = createImage((int)(originalWidth * scaleFactorX), (int) (originalHeight * scaleFactorY), Transparency.TRANSLUCENT)

            final Graphics2D G2 = images[layerName].createGraphics()
            addRenderingHints(G2)
            convertLayer(layer, G2)
            G2.dispose()
        }
        return images
    }

    public BufferedImage parseLayer(final Node FXG, final String LAYER_NAME, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        prepareParameters(FXG, WIDTH, HEIGHT, KEEP_ASPECT)

        def layer = FXG.Group.find {('layer' == it.attribute(D.type)) && (LAYER_NAME == it.attribute(D.userLabel))}

        final BufferedImage IMAGE = createImage((int) WIDTH, (int) HEIGHT, Transparency.TRANSLUCENT)
        final Graphics2D G2 = IMAGE.createGraphics()
        addRenderingHints(G2)

        convertLayer(layer, G2)

        G2.dispose()

        return IMAGE
    }

    public Map<String, List<FxgElement>> getElements(final Node FXG) {
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
        layers.each {def layer ->
            layerName = elements.keySet().contains(layer.attribute(D.userLabel)) ? layer.attribute(D.userLabel) : layer.attribute(D.userLabel) + "_1"
            shapeIndex = 0
            List shapes = []
            convertLayer(layerName, layer, elements, shapes)
        }

        return elements
    }

    // ********************   P R I V A T E   M E T H O D S   **********************************************************
    private RoundRectangle2D parseRectangle(final NODE) {
        double x = (NODE.@x ?: 0).toDouble() * scaleFactorX
        double y = (NODE.@y ?: 0).toDouble() * scaleFactorY
        double width = (NODE.@width ?: 0).toDouble() * scaleFactorX
        double height = (NODE.@height ?: 0).toDouble() * scaleFactorY
        //double scaleX = (NODE.@scaleX ?: 0).toDouble()
        //double scaleY = (NODE.@scaleY ?: 0).toDouble()
        //double rotation = (NODE.@rotation ?: 0).toDouble()
        int alpha = (NODE.@alpha ?: 1).toDouble() * 255
        double radiusX = (NODE.@radiusX ?: 0).toDouble() * scaleFactorX
        double radiusY = (NODE.@radiusY ?: 0).toDouble() * scaleFactorY

        if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
            return new RoundRectangle2D.Double(x, y, width, height, 0, 0)
        } else {
            return new RoundRectangle2D.Double(x, y, width, height, radiusX, radiusY)
        }
    }

    private Ellipse2D parseEllipse(final NODE) {
        double x = (NODE.@x ?: 0).toDouble() * scaleFactorX
        double y = (NODE.@y ?: 0).toDouble() * scaleFactorY
        double width = (NODE.@width ?: 0).toDouble() * scaleFactorX
        double height = (NODE.@height ?: 0).toDouble() * scaleFactorY
        //double scaleX = (NODE.@scaleX ?: 0).toDouble()
        //double scaleY = (NODE.@scaleY ?: 0).toDouble()
        //double rotation = (NODE.@rotation ?: 0).toDouble()
        //int alpha = (NODE.@alpha ?: 1).toDouble() * 255

        return new Ellipse2D.Double(x, y, width, height)
    }

    private Line2D parseLine(final NODE) {
        double xFrom = (NODE.@xFrom ?: 0).toDouble() * scaleFactorX
        double yFrom = (NODE.@yFrom ?: 0).toDouble() * scaleFactorY
        double xTo = (NODE.@xTo ?: 0).toDouble() * scaleFactorX
        double yTo = (NODE.@yTo ?: 0).toDouble() * scaleFactorX
        //double scaleX = (NODE.@scaleX ?: 0).toDouble()
        //double scaleY = (NODE.@scaleY ?: 0).toDouble()
        //double rotation = (NODE.@rotation ?: 0).toDouble()
        //int alpha = (NODE.@alpha ?: 1).toDouble() * 255

        return new Line2D.Double(xFrom, yFrom, xTo, yTo)
    }

    private GeneralPath parsePath(final NODE) {
        String data = NODE.@data == null ? '' : NODE.@data
        double x = (NODE.@x ?: 0).toDouble() * scaleFactorX
        double y = (NODE.@y ?: 0).toDouble() * scaleFactorY
        //double scaleX = (NODE.@scaleX ?: 0).toDouble()
        //double scaleY = (NODE.@scaleY ?: 0).toDouble()
        //double rotation = (NODE.@rotation ?: 0).toDouble()
        //int alpha = (NODE.@alpha ?: 1).toDouble() * 255
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

        processPath(pathList, pathReader, PATH)

        return PATH
    }

    private FxgText parseRichText(final NODE) {
        FxgText fxgText = new FxgText()
        def fxgLabel = NODE.content[0].p[0]
        float x = (NODE.@x ?: 0).toDouble() * (float) scaleFactorX
        float y = (NODE.@y ?: 0).toDouble() * (float) scaleFactorY
        String fontFamily = (fxgLabel.@fontFamily ?: 'sans-serif')
        String fontStyle = (NODE.@fontStyle ?: 'normal')
        String textDecoration = (NODE.@textDecoration ?: 'none')
        String lineThrough = (NODE.@lineThrough ?: 'false')
        double fontSize = (fxgLabel.@fontSize ?: 10).toDouble() * scaleFactorX
        String colorString = (NODE.content.p.@color[0] ?: '#000000')
        int alpha = (NODE.@alpha ?: 1).toDouble() * 255
        fxgText.x = x
        fxgText.y = (y + (float) fontSize)
        fxgText.fontSize = fontSize
        fxgText.fontFamily = fontFamily
        fxgText.color = parseColor(colorString, alpha)
        fxgText.bold = (fxgLabel.@fontWeight ?: 'normal') == 'bold'
        fxgText.italic = fontStyle == 'italic'
        fxgText.underline = textDecoration == 'underline'
        fxgText.lineThrough = lineThrough == 'true'
        int style = fxgText.italic ? Font.PLAIN | Font.ITALIC : Font.PLAIN
        fxgText.font = new Font(fontFamily, style, (float) fontSize)
        fxgText.text = fxgLabel.text()

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
                String caps = (solidColorStroke[0].@caps ?: 'caps')
                String joints = (solidColorStroke[0].@joints ?: 'round')
                int alpha = (solidColorStroke[0].@alpha ?: 1).toDouble() * 255
                color =  parseColor(colorString, alpha)
                final int  JOIN
                switch(joints) {
                    case 'miter':
                        JOIN = BasicStroke.JOIN_MITER
                        break
                    case 'bevel':
                        JOIN = BasicStroke.JOIN_BEVEL
                        break
                    case 'round':
                    default:
                        JOIN = BasicStroke.JOIN_ROUND
                    break
                }
                final CAP
                switch(caps){
                    case 'none':
                        CAP = BasicStroke.CAP_BUTT
                        break
                    case 'square':
                        CAP = BasicStroke.CAP_SQUARE
                        break
                    case 'round':
                    default:
                        CAP = BasicStroke.CAP_ROUND
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
        def matrix = NODE.transform[0].Transform[0].matrix[0].Matrix[0]
        double matrixA = (matrix[0].@a ?: 1).toDouble()
        double matrixB = (matrix[0].@b ?: 1).toDouble()
        double matrixC = (matrix[0].@c ?: 1).toDouble()
        double matrixD = (matrix[0].@d ?: 1).toDouble()
        double matrixTx = (matrix[0].@tx ?: 1).toDouble()
        double matrixTy = (matrix[0].@ty ?: 1).toDouble()
    }

    private void parseFilter(final NODE, final Graphics2D G2, final Shape SHAPE, final Paint SHAPE_PAINT) {
        if (NODE.DropShadowFilter) {
            NODE.DropShadowFilter.each {def shadow->
                int angle = (shadow.@angle ?: 0).toInteger()
                String colorString = (shadow.@color ?: '#000000')
                int distance = (shadow.@distance ?: 0).toDouble() * scaleFactorX
                int alpha = (shadow.@alpha ?: 1).toDouble() * 255
                int blurX = (shadow.@blurX ?: 0).toDouble() * scaleFactorX
                int blurY = (shadow.@blurY ?: 0).toDouble() * scaleFactorY
                boolean inner = (shadow.@inner ?: false)
                Color color = parseColor(colorString, alpha)

                if (inner) {  // inner shadow
                    G2.drawImage(createInnerShadow(SHAPE, SHAPE_PAINT, distance, alpha, color, blurX, angle), (int)SHAPE.getBounds2D().getX(), (int)SHAPE.getBounds2D().getY(), null)
                }             // dropShadow
            }
        }
    }

    private FxgFilter parseFilter(final NODE) {
        if (NODE.DropShadowFilter) {
            FxgDropShadow fxgFilter = new FxgDropShadow()
            NODE.DropShadowFilter.each {def shadow->
                fxgFilter.angle = (shadow.@angle ?: 0).toInteger()
                String colorString = (shadow.@color ?: '#000000')
                fxgFilter.distance = (shadow.@distance ?: 0).toDouble() * scaleFactorX
                fxgFilter.alpha = (shadow.@alpha ?: 1).toDouble() * 255
                fxgFilter.blurX = (shadow.@blurX ?: 0).toDouble() * scaleFactorX
                fxgFilter.blurY = (shadow.@blurY ?: 0).toDouble() * scaleFactorY
                fxgFilter.inner = (shadow.@inner ?: false)
                fxgFilter.color = parseColor(colorString, alpha)
            }
        }
        return null
    }

    private Color parseColor(final NODE) {
        String color = (NODE.@color ?: '#000000')
        int alpha = (NODE.@alpha ?: 1).toDouble() * 255
        return parseColor(color, alpha)
    }

    private Color parseColor(final String COLOR, final int ALPHA) {
        assert COLOR.size() == 7
        int red = Integer.valueOf(COLOR[1..2], 16).intValue()
        int green = Integer.valueOf(COLOR[3..4], 16).intValue()
        int blue = Integer.valueOf(COLOR[5..6], 16).intValue()
        new Color(red, green, blue, ALPHA)
    }

    private def processPath(final PATH_LIST, final FxgPathReader READER, final GeneralPath PATH) {
        while (PATH_LIST) {
            switch (READER.read()) {
                case "M":
                    PATH.moveTo(READER.nextX(), READER.nextY())
                    break
                case "L":
                    PATH.lineTo(READER.nextX(), READER.nextY())
                    break
                case "C":
                    PATH.curveTo(READER.nextX(), READER.nextY(), READER.nextX(), READER.nextY(), READER.nextX(), READER.nextY())
                    break
                case "Q":
                    PATH.quadTo(READER.nextX(), READER.nextY(), READER.nextX(), READER.nextY())
                    break
                case "Z":
                    PATH.closePath()
                    break
            }
        }
    }

    private def convertSolidColor(paint, node) {
        paint.color = parseColor((node.SolidColor[0] ?: '#000000'))
    }

    private def convertLinearGradient(paint, node) {
        def linearGradient = node.LinearGradient[0]
        double x1 = (linearGradient.@x ?: 0).toDouble() + offsetX
        double y1 = (linearGradient.@y ?: 0).toDouble() + offsetY
        double scaleX = (linearGradient.@scaleX ?: 1).toDouble()
        double scaleY = (linearGradient.@scaleY ?: 1).toDouble()
        double rotation = (linearGradient.@rotation ?: 0).toDouble()
        double x2 = Math.cos(Math.toRadians(rotation)) * scaleX + x1
        double y2 = Math.sin(Math.toRadians(rotation)) * scaleX + y1

        Point2D start = new Point2D.Double((x1 * scaleFactorX), (y1 * scaleFactorY))
        Point2D stop = new Point2D.Double((x2 * scaleFactorX), (y2 * scaleFactorY))

        if (start.equals(stop)) {  // make sure that the gradient start point is different from the stop point
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

    private def convertRadialGradient(paint, node) {
        def radialGradient = node.RadialGradient[0]
        double x1 = (radialGradient.@x ?: 0).toDouble() + offsetX
        double y1 = (radialGradient.@y ?: 0).toDouble() + offsetY
        double scaleX = (radialGradient.@scaleX ?: 0).toDouble()
        //double scaleY = (radialGradient.@scaleY ?: 0).toDouble()
        double rotation = (radialGradient.@rotation ?: 0).toDouble()
        double x2 = Math.cos(Math.toRadians(rotation)) * scaleX + x1
        double y2 = Math.sin(Math.toRadians(rotation)) * scaleX + y1
        Point2D center = new Point2D.Double((x1 * scaleFactorX), (y1 * scaleFactorY))
        Point2D stop = new Point2D.Double((x2 * scaleFactorX), (y2 * scaleFactorY))
        float radius = (float) (center.distance(stop) / 2)

        def gradientEntries = radialGradient.GradientEntry
        float[] fractions = new float[gradientEntries.size()]
        Color[] colors = new Color[gradientEntries.size()]

        convertGradientEntries(gradientEntries, fractions, colors)

        paint.center = center
        paint.radius = radius ?: 0.001f
        paint.fractions = fractions
        paint.colors = colors
    }

    private def convertGradientEntries(gradientEntries, float[] fractions, Color[] colors) {
        float fraction = 0f
        float oldFraction = -1f
        Color color
        int alpha = 0f
        def gradientMap = new HashMap<Float, java.awt.Color>(16)

        gradientEntries.each { def gradientEntry->
            fraction = (gradientEntry.@ratio ?: 0).toFloat()
            alpha = (gradientEntry.@alpha ?: 1).toDouble() * 255
            if (fraction.compareTo(oldFraction) == 0) { // make sure that the current fraction is different from the last
                fraction += 0.0001f
            }
            color = gradientEntry.@color == null ? Color.BLACK : parseColor(gradientEntry.@color, alpha)
            gradientMap.put(fraction, color)
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
        if (NODE.fill) {
            G2.setPaint(parseFill(NODE).getPaint())
            G2.fill(SHAPE)
        }
        if (NODE.filters) {
            parseFilter(NODE.filters, G2, SHAPE, G2.getPaint())
        }
        if (NODE.stroke) {
            FxgStroke fxgStroke = parseStroke(NODE)
            G2.setColor(fxgStroke.color)
            G2.setStroke(fxgStroke.stroke)
            G2.draw(SHAPE)
        }
    }

    private void convertLayer(final LAYER, final Graphics2D G2) {
        LAYER.each {Node node->
            Shape shape
            Paint paint
            FxgStroke stroke
            switch(node.name()) {
                case FXG.Group:
                    elementName = node.attribute(D.userLabel)
                    convertLayer(node, G2)
                    break
                case FXG.Rect:
                    shape = parseRectangle(node)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    paintShape(G2, shape, node)
                    break
                case FXG.Ellipse:
                    shape = parseEllipse(node)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    paintShape(G2, shape, node)
                    break
                case FXG.Line:
                    shape = parseLine(node)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    paintShape(G2, shape, node)
                    break
                case FXG.Path:
                    offsetX = 0
                    offsetY = 0
                    elementName = node.attribute(D.userLabel) ?: elementName
                    shape = parsePath(node)
                    if (elementName != null) {
                        E_MATCHER.reset(elementName)
                        if (E_MATCHER.matches()) {
                            shape = new Ellipse2D.Double(shape.bounds2D.getX(), shape.bounds2D.getY(), shape.bounds2D.getWidth(), shape.bounds2D.getHeight())
                        }
                        RR_MATCHER.reset(elementName)
                        if (RR_MATCHER.matches()) {
                            double cornerRadius = RR_MATCHER.group(4) == null ? RR_MATCHER.group(2).toDouble() * scaleFactorX * 2 : (RR_MATCHER.group(2) + "." + RR_MATCHER.group(5)).toDouble() * scaleFactorX * 2
                            shape = new RoundRectangle2D.Double(shape.bounds2D.getX(), shape.bounds2D.getY(), shape.bounds2D.getWidth(), shape.bounds2D.getHeight(), cornerRadius, cornerRadius)
                        }
                    }
                    paintShape(G2, shape, node)
                    break
                case FXG.RichText:
                    def fxgText = parseRichText(node)
                    final AttributedString STRING = new AttributedString(fxgText.text)
                    STRING.addAttribute(TextAttribute.FONT, fxgText.fontFamily)
                    STRING.addAttribute(TextAttribute.SIZE, (float) fxgText.fontSize)
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
                    G2.setPaint(fxgText.color)
                    G2.setFont(fxgText.font)
                    float offsetY = fxgText.y - (new TextLayout(fxgText.text, G2.getFont(), G2.getFontRenderContext())).descent
                    G2.drawString(STRING.getIterator(), fxgText.x, offsetY)
                    break
            }
        }
    }

    private void convertLayer(final String LAYER_NAME, final Node LAYER, Map<String, List<FxgElement>> elements, List shapes) {
        LAYER.each {Node node->
            Shape shape
            Paint paint
            FxgStroke stroke
            FxgShape fxgShape = null
            switch(node.name()) {
                case FXG.Group:
                    elementName = node.attribute(D.userLabel)?:"Group_${shapeIndex += 1}"
                    shapeIndex += 1
                    lastNodeType = "Group"
                    convertLayer(LAYER_NAME, node, elements, shapes)
                    break
                case FXG.Rect:
                    elementName = node.attribute(D.userLabel)?:"Rectangle_${shapeIndex += 1}"
                    shape = parseRectangle(node)
                    fxgShape = new FxgRectangle(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height, radiusX: ((RoundRectangle2D) shape).arcWidth, radiusY: ((RoundRectangle2D) shape).arcHeight)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    lastNodeType = "Rect"
                    break
                case FXG.Ellipse:
                    elementName = node.attribute(D.userLabel)?:"Ellipse_${shapeIndex += 1}"
                    shape = parseEllipse(node)
                    fxgShape = new FxgEllipse(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    lastNodeType = "Ellipse"
                    break
                case FXG.Line:
                    elementName = node.attribute(D.userLabel)?:"Line_${shapeIndex += 1}"
                    shape = parseLine(node)
                    fxgShape = new FxgLine(layerName: LAYER_NAME, shapeName: elementName, x1: ((Line2D)shape).p1.x, y1: ((Line2D)shape).p1.y, x2: ((Line2D)shape).p2.x, y2: ((Line2D)shape).p2.y)
                    offsetX = shape.bounds2D.x
                    offsetY = shape.bounds2D.y
                    lastNodeType = "Line"
                    break
                case FXG.Path:
                    elementName = lastNodeType == "Group" ?  elementName : node.attribute(D.userLabel)?:"Path_${shapeIndex += 1}"
                    shape = parsePath(node)
                    if (elementName != null) {
                        E_MATCHER.reset(elementName)
                        if (E_MATCHER.matches()) {
                            shape = new Ellipse2D.Double(shape.bounds2D.getX(), shape.bounds2D.getY(), shape.bounds2D.getWidth(), shape.bounds2D.getHeight())
                            fxgShape = new FxgEllipse(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height)
                            break;
                        }
                        RR_MATCHER.reset(elementName)
                        if (RR_MATCHER.matches()) {
                            double cornerRadius = RR_MATCHER.group(4) == null ? RR_MATCHER.group(2).toDouble() * scaleFactorX : (RR_MATCHER.group(2) + "." + RR_MATCHER.group(5)).toDouble() * scaleFactorX
                            shape = new RoundRectangle2D.Double(shape.bounds2D.getX(), shape.bounds2D.getY(), shape.bounds2D.getWidth(), shape.bounds2D.getHeight(), cornerRadius, cornerRadius)
                            fxgShape = new FxgRectangle(layerName: LAYER_NAME, shapeName: elementName, x: shape.bounds2D.x, y: shape.bounds2D.y, width: shape.bounds2D.width, height: shape.bounds2D.height, radiusX: ((RoundRectangle2D) shape).arcWidth, radiusY: ((RoundRectangle2D) shape).arcHeight)
                            break;
                        }
                        fxgShape = new FxgPath(layerName: LAYER_NAME, shapeName: elementName, path: shape)
                    }
                    offsetX = 0
                    offsetY = 0
                    lastNodeType = "Path"
                    break
                case FXG.RichText:
                    elementName = node.attribute(D.userLabel)?:"Text_${shapeIndex += 1}"
                    def fxgText = parseRichText(node)
                    FxgFill fxgFill = new FxgColor(layerName: LAYER_NAME, shapeName: elementName, hexColor: Integer.toHexString((int)(fxgText.color.getRGB()) & 0x00ffffff), alpha: (float)(fxgText.color.alpha / 255), color: fxgText.color)
                    fxgShape = new FxgRichText(layerName: LAYER_NAME, shapeName: elementName, x: fxgText.x, y: fxgText.y, text: fxgText.text, fill: fxgFill, font: fxgText.font, italic: fxgText.italic, bold: fxgText.bold, underline: fxgText.underline, lineThrough: fxgText.lineThrough, fontFamily: fxgText.fontFamily)
                    lastNodeType = "RichText"
                    break
            }
            if (fxgShape != null) {
                if (node.fill) {
                    FxgFill fxgFill
                    paint = parseFill(node).getPaint()
                    if (paint instanceof Color) {
                        fxgFill = new FxgColor(layerName: LAYER_NAME, shapeName: elementName, hexColor: Integer.toHexString((int) ((Color) paint).getRGB() & 0x00ffffff), alpha: (float)(((Color) paint).alpha / 255), color: (Color) paint)
                    } else if (paint instanceof LinearGradientPaint) {
                        fxgFill = new FxgLinearGradient(layerName: LAYER_NAME, shapeName: elementName, start: ((LinearGradientPaint) paint).startPoint, stop: ((LinearGradientPaint) paint).endPoint, fractions: ((LinearGradientPaint) paint).fractions, colors: ((LinearGradientPaint) paint).colors)
                    } else if (paint instanceof RadialGradientPaint) {
                        fxgFill = new FxgRadialGradient(layerName: LAYER_NAME, shapeName: elementName, center: ((RadialGradientPaint) paint).centerPoint, radius: ((RadialGradientPaint) paint).radius, fractions: ((RadialGradientPaint) paint).fractions, colors: ((RadialGradientPaint) paint).colors)
                    } else {
                        fxgFill = null
                    }
                    fxgShape.fill = fxgFill
                    fxgShape.filled = true
                }
                if (node.stroke) {
                    FxgStroke fxgStroke = parseStroke(node)
                    fxgShape.stroke = new fxg.FxgStroke(name: elementName, color: fxgStroke.color, stroke: fxgStroke.stroke)
                    fxgShape.stroked = true
                }
                if (node.filter) {
                    fxgShape.filter = parseFilter(node)
                }
                fxgShape.referenceWidth = originalWidth
                fxgShape.referenceHeight = originalHeight
                shapes.add(new FxgElement(name: elementName, shape: fxgShape))
            }
        }
        elements.put(LAYER_NAME, shapes)
    }

    private void prepareParameters(def fxg, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        originalWidth = (int)(fxg.@viewWidth ?: 100).toDouble()
        originalHeight = (int)(fxg.@viewHeight ?: 100).toDouble()

        width = WIDTH
        height = KEEP_ASPECT ? WIDTH * (originalHeight / originalWidth) : HEIGHT

        aspectRatio = originalHeight / originalWidth

        scaleFactorX = width / originalWidth
        scaleFactorY = height / originalHeight
    }

    private BufferedImage createImage(final int WIDTH, final int HEIGHT, final int TRANSPARENCY) {
        final GraphicsConfiguration GFX_CONF = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
        if (WIDTH <= 0 || HEIGHT <= 0) {
            return GFX_CONF.createCompatibleImage(1, 1, TRANSPARENCY)
        }
        final BufferedImage IMAGE = GFX_CONF.createCompatibleImage(WIDTH, HEIGHT, TRANSPARENCY)
        return IMAGE
    }

    private void prepareSoftClipImage(final Graphics2D G2, final Shape SHAPE, final Paint SHAPE_PAINT) {
        G2.setComposite(java.awt.AlphaComposite.Clear)
        G2.fillRect(0, 0, (int) SHAPE.getBounds2D().getWidth(), (int) SHAPE.getBounds2D().getHeight())

        G2.setComposite(AlphaComposite.Src)
        addRenderingHints(G2)
        if (SHAPE_PAINT != null) {
            G2.setPaint(SHAPE_PAINT)
            G2.translate(-SHAPE.getBounds2D().getX(), -SHAPE.getBounds2D().getY())
            G2.fill(SHAPE)
        }
    }

    private BufferedImage createInnerShadow(final Shape SHAPE, final Paint SHAPE_PAINT, final int DISTANCE, final float ALPHA, final Color COLOR, final int BLUR, final int ANGLE) {
        final float COLOR_CONSTANT = 1f / 255f
        final float RED = COLOR_CONSTANT * COLOR.getRed()
        final float GREEN = COLOR_CONSTANT * COLOR.getGreen()
        final float BLUE = COLOR_CONSTANT * COLOR.getBlue()
        final float MAX_STROKE_WIDTH = BLUR * 1.5
        final float ALPHA_FACTOR = (ALPHA - 100) / (180 * BLUR - Math.pow(BLUR, 2))
        final double TRANSLATE_X = (DISTANCE * Math.cos(Math.toRadians(ANGLE)))
        final double TRANSLATE_Y = (DISTANCE * Math.sin(Math.toRadians(ANGLE)))
        final BufferedImage IMAGE = createImage((int)SHAPE.bounds2D.width, (int)SHAPE.bounds2D.height, Transparency.TRANSLUCENT)
        final Graphics2D G2 = IMAGE.createGraphics()
        prepareSoftClipImage(G2, SHAPE, SHAPE_PAINT)

        // Create the inner shadow
        G2.setComposite(AlphaComposite.SrcAtop)
        G2.translate(TRANSLATE_X, -TRANSLATE_Y)
        G2.setClip(SHAPE)
        float variableAlpha
        for (float strokeWidth = BLUR; strokeWidth.compareTo(1); strokeWidth -= 1) {
            variableAlpha = (1 - Math.pow(strokeWidth, -1.5)) * ALPHA_FACTOR
            G2.setColor(new Color(RED, GREEN, BLUE, variableAlpha))
            G2.setStroke(new BasicStroke((float)(MAX_STROKE_WIDTH * Math.pow(0.87 + BLUR / 1000, strokeWidth))))
            G2.draw(SHAPE)
        }

        G2.dispose()

        return IMAGE
    }

    private void addRenderingHints(final Graphics2D G2) {
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }
}
