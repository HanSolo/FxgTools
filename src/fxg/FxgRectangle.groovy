package fxg

import java.awt.geom.RoundRectangle2D

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 06:54
 * To change this template use File | Settings | File Templates.
 */
class FxgRectangle extends FxgShape {
    FxgShapeType type = FxgShapeType.RECT
    double x
    double y
    double width
    double height
    double radiusX
    double radiusY

    RoundRectangle2D getRectangle() {
        return new RoundRectangle2D.Double(x, y, width, height, radiusX, radiusY)
    }

    String translateTo(final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}"
        switch (LANGUAGE) {
            case Language.JAVA:
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    code.append("        Rectangle2D ${name} = new Rectangle2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT, ${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT);\n")
                } else {
                    code.append("        RoundRectangle2D ${name} = new RoundRectangle2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT, ${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT, ${radiusX / referenceWidth} * IMAGE_WIDTH, ${radiusY / referenceHeight} * IMAGE_HEIGHT);\n")
                }
                if (filled) {
                    appendJavaPaint(code)
                    code.append("        G2.fill(${name});\n")
                }
                if (stroked) {
                    appendJavaStroke(code)
                    code.append("        G2.draw(${name});\n")
                }
                code.append("\n")
                return code.toString()

            case Language.JAVAFX:
                code.append("        Rectangle ${name} = new Rectangle(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT, ${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT);\n")
                if (radiusX > 0) {
                    code.append("        ${name}.setArcWidth(${radiusX / referenceWidth} * IMAGE_WIDTH);\n")
                }
                if (radiusY > 0) {
                    code.append("        ${name}.setArcHeight(${radiusY / referenceHeight} * IMAGE_HEIGHT);\n")
                }
                appendJavaFxFill(code, name)
                code.append("\n")
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
