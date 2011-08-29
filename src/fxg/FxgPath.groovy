package fxg

import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import java.awt.geom.Path2D

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 07:59
 * To change this template use File | Settings | File Templates.
 */
class FxgPath extends FxgShape {
    FxgShapeType type = FxgShapeType.PATH
    GeneralPath path

    String translateTo(final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}"
        switch (LANGUAGE) {
            case Language.JAVA:
                code.append("GeneralPath $name = new GeneralPath();\n")
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                code.append(PATH_ITERATOR.windingRule == Path2D.WIND_EVEN_ODD ? "${name}.setWindingRule(Path2D.WIND_EVEN_ODD);\n" : "${name}.setWindingRule(Path2D.WIND_NON_ZERO);\n")
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("${name}.moveTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("${name}.lineTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("${name}.quadTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT, ${COORDINATES[2] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[3] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("${name}.curveTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT, ${COORDINATES[2] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[3] / referenceHeight} * IMAGE_HEIGHT, ${COORDINATES[4] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[5] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("${name}.closePath();\n")
                            break;
                    }
                    PATH_ITERATOR.next();
                }

                if (filled) {
                    appendJavaPaint(code)
                    code.append("G2.fill($name);\n")
                }
                if (stroked) {
                    appendJavaStroke(code)
                    code.append("G2.draw($name);\n")
                }
                code.append("\n")
                return code.toString()

            case Language.JAVAFX:
                code.append("Path $name = new Path();\n")
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                code.append(PATH_ITERATOR.windingRule == Path2D.WIND_EVEN_ODD ? "${name}.setFillRule(FillRule.EVEN_ODD);\n" : "${name}.setFillRule(FillRule.NON_ZERO);\n")
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    PATH_ITERATOR.windingRule
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("${name}.getElements().add(new MoveTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT));\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("${name}.getElements().add(new LineTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT));\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("${name}.getElements().add(new QuadCurveTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT, ${COORDINATES[2] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[3] / referenceHeight} * IMAGE_HEIGHT));\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("${name}.getElements().add(new CubicCurveTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT, ${COORDINATES[2] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[3] / referenceHeight} * IMAGE_HEIGHT, ${COORDINATES[4] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[5] / referenceHeight} * IMAGE_HEIGHT));\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("${name}.getElements().add(new ClosePath());\n")
                            break;
                    }
                    PATH_ITERATOR.next();
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
