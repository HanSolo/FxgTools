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
        return new Point2D.Double((width / 2) + x, (height / 2) + y)
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
                code.append("        Ellipse2D $name = new Ellipse2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT, ${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT);\n")
                if (filled) {
                    appendJavaPaint(code)
                    code.append("        G2.fill($name);\n")
                }
                if (stroked) {
                    appendJavaStroke(code)
                    code.append("        G2.draw($name);\n")
                }
                code.append("\n")
                return code.toString()

            case Language.JAVAFX:
                if (width.compareTo(height) == 0) {
                    code.append("Circle ${name} = new Circle(${center.x / referenceWidth} * IMAGE_WIDTH, ${center.y / referenceHeight} * IMAGE_HEIGHT, ${getRadiusX() / referenceWidth} * IMAGE_WIDTH);\n")
                } else {
                    code.append("Ellipse ${name} = new Ellipse(${center.x / referenceWidth} * IMAGE_WIDTH, ${center.y / referenceHeight} * IMAGE_HEIGHT, ${radiusX / referenceWidth} * IMAGE_WIDTH, ${radiusY / referenceHeight} * IMAGE_HEIGHT;\n")
                }
                appendJavaFxFill(code, name)
                code.append("\n")
                return code.toString()

            case Language.GWT:
                return "GWT"

            case Language.CANVAS:
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                code.append("        ctx.scale(${width / height}, 1);\n")
                code.append("        ctx.beginPath();\n")
                code.append("        ctx.arc(${center.x / referenceWidth / (width / height)} * imageWidth, ${center.y / referenceHeight} * imageHeight, ${radiusX / referenceWidth / (width / height)} * imageWidth, 0, 2 * Math.PI, false);\n")
                code.append("        ctx.restore();\n")
                if (filled) {
                    appendCanvasFill(code, name)
                    code.append("        ctx.fill();\n")
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                    code.append("        ctx.stroke();\n")
                }
                code.append("\n")
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }
}
