import fxg.FxgElement
import fxg.Language

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 20:36
 * To change this template use File | Settings | File Templates.
 */
class FxgTranslator {


    public void translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language TO_LANGUAGE) {
        final String CLASS_NAME = FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))
        final String USER_HOME = System.getProperties().getProperty("user.home")
        final String EXPORT_FILE_NAME = USER_HOME + File.separator + "Desktop" + File.separator + FILE_NAME

        StringBuilder codeToExport = new StringBuilder();

        // Export the header of the language specific template
        switch(TO_LANGUAGE) {
            case Language.JAVA:     codeToExport.append(javaTemplateHeader(CLASS_NAME))
                                    break;
            case Language.JAVAFX:   codeToExport.append(javaFxTemplateHeader(CLASS_NAME))
                                    break;
            case Language.GWT:      codeToExport.append(gwtTemplateHeader(CLASS_NAME))
                                    break;
            case Language.CANVAS:   codeToExport.append(canvasTemplateHeader(CLASS_NAME))
                                    break;
            default: throw new Exception("Language not supported...")
        }

        // Export the language specific graphics code
        codeToExport.append(code(layerMap, TO_LANGUAGE))

        // Export the rest of the language specific template
        switch(TO_LANGUAGE) {
            case Language.JAVA:     codeToExport.append(javaTemplateFooter(CLASS_NAME))
                                    break;
            case Language.JAVAFX:   codeToExport.append(javaFxTemplateFooter(CLASS_NAME))
                                    break;
            case Language.GWT:      codeToExport.append(gwtTemplateFooter(CLASS_NAME))
                                    break;
            case Language.CANVAS:   codeToExport.append(canvasTemplateFooter(CLASS_NAME))
                                    break;
        }

        writeToFile(EXPORT_FILE_NAME, codeToExport.toString())
    }

    // JAVA
    private String javaTemplateHeader(final String CLASS_NAME) {
        return "JavaHeader\n"
    }

    private String javaImageMethodStart(final String LAYER_NAME) {
        StringBuilder imageCode = new StringBuilder()
        imageCode.append("    private BufferedImage create_${LAYER_NAME}_Image(final int WIDTH, final int HEIGHT) {\n")
        imageCode.append("        final GraphicsConfiguration GFX_CONF = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();\n")
        imageCode.append("        if (WIDTH <= 0 || HEIGHT <= 0) {\n")
        imageCode.append("            return GFX_CONF.createCompatibleImage(1, 1, java.awt.Transparency.TRANSLUCENT);\n")
        imageCode.append("        }\n")
        imageCode.append("        final BufferedImage IMAGE = GFX_CONF.createCompatibleImage(WIDTH, HEIGHT, Transparency.TRANSLUCENT);\n")
        imageCode.append("        final Graphics2D G2 = IMAGE.createGraphics();\n")
        imageCode.append("        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n")
        imageCode.append("        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);\n")
        imageCode.append("        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);\n")
        imageCode.append("        G2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);\n")
        imageCode.append("\n")
        imageCode.append("        final FontRenderContext RENDER_CONTEXT = new FontRenderContext(null, true, true);\n")
        imageCode.append("\n")
        imageCode.append("        final int IMAGE_WIDTH = IMAGE.getWidth();\n")
        imageCode.append("        final int IMAGE_HEIGHT = IMAGE.getHeight();\n")
        return imageCode.toString()
    }

    private String javaImageMethodStop() {
        StringBuilder imageCode = new StringBuilder()
        imageCode.append("        G2.dispose();\n")
        imageCode.append("        return IMAGE;\n")
        imageCode.append("    }\n\n")
    }

    private String javaTemplateFooter(final String CLASS_NAME) {
        StringBuilder footerCode = new StringBuilder()
        footerCode.append("    @Override\n")
        footerCode.append("    public String toString() {\n")
        footerCode.append("        return \"${CLASS_NAME}\";\n")
        footerCode.append("    }\n")
        footerCode.append("}\n\n")
        return footerCode.toString()
    }

    // JAVAFX
    private String javaFxTemplateHeader(final String CLASS_NAME) {
        return "JavaFxHeader\n"
    }

    private String javaFxTemplateFooter(final String CLASS_NAME) {
        return "JavaFxFooter\n"
    }

    // GWT
    private String gwtTemplateHeader(final String CLASS_NAME) {
        return "GwtHeader\n"
    }

    private String gwtTemplateFooter(final String CLASS_NAME) {
        return "GwtFooter\n"
    }

    // CANVAS
    private String canvasTemplateHeader(final String CLASS_NAME) {
        return "CanvasHeader\n"
    }

    private String canvasTemplateFooter(final String CLASS_NAME) {
        return "CanvasFooter\n"
    }

    // CODE
    private String code(Map<String, List<FxgElement>> layerMap, final Language TO_LANGUAGE) {
        StringBuilder code = new StringBuilder()
        for (String layer : layerMap.keySet()) {
            switch(TO_LANGUAGE) {
                case Language.JAVA: code.append(javaImageMethodStart(layer))
                    break;
                case Language.JAVAFX:
                    break;
                case Language.GWT:
                    break;
                case Language.CANVAS:
                    break;
            }

            for (FxgElement element : layerMap[layer]) {
                code.append(element.shape.translateTo(TO_LANGUAGE))
            }

            switch(TO_LANGUAGE) {
                case Language.JAVA: code.append(javaImageMethodStop())
                    break;
                case Language.JAVAFX:
                    break;
                case Language.GWT:
                    break;
                case Language.CANVAS:
                    break;
            }
        }
        return code.toString()
    }


    public void writeToFile(final String FILE_NAME, String codeToExport) {
        new File("$FILE_NAME").withWriter { out ->
            out.println codeToExport
    }
}
}
