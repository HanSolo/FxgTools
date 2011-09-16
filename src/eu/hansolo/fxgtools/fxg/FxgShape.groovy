package eu.hansolo.fxgtools.fxg

import java.awt.Color

import java.awt.BasicStroke

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
    double referenceWidth
    double referenceHeight

    abstract String translateTo(final Language LANGUAGE)

    protected StringBuilder makeNicer(StringBuilder code) {
        // replace: 0.0 * imageWidth -> 0.0

        // replace: 0.0 * imageHeight -> 0.0

        // replace: 1.0 * imageWidth -> imageWidth

        // replace: 1.0 * imageHeight -> imageHeight

        return code
    }

    // JAVA
    protected void appendJavaPaint(StringBuilder code, String elementName, FxgShapeType shapeType) {
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                code.append('        G2.setPaint(')
                appendJavaColor(code, fill.color)
                code.append(");\n")
                if (shapeType != FxgShapeType.TEXT) {
                    code.append("        G2.fill(${elementName});\n")
                }
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("        G2.setPaint(new LinearGradientPaint(new Point2D.Double(${fill.start.x / referenceWidth} * IMAGE_WIDTH, ${fill.start.y / referenceHeight} * IMAGE_HEIGHT), new Point2D.Double(${fill.stop.x / referenceWidth} * IMAGE_WIDTH, ${fill.stop.y / referenceHeight} * IMAGE_HEIGHT), ")
                appendJavaFractions(code, fill.fractions)
                appendJavaColors(code, fill.colors)
                code.append("));\n")
                code.append("        G2.fill(${elementName});\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                code.append("        G2.setPaint(new RadialGradientPaint(new Point2D.Double(${fill.center.x / referenceWidth} * IMAGE_WIDTH, ${fill.center.y / referenceHeight} * IMAGE_HEIGHT), ")
                code.append("(float)(${fill.radius / referenceWidth} * IMAGE_WIDTH), ")
                appendJavaFractions(code, fill.fractions)
                appendJavaColors(code, fill.colors)
                code.append("));\n")
                code.append("        G2.fill(${elementName});\n")
                break
        }
    }

    protected void appendJavaStroke(StringBuilder code, String elementName) {
        code.append("        G2.setPaint(")
        appendJavaColor(code, stroke.color)
        code.append(");\n")
        code.append("        G2.setStroke(new BasicStroke((float)(${stroke.stroke.lineWidth / referenceWidth} * IMAGE_WIDTH), ${stroke.stroke.endCap}, ${stroke.stroke.lineJoin}));\n")
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

    private void appendJavaFractions(StringBuilder code, float[] fractions) {
        code.append("new float[]{")
        fill.fractions.eachWithIndex { fraction, i ->
            code.append(fraction).append("f")
            if (i.compareTo(fill.fractions.length - 1) != 0) {
                code.append(", ")
            }
        }
        code.append("}")
    }

    private void appendJavaColors(StringBuilder code, Color[] colors) {
        code.append(", new Color[]{")
        fill.colors.eachWithIndex { color, i ->
            code.append("new Color(${color.red / 255}f, ${color.green / 255}f, ${color.blue / 255}f, ${color.alpha / 255}f)")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(", ")
            }
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
            code.append("        ${elementName}.setStrokeWidth(${stroke.stroke.lineWidth / referenceWidth} * imageWidth);\n")
            code.append("        ${elementName}.setStroke(")
            appendJavaFxColor(code, stroke.color)
            code.append(");\n")
        } else {
            code.append("        ${elementName}.setStroke(null);\n")
        }
    }

    protected void appendJavaFxPaint(StringBuilder code, String elementName) {
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                code.append("        ${elementName}.setFill(")
                appendJavaFxColor(code, fill.color)
                code.append(");\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("        ${elementName}.setFill(new LinearGradient(${fill.start.x / referenceWidth} * imageWidth, ${fill.start.y / referenceHeight} * imageHeight, ${fill.stop.x / referenceWidth} * imageWidth, ${fill.stop.y / referenceHeight} * imageHeight, ")
                code.append("false, CycleMethod.NO_CYCLE, ")
                appendJavaFxStops(code, fill.fractions, fill.colors)
                code.append("));\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                code.append("        ${elementName}.setFill(new RadialGradient(0, 0, ${fill.center.x / referenceWidth} * imageWidth, ${fill.center.y / referenceHeight} * imageHeight, ")
                code.append("${fill.radius / referenceWidth} * imageWidth, ")
                code.append("false, CycleMethod.NO_CYCLE, ")
                appendJavaFxStops(code, fill.fractions, fill.colors)
                code.append("));\n")
                break
            case FxgFillType.NONE:
                code.append("        ${elementName}.setFill(null);\n")
                break
        }
    }

    protected void appendJavaFxFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            String lastFilterName
            filters.eachWithIndex { filter, i ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {
                            code.append("        InnerShadow ${elementName}_InnerShadow${i} = new InnerShadow(${filter.blurX / referenceWidth} * imageWidth, ${filter.getOffset().x / referenceWidth} * imageWidth, ${filter.getOffset().y / referenceHeight} * imageHeight, ")
                            code.append("new Color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255})")
                            code.append(");\n")
                            if (i > 0) {
                                code.append("        ${elementName}_InnerShadow${i}.inputProperty().set(${lastFilterName});\n")
                                code.append("        ${elementName}.setEffect(${elementName}_InnerShadow${i});\n")
                            }
                            lastFilterName = "${elementName}_InnerShadow${i}"
                        } else {
                            code.append("        DropShadow ${elementName}_DropShadow${i} = new DropShadow();\n")
                            code.append("        ${elementName}_DropShadow${i}.setOffsetX(${filter.getOffset().x / referenceWidth} * imageWidth);\n")
                            code.append("        ${elementName}_DropShadow${i}.setOffsetY(${filter.getOffset().y / referenceHeight} * imageHeight);\n")
                            code.append("        ${elementName}_DropShadow${i}.setRadius(${filter.blurX / referenceWidth} * imageWidth);\n")
                            code.append("        ${elementName}_DropShadow${i}.setColor(new Color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255}));\n")
                            if (i > 0) {
                                code.append("        ${elementName}_DropShadow${i}.inputProperty().set(${lastFilterName});\n")
                                code.append("        ${elementName}.setEffect(${elementName}_DropShadow${i});\n")
                            }
                            lastFilterName = "${elementName}_DropShadow"
                        }
                        break;
                }
            }
        }
    }

    private void appendJavaFxColor(StringBuilder code, Color color) {
        code.append("new Color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255})")
    }

    private void appendJavaFxStops(StringBuilder code, float[] fractions, Color[] colors) {
        code.append("new Stop[]{")
        fill.colors.eachWithIndex { color, i ->
            code.append("new Stop(${fractions[i]}, new Color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255}))")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(", ")
            }
        }
        code.append("}")
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
                            code.append("new Color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255})")
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
                            code.append("        ${elementName}_DropShadow${i}.color = new Color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255})\n")
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
        code.append("new Color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255})")
    }

    private void appendGroovyFxStops(StringBuilder code, float[] fractions, Color[] colors) {
        code.append("[")
        fill.colors.eachWithIndex { color, i ->
            code.append("new Stop(${fractions[i]}, new Color(${color.red / 255}, ${color.green / 255}, ${color.blue / 255}, ${color.alpha / 255}))")
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
}
