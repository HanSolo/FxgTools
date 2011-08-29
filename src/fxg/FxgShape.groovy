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
    FxgFilter filter
    boolean filled
    boolean stroked
    double referenceWidth
    double referenceHeight

    abstract String translateTo(final Language LANGUAGE)

    // JAVA
    protected void appendJavaPaint(StringBuilder code) {
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                code.append("G2.setPaint(")
                appendJavaColor(code, fill.color)
                code.append(");\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("G2.setPaint(new LinearGradientPaint(new Point2D.Double(${fill.start.x / referenceWidth} * IMAGE_WIDTH, ${fill.start.y / referenceHeight} * IMAGE_HEIGHT), new Point2D.Double(${fill.stop.x / referenceWidth} * IMAGE_WIDTH, ${fill.stop.y / referenceHeight} * IMAGE_HEIGHT), ")
                appendJavaFractions(code, fill.fractions)
                appendJavaColors(code, fill.colors)
                code.append("));\n")
                break;
            case FxgFillType.RADIAL_GRADIENT:
                code.append("G2.setPaint(new RadialGradientPaint(new Point2D.Double(${fill.center.x / referenceWidth} * IMAGE_WIDTH, ${fill.center.y / referenceHeight} * IMAGE_HEIGHT), ")
                code.append("(float)(${fill.radius / referenceWidth} * IMAGE_WIDTH), ")
                appendJavaFractions(code, fill.fractions)
                appendJavaColors(code, fill.colors)
                code.append("));\n")
                break;
        }
    }

    protected void appendJavaStroke(StringBuilder code) {
        code.append("G2.setPaint(")
        appendJavaColor(code, stroke.color)
        code.append(");\n")
        code.append("G2.setStroke(new BasicStroke((float)(${stroke.stroke.lineWidth / referenceWidth} * IMAGE_WIDTH), ${stroke.stroke.endCap}, ${stroke.stroke.lineJoin}));\n")
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
            code.append("new Color(${color.getRed() / 255}f, ${color.getGreen() / 255}f, ${color.getBlue() / 255}f, ${color.getAlpha() / 255}f)")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(", ")
            }
        }
        code.append("}")
    }

    private void appendJavaColor(StringBuilder code, Color color) {
        code.append("new Color(${color.getRed() / 255}f, ${color.getGreen() / 255}f, ${color.getBlue() / 255}f, ${color.getAlpha() / 255}f)")
    }

    // JAVA_FX
    protected void appendJavaFxFill(StringBuilder code, String elementName) {
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                code.append("${elementName}.setFill(")
                appendJavaFxColor(code, fill.color)
                code.append(");\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("${elementName}.setFill(new LinearGradient(${fill.start.x / referenceWidth} * IMAGE_WIDTH, ${fill.start.y / referenceHeight} * IMAGE_HEIGHT, ${fill.stop.x / referenceWidth} * IMAGE_WIDTH, ${fill.stop.y / referenceHeight} * IMAGE_HEIGHT, ")
                code.append("true, CycleMethod.No_CYCLE, ")
                appendJavaFxStops(code, fill.fractions, fill.colors)
                code.append("));\n")
                break;
            case FxgFillType.RADIAL_GRADIENT:
                code.append("${elementName}.setFill(new RadialGradient(0, 0, ${fill.center.x / referenceWidth} * IMAGE_WIDTH, ${fill.center.y / referenceHeight} * IMAGE_HEIGHT, ")
                code.append("${fill.radius / referenceWidth} * IMAGE_WIDTH, ")
                code.append("true, CycleMethod.No_CYCLE, ")
                appendJavaFxStops(code, fill.fractions, fill.colors)
                code.append("));\n")
                break;
        }
        if (stroked) {
            code.append("${elementName}.setStrokeType(StrokeType.CENTERED);\n")
            switch (stroke.stroke.endCap) {
                case BasicStroke.CAP_BUTT:
                    code.append("${elementName}.setStrokeLineCap(StrokeLineCap.BUTT);\n")
                    break;
                case BasicStroke.CAP_ROUND:
                    code.append("${elementName}.setStrokeLineCap(StrokeLineCap.ROUND);\n")
                    break;
                case BasicStroke.CAP_SQUARE:
                    code.append("${elementName}.setStrokeLineCap(StrokeLineCap.SQUARE);\n")
                    break;
            }
            switch (stroke.stroke.lineJoin) {
                case BasicStroke.JOIN_BEVEL:
                    code.append("${elementName}.setStrokeLineJoin(StrokeLineJoin.BEVEL);\n")
                    break;
                case BasicStroke.JOIN_ROUND:
                    code.append("${elementName}.setStrokeLineJoin(StrokeLineJoin.ROUND);\n")
                    break;
                case BasicStroke.JOIN_MITER:
                    code.append("${elementName}.setStrokeLineJoin(StrokeLineJoin.MITER);\n")
                    break;
            }
            code.append("${elementName}.setStrokeWidth(${stroke.stroke.lineWidth / referenceWidth} * IMAGE_WIDTH);\n")
        }
    }

    private void appendJavaFxColor(StringBuilder code, Color color) {
        code.append("new Color(${color.getRed() / 255}, ${color.getGreen() / 255}, ${color.getBlue() / 255}, ${color.getAlpha() / 255})")
    }

    private void appendJavaFxStops(StringBuilder code, float[] fractions, Color[] colors) {
        code.append("new Stop[]{")
        fill.colors.eachWithIndex { color, i ->
            code.append("new Stop(${fractions[i]}, new Color(${color.getRed() / 255}, ${color.getGreen() / 255}, ${color.getBlue() / 255}, ${color.getAlpha() / 255}))")
            if (i.compareTo(fill.colors.length - 1) != 0) {
                code.append(", ")
            }
        }
        code.append("}")
    }
}
