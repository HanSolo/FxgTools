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
    FxgShapeType type     = FxgShapeType.RECT
    double       x
    double       y
    double       rotation
    double       scaleX
    double       scaleY
    double       width
    double       height
    double       radiusX
    double       radiusY
    double       alpha

    RoundRectangle2D getRectangle() {
        return new RoundRectangle2D.Double(x, y, width, height, radiusX * 2, radiusY * 2)
    }

    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX, final HashSet<String> NAME_SET) {
        StringBuilder code = new StringBuilder()
        String name = checkName()
        switch (LANGUAGE) {
            case Language.JAVA:
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}${shapeName.toUpperCase()}${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                int nameLength = name.length()
                if (transformed) {
                    code.append("        AffineTransform transformBefore${name} = G2.getTransform();\n")
                    code.append("        AffineTransform ${name}Transform = new AffineTransform();\n")
                    code.append("        ${name}Transform.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * IMAGE_WIDTH, ${transform.translateY / referenceHeight} * IMAGE_HEIGHT);\n")
                    code.append("        G2.setTransform(${name}Transform);\n")
                }
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    code.append("        final Rectangle2D ${name} = new Rectangle2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT,\n")
                    code.append("                          ")
                    for (int i = 0 ; i < nameLength ; i++) {
                        code.append(" ")
                    }
                    code.append("                          ")
                    code.append("${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT);\n")
                } else {
                    code.append("        final RoundRectangle2D ${name} = new RoundRectangle2D.Double(${x / referenceWidth} * IMAGE_WIDTH, ${y / referenceHeight} * IMAGE_HEIGHT,\n")
                    code.append("        ")
                    for (int i = 0 ; i < nameLength ; i++) {
                        code.append(" ")
                    }
                    code.append("                                                      ")
                    code.append("${width / referenceWidth} * IMAGE_WIDTH, ${height / referenceHeight} * IMAGE_HEIGHT,\n")
                    code.append("        ")
                    for (int i = 0 ; i < nameLength ; i++) {
                        code.append(" ")
                    }
                    code.append("                                                      ")
                    code.append("${radiusX * 2 / referenceWidth} * IMAGE_WIDTH, ${radiusY * 2 / referenceHeight} * IMAGE_HEIGHT);\n")
                }
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
                importSet.add("import javafx.scene.shape.Rectangle;")
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}${shapeName.toUpperCase()}${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                int nameLength = name.length()
                code.append("        final Rectangle ${name} = new Rectangle(${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT,\n")
                code.append("                        ")
                for (int i = 0 ; i < nameLength ; i++) {
                    code.append(" ")
                }
                code.append("                 ")
                code.append("${width / referenceWidth} * WIDTH, ${height / referenceHeight} * HEIGHT);\n")

                if (radiusX > 0) {
                    code.append("        ${name}.setArcWidth(${radiusX * 2 / referenceWidth} * WIDTH);\n")
                }
                if (radiusY > 0) {
                    code.append("        ${name}.setArcHeight(${radiusY * 2 / referenceHeight} * HEIGHT);\n")
                }
                if (transformed) {
                    code.append("        final Affine ${name}Transform = new Affine();\n")
                    code.append("        ${name}Transform.setMxx(${transform.scaleX});\n")
                    code.append("        ${name}Transform.setMyx(${transform.shearY});\n")
                    code.append("        ${name}Transform.setMxy(${transform.shearX});\n")
                    code.append("        ${name}Transform.setMyy(${transform.scaleY});\n")
                    code.append("        ${name}Transform.setTx(${transform.translateX / referenceWidth} * WIDTH);\n")
                    code.append("        ${name}Transform.setTy(${transform.translateY / referenceHeight} * HEIGHT);\n")
                    code.append("        ${name}.getTransforms().add(${name}Transform);\n")
                }
                appendJavaFxFillAndStroke(code, name)
                appendJavaFxFilter(code, name)
                code.append("\n")
                importSet.add("import javafx.scene.transform.Affine;")
                importSet.add("import javafx.scene.transform.Rotate;")
                importSet.add("import javafx.scene.transform.Scale;")
                return code.toString()

            case Language.GWT:

            case Language.CANVAS:
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}${shapeName.toUpperCase()}${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                if (transformed) {
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * imageWidth, ${transform.translateY / referenceHeight} * imageHeight);\n")
                }
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    code.append("        ctx.beginPath();\n")
                    code.append("        ctx.rect(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight, ${width / referenceWidth} * imageWidth, ${height / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.closePath();\n")
                } else {
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
                }
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
                code.append("        def ${name} = new Rectangle(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight, ${width / referenceWidth} * imageWidth, ${height / referenceHeight} * imageHeight)\n")
                if (radiusX > 0) {
                    code.append("        ${name}.arcWidth = ${radiusX * 2 / referenceWidth} * imageWidth\n")
                }
                if (radiusY > 0) {
                    code.append("        ${name}.arcHeight = ${radiusY * 2 / referenceHeight} * imageHeight\n")
                }
                if (transformed) {
                    code.append("        def ${name}Transform = new Affine()\n")
                    code.append("        ${name}Transform.mxx = ${transform.scaleX}\n")
                    code.append("        ${name}Transform.myx = ${transform.shearY}\n")
                    code.append("        ${name}Transform.mxy = ${transform.shearX}\n")
                    code.append("        ${name}Transform.myy = ${transform.scaleY}\n")
                    code.append("        ${name}Transform.tx = ${transform.translateX / referenceWidth} * imageWidth\n")
                    code.append("        ${name}Transform.ty = ${transform.translateY / referenceHeight} * imageHeight\n")
                    code.append("        ${name}.transforms.add(${name}Transform)\n")
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

    private String checkName() {
        String name = "${shapeName.toUpperCase()}"
        name = name.startsWith("E_") ? name.replaceFirst("E_", "") : name
        name = name.replaceAll("_?RR[0-9]+_([0-9]+_)?", "")
        name = name.replace("_E_", "")
        name = name.startsWith("_") ? name.replaceFirst("_", "") : name
        return name
    }
}
