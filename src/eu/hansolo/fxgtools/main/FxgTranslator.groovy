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
    private int nextSplit = 50000
    private int splitNumber = 0
    private List<String> layerSelection = []

    // Translate given elements to given language
    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE) {
        return translate(FILE_NAME, layerMap, LANGUAGE, WIDTH, HEIGHT, EXPORT_TO_FILE, COMPONENT_TYPE.JCOMPONENT, "")
    }

    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE, final COMPONENT_TYPE TYPE, final String NAME_PREFIX) {
        fireTranslationEvent(new TranslationEvent(this, TranslationState.RUNNING))
        final String CLASS_NAME = (FILE_NAME.contains(".") ? (FILE_NAME.substring(0, FILE_NAME.lastIndexOf('.')) + NAME_PREFIX) : (FILE_NAME + NAME_PREFIX)).capitalize()
        final String USER_HOME = System.properties.getProperty('user.home')
        StringBuilder desktopPath = new StringBuilder(USER_HOME).append(File.separator).append('Desktop').append(File.separator)
        StringBuilder exportFileName = new StringBuilder(desktopPath).append(CLASS_NAME)
        if (layerSelection.isEmpty()) {
            layerSelection.addAll(layerMap.keySet())
        }

        splitCounter = 0
        nextSplit = 40000

        StringBuilder codeToExport = new StringBuilder()

        // Export the header of the language specific template
        switch(LANGUAGE) {
            case Language.JAVA:
                if (EXPORT_TO_FILE) {
                    writeToFile(desktopPath.append('JavaShadow.java').toString(), javaShadowFile())
                }
                codeToExport.append(javaTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE, TYPE))
                exportFileName.append('.java')
                break
            case Language.JAVAFX:
                if (EXPORT_TO_FILE) {
                    writeToFile(desktopPath.append('FxgTest.java').toString(), javaFxTestTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                }
                codeToExport.append(javaFxTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
                exportFileName.append('.java')
                break
            case Language.GWT:
                codeToExport.append(gwtTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
                exportFileName.append('.java')
                break
            case Language.CANVAS:
                if (EXPORT_TO_FILE) {
                    writeToFile(exportFileName + '.html', htmlTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                }
                codeToExport.append(canvasTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
                exportFileName.append(".js")
                break
            case Language.GROOVYFX:
                if (EXPORT_TO_FILE) {
                    writeToFile(desktopPath.append('FxgTest.groovy').toString(), groovyFxTestTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                }
                codeToExport.append(javaFxTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
                exportFileName.append('.groovy')
                break
            case Language.ANDROID:
                if (EXPORT_TO_FILE) {
                    writeToFile(desktopPath.append('AndroidTest.java').toString(), androidTestTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                }
                codeToExport.append(androidTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
                exportFileName.append('.java')
                break
            default:
                fireTranslationEvent(new TranslationEvent(this, TranslationState.ERROR))
                throw Exception
        }
        if (EXPORT_TO_FILE) {
            writeToFile(exportFileName.toString(), codeToExport.toString())
        }
        fireTranslationEvent(new TranslationEvent(this, TranslationState.FINISHED))
        return codeToExport.toString()
    }

    String getDrawingCode(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        String result = code(layerMap, LANGUAGE)
        fireTranslationEvent(new TranslationEvent(this, TranslationState.FINISHED))
        return result
    }

    void setLayerSelection(List<String> selectedLayers) {
        layerSelection.clear()
        layerSelection.addAll(selectedLayers)
    }

    private String createVarName(String varName) {
        "${varName.charAt(0).toLowerCase()}${varName.substring(1)}"
    }

    // JAVA
    private String javaTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final COMPONENT_TYPE TYPE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/java.txt')
        String codeToExport = template.text

        StringBuilder imageDeclaration = new StringBuilder()
        StringBuilder imageInitialization = new StringBuilder()
        StringBuilder imageCreation = new StringBuilder()
        StringBuilder drawImage = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName)) {
                String varName = createVarName(layerName)
                imageDeclaration.append("    private BufferedImage ${varName}_Image;\n")
                imageInitialization.append("        ${varName}_Image = createImage(${WIDTH}, ${HEIGHT}, Transparency.TRANSLUCENT);\n")
                imageCreation.append("        if (${varName}_Image != null) {\n")
                imageCreation.append("            ${varName}_Image.flush();\n")
                imageCreation.append("        }\n")
                imageCreation.append("        ${varName}_Image = create_${layerName}_Image(WIDTH, HEIGHT);\n")
                drawImage.append("        G2.drawImage(${varName}_Image, 0, 0, null);\n")
            }
        }

        codeToExport = codeToExport.replace("\$componentImport", TYPE.IMPORT_STATEMENT);
        codeToExport = codeToExport.replace("\$componentType", TYPE.CODE)
        if (TYPE == COMPONENT_TYPE.TOPCOMPONENT) {
            codeToExport = codeToExport.replace("\$topComponentConstructor", "        setDisplayName(\"\$className\");")
        } else {
            codeToExport = codeToExport.replace("\$topComponentConstructor", "")
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
        layerCode.append("    public BufferedImage create_${LAYER_NAME}_Image(final int WIDTH, final int HEIGHT) {\n")
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

    private String javaShadowFile() {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javashadow.txt')
        String codeToExport = template.text
        return codeToExport
    }

    private void javaSplitLayer(String layerName, int splitNumber, StringBuilder code) {
        if (splitNumber == 1 ) {
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
        if (allLayers.length() > 31) {
            allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        }
        codeToExport = codeToExport.replace("\$layerList", allLayers.toString())

        return codeToExport
    }

    private String javaFxLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("\n")
        layerCode.append("    public Group create_${LAYER_NAME}_Layer(int imageWidth, int imageHeight) {\n")
        layerCode.append("        Group $LAYER_NAME = new Group();\n")
        return layerCode.toString()
    }

    private String javaFxLayerMethodStop(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
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

    private void javaFxSplitLayer(String layerName, int splitNumber, StringBuilder code, StringBuilder allElements) {
        if (splitNumber == 1) {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${layerName}.getChildren().addAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${layerName}, imageWidth, imageHeight);\n\n")
            code.append("        return ${layerName};\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(Group ${layerName}, int imageWidth, int imageHeight) {\n")
        } else {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${layerName}.getChildren().addAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${layerName}, imageWidth, imageHeight);\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(Group ${layerName}, int imageWidth, int imageHeight) {\n")
        }
    }


    // GWT
    private String gwtTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/gwt.txt')
        String codeToExport = template.text
        StringBuilder drawImagesToContext = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName)){
                drawImagesToContext.append("        draw_${layerName}_Image(context, CANVAS_WIDTH, CANVAS_HEIGHT);\n\n")
            }
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
            if (layerSelection.contains(layerName)) {
                createBuffers.append("    var ${layerName}_Buffer = document.createElement('canvas');\n")
                createBuffers.append("    ${layerName}_Buffer.width = imageWidth;\n")
                createBuffers.append("    ${layerName}_Buffer.height = imageHeight;\n")
                createBuffers.append("    var ${layerName}_Ctx = ${layerName}_Buffer.getContext('2d');\n\n")
                drawImagesToBuffer.append("        draw_${layerName}_Image(${layerName}_Ctx);\n\n")
                drawImagesToCanvas.append("        mainCtx.drawImage(${layerName}_Buffer, 0, 0);\n\n")
            }
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


    // GROOVYFX
    private String groovyFxTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/groovyfx.txt')
        String codeToExport = template.text

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$drawingCode", code(layerMap, LANGUAGE))
        if (allLayers.length() > 31) {
            allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        }
        codeToExport = codeToExport.replace("\$layerList", allLayers.toString())

        return codeToExport
    }

    private String groovyFxLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("\n")
        layerCode.append("    public Group create_${LAYER_NAME}_Layer(imageWidth, imageHeight) {\n")
        layerCode.append("        def $LAYER_NAME = new Group()\n")
        return layerCode.toString()
    }

    private String groovyFxLayerMethodStop(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("        return $LAYER_NAME")
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private String groovyFxTestTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/groovyfxtest.txt')
        String codeToExport = template.text

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$width", WIDTH)
        codeToExport = codeToExport.replace("\$height", HEIGHT)

        return codeToExport
    }

    private void groovyFxSplitLayer(String layerName, int splitNumber, StringBuilder code, StringBuilder allElements) {
        if (splitNumber == 1) {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${layerName}.children.addAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${layerName}, imageWidth, imageHeight)\n\n")
            code.append("        return ${layerName};\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(def ${layerName}, imageWidth, imageHeight) {\n")
        } else {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${layerName}.children.addAll(")
            code.append(allElements.toString())
            code.append(")\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${layerName}, imageWidth, imageHeight)\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(def ${layerName}, imageWidth, imageHeight) {\n")
        }
    }


    // ANDROID
    private String androidTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
           def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/android.txt')
           String codeToExport = template.text

           StringBuilder imageDeclaration = new StringBuilder()
           //StringBuilder imageInitialization = new StringBuilder()
           StringBuilder resizeImagesSquare = new StringBuilder()
           StringBuilder resizeImages = new StringBuilder()
           StringBuilder imageCreation = new StringBuilder()
           StringBuilder drawImage = new StringBuilder()

           layerMap.keySet().each {String layerName ->
               if (layerSelection.contains(layerName)) {
                   imageDeclaration.append("    private Bitmap ${layerName}Image;\n")
                   //imageInitialization.append("        ${layerName}Image = Bitmap.createBitmap(${WIDTH}, ${HEIGHT}, Bitmap.Config.ARGB_8888);\n")
                   resizeImagesSquare.append("            ${layerName}Image = create_${layerName}_Image(size, size);\n")
                   resizeImages.append("            ${layerName}Image = create_${layerName}_Image(width, height);\n")
                   imageCreation.append("        ${layerName}Image = create_${layerName}_Image(imageWidth, imageHeight);\n")
                   drawImage.append("        canvas.drawBitmap(${layerName}Image, 0, 0, paint);\n")
               }
           }

           codeToExport = codeToExport.replace("\$className", CLASS_NAME)
           codeToExport = codeToExport.replace("\$minimumWidth", WIDTH)
           codeToExport = codeToExport.replace("\$minimumHeight", HEIGHT)
           codeToExport = codeToExport.replace("\$imageDeclaration", imageDeclaration.toString())
           //codeToExport = codeToExport.replace("\$imageInitialization", imageInitialization.toString())
           codeToExport = codeToExport.replace("\$resizeImagesSquare", resizeImagesSquare.toString())
           codeToExport = codeToExport.replace("\$resizeImages", resizeImages.toString())
           codeToExport = codeToExport.replace("\$imageCreation", imageCreation.toString())
           codeToExport = codeToExport.replace("\$drawImage", drawImage.toString())
           codeToExport = codeToExport.replace("\$creationMethods", code(layerMap, LANGUAGE))

           return codeToExport
       }

    private String androidLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    public Bitmap create_${LAYER_NAME}_Image(int imageWidth, int imageHeight) {\n")
        layerCode.append("        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);\n")
        layerCode.append("        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);\n");
        layerCode.append("        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);\n");
        layerCode.append("        Canvas canvas = new Canvas(image);\n")
        layerCode.append("\n")
        return layerCode.toString()
   }

    private String androidLayerMethodStop() {
       StringBuilder layerCode = new StringBuilder()
       layerCode.append("        return image;\n")
       layerCode.append("    }\n\n")
       return layerCode.toString()
   }

    private String androidTestTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/androidtest.txt')
        String codeToExport = template.text

        codeToExport = codeToExport.replace("\$className", CLASS_NAME)
        codeToExport = codeToExport.replace("\$width", WIDTH)
        codeToExport = codeToExport.replace("\$height", HEIGHT)

        return codeToExport
    }

    private void androidSplitLayer(String layerName, int splitNumber, StringBuilder code) {
           if (splitNumber == 1) {
               code.append("        addSplit_${layerName}_${splitNumber}(canvas, paint, stroke,  imageWidth, imageHeight);\n\n")
               code.append("        return image;\n")
               code.append("    }\n\n")
               code.append("    private void addSplit_${layerName}_${splitNumber}(Canvas canvas, Paint paint, Paint stroke, int imageWidth, int imageHeight) {\n")
           } else {
               code.append("        addSplit_${layerName}_${splitNumber}(canvas, paint, stroke, imageWidth, imageHeight);\n\n")
               code.append("    }\n\n")
               code.append("    private void addSplit_${layerName}_${splitNumber}(Canvas canvas, Paint paint, Paint stroke, int imageWidth, int imageHeight) {\n")
           }
       }


    // CODE
    private String code(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        allLayers.length = 0
        allElements.length = 0
        layerMap.keySet().each {String layer->
            if (layerSelection.contains(layer)) {
                splitNumber = 0
                int shapeIndex = 0
                switch(LANGUAGE) {
                    case Language.JAVA: code.append(javaLayerMethodStart(layer))
                        break
                    case Language.JAVAFX: code.append(javaFxLayerMethodStart(layer))
                        break
                    case Language.GWT: code.append(gwtLayerMethodStart(layer))
                        break
                    case Language.CANVAS: code.append(canvasLayerMethodStart(layer))
                        break
                    case Language.GROOVYFX: code.append(groovyFxLayerMethodStart(layer))
                        break
                    case Language.ANDROID: code.append(androidLayerMethodStart(layer))
                        break
                }

                layerMap[layer].each {FxgElement element ->
                    shapeIndex += 1
                    code.append(element.shape.translateTo(LANGUAGE, shapeIndex))
                    if (LANGUAGE == Language.JAVAFX || LANGUAGE == LANGUAGE.GROOVYFX){
                        allElements.append("${layer}_${element.shape.shapeName}_${shapeIndex}").append(",\n")
                        for(def n = 0 ; n < layer.length() + 30 ; n+=1) {
                            allElements.append(" ")
                        }
                    }

                    splitCounter = code.length()
                    if (splitCounter.compareTo(nextSplit) > 0) {
                        nextSplit = splitCounter + 50000
                        splitCounter = 0
                        splitNumber += 1

                        if (LANGUAGE == Language.JAVA) {
                            javaSplitLayer(layer, splitNumber, code)
                        }
                        if (LANGUAGE == Language.JAVAFX) {
                            javaFxSplitLayer(layer, splitNumber, code, allElements)
                        }
                        if (LANGUAGE == Language.GROOVYFX) {
                            groovyFxSplitLayer(layer, splitNumber, code, allElements)
                        }
                        if (LANGUAGE == Language.GWT) {
                            gwtSplitLayer(layer, splitNumber, code)
                        }
                        if (LANGUAGE == Language.ANDROID) {
                            androidSplitLayer(layer, splitNumber, code)
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
                        if (splitNumber == 0) {
                            code.append("        return ${layer};\n")
                        }
                        code.append(javaFxLayerMethodStop(layer))
                        allLayers.append("create_${layer}_Layer(imageWidth, imageHeight)").append(",\n                             ")
                        break
                    case Language.GWT:
                        code.append(gwtLayerMethodStop())
                        break
                    case Language.CANVAS:
                        code.append(canvasLayerMethodStop())
                        break
                    case Language.GROOVYFX:
                        if (allElements.length() > layer.length() + 32) {
                            allElements.replace(allElements.length() - (layer.length() + 32), allElements.length(), "")
                        }
                        code.append("        ${layer}.children.addAll(")
                        code.append(allElements.toString())
                        code.append(")\n")
                        allElements.length = 0
                        if (splitNumber == 0) {
                            code.append("        return ${layer}\n")
                        }
                        code.append(javaFxLayerMethodStop(layer))
                        allLayers.append("create_${layer}_Layer(imageWidth, imageHeight)").append(",\n                             ")
                        break
                    case Language.ANDROID:
                        if (splitNumber > 0) {
                            code.append("    }\n\n")
                        } else {
                            code.append(androidLayerMethodStop())
                        }
                        break
                }
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
