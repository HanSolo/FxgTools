package fxg

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

    Line2D getLine() {
            return new Line2D.Double(x1, y1, x2, y2)
        }

    String translateTo(final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}"
        switch (LANGUAGE) {
            case Language.JAVA:
                code.append("Line2D $name = new Line2D.Double(${x1 / referenceWidth} * IMAGE_WIDTH, ${y1 / referenceHeight} * IMAGE_HEIGHT, ${x2 / referenceWidth} * IMAGE_WIDTH, ${y2 / referenceHeight} * IMAGE_HEIGHT);\n")
                if (filled) {
                    appendJavaPaint(code)
                    code.append("G2.fill($name);\n")
                }
                if (stroked) {
                    appendJavaStroke(code)
                    code.append("G2.draw($name);\n")
                }
                return code.toString()

            case Language.JAVAFX:
                code.append("Line ${name} = new Line(${x1 / referenceWidth} * IMAGE_WIDTH, ${y1 / referenceHeight} * IMAGE_HEIGHT, ${x2 / referenceWidth} * IMAGE_WIDTH, ${y2 / referenceHeight} * IMAGE_HEIGHT);\n")
                appendJavaFxFill(code, name)
                return code.toString()

            case Language.GWT:
                return "GWT"

            case Language.CANVAS:
                return "CANVAS"

            default:
                return "NOT SUPPORTED"
        }
    }
}
