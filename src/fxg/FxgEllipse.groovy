package fxg

import java.awt.geom.Ellipse2D
import java.awt.Color
import java.awt.geom.Point2D

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 07:57
 * To change this template use File | Settings | File Templates.
 */
class FxgEllipse extends FxgShape {
    FxgShapeType type = FxgShapeType.ELLIPSE
    double x
    double y
    double width
    double height

    Ellipse2D getEllipse() {
            return new Ellipse2D.Double(x, y, width, height)
        }

    Point2D getCenter() {
        return new Point2D.Double((width - x) / 2, (height - y) / 2)
    }

    double getRadiusX() {
        return width/2
    }

    double getRadiusY() {
        return height/2
    }

    String translateTo(final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}"
        switch (LANGUAGE) {
            case Language.JAVA:
                code.append("Ellipse2D $name = new Ellipse2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT, ${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT);\n")
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
                if (width.compareTo(height) == 0) {
                    code.append("Circle ${name} = new Circle(${getCenter().x / referenceWidth} * IMAGE_WIDTH, ${getCenter().y / referenceHeight} * IMAGE_HEIGHT, ${getRadiusX() / referenceWidth} * IMAGE_WIDTH);\n")
                } else {
                    code.append("Ellipse ${name} = new Ellipse(${getCenter().x / referenceWidth} * IMAGE_WIDTH, ${getCenter().y / referenceHeight} * IMAGE_HEIGHT, ${getRadiusX() / referenceWidth} * IMAGE_WIDTH, ${getRadiusY() / referenceHeight} * IMAGE_HEIGHT;\n")
                }
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
