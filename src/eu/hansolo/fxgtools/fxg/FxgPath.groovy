package eu.hansolo.fxgtools.fxg

import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import java.awt.geom.PathIterator

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 07:59
 * To change this template use File | Settings | File Templates.
 */
class FxgPath extends FxgShape {
    FxgShapeType type     = FxgShapeType.PATH
    GeneralPath  path
    double       rotation
    double       scaleX
    double       scaleY
    double       alpha

    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX, final HashSet<String> NAME_SET) {
        StringBuilder code = new StringBuilder()
        String name = "${shapeName}"
        //if (NAME_SET.contains(name)) {
        //    name = "${layerName}_${shapeName}_${SHAPE_INDEX}"
        //} else {
        //    NAME_SET.add(name)
        //}
        switch (LANGUAGE) {
            case Language.JAVA:
                name = "${shapeName.toUpperCase()}"
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}_${shapeName.toUpperCase()}_${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                name = name.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
                name = name.replace("_E_", "_")
                int nameLength = name.length()
                if (transformed) {
                    code.append("        AffineTransform transformBefore${name} = G2.getTransform();\n")
                    code.append("        AffineTransform ${name}_Transform = new AffineTransform();\n")
                    code.append("        ${name}_Transform.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * IMAGE_WIDTH, ${transform.translateY / referenceHeight} * IMAGE_HEIGHT);\n")
                    code.append("        G2.setTransform(${name}_Transform);\n")
                }
                code.append("        final GeneralPath $name = new GeneralPath();\n")
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                code.append(PATH_ITERATOR.windingRule == Path2D.WIND_EVEN_ODD ? "        ${name}.setWindingRule(Path2D.WIND_EVEN_ODD);\n" : "        ${name}.setWindingRule(Path2D.WIND_NON_ZERO);\n")
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("        ${name}.moveTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("        ${name}.lineTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("        ${name}.quadTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT,\n")
                            code.append("        ")
                            for (int i = 0 ; i < nameLength ; i++) {
                                code.append(" ")
                            }
                            code.append("        ")
                            code.append("${COORDINATES[2] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[3] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("        ${name}.curveTo(${COORDINATES[0] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[1] / referenceHeight} * IMAGE_HEIGHT,\n")
                            code.append("        ")
                            for (int i = 0 ; i < nameLength ; i++) {
                                code.append(" ")
                            }
                            code.append("         ")
                            code.append("${COORDINATES[2] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[3] / referenceHeight} * IMAGE_HEIGHT,\n")
                            code.append("        ")
                            for (int i = 0 ; i < nameLength ; i++) {
                                code.append(" ")
                            }
                            code.append("         ")
                            code.append("${COORDINATES[4] / referenceWidth} * IMAGE_WIDTH, ${COORDINATES[5] / referenceHeight} * IMAGE_HEIGHT);\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("        ${name}.closePath();\n")
                            break;
                    }
                    PATH_ITERATOR.next();
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
                importSet.add("import javafx.scene.shape.ClosePath;")
                importSet.add("import javafx.scene.shape.CubicCurveTo;")
                importSet.add("import javafx.scene.shape.FillRule;")
                importSet.add("import javafx.scene.shape.LineTo;")
                importSet.add("import javafx.scene.shape.MoveTo;")
                importSet.add("import javafx.scene.shape.Path;")
                importSet.add("import javafx.scene.transform.Affine;")
                importSet.add("import javafx.scene.transform.Rotate;")
                importSet.add("import javafx.scene.transform.Scale;")
                name = "${shapeName.toUpperCase()}"
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}_${shapeName.toUpperCase()}_${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                name = name.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
                name = name.replace("_E_", "_")
                int nameLength = name.length()

                code.append("        final Path $name = new Path();\n")
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                code.append(PATH_ITERATOR.windingRule == Path2D.WIND_EVEN_ODD ? "        ${name}.setFillRule(FillRule.EVEN_ODD);\n" : "        ${name}.setFillRule(FillRule.NON_ZERO);\n")
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    PATH_ITERATOR.windingRule
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("        ${name}.getElements().add(new MoveTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT));\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("        ${name}.getElements().add(new LineTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT));\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("        ${name}.getElements().add(new QuadCurveTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT,\n")
                            code.append("        ");
                            for (int i = 0 ; i < nameLength ; i++) {
                                code.append(" ")
                            }
                            code.append("                                   ")
                            code.append("${COORDINATES[2] / referenceWidth} * WIDTH, ${COORDINATES[3] / referenceHeight} * HEIGHT));\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("        ${name}.getElements().add(new CubicCurveTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT,\n")
                            code.append("        ")
                            for (int i = 0 ; i < nameLength ; i++) {
                                code.append(" ")
                            }
                            code.append("                                    ")
                            code.append("${COORDINATES[2] / referenceWidth} * WIDTH, ${COORDINATES[3] / referenceHeight} * HEIGHT,\n")
                            code.append("        ")
                            for (int i = 0 ; i < nameLength ; i++) {
                                code.append(" ")
                            }
                            code.append("                                    ")
                            code.append("${COORDINATES[4] / referenceWidth} * WIDTH, ${COORDINATES[5] / referenceHeight} * HEIGHT));\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("        ${name}.getElements().add(new ClosePath());\n")
                            break;
                    }
                    PATH_ITERATOR.next();
                }
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
                return code.toString()

            case Language.JAVAFX_CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        CTX.save();\n")
                if (transformed) {
                    code.append("        CTX.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * WIDTH, ${transform.translateY / referenceHeight} * HEIGHT);\n")
                }
                code.append("        CTX.beginPath();\n")
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("        CTX.moveTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT);\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("        CTX.lineTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT);\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("        CTX.quadraticCurveTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT, ${COORDINATES[2] / referenceWidth} * WIDTH, ${COORDINATES[3] / referenceHeight} * HEIGHT);\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("        CTX.bezierCurveTo(${COORDINATES[0] / referenceWidth} * WIDTH, ${COORDINATES[1] / referenceHeight} * HEIGHT, ${COORDINATES[2] / referenceWidth} * WIDTH, ${COORDINATES[3] / referenceHeight} * HEIGHT, ${COORDINATES[4] / referenceWidth} * WIDTH, ${COORDINATES[5] / referenceHeight} * HEIGHT);\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("        CTX.closePath();\n")
                            break;
                    }
                    PATH_ITERATOR.next();
                }

                appendJavaFxCanvasFillAndStroke(code, name)
                appendJavaFxCanvasFilter(code, name)

                code.append("        CTX.restore();\n")
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
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("        ctx.moveTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight);\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("        ctx.lineTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight);\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("        ctx.quadraticCurveTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight, ${COORDINATES[2] / referenceWidth} * imageWidth, ${COORDINATES[3] / referenceHeight} * imageHeight);\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("        ctx.bezierCurveTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight, ${COORDINATES[2] / referenceWidth} * imageWidth, ${COORDINATES[3] / referenceHeight} * imageHeight, ${COORDINATES[4] / referenceWidth} * imageWidth, ${COORDINATES[5] / referenceHeight} * imageHeight);\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("        ctx.closePath();\n")
                            break;
                    }
                    PATH_ITERATOR.next();
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
                code.append("        def $name = new Path()\n")
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                code.append(PATH_ITERATOR.windingRule == Path2D.WIND_EVEN_ODD ? "        ${name}.fillRule = FillRule.EVEN_ODD\n" : "        ${name}.fillRule = FillRule.NON_ZERO\n")
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    PATH_ITERATOR.windingRule
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("        ${name}.elements.add(new MoveTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight))\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("        ${name}.elements.add(new LineTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight))\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("        ${name}.elements.add(new QuadCurveTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight, ${COORDINATES[2] / referenceWidth} * imageWidth, ${COORDINATES[3] / referenceHeight} * imageHeight))\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("        ${name}.elements.add(new CubicCurveTo(${COORDINATES[0] / referenceWidth} * imageWidth, ${COORDINATES[1] / referenceHeight} * imageHeight, ${COORDINATES[2] / referenceWidth} * imageWidth, ${COORDINATES[3] / referenceHeight} * imageHeight, ${COORDINATES[4] / referenceWidth} * imageWidth, ${COORDINATES[5] / referenceHeight} * imageHeight))\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("        ${name}.elements.add(new ClosePath())\n")
                            break;
                    }
                    PATH_ITERATOR.next();
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
                code.append("        Path $name = new Path();\n")
                final PathIterator PATH_ITERATOR = path.getPathIterator(null);
                code.append(PATH_ITERATOR.windingRule == Path2D.WIND_EVEN_ODD ? "        ${name}.setFillType(FillType.EVEN_ODD);\n" : "        ${name}.setFillType(FillType.WINDING);\n")
                while (!PATH_ITERATOR.isDone()) {
                    final double[] COORDINATES = new double[6];
                    switch (PATH_ITERATOR.currentSegment(COORDINATES)) {
                        case PathIterator.SEG_MOVETO:
                            code.append("        ${name}.moveTo(${COORDINATES[0] / referenceWidth}f * imageWidth, ${COORDINATES[1] / referenceHeight}f * imageHeight);\n")
                            break;
                        case PathIterator.SEG_LINETO:
                            code.append("        ${name}.lineTo(${COORDINATES[0] / referenceWidth}f * imageWidth, ${COORDINATES[1] / referenceHeight}f * imageHeight);\n")
                            break;
                        case PathIterator.SEG_QUADTO:
                            code.append("        ${name}.quadTo(${COORDINATES[0] / referenceWidth}f * imageWidth, ${COORDINATES[1] / referenceHeight}f * imageHeight, ${COORDINATES[2] / referenceWidth}f * imageWidth, ${COORDINATES[3] / referenceHeight}f * imageHeight);\n")
                            break;
                        case PathIterator.SEG_CUBICTO:
                            code.append("        ${name}.cubicTo(${COORDINATES[0] / referenceWidth}f * imageWidth, ${COORDINATES[1] / referenceHeight}f * imageHeight, ${COORDINATES[2] / referenceWidth}f * imageWidth, ${COORDINATES[3] / referenceHeight}f * imageHeight, ${COORDINATES[4] / referenceWidth}f * imageWidth, ${COORDINATES[5] / referenceHeight}f * imageHeight);\n")
                            break;
                        case PathIterator.SEG_CLOSE:
                            code.append("        ${name}.close();\n")
                            break;
                    }
                    PATH_ITERATOR.next();
                }
                if (filled) {
                    code.append("        canvas.drawPath($name, paint);\n")
                }
                if (stroked) {
                    code.append("        canvas.drawPath($name, stroke);\n")
                }
                appendAndroidFilter(code, name)
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }
}
