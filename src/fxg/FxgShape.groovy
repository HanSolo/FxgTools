package fxg

import java.awt.Color
import java.awt.Stroke
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
                            code.append("        new Color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.color.alpha / 255})")
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
}
