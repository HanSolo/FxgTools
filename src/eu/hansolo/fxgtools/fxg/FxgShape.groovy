package eu.hansolo.fxgtools.fxg

import java.awt.Color

import java.awt.BasicStroke
import java.awt.geom.AffineTransform

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 06:46
 * To change this template use File | Settings | File Templates.
 */
abstract class FxgShape {
    String layerName
    String shapeName
    FxgShapeType type
    FxgFill fill
    FxgStroke stroke
    List<FxgFilter> filters = []
    boolean filled
    boolean stroked
    boolean transformed
    double referenceWidth
    double referenceHeight
    AffineTransform transform

    abstract String translateTo(final Language LANGUAGE, final int SHAPE_INDEX)

    protected StringBuilder makeNicer(StringBuilder code) {
        // replace: 0.0 * imageWidth -> 0.0

        // replace: 0.0 * imageHeight -> 0.0

        // replace: 1.0 * imageWidth -> imageWidth

        // replace: 1.0 * imageHeight -> imageHeight

        return code
    }


    // JAVA
    protected void appendJavaPaint(StringBuilder code, String elementName, FxgShapeType shapeType) {
        elementName = elementName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
        elementName = elementName.replace("_E_", "_")
        int nameLength = elementName.length()

        code.append("        final Paint ${elementName}_FILL = ")

        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                appendJavaColor(code, fill.color)
                code.append(";\n")
                code.append("        G2.setPaint(${elementName}_FILL);\n")
                if (shapeType != FxgShapeType.TEXT) {
                    code.append("        G2.fill(${elementName});\n")
                }
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("new LinearGradientPaint(new Point2D.Double(${fill.start.x / referenceWidth} * IMAGE_WIDTH, ${fill.start.y / referenceHeight} * IMAGE_HEIGHT),\n")
                intendCode(code, 8, nameLength, 44)
                code.append("new Point2D.Double(${fill.stop.x / referenceWidth} * IMAGE_WIDTH, ${fill.stop.y / referenceHeight} * IMAGE_HEIGHT),\n")
                intendCode(code, 8, nameLength, 44)
                appendJavaFractions(code, fill.fractions, (52 + nameLength))
                intendCode(code, 8, nameLength, 44)
                appendJavaColors(code, fill.colors, (52 + nameLength))
                code.append(");\n")
                code.append("        G2.setPaint(${elementName}_FILL);\n")
                code.append("        G2.fill(${elementName});\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                code.append("new RadialGradientPaint(new Point2D.Double(${fill.center.x / referenceWidth} * IMAGE_WIDTH, ${fill.center.y / referenceHeight} * IMAGE_HEIGHT),\n")
                intendCode(code, 8, nameLength, 44)
                code.append("${fill.radius / referenceWidth}f * IMAGE_WIDTH,\n")
                intendCode(code, 8, nameLength, 44)
                appendJavaFractions(code, fill.fractions, (52 + nameLength))
                intendCode(code, 8, nameLength, 44)
                appendJavaColors(code, fill.colors, (52 + nameLength))
                code.append(");\n")
                code.append("        G2.setPaint(${elementName}_FILL);\n")
                code.append("        G2.fill(${elementName});\n")
                break
        }
    }

    protected void appendJavaStroke(StringBuilder code, String elementName) {
        code.append("        G2.setPaint(")
        appendJavaColor(code, stroke.color)
        code.append(");\n")
        code.append("        G2.setStroke(new BasicStroke((${stroke.stroke.lineWidth / referenceWidth}f * IMAGE_WIDTH), ${stroke.stroke.endCap}, ${stroke.stroke.lineJoin}));\n")
        code.append("        G2.draw(${elementName});\n")
    }

    protected void appendJavaFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            filters.each { filter ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {
                            code.append("        //G2.drawImage(JavaShadow.INSTANCE.createInnerShadow((Shape) ${elementName}, INSERT PAINT OF ${elementName}, (int) (${filter.distance / referenceWidth} * IMAGE_WIDTH), ${filter.alpha / 255}f, new Color(${filter.color.red}, ${filter.color.green}, ${filter.color.blue}, ${filter.color.alpha}), (int) ${filter.blurX}, (int) ${filter.angle}), ${elementName}.getBounds().x, ${elementName}.getBounds().y, null);\n")
                        } else {
                            code.append("        //G2.drawImage(JavaShadow.INSTANCE.createDropShadow((Shape) ${elementName}, INSERT PAINT OF ${elementName}, (int) (${filter.distance / referenceWidth} * IMAGE_WIDTH), ${filter.alpha / 255}f, new Color(${filter.color.red}, ${filter.color.green}, ${filter.color.blue}, ${filter.color.alpha}), (int) ${filter.blurX}, (int) ${filter.angle}), ${elementName}.getBounds().x, ${elementName}.getBounds().y, null);\n")
                        }
                        break;
                }
            }
        }
    }

    private void appendJavaFractions(StringBuilder code, float[] fractions, int offset) {
        code.append("new float[]{\n")
        for (int j = 0 ; j < (offset + 4) ; j++) {
            code.append(" ")
        }
        fill.fractions.eachWithIndex { fraction, i ->
            code.append(fraction).append("f")
            if (i.compareTo(fill.fractions.length - 1) != 0) {
                code.append(",\n")
                for (int j = 0 ; j < (offset + 4) ; j++) {
                    code.append(" ")
                }
            }
        }
        code.append("\n")
        for (int j = 0 ; j < (offset) ; j++) {
            code.append(" ")
        }
        code.append("},\n")
    }

    private void appendJavaColors(StringBuilder code, Color[] colors, int offset) {
        code.append("new Color[]{\n")
        for (int j = 0 ; j < (offset + 4) ; j++) {
            code.append(" ")
        }
        fill.colors.eachWithIndex { color, i ->
            code.append("new Color(${color.red / 255}f, ${color.green / 255}f, ${color.blue / 255}f, ${color.alpha / 255}f)")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(",\n")
                for (int j = 0 ; j < (offset + 4) ; j++) {
                    code.append(" ")
                }
            }
        }
        code.append("\n")
        for (int j = 0 ; j < (offset) ; j++) {
            code.append(" ")
        }
        code.append("}")
    }

    private void appendJavaColor(StringBuilder code, Color color) {
        code.append("new Color(${color.red / 255}f, ${color.green / 255}f, ${color.blue / 255}f, ${color.alpha / 255}f)")
    }


    // JAVA_FX
    protected void appendJavaFxFillAndStroke(StringBuilder code, String elementName) {
        if (filled) {
            appendJavaFxPaint(code, elementName)
        }
        if (stroked) {
            if (stroke.stroke.lineWidth < 2) {
                code.append("        ${elementName}.setStrokeType(StrokeType.OUTSIDE);\n")
            } else {
                code.append("        ${elementName}.setStrokeType(StrokeType.CENTERED);\n")
            }
            switch (stroke.stroke.endCap) {
                case BasicStroke.CAP_BUTT:
                    code.append("        ${elementName}.setStrokeLineCap(StrokeLineCap.BUTT);\n")
                    break
                case BasicStroke.CAP_ROUND:
                    code.append("        ${elementName}.setStrokeLineCap(StrokeLineCap.ROUND);\n")
                    break
                case BasicStroke.CAP_SQUARE:
                    code.append("        ${elementName}.setStrokeLineCap(StrokeLineCap.SQUARE);\n")
                    break
            }
            switch (stroke.stroke.lineJoin) {
                case BasicStroke.JOIN_BEVEL:
                    code.append("        ${elementName}.setStrokeLineJoin(StrokeLineJoin.BEVEL);\n")
                    break
                case BasicStroke.JOIN_ROUND:
                    code.append("        ${elementName}.setStrokeLineJoin(StrokeLineJoin.ROUND);\n")
                    break
                case BasicStroke.JOIN_MITER:
                    code.append("        ${elementName}.setStrokeLineJoin(StrokeLineJoin.MITER);\n")
                    break
            }
            code.append("        ${elementName}.setStrokeWidth(${stroke.stroke.lineWidth / referenceWidth} * WIDTH);\n")
            code.append("        ${elementName}.setStroke(")
            appendJavaFxColor(code, stroke.color)
            code.append(");\n")
        } else {
            code.append("        ${elementName}.setStroke(null);\n")
        }
    }

    protected void appendJavaFxPaint(StringBuilder code, String elementName) {
        elementName = elementName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
        elementName = elementName.replace("_E_", "_")
        int nameLength = elementName.length()

        // add call to css id
        code.append("        //${elementName}.setId(\"${elementName.toLowerCase().replaceAll('_', '-')}-fill\");\n")

        code.append("        final Paint ${elementName}_FILL = ")

        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                appendJavaFxColor(code, fill.color)
                code.append(";\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("new LinearGradient(${fill.start.x / referenceWidth} * WIDTH, ${fill.start.y / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("${fill.stop.x / referenceWidth} * WIDTH, ${fill.stop.y / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("false, CycleMethod.NO_CYCLE,\n")
                intendCode(code, 8, nameLength, 39)
                appendJavaFxStops(code, fill.fractions, fill.colors, (47 + nameLength))
                code.append(");\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                code.append("new RadialGradient(0, 0,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("${fill.center.x / referenceWidth} * WIDTH, ${fill.center.y / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("${fill.radius / referenceWidth} * WIDTH,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("false, CycleMethod.NO_CYCLE,\n")
                intendCode(code, 8, nameLength, 39)
                appendJavaFxStops(code, fill.fractions, fill.colors, (47 + nameLength))
                code.append(");\n")
                break
            case FxgFillType.NONE:
                code.append("null;\n")
                break
        }
        code.append("        ${elementName}.setFill(${elementName}_FILL);\n")
    }

    protected void appendJavaFxFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            String lastFilterName
            filters.eachWithIndex { filter, i ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {
                            code.append("        final InnerShadow ${elementName}_INNER_SHADOW${i} = new InnerShadow(${filter.blurX / referenceWidth} * WIDTH, ${filter.getOffset().x / referenceWidth} * WIDTH, ${filter.getOffset().y / referenceHeight} * HEIGHT, ")
                            code.append("Color.color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255})")
                            code.append(");\n")
                            if (i > 0 || filters.size() == 1) {
                                code.append("        ${elementName}_INNER_SHADOW${i}.inputProperty().set(${lastFilterName});\n")
                                code.append("        ${elementName}.setEffect(${elementName}_INNER_SHADOW${i});\n")
                            }
                            lastFilterName = "${elementName}_INNER_SHADOW${i}"
                        } else {
                            code.append("        final DropShadow ${elementName}_DROP_SHADOW${i} = new DropShadow();\n")
                            code.append("        ${elementName}_DROP_SHADOW${i}.setOffsetX(${filter.getOffset().x / referenceWidth} * WIDTH);\n")
                            code.append("        ${elementName}_DROP_SHADOW${i}.setOffsetY(${filter.getOffset().y / referenceHeight} * HEIGHT);\n")
                            code.append("        ${elementName}_DROP_SHADOW${i}.setRadius(${filter.blurX / referenceWidth} * WIDTH);\n")
                            code.append("        ${elementName}_DROP_SHADOW${i}.setColor(Color.color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255}));\n")
                            if (i > 0 || filters.size() == 1) {
                                code.append("        ${elementName}_DROP_SHADOW${i}.inputProperty().set(${lastFilterName});\n")
                                code.append("        ${elementName}.setEffect(${elementName}_DROP_SHADOW${i});\n")
                            }
                            lastFilterName = "${elementName}_DROP_SHADOW"
                        }
                        break;
                }
            }
        }
    }

    private void appendJavaFxColor(StringBuilder code, Color color) {
        code.append("Color.color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255})")
    }

    private void appendJavaFxStops(StringBuilder code, float[] fractions, Color[] colors, int offset) {
        fill.colors.eachWithIndex { color, i ->
            code.append("new Stop(${fractions[i]}, Color.color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255}))")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(",\n")
                for (int j = 0 ; j < offset ; j++) {
                    code.append(" ")
                }
            }
        }
    }

    public String createCssFill(String elementName) {
        StringBuilder cssCode = new StringBuilder()
        elementName = elementName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
        elementName = elementName.replace("_E_", "_")

        cssCode.append("#")
        cssCode.append("${elementName.toLowerCase().replaceAll('_', '-')}")
        cssCode.append("-fill {\n")
        cssCode.append("    -fx-fill: ")

        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                cssCode.append("rgba(")
                cssCode.append("${fill.color.getRed()}, ")
                cssCode.append("${fill.color.getGreen()}, ")
                cssCode.append("${fill.color.getBlue()}, ")
                cssCode.append("${fill.color.getAlpha() / 255});\n")
                cssCode.append("}\n\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                cssCode.append("linear-gradient(")
                cssCode.append("from ${(int) (fill.start.x / referenceWidth * 100)}% ${(int) (fill.start.y / referenceHeight * 100)}% ")
                cssCode.append("to ${(int) (fill.stop.x / referenceWidth * 100)}% ${(int) (fill.stop.y / referenceHeight * 100)}%, ")
                for (int i = 0 ; i < fill.colors.length ; i++) {
                    cssCode.append("rgba(")
                    cssCode.append("${fill.colors[i].getRed()}, ")
                    cssCode.append("${fill.colors[i].getGreen()}, ")
                    cssCode.append("${fill.colors[i].getBlue()}, ")
                    cssCode.append("${fill.colors[i].getAlpha() / 255}) ")
                    cssCode.append("${(int) (fill.fractions[i] * 100)}%")
                    if (i < fill.colors.length - 1) {
                        cssCode.append(", ")
                    }
                }
                cssCode.append(");\n")
                cssCode.append("}\n\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                cssCode.append("radial-gradient(")
                cssCode.append("focus-angle 0deg, focus-distance 0%, ")
                cssCode.append("center ${(int) (fill.center.x / referenceWidth * 100)}% ${(int) (fill.center.y / referenceHeight * 100)}%, ")
                cssCode.append("${(int) (fill.radius / referenceWidth * 100)}%, ")
                cssCode.append("reflect, ")
                for (int i = 0 ; i < fill.colors.length ; i++) {
                    cssCode.append("rgba(")
                    cssCode.append("${fill.colors[i].getRed()}, ")
                    cssCode.append("${fill.colors[i].getGreen()}, ")
                    cssCode.append("${fill.colors[i].getBlue()}, ")
                    cssCode.append("${fill.colors[i].getAlpha() / 255}) ")
                    cssCode.append("${(int) (fill.fractions[i] * 100)}%")
                    if (i < fill.colors.length - 1) {
                        cssCode.append(", ")
                    }
                }
                cssCode.append(");\n")
                cssCode.append("}\n\n")
                break
            case FxgFillType.NONE:
                cssCode.append("rgba(0, 0, 0, 0);\n")
                cssCode.append("}\n\n")
                break
        }
        return cssCode.toString()
    }

    public String createCssStroke(String elementName) {
        StringBuilder cssCode = new StringBuilder()
        elementName = elementName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
        elementName = elementName.replace("_E_", "_")

        cssCode.append("#")
        cssCode.append("${elementName}")
        cssCode.append("-stroke {\n")
        cssCode.append("    -fx-stroke: ")
        cssCode.append("rgba(")
        cssCode.append("${stroke.color.getRed()}, ")
        cssCode.append("${stroke.color.getGreen()}, ")
        cssCode.append("${stroke.color.getBlue()}, ")
        cssCode.append("${stroke.color.getAlpha() / 255});\n\n")

        cssCode.append("    -fx-stroke-line-cap: ")
        switch (stroke.stroke.endCap) {
            case BasicStroke.CAP_BUTT:
                cssCode.append("butt;\n")
                break
            case BasicStroke.CAP_ROUND:
                cssCode.append("round;\n")
                break
            case BasicStroke.CAP_SQUARE:
                cssCode.append("square;\n")
                break
        }

        cssCode.append("    -fx-stroke-line-join: ")
        switch (stroke.stroke.lineJoin) {
            case BasicStroke.JOIN_BEVEL:
                cssCode.append("bevel;\n")
                break
            case BasicStroke.JOIN_ROUND:
                cssCode.append("round;\n")
                break
            case BasicStroke.JOIN_MITER:
                cssCode.append("miter;\n")
                break
        }

        cssCode.append("    -fx-stroke-width: ")
        cssCode.append("${(int) (stroke.stroke.lineWidth / referenceWidth * 100)}%;\n")

        cssCode.append("}\n\n")
        return cssCode.toString()
    }


    // CANVAS & GWT
    protected void appendCanvasFill(StringBuilder code, String elementName, boolean GWT) {
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                code.append("        ctx.fillStyle = ")
                appendCanvasColor(code, fill.color)
                code.append(";\n")
                code.append("        ctx.fill();\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                GWT ? code.append("        CanvasGradient ") : code.append("        var ")
                code.append("${elementName}_Fill = ctx.createLinearGradient((${(fill.start.x) / referenceWidth} * imageWidth), (${(fill.start.y) / referenceHeight} * imageHeight), ((${(fill.stop.x) / referenceWidth}) * imageWidth), ((${(fill.stop.y) / referenceHeight}) * imageHeight));\n")
                appendCanvasStops(code, fill.fractions, fill.colors, elementName)
                code.append("        ctx.fillStyle = ${elementName}_Fill;\n")
                code.append("        ctx.fill();\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                GWT ? code.append("        CanvasGradient ") : code.append("        var ")
                code.append("${elementName}_Fill = ctx.createRadialGradient((${fill.center.x / referenceWidth}) * imageWidth, ((${fill.center.y / referenceHeight}) * imageHeight), 0, ((${fill.center.x / referenceWidth}) * imageWidth), ((${fill.center.y / referenceHeight}) * imageHeight), ${fill.radius / referenceWidth} * imageWidth);\n")
                appendCanvasStops(code, fill.fractions, fill.colors, elementName)
                code.append("        ctx.fillStyle = ${elementName}_Fill;\n")
                code.append("        ctx.fill();\n")
                break
        }
    }

    protected void appendCanvasStroke(StringBuilder code, String elementName) {
            switch (stroke.stroke.endCap) {
                case BasicStroke.CAP_BUTT:
                    code.append("        ctx.lineCap = 'butt';\n")
                    break
                case BasicStroke.CAP_ROUND:
                    code.append("        ctx.lineCap = 'round';\n")
                    break
                case BasicStroke.CAP_SQUARE:
                    code.append("        ctx.lineCap = 'square';\n")
                    break
            }
            switch (stroke.stroke.lineJoin) {
                case BasicStroke.JOIN_BEVEL:
                    code.append("        ctx.lineJoin = 'bevel';\n")
                    break
                case BasicStroke.JOIN_ROUND:
                    code.append("        ctx.lineJoin = 'round';\n")
                    break
                case BasicStroke.JOIN_MITER:
                    code.append("        ctx.lineJoin = 'miter';\n")
                    break
            }
            code.append("        ctx.lineWidth = ${stroke.stroke.lineWidth / referenceWidth} * imageWidth;\n")
            code.append("        ctx.strokeStyle = ")
            appendCanvasColor(code, stroke.color)
            code.append(";\n")
            code.append("        ctx.stroke();\n")
    }

    protected void appendCanvasFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            filters.each { filter ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {

                        } else {
                            code.append("        ctx.shadowOffsetX = ${filter.getOffset().x / referenceWidth} * imageWidth;\n")
                            code.append("        ctx.shadowOffsetY = ${filter.getOffset().y / referenceHeight} * imageHeight;\n")
                            code.append("        ctx.shadowColor = ")
                            code.append("'rgba(${filter.color.red}, ${filter.color.green}, ${filter.color.blue}, ${filter.color.alpha / 255})'")
                            code.append(";\n")
                            code.append("        ctx.shadowBlur = ${filter.blurX / referenceWidth} * imageWidth;\n")
                            code.append("        ctx.fill();\n")
                        }
                        break;
                }
            }
        }
    }

    private void appendCanvasColor(StringBuilder code, Color color) {
        if (color.getAlpha().compareTo(255) == 0) {
            code.append("'rgb(${color.red}, ${color.green}, ${color.blue})'")
        } else {
            code.append("'rgba(${color.red}, ${color.green}, ${color.blue}, ${color.alpha / 255})'")
        }
    }

    private void appendCanvasStops(StringBuilder code, float[] fractions, Color[] colors, String elementName) {
        fill.colors.eachWithIndex { color, i ->
            code.append("        ${elementName}_Fill.addColorStop(${fractions[i]}, ")
            if (colors[i].alpha.compareTo(255) == 0) {
                code.append("'rgb(${colors[i].red}, ${colors[i].green}, ${colors[i].blue})'")
            } else {
                code.append("'rgba(${colors[i].red}, ${colors[i].green}, ${colors[i].blue}, ${colors[i].alpha / 255})'")
            }
            code.append(");\n")
        }
    }


    // GROOVYFX
    protected void appendGroovyFxFillAndStroke(StringBuilder code, String elementName) {
        if (filled) {
            appendGroovyFxPaint(code, elementName)
        }

        if (stroked) {
            if (stroke.stroke.lineWidth < 2) {
                code.append("        ${elementName}.strokeType = StrokeType.OUTSIDE\n")
            } else {
                code.append("        ${elementName}.strokeType = StrokeType.CENTERED\n")
            }
            switch (stroke.stroke.endCap) {
                case BasicStroke.CAP_BUTT:
                    code.append("        ${elementName}.strokeLineCap = StrokeLineCap.BUTT\n")
                    break
                case BasicStroke.CAP_ROUND:
                    code.append("        ${elementName}.strokeLineCap = StrokeLineCap.ROUND\n")
                    break
                case BasicStroke.CAP_SQUARE:
                    code.append("        ${elementName}.strokeLineCap = StrokeLineCap.SQUARE\n")
                    break
            }
            switch (stroke.stroke.lineJoin) {
                case BasicStroke.JOIN_BEVEL:
                    code.append("        ${elementName}.strokeLineJoin = StrokeLineJoin.BEVEL\n")
                    break
                case BasicStroke.JOIN_ROUND:
                    code.append("        ${elementName}.strokeLineJoin = StrokeLineJoin.ROUND\n")
                    break
                case BasicStroke.JOIN_MITER:
                    code.append("        ${elementName}.strokeLineJoin = StrokeLineJoin.MITER\n")
                    break
            }
            code.append("        ${elementName}.strokeWidth = ${stroke.stroke.lineWidth / referenceWidth} * imageWidth\n")
            code.append("        ${elementName}.stroke = ")
            appendJavaFxColor(code, stroke.color)
            code.append("\n")
        } else {
            code.append("        ${elementName}.stroke = null\n")
        }
    }

    protected void appendGroovyFxPaint(StringBuilder code, String elementName) {
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                code.append("        ${elementName}.fill = ")
                appendGroovyFxColor(code, fill.color)
                code.append("\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("        ${elementName}.fill = new LinearGradient(${fill.start.x / referenceWidth} * imageWidth, ${fill.start.y / referenceHeight} * imageHeight, ${fill.stop.x / referenceWidth} * imageWidth, ${fill.stop.y / referenceHeight} * imageHeight, ")
                code.append("false, CycleMethod.NO_CYCLE, ")
                appendGroovyFxStops(code, fill.fractions, fill.colors)
                code.append(")\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                code.append("        ${elementName}.fill = new RadialGradient(0, 0, ${fill.center.x / referenceWidth} * imageWidth, ${fill.center.y / referenceHeight} * imageHeight, ")
                code.append("${fill.radius / referenceWidth} * imageWidth, ")
                code.append("false, CycleMethod.NO_CYCLE, ")
                appendGroovyFxStops(code, fill.fractions, fill.colors)
                code.append(")\n")
                break
            case FxgFillType.NONE:
                code.append("        ${elementName}.fill = null\n")
                break
        }
    }

    protected void appendGroovyFxFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            String lastFilterName
            filters.eachWithIndex { filter, i ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {
                            code.append("        def ${elementName}_InnerShadow${i} = new InnerShadow(${filter.blurX / referenceWidth} * imageWidth, ${filter.getOffset().x / referenceWidth} * imageWidth, ${filter.getOffset().y / referenceHeight} * imageHeight, ")
                            code.append("Color.color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255})")
                            code.append(");\n")
                            if (i > 0) {
                                code.append("        ${elementName}_InnerShadow${i}.inputProperty().set(${lastFilterName})\n")
                                code.append("        ${elementName}.effect = ${elementName}_InnerShadow${i}\n")
                            }
                            lastFilterName = "${elementName}_InnerShadow${i}"
                        } else {
                            code.append("        def ${elementName}_DropShadow${i} = new DropShadow()\n")
                            code.append("        ${elementName}_DropShadow${i}.offsetX = ${filter.getOffset().x / referenceWidth} * imageWidth\n")
                            code.append("        ${elementName}_DropShadow${i}.offsetY = ${filter.getOffset().y / referenceHeight} * imageHeight\n")
                            code.append("        ${elementName}_DropShadow${i}.radius = ${filter.blurX / referenceWidth} * imageWidth\n")
                            code.append("        ${elementName}_DropShadow${i}.color = Color.color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255})\n")
                            if (i > 0) {
                                code.append("        ${elementName}_DropShadow${i}.inputProperty().set(${lastFilterName})\n")
                                code.append("        ${elementName}.effect = ${elementName}_DropShadow${i}\n")
                            }
                            lastFilterName = "${elementName}_DropShadow"
                        }
                        break;
                }
            }
        }
    }

    private void appendGroovyFxColor(StringBuilder code, Color color) {
        code.append("Color.color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255})")
    }

    private void appendGroovyFxStops(StringBuilder code, float[] fractions, Color[] colors) {
        code.append("[")
        fill.colors.eachWithIndex { color, i ->
            code.append("new Stop(${fractions[i]}, Color.color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255}))")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(", ")
            }
        }
        code.append("]")
    }


    // ANDROID
    protected void appendAndroidFillAndStroke(StringBuilder code, String elementName, FxgShapeType shapeType) {
        code.append("\n");
        if (filled) {
            code.append("        paint.reset();\n")
            code.append("        paint.setAntiAlias(true);\n")
            code.append("        paint.setStyle(Style.FILL);\n")
            switch(fill.type) {
                case FxgFillType.SOLID_COLOR:
                    code.append("        paint.setColor(")
                    appendAndroidColor(code, fill.color)
                    code.append(");\n")
                    break
                case FxgFillType.LINEAR_GRADIENT:
                    code.append("        paint.setShader(new LinearGradient(${fill.start.x / referenceWidth}f * imageWidth, ${fill.start.y / referenceHeight}f * imageHeight, ${fill.stop.x / referenceWidth}f * imageWidth, ${fill.stop.y / referenceHeight}f * imageHeight")
                    appendAndroidColors(code, fill.colors)
                    appendAndroidFractions(code, fill.fractions)
                    code.append(", Shader.TileMode.CLAMP));\n")
                    break
                case FxgFillType.RADIAL_GRADIENT:
                    code.append("        paint.setShader(new RadialGradient(${fill.center.x / referenceWidth}f * imageWidth, ${fill.center.y / referenceHeight}f * imageHeight")
                    code.append(", ${fill.radius / referenceWidth}f * imageWidth")
                    appendAndroidColors(code, fill.colors)
                    appendAndroidFractions(code, fill.fractions)
                    code.append(", Shader.TileMode.CLAMP));\n")
                    break
                default:
                    code.append("        paint.setColor(0x000000);\n")
                    break
            }
        }

        if (stroked) {
            code.append("        stroke.reset();\n")
            code.append("        stroke.setAntiAlias(true);\n")
            code.append("        stroke.setStyle(Style.STROKE);\n")
            switch (stroke.stroke.endCap) {
                case BasicStroke.CAP_BUTT:
                    code.append("        stroke.setStrokeCap(Cap.BUTT);\n")
                    break
                case BasicStroke.CAP_ROUND:
                    code.append("        stroke.setStrokeCap(Cap.ROUND);\n")
                    break
                case BasicStroke.CAP_SQUARE:
                    code.append("        stroke.setStrokeCap(Cap.SQUARE);\n")
                    break
            }
            switch (stroke.stroke.lineJoin) {
                case BasicStroke.JOIN_BEVEL:
                    code.append("        stroke.setStrokeJoin(Join.BEVEL);\n")
                    break
                case BasicStroke.JOIN_ROUND:
                    code.append("        stroke.setStrokeJoin(Join.ROUND);\n")
                    break
                case BasicStroke.JOIN_MITER:
                    code.append("        stroke.setStrokeJoin(Join.MITER);\n")
                    break
            }
            code.append("        stroke.setStrokeWidth(${stroke.stroke.lineWidth / referenceWidth}f * imageWidth);\n")
            code.append("        stroke.setColor(")
            appendAndroidColor(code, stroke.color)
            code.append(");\n")
        }
    }

    protected void appendAndroidFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            filters.each { filter ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {
                            code.append("        paint.setMaskFilter(new BlurMaskFilter(${filter.blurX / referenceWidth}f * imageWidth, android.graphics.BlurMaskFilter.Blur.INNER));\n")
                            code.append("        canvas.drawPath(${elementName}, paint);\n")
                        } else {
                            code.append("        paint.setColor(Color.argb(${filter.color.alpha}, ${filter.color.red}, ${filter.color.green}, ${filter.color.blue}));\n")
                            code.append("        paint.setMaskFilter(new BlurMaskFilter(${filter.blurX / referenceWidth}f * imageWidth, Blur.OUTER));\n")
                            code.append("        canvas.drawPath(${elementName}, paint);\n")
                        }
                        break;
                }
            }
        }
    }

    private void appendAndroidFractions(StringBuilder code, float[] fractions) {
        code.append(", new float[]{")
        fill.fractions.eachWithIndex { fraction, i ->
            code.append(fraction).append("f")
            if (i.compareTo(fill.fractions.length - 1) != 0) {
                code.append(", ")
            }
        }
        code.append("}")
    }

    private void appendAndroidColors(StringBuilder code, Color[] colors) {
        code.append(", new int[]{")
        fill.colors.eachWithIndex { color, i ->
            code.append("Color.argb(${color.alpha}, ${color.red}, ${color.green}, ${color.blue})")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(", ")
            }
        }
        code.append("}")
    }

    private void appendAndroidColor(StringBuilder code, Color color) {
        code.append("Color.argb(${color.alpha}, ${color.red}, ${color.green}, ${color.blue})")
    }


    // TOOLS
    private void intendCode(StringBuilder code, int intend, int nameLength, int offset) {
        int numberOfSpaces = intend + nameLength + offset
        for (int i = 0 ; i < numberOfSpaces ; i++) {
            code.append(" ")
        }
    }
}
