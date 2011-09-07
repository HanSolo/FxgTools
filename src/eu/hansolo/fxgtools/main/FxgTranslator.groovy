package eu.hansolo.fxgtools.main

import eu.hansolo.fxgtools.fxg.FxgElement
import eu.hansolo.fxgtools.fxg.Language
import javax.swing.event.EventListenerList

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 20:36
 * To change this template use File | Settings | File Templates.
 */
class FxgTranslator {
    private EventListenerList eventListenerList = new EventListenerList()
    private StringBuilder allLayers = new StringBuilder()
    private StringBuilder allElements = new StringBuilder()
    private int splitCounter = 0
    private int nextSplit = 40000
    private int splitNumber = 0

    // Translate given elements to given language
    void translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT) {
        fireTranslationEvent(new TranslationEvent(this, TranslationState.RUNNING))
        final String CLASS_NAME = FILE_NAME.contains(".") ? FILE_NAME.substring(0, FILE_NAME.lastIndexOf('.')) : FILE_NAME
        final String USER_HOME = System.properties.getProperty('user.home')
        StringBuilder desktopPath = new StringBuilder(USER_HOME).append(File.separator).append('Desktop').append(File.separator)
        StringBuilder exportFileName = new StringBuilder(desktopPath).append(CLASS_NAME)

        splitCounter = 0
        nextSplit = 40000

        StringBuilder codeToExport = new StringBuilder()

        // Export the header of the language specific template
        switch(LANGUAGE) {
            case Language.JAVA:     codeToExport.append(javaTemplate(CLASS_NAME, WIDTH, HEIGHT, layerMap, LANGUAGE))
                                    exportFileName.append('.java')
                                    break
            case Language.JAVAFX:   writeToFile(desktopPath.append('FxgTest.java').toString(), javaFxTestTemplate(CLASS_NAME, WIDTH, HEIGHT))
                                    codeToExport.append(javaFxTemplate(CLASS_NAME, WIDTH, HEIGHT, layerMap, LANGUAGE))
                                    exportFileName.append('.java')
                                    break
            case Language.GWT:      codeToExport.append(gwtTemplate(CLASS_NAME, WIDTH, HEIGHT, layerMap, LANGUAGE))
                                    exportFileName.append('.java')
                                    break
            case Language.CANVAS:   writeToFile(exportFileName + '.html', htmlTemplate(CLASS_NAME, WIDTH, HEIGHT))
                                    codeToExport.append(canvasTemplate(CLASS_NAME, WIDTH, HEIGHT, layerMap, LANGUAGE))
                                    exportFileName.append(".js")
                                    break
            default:                fireTranslationEvent(new TranslationEvent(this, TranslationState.ERROR))
                                    throw Exception
        }
        writeToFile(exportFileName.toString(), codeToExport.toString())
        fireTranslationEvent(new TranslationEvent(this, TranslationState.FINISHED))
    }


    // JAVA
    private String javaTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/java.txt')
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
            imageCreation.append("        }\n")
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
        codeToExport = codeToExport.replace("\$creationMethods", code(layerMap, LANGUAGE))

        return codeToExport
    }

    private String javaLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    private BufferedImage create_${LAYER_NAME}_Image(final int WIDTH, final int HEIGHT) {\n")
        layerCode.append("        final GraphicsConfiguration GFX_CONF = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();\n")
        layerCode.append("        if (WIDTH <= 0 || HEIGHT <= 0) {\n")
        layerCode.append("            return GFX_CONF.createCompatibleImage(1, 1, java.awt.Transparency.TRANSLUCENT);\n")
        layerCode.append("        }\n")
        layerCode.append("        final BufferedImage IMAGE = GFX_CONF.createCompatibleImage(WIDTH, HEIGHT, Transparency.TRANSLUCENT);\n")
        layerCode.append("        final Graphics2D G2 = IMAGE.createGraphics();\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);\n")
        layerCode.append("\n")
        layerCode.append("        final int IMAGE_WIDTH = IMAGE.getWidth();\n")
        layerCode.append("        final int IMAGE_HEIGHT = IMAGE.getHeight();\n")
        return layerCode.toString()
    }

    private String javaLayerMethodStop() {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("        G2.dispose();\n")
        layerCode.append("        return IMAGE;\n")
        layerCode.append("    }\n\n")
        return layerCode.toString()
    }

    private void javaSplitLayer(String layerName, int splitNumber, StringBuilder code) {
        if (splitNumber == 1) {
            code.append("        addSplit_${layerName}_${splitNumber}(G2, IMAGE_WIDTH, IMAGE_HEIGHT);\n\n")
            code.append("        G2.dispose();\n\n")
            code.append("        return IMAGE;\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(final Graphics2D G2, final int IMAGE_WIDTH, final int IMAGE_HEIGHT) {\n")
        } else {
            code.append("        addSplit_${layerName}_${splitNumber}(G2, IMAGE_WIDTH, IMAGE_HEIGHT);\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(final Graphics2D G2, final int IMAGE_WIDTH, final int IMAGE_HEIGHT) {\n")
        }
    }


    // JAVAFX
    private String javaFxTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx.txt')
        String codeToExport = template.text

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$drawingCode", code(layerMap, LANGUAGE))
        allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        codeToExport = codeToExport.replace("\$layerList", allLayers.toString())

        return codeToExport
    }

    private String javaFxLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("\n")
        layerCode.append("    private Group create_${LAYER_NAME}_Layer(int imageWidth, int imageHeight) {\n")
        layerCode.append("        Group $LAYER_NAME = new Group();\n")
        return layerCode.toString()
    }

    private String javaFxLayerMethodStop(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("        return ${LAYER_NAME};\n")
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private String javaFxTestTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafxtest.txt')
        String codeToExport = template.text

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$width", WIDTH)
        codeToExport = codeToExport.replace("\$height", HEIGHT)

        return codeToExport
    }


    // GWT
    private String gwtTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/gwt.txt')
        String codeToExport = template.text
        StringBuilder drawImagesToContext = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            drawImagesToContext.append("        draw_${layerName}_Image(context, CANVAS_WIDTH, CANVAS_HEIGHT);\n\n")
        }

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$originalWidth", WIDTH)
        codeToExport = codeToExport.replace("\$originalHeight", HEIGHT)
        codeToExport = codeToExport.replace("\$drawImagesToContext", drawImagesToContext.toString())
        codeToExport = codeToExport.replace("\$creationMethods", code(layerMap, LANGUAGE))

        return codeToExport
    }

    private String gwtLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    public void draw_${LAYER_NAME}_Image(Context2d ctx, int imageWidth, int imageHeight) {\n")
        layerCode.append("        ctx.save();\n\n")
        return layerCode.toString()
    }

    private String gwtLayerMethodStop() {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("        ctx.restore();\n")
        layerCode.append("    }\n\n")
        return layerCode.toString()
    }

    private void gwtSplitLayer(String layerName, int splitNumber, StringBuilder code) {
        if (splitNumber == 1) {
            code.append("        addSplit_${layerName}_${splitNumber}(ctx, imageWidth, imageHeight);\n\n")
            code.append("        ctx.restore();\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(Context2d ctx, int imageWidth, int imageHeight) {\n")
        } else {
            code.append("        addSplit_${layerName}_${splitNumber}(ctx, imageWidth, imageHeight);\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(Context2d ctx, int imageWidth, int imageHeight) {\n")
        }
    }


    // CANVAS
    private String canvasTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/canvas.txt')
        String codeToExport = template.text

        StringBuilder createBuffers = new StringBuilder()
        StringBuilder drawImagesToBuffer = new StringBuilder()
        StringBuilder drawImagesToCanvas = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            createBuffers.append("    var ${layerName}_Buffer = document.createElement('canvas');\n")
            createBuffers.append("    ${layerName}_Buffer.width = imageWidth;\n")
            createBuffers.append("    ${layerName}_Buffer.height = imageHeight;\n")
            createBuffers.append("    var ${layerName}_Ctx = ${layerName}_Buffer.getContext('2d');\n\n")
            drawImagesToBuffer.append("        draw_${layerName}_Image(${layerName}_Ctx);\n\n")
            drawImagesToCanvas.append("        mainCtx.drawImage(${layerName}_Buffer, 0, 0);\n\n")
        }

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$createBuffers", createBuffers.toString())
        codeToExport = codeToExport.replace("\$drawImagesToBuffer", drawImagesToBuffer.toString())
        codeToExport = codeToExport.replace("\$drawImagesToCanvas", drawImagesToCanvas.toString())
        codeToExport = codeToExport.replace("\$creationMethods", code(layerMap, LANGUAGE))

        return codeToExport
    }

    private String canvasLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    var draw_${LAYER_NAME}_Image = function(ctx) {\n")
        layerCode.append("        ctx.save();\n\n")
        return layerCode.toString()
    }

    private String canvasLayerMethodStop() {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("        ctx.restore();\n")
        layerCode.append("    }\n\n")
        return layerCode.toString()
    }

    private String htmlTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/html.txt')
        String codeToExport = template.text

        codeToExport = codeToExport.replace("\$jsFileName", CLASS_NAME + ".js")
        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$width", WIDTH)
        codeToExport = codeToExport.replace("\$height", HEIGHT)

        return codeToExport
    }


    // CODE
    private String code(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        allLayers.length = 0
        allElements.length = 0
        layerMap.keySet().each {String layer->
            splitNumber = 0
            switch(LANGUAGE) {
                case Language.JAVA: code.append(javaLayerMethodStart(layer))
                    break
                case Language.JAVAFX: code.append(javaFxLayerMethodStart(layer))
                    break
                case Language.GWT: code.append(gwtLayerMethodStart(layer))
                    break
                case Language.CANVAS: code.append(canvasLayerMethodStart(layer))
                    break
            }

            layerMap[layer].eachWithIndex {FxgElement element, i ->
                code.append(element.shape.translateTo(LANGUAGE))
                splitCounter = code.length()
                if (splitCounter.compareTo(nextSplit) > 0) {
                    nextSplit = splitCounter + 40000
                    splitCounter = 0
                    splitNumber += 1

                    if (LANGUAGE == Language.JAVA) {
                        javaSplitLayer(layer, splitNumber, code)
                    }
                    if (LANGUAGE == Language.GWT) {
                        gwtSplitLayer(layer, splitNumber, code)
                    }
                }

                if (LANGUAGE == Language.JAVAFX){
                    allElements.append("${layer}_${element.shape.shapeName}").append(",\n")
                    for(def n = 0 ; n < layer.length() + 30 ; n+=1) {
                        allElements.append(" ")
                    }
                }
            }

            switch(LANGUAGE) {
                case Language.JAVA:
                    if (splitNumber > 0) {
                        code.append("    }\n\n")
                    } else {
                        code.append(javaLayerMethodStop())
                    }
                    break
                case Language.JAVAFX:
                    if (allElements.length() > layer.length() + 32) {
                        allElements.replace(allElements.length() - (layer.length() + 32), allElements.length(), "")
                    }
                    code.append("        ${layer}.getChildren().addAll(")
                    code.append(allElements.toString())
                    code.append(");\n")
                    allElements.length = 0
                    code.append(javaFxLayerMethodStop(layer))
                    allLayers.append("create_${layer}_Layer(imageWidth, imageHeight)").append(",\n                             ")
                    break
                case Language.GWT:
                    code.append(gwtLayerMethodStop())
                    break
                case Language.CANVAS:
                    code.append(canvasLayerMethodStop())
                    break
            }
        }
        return code.toString()
    }


    // OUTPUT
    private void writeToFile(final String FILE_NAME, String codeToExport) {
        new File("$FILE_NAME").withWriter { out ->
            out.println codeToExport
    }
}


    // TRANSLATION EVENT LISTENER
    public void addTranslationListener(TranslationListener listener) {
        eventListenerList.add(TranslationListener.class, listener)
    }

    public void removeTranslationListener(TranslationListener listener) {
        eventListenerList.remove(TranslationListener.class, listener)
    }

    protected void fireTranslationEvent(TranslationEvent event) {
        Object[] listeners = eventListenerList.getListenerList()
        int max = listeners.length
        for (int i = 0; i < max; i++) {
            if ( listeners[i] == TranslationListener.class ) {
                ((TranslationListener) listeners[i + 1]).translationEventPerformed(event)
            }
        }
    }
}
