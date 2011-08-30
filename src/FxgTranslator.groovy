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


    public void translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language TO_LANGUAGE, final String WIDTH, final String HEIGHT) {
        final String CLASS_NAME = FILE_NAME.contains(".") ? FILE_NAME.substring(0, FILE_NAME.lastIndexOf(".")) : FILE_NAME
        final String USER_HOME = System.getProperties().getProperty("user.home")
        StringBuilder exportFileName = new StringBuilder()
        exportFileName.append(USER_HOME).append(File.separator).append("Desktop").append(File.separator).append(CLASS_NAME)

        StringBuilder codeToExport = new StringBuilder();

        // Export the header of the language specific template
        switch(TO_LANGUAGE) {
            case Language.JAVA:     codeToExport.append(javaTemplate(CLASS_NAME, WIDTH, HEIGHT, layerMap, TO_LANGUAGE))
                                    exportFileName.append(".java")
                                    break;
            case Language.JAVAFX:   codeToExport.append(javaFxTemplate(CLASS_NAME))
                                    exportFileName.append(".jfx")
                                    break;
            case Language.GWT:      codeToExport.append(gwtTemplate(CLASS_NAME))
                                    exportFileName.append(".java")
                                    break;
            case Language.CANVAS:   codeToExport.append(canvasTemplate(CLASS_NAME))
                                    exportFileName.append(".js")
                                    break;
            default: throw new Exception("Language not supported...")
        }

        writeToFile(exportFileName.toString(), codeToExport.toString())
    }

    // JAVA
    private String javaTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language TO_LANGUAGE) {
        def template = new File('./resources/java.txt')
        String codeToExport = template.text

        StringBuilder imageDeclaration = new StringBuilder()
        StringBuilder imageInitialization = new StringBuilder()
        StringBuilder imageCreation = new StringBuilder()
        StringBuilder drawImage = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            imageDeclaration.append("    private BufferedImage ${layerName}Image;\n")
            imageInitialization.append("        ${layerName}Image = createImage(${WIDTH}, ${HEIGHT}, Transparency.TRANSLUCENT);\n")
            imageCreation.append("        if (${layerName}Image != null) {\n")
            imageCreation.append("            ${layerName}Image.flush();\n")
            imageCreation.append("        };\n")
            imageCreation.append("        ${layerName}Image = create_${layerName}_Image(WIDTH, HEIGHT);\n")
            drawImage.append("        G2.drawImage(${layerName}Image, 0, 0, null);\n")
        }

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$minimumWidth", WIDTH)
        codeToExport = codeToExport.replace("\$minimumHeight", HEIGHT)
        codeToExport = codeToExport.replace("\$imageDeclaration", imageDeclaration.toString())
        codeToExport = codeToExport.replace("\$imageInitialization", imageInitialization.toString())
        codeToExport = codeToExport.replace("\$imageCreation", imageCreation.toString())
        codeToExport = codeToExport.replace("\$drawImage", drawImage.toString())
        codeToExport = codeToExport.replace("\$creationMethods", code(layerMap, TO_LANGUAGE))

        return codeToExport
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

    // JAVAFX
    private String javaFxTemplate(final String CLASS_NAME) {
        return "JavaFxHeader\n"
    }

    // GWT
    private String gwtTemplate(final String CLASS_NAME) {
        return "GwtHeader\n"
    }

    // CANVAS
    private String canvasTemplate(final String CLASS_NAME) {
        return "CanvasHeader\n"
    }

    // CODE
    private String code(Map<String, List<FxgElement>> layerMap, final Language TO_LANGUAGE) {
        StringBuilder code = new StringBuilder()
        layerMap.keySet().each {String layer->
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

            layerMap[layer].each {FxgElement element ->
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
