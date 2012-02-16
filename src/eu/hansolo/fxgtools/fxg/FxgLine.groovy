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
    FxgShapeType type     = FxgShapeType.LINE
    double       x1
    double       y1
    double       x2
    double       y2
    double       rotation
    double       scaleX
    double       scaleY
    double       alpha

    Line2D getLine() {
            return new Line2D.Double(x1, y1, x2, y2)
        }

    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX, final HashSet<String> NAME_SET) {
        StringBuilder code = new StringBuilder()
        String name = "${shapeName}"
        if (NAME_SET.contains(name)) {
            name = "${layerName}_${shapeName}_${SHAPE_INDEX}"
        } else {
            NAME_SET.add(name)
        }
        switch (LANGUAGE) {
            case Language.JAVA:
                name = "${shapeName.toUpperCase()}"
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}_${shapeName.toUpperCase()}_${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                if (transformed) {
                    code.append("        AffineTransform transformBefore${name} = G2.getTransform();\n")
                    code.append("        AffineTransform ${name}_Transform = new AffineTransform();\n")
                    code.append("        ${name}_Transform.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * IMAGE_WIDTH, ${transform.translateY / referenceHeight} * IMAGE_HEIGHT);\n")
                    code.append("        G2.setTransform(${name}_Transform);\n")
                }
                code.append("        final Line2D $name = new Line2D.Double(${x1 / referenceWidth} * IMAGE_WIDTH, ${y1 / referenceHeight} * IMAGE_HEIGHT, ${x2 / referenceWidth} * IMAGE_WIDTH, ${y2 / referenceHeight} * IMAGE_HEIGHT);\n")
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
                importSet.add("import javafx.scene.shape.Line;")
                name = "${shapeName.toUpperCase()}"
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}_${shapeName.toUpperCase()}_${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                code.append("        final Line ${name} = new Line(${x1 / referenceWidth} * WIDTH, ${y1 / referenceHeight} * HEIGHT, ${x2 / referenceWidth} * WIDTH, ${y2 / referenceHeight} * HEIGHT);\n")
                if (transformed) {
                    code.append("        final Affine ${name}_Transform = new Affine();\n")
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
                importSet.add("import javafx.scene.transform.Affine;")
                importSet.add("import javafx.scene.transform.Rotate;")
                importSet.add("import javafx.scene.transform.Scale;")
                return code.toString()

            case Language.GWT:

            case Language.CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                if (transformed) {
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * imageWidth, ${transform.translateY / referenceHeight} * imageHeight);\n")
                }
                code.append("        ctx.beginPath();\n")
                code.append("        ctx.moveTo(${x1 / referenceWidth} * imageWidth, ${y1 / referenceHeight} * imageHeight);\n")
                code.append("        ctx.lineTo(${x2 / referenceWidth} * imageWidth, ${y2 / referenceHeight} * imageHeight);\n")
                code.append("        ctx.closePath();\n")
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
                code.append("        def ${name} = new Line(${x1 / referenceWidth} * imageWidth, ${y1 / referenceHeight} * imageHeight, ${x2 / referenceWidth} * imageWidth, ${y2 / referenceHeight} * imageHeight)\n")
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
                if (filled) {
                    code.append("        canvas.drawLine(${x1 / referenceWidth}f * imageWidth, ${y1 / referenceHeight}f * imageHeight, ${x2 / referenceWidth}f * imageWidth, ${y2 / referenceHeight}f * imageHeight, paint);\n")
                }
                if (stroked) {
                    code.append("        canvas.drawLine(${x1 / referenceWidth}f * imageWidth, ${y1 / referenceHeight}f * imageHeight, ${x2 / referenceWidth}f * imageWidth, ${y2 / referenceHeight}f * imageHeight, stroke);\n")
                }
                appendAndroidFilter(code, name)
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }
}
