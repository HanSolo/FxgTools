package eu.hansolo.fxgtools.fxg

import java.awt.geom.Ellipse2D

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
    double rotation
    double scaleX
    double scaleY
    double width
    double height
    double alpha

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

    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}_${SHAPE_INDEX}"
        switch (LANGUAGE) {
            case Language.JAVA:
                name = "${layerName.toUpperCase()}_${shapeName.toUpperCase()}_${SHAPE_INDEX}"
                name = name.replace("_E_", "_")
                int nameLength = name.length()
                if (transformed) {
                    code.append("        AffineTransform transformBefore${name} = G2.getTransform();\n")
                    code.append("        AffineTransform ${name}_Transform = new AffineTransform();\n")
                    code.append("        ${name}_Transform.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * IMAGE_WIDTH, ${transform.translateY / referenceHeight} * IMAGE_HEIGHT);\n")
                    code.append("        G2.setTransform(${name}_Transform);\n")
                }
                code.append("        final Ellipse2D $name = new Ellipse2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT,\n")
                code.append("        ")
                for (int i = 0 ; i < nameLength ; i++) {
                    code.append(" ")
                }
                code.append("                                        ")
                code.append("${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT);\n")
                if (filled) {
                    appendJavaPaint(code, name, type)
                }
                if (stroked) {
                    appendJavaStroke(code, name)
                }
                appendJavaFilter(code, name)
                if (transformed) {
                    code.append("        G2.setTransform(transformBefore${name});\n")
                }
                code.append("\n")
                return code.toString()

            case Language.JAVAFX:
                name = "${layerName.toUpperCase()}_${shapeName.toUpperCase()}_${SHAPE_INDEX}"
                name = name.replace("_E_", "_")
                int nameLength = name.length()

                if (width.compareTo(height) == 0) {
                    code.append("        final Circle ${name} = new Circle(${center.x / referenceWidth} * WIDTH, ${center.y / referenceHeight} * HEIGHT, ${getRadiusX() / referenceWidth} * WIDTH);\n")
                } else {
                    code.append("        final Ellipse ${name} = new Ellipse(${center.x / referenceWidth} * WIDTH, ${center.y / referenceHeight} * HEIGHT,\n")
                    code.append("                      ")
                    for (int i = 0 ; i < nameLength ; i++) {
                        code.append(" ")
                    }
                    code.append("               ")
                    code.append("${radiusX / referenceWidth} * WIDTH, ${radiusY / referenceHeight} * HEIGHT);\n")
                }
                if (transformed) {
                    code.append("        Affine ${name}_Transform = new Affine();\n")
                    code.append("        ${name}_Transform.setMxx(${transform.scaleX});\n")
                    code.append("        ${name}_Transform.setMyx(${transform.shearY});\n")
                    code.append("        ${name}_Transform.setMxy(${transform.shearX});\n")
                    code.append("        ${name}_Transform.setMyy(${transform.scaleY});\n")
                    code.append("        ${name}_Transform.setTx(${transform.translateX / referenceWidth} * WIDTH);\n")
                    code.append("        ${name}_Transform.setTy(${transform.translateY / referenceHeight} * HEIGHT);\n")
                    code.append("        ${name}.getTransforms().add(${name}_Transform);\n")
                }
                appendJavaFxFillAndStroke(code, name)
                appendJavaFxFilter(code, name)
                code.append("\n")
                return code.toString()

            case Language.GWT:

            case Language.CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                if (transformed) {
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * imageWidth, ${transform.translateY / referenceHeight} * imageHeight);\n")
                }
                code.append("        ctx.scale(${width / height}, 1);\n")
                code.append("        ctx.beginPath();\n")
                code.append("        ctx.arc(${center.x / referenceWidth / (width / height)} * imageWidth, ${center.y / referenceHeight} * imageHeight, ${radiusX / referenceWidth / (width / height)} * imageWidth, 0, 2 * Math.PI, false);\n")
                if (filled) {
                    appendCanvasFill(code, name, LANGUAGE == Language.GWT)
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                }
                appendCanvasFilter(code, name)
                code.append("        ctx.restore();\n")
                return code.toString()

            case Language.GROOVYFX:
                if (width.compareTo(height) == 0) {
                    code.append("        def ${name} = new Circle(${center.x / referenceWidth} * imageWidth, ${center.y / referenceHeight} * imageHeight, ${getRadiusX() / referenceWidth} * imageWidth)\n")
                } else {
                    code.append("        def ${name} = new Ellipse(${center.x / referenceWidth} * imageWidth, ${center.y / referenceHeight} * imageHeight, ${radiusX / referenceWidth} * imageWidth, ${radiusY / referenceHeight} * imageHeight)\n")
                }
                if (transformed) {
                    code.append("        def ${name}_Transform = new Affine()\n")
                    code.append("        ${name}_Transform.mxx = ${transform.scaleX}\n")
                    code.append("        ${name}_Transform.myx = ${transform.shearY}\n")
                    code.append("        ${name}_Transform.mxy = ${transform.shearX}\n")
                    code.append("        ${name}_Transform.myy = ${transform.scaleY}\n")
                    code.append("        ${name}_Transform.tx = ${transform.translateX / referenceWidth} * imageWidth\n")
                    code.append("        ${name}_Transform.ty = ${transform.translateY / referenceHeight} * imageHeight\n")
                    code.append("        ${name}.transforms.add(${name}_Transform)\n")
                }
                appendGroovyFxFillAndStroke(code, name)
                appendGroovyFxFilter(code, name)
                code.append("\n")
                return code.toString()

            case Language.ANDROID:
                appendAndroidFillAndStroke(code, name, type)
                if (width.compareTo(height) == 0) {
                    if (filled) {
                        code.append("        canvas.drawCircle(${center.x / referenceWidth}f * imageWidth, ${center.y / referenceHeight}f * imageHeight, ${getRadiusX() / referenceWidth}f * imageWidth, paint);\n")
                    }
                    if (stroked) {
                        code.append("        canvas.drawCircle(${center.x / referenceWidth}f * imageWidth, ${center.y / referenceHeight}f * imageHeight, ${getRadiusX() / referenceWidth}f * imageWidth, stroke);\n")
                    }
                } else {
                    code.append("        RectF ${name} = new RectF(${x / referenceWidth}f * imageWidth, ${y / referenceHeight}f * imageHeight, ${x / referenceWidth}f * imageWidth + ${width / referenceWidth}f * imageWidth, ${y / referenceHeight}f * imageHeight + ${height / referenceHeight}f * imageHeight);\n")
                    if (filled) {
                        code.append("        canvas.drawOval(${name}, paint);\n")
                    }
                    if (stroked) {
                        code.append("        canvas.drawOval(${name}, stroke);\n")
                    }
                }
                appendAndroidFilter(code, name)
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }
}
