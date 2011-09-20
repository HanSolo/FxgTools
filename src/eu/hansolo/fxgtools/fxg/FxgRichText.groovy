package eu.hansolo.fxgtools.fxg

import java.awt.Font
import java.text.AttributedString
import java.awt.font.TextAttribute
import java.awt.Color

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:36
 * To change this template use File | Settings | File Templates.
 */
class FxgRichText extends FxgShape{
    FxgShapeType type = FxgShapeType.TEXT
    double x
    double y
    double descent
    double fontSize
    AttributedString string
    Font font
    boolean underline
    boolean italic
    boolean bold
    boolean lineThrough
    String text
    String fontFamily
    double alpha
    Color color

    AttributedString getAttributedString() {
        AttributedString string = new AttributedString(text)
        string.addAttribute(TextAttribute.FONT, fontFamily)
        string.addAttribute(TextAttribute.SIZE, (float) fontSize)
        string.addAttribute(TextAttribute.FONT, font)
        if (bold){
            string.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)
        }
        if (underline) {
            string.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)
        }
        if (lineThrough) {
            string.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON)
        }
        return string
    }

    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX) {
        StringBuilder code = new StringBuilder()
        String name = "${layerName}_${shapeName}_${SHAPE_INDEX}"
        switch (LANGUAGE) {
            case Language.JAVA:
                if (transformed) {
                    code.append("        AffineTransform transformBefore${name} = G2.getTransform();\n")
                    code.append("        AffineTransform ${name}_Transform = new AffineTransform();\n")
                    code.append("        ${name}_Transform.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * IMAGE_WIDTH, ${transform.translateY / referenceHeight} * IMAGE_HEIGHT);\n")
                    code.append("        G2.setTransform(${name}_Transform);\n")
                }
                StringBuilder style = new StringBuilder();
                style.append(font.bold ? "Font.BOLD" : "Font.PLAIN")
                style.append(font.italic ? " | Font.ITALIC" : "")
                code.append("        final Font ${name}_Font = new Font(\"${font.family}\", ${style.toString()}, (int)(${font.size2D / referenceWidth} * IMAGE_WIDTH));\n")
                code.append("        final AttributedString ${name} = new AttributedString(\"${text.trim()}\");\n")
                code.append("        ${name}.addAttribute(TextAttribute.FONT, ${name}_Font);\n")
                if (bold){
                    code.append("        ${name}.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);\n")
                }
                if (underline) {
                    code.append("        ${name}.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);\n")
                }
                if (lineThrough) {
                    code.append("        ${name}.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);\n")
                }
                if (fill.type != null) {
                    appendJavaPaint(code, name, type)
                }
                code.append("        G2.setFont(${name}_Font);\n")
                code.append("        float ${name}_offsetY = (${y / referenceHeight}f * IMAGE_HEIGHT) - (new TextLayout(\"$text\", G2.getFont(), G2.getFontRenderContext()).getDescent());\n")
                code.append("        G2.drawString(${name}.getIterator(), (${x / referenceWidth}f * IMAGE_WIDTH), ${name}_offsetY);\n")
                if (transformed) {
                    code.append("        G2.setTransform(transformBefore${name});\n")
                }
                code.append("\n")
                return code.toString()

            case Language.JAVAFX:
                String fontWeight = (font.bold ? "FontWeight.BOLD" : "FontWeight.NORMAL")
                String fontPosture = (font.italic ? "FontPosture.ITALIC" : "FontPosture.REGULAR")
                code.append("        Text ${name} = new Text();\n")
                code.append("        ${name}.setText(\"${text.trim()}\");\n")
                code.append("        ${name}.setFont(Font.font(\"${font.family}\", ${fontWeight}, ${fontPosture}, ${font.size2D / referenceWidth} * imageWidth));\n")
                code.append("        ${name}.setX(${x / referenceWidth} * imageWidth);\n")
                code.append("        ${name}.setY(${y / referenceHeight} * imageHeight);\n")
                code.append("        ${name}.setTextOrigin(VPos.BOTTOM);\n")
                code.append(lineThrough ? "        ${name}.setStrikeThrough(true);\n" : "")
                code.append(underline ? "        ${name}.setUnderline(true);\n" : "")
                if (transformed) {
                    code.append("        Affine ${name}_Transform = new Affine();\n")
                    code.append("        ${name}_Transform.setMxx(${transform.scaleX});\n")
                    code.append("        ${name}_Transform.setMyx(${transform.shearY});\n")
                    code.append("        ${name}_Transform.setMxy(${transform.shearX});\n")
                    code.append("        ${name}_Transform.setMyy(${transform.scaleY});\n")
                    code.append("        ${name}_Transform.setTx(${transform.translateX / referenceWidth} * imageWidth);\n")
                    code.append("        ${name}_Transform.setTy(${transform.translateY / referenceHeight} * imageHeight);\n")
                    code.append("        ${name}.getTransforms().add(${name}_Transform);\n")
                }
                appendJavaFxPaint(code, name)
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
                if (fill.type != null) {
                    appendCanvasFill(code, name, LANGUAGE == Language.GWT)
                    code.append("        ctx.font = '")
                    italic ? code.append("italic "):code.append("")
                    bold ? code.append("bold "):code.append("")
                    code.append("${font.size2D}px ")
                    code.append("${font.family}';\n")
                    code.append("        ctx.textBaseline = 'bottom';\n")
                    code.append("        ctx.fillText('${text.trim()}', ${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                    code.append("        ctx.strokeText('${text.trim()}', ${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
                }
                appendCanvasFilter(code, name)
                code.append("        ctx.restore();\n")
                return code.toString()

            case Language.GROOVYFX:
                String fontWeight = (font.bold ? "FontWeight.BOLD" : "FontWeight.NORMAL")
                String fontPosture = (font.italic ? "FontPosture.ITALIC" : "FontPosture.REGULAR")
                code.append("        def ${name} = new Text()\n")
                code.append("        ${name}.text = \"${text.trim()}\"\n")
                code.append("        ${name}.font = Font.font(\"${font.family}\", ${fontWeight}, ${fontPosture}, ${font.size2D / referenceWidth} * imageWidth)\n")
                code.append("        ${name}.x = ${x / referenceWidth} * imageWidth\n")
                code.append("        ${name}.y = ${y / referenceHeight} * imageHeight\n")
                code.append("        ${name}.textOrigin = VPos.BOTTOM\n")
                code.append(lineThrough ? "        ${name}.strikeThrough = true\n" : "")
                code.append(underline ? "        ${name}.underline = true\n" : "")
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
                appendGroovyFxPaint(code, name)
                appendGroovyFxFilter(code, name)
                code.append("\n")
                return code.toString()

            case Language.ANDROID:
                code.append("        paint.setColor(Color.argb(${color.alpha}, ${color.red}, ${color.green}, ${color.blue}));\n")
                String fontWeight
                if (bold && italic) {
                    fontWeight = "Typeface.BOLD_ITALIC"
                } else if (bold) {
                    fontWeight = "Typeface.BOLD"
                } else if (italic) {
                    fontWeight = "Typeface.ITALIC"
                } else {
                    fontWeight = "Typeface.NORMAL"
                }

                code.append("        Typeface ${name}_TypeFace = Typeface.create(\"${font.family}\", ${fontWeight});\n")
                code.append("        paint.setTypeface(${name}_TypeFace);\n")
                code.append("        Paint.FontMetrics ${name}_Metrics = paint.getFontMetrics();\n")
			    code.append("        paint.setTextSize(${font.size2D / referenceWidth}f * imageWidth);\n")
                if (underline) {
                    code.append("        paint.setUnderlineText(true);\n")
                }
                if (lineThrough) {
                    code.append("        paint.setStrikeThruText(true);\n")
                }
                code.append("        canvas.drawText(\"${text.trim()}\", ${x / referenceWidth}f * imageWidth, ${y / referenceHeight}f * imageHeight - ${name}_Metrics.descent, paint);\n")
                code.append("        paint.setUnderlineText(false);\n")
                code.append("        paint.setStrikeThruText(false);\n")
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }
}
