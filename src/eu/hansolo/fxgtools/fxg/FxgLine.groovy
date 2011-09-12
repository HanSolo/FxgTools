package eu.hansolo.fxgtools.fxg

import java.awt.geom.Line2D

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 07:58
 * To change this template use File | Settings | File Templates.
 */
class FxgLine extends FxgShape {
    FxgShapeType type = FxgShapeType.LINE
    double x1
    double y1
    double x2
    double y2
    double alpha

    Line2D getLine() {
            return new Line2D.Double(x1, y1, x2, y2)
        }

    String translateTo(final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}"
        switch (LANGUAGE) {
            case Language.JAVA:
                code.append("        final Line2D $name = new Line2D.Double(${x1 / referenceWidth} * IMAGE_WIDTH, ${y1 / referenceHeight} * IMAGE_HEIGHT, ${x2 / referenceWidth} * IMAGE_WIDTH, ${y2 / referenceHeight} * IMAGE_HEIGHT);\n")
                if (filled) {
                    appendJavaPaint(code, name, type)
                }
                if (stroked) {
                    appendJavaStroke(code, name)
                }
                appendJavaFilter(code, name)
                code.append("\n")
                return code.toString()

            case Language.JAVAFX:
                code.append("        Line ${name} = new Line(${x1 / referenceWidth} * imageWidth, ${y1 / referenceHeight} * imageHeight, ${x2 / referenceWidth} * imageWidth, ${y2 / referenceHeight} * imageHeight);\n")
                appendJavaFxFillAndStroke(code, name)
                appendJavaFxFilter(code, name)
                code.append("\n")
                return code.toString()

            case Language.GWT:

            case Language.CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                code.append("        ctx.beginPath();\n")
                code.append("        ctx.moveTo(${x1 / referenceWidth} * imageWidth, ${y1 / referenceHeight} * imageHeight);\n")
                code.append("        ctx.lineTo(${x2 / referenceWidth} * imageWidth, ${y2 / referenceHeight} * imageHeight);\n")
                code.append("        ctx.closePath();\n")
                code.append("        ctx.restore();\n")
                if (filled) {
                    appendCanvasFill(code, name, LANGUAGE == Language.GWT)
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                }
                appendCanvasFilter(code, name)
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }
}
