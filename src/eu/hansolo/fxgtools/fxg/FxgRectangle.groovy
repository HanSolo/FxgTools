package eu.hansolo.fxgtools.fxg

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
    double alpha

    RoundRectangle2D getRectangle() {
        return new RoundRectangle2D.Double(x, y, width, height, radiusX * 2, radiusY * 2)
    }

    String translateTo(final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}"
        switch (LANGUAGE) {
            case Language.JAVA:
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    code.append("        final Rectangle2D ${name} = new Rectangle2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT, ${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT);\n")
                } else {
                    code.append("        final RoundRectangle2D ${name} = new RoundRectangle2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT, ${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT, ${radiusX * 2 / referenceWidth} * IMAGE_WIDTH, ${radiusY * 2 / referenceHeight} * IMAGE_HEIGHT);\n")
                }
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
                code.append("        Rectangle ${name} = new Rectangle(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight, ${width / referenceWidth} * imageWidth, ${height / referenceHeight} * imageHeight);\n")
                if (radiusX > 0) {
                    code.append("        ${name}.setArcWidth(${radiusX * 2 / referenceWidth} * imageWidth);\n")
                }
                if (radiusY > 0) {
                    code.append("        ${name}.setArcHeight(${radiusY * 2 / referenceHeight} * imageHeight);\n")
                }
                appendJavaFxFillAndStroke(code, name)
                appendJavaFxFilter(code, name)
                code.append("\n")
                return code.toString()

            case Language.GWT:

            case Language.CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    code.append("        ctx.save();\n")
                    code.append("        ctx.beginPath();\n")
                    code.append("        ctx.rect(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight, ${width / referenceWidth} * imageWidth, ${height / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.closePath();\n")
                    code.append("        ctx.restore();\n")
                } else {
                    code.append("        ctx.save();\n")
                    code.append("        ctx.beginPath();\n")
                    code.append("        ctx.moveTo(${x / referenceWidth} * imageWidth + ${radiusX / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * imageWidth + ${width / referenceWidth} * imageWidth - ${radiusX / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * imageWidth + ${width / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight, ${x / referenceWidth} * imageWidth + ${width / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${radiusX / referenceWidth} * imageWidth);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * imageWidth + ${width / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${height / referenceHeight} * imageHeight - ${radiusX / referenceWidth} * imageWidth);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * imageWidth + ${width / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${height / referenceHeight} * imageHeight, ${x / referenceWidth} * imageWidth + ${width / referenceWidth} * imageWidth - ${radiusX / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${height / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * imageWidth + ${radiusX / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${height / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${height / referenceHeight} * imageHeight, ${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${height / referenceHeight} * imageHeight - ${radiusX / referenceWidth} * imageWidth);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight + ${radiusX / referenceWidth} * imageWidth);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight, ${x / referenceWidth} * imageWidth + ${radiusX / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.closePath();\n")
                    code.append("        ctx.restore();\n")
                }
                if (filled) {
                    appendCanvasFill(code, name, LANGUAGE.is(Language.GWT))
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                }
                appendCanvasFilter(code, name)
                return code.toString()

            case Language.GROOVYFX:
                code.append("        def ${name} = new Rectangle(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight, ${width / referenceWidth} * imageWidth, ${height / referenceHeight} * imageHeight)\n")
                if (radiusX > 0) {
                    code.append("        ${name}.arcWidth = ${radiusX * 2 / referenceWidth} * imageWidth\n")
                }
                if (radiusY > 0) {
                    code.append("        ${name}.arcHeight = ${radiusY * 2 / referenceHeight} * imageHeight\n")
                }
                appendGroovyFxFillAndStroke(code, name)
                appendGroovyFxFilter(code, name)
                code.append("\n")
                return code.toString()

            case Language.ANDROID:
                appendAndroidFillAndStroke(code, name, type)
                code.append("        RectF ${name} = new RectF(${x / referenceWidth}f * imageWidth, ${y / referenceHeight}f * imageHeight, ${x / referenceWidth}f * imageWidth + ${width / referenceWidth}f * imageWidth, ${y / referenceHeight}f * imageHeight + ${height / referenceHeight}f * imageHeight);\n")
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    if (filled) {
                        code.append("        canvas.drawRect(${name}, paint);\n")
                    }
                    if (stroked) {
                        code.append("        canvas.drawRect(${name}, stroke);\n")
                    }
                } else {
                    if (filled) {
                        code.append("        canvas.drawRoundRect(${name}, ${radiusX / referenceWidth}f * imageWidth, ${radiusY / referenceHeight}f * imageHeight, paint);\n")
                    }
                    if (stroked) {
                        code.append("        canvas.drawRoundRect(${name}, ${radiusX / referenceWidth}f * imageWidth, ${radiusY / referenceHeight}f * imageHeight, stroke);\n")
                    }
                }
                appendAndroidFilter(code, name)
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }
}
