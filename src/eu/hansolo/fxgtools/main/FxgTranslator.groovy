package eu.hansolo.fxgtools.main

import eu.hansolo.fxgtools.fxg.FxgElement
import eu.hansolo.fxgtools.fxg.Language
import javax.swing.event.EventListenerList
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 20:36
 * To change this template use File | Settings | File Templates.
 */
class FxgTranslator {
    private EventListenerList eventListenerList = new EventListenerList()
    private StringBuilder     allLayers         = new StringBuilder()
    private StringBuilder     allElements       = new StringBuilder()
    private int               splitCounter      = 0
    private int               nextSplit         = 50000
    private int               splitNumber       = 0
    private String            packageInfo       = "eu.hansolo.fx"
    private List<String>      layerSelection    = []


    // ******************** Translate given elements to given language ********
    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE) {
        return translate(FILE_NAME, layerMap, LANGUAGE, WIDTH, HEIGHT, EXPORT_TO_FILE, COMPONENT_TYPE.JCOMPONENT, "", new HashMap<String, String>())
    }

    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE, final HashMap<String, String> PROPERTIES) {
        return translate(FILE_NAME, layerMap, LANGUAGE, WIDTH, HEIGHT, EXPORT_TO_FILE, COMPONENT_TYPE.JCOMPONENT, "", PROPERTIES)
    }

    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE, final COMPONENT_TYPE TYPE, final String NAME_PREFIX, final HashMap<String, String> PROPERTIES) {
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
                codeToExport.append(javaTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE, TYPE, PROPERTIES))
                exportFileName.append('.java')
                break
            case Language.JAVAFX:
                if (EXPORT_TO_FILE) {
                    String path = new StringBuilder(USER_HOME).append(File.separator).append('Desktop').append(File.separator).toString()
                    writeToFile(desktopPath.append('FxgTest.java').toString(), javaFxTestTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                    writeToFile(path + ("${CLASS_NAME}.java").toString(), javaFxControlTemplate(CLASS_NAME, PROPERTIES))
                    writeToFile(path + ("${CLASS_NAME}Behavior.java").toString(), javaFxBehaviorTemplate(CLASS_NAME))
                    writeToFile(path + ("${CLASS_NAME.toLowerCase()}.css").toString(), javaFxCssTemplate(CLASS_NAME, layerMap))
                }
                codeToExport.append(javaFxSkinTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
                exportFileName.append('Skin.java')
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
                codeToExport.append(groovyFxTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
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
            writeToFile(exportFileName.toString(), makeNicer(codeToExport, LANGUAGE))
        }
        fireTranslationEvent(new TranslationEvent(this, TranslationState.FINISHED))
        return codeToExport.toString()
    }

    String getDrawingCode(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        String result = code(layerMap, LANGUAGE)
        fireTranslationEvent(new TranslationEvent(this, TranslationState.FINISHED))
        return result
    }

    void setPackageInfo(final String PACKAGE_INFO) {
        packageInfo = PACKAGE_INFO.isEmpty() ? "eu.hansolo.fx" : PACKAGE_INFO;
    }

    void setLayerSelection(List<String> selectedLayers) {
        layerSelection.clear()
        layerSelection.addAll(selectedLayers)
    }

    private String createVarName(final String VAR_NAME) {
        String varName
        if (VAR_NAME.contains("_")) {
            String[] varNameParts = VAR_NAME.toLowerCase().split("_")
            StringBuilder output = new StringBuilder()
            varNameParts.each {output.append(it.capitalize())}
            varName = output.substring(0,1).toLowerCase() + output.substring(1)
        } else if (VAR_NAME == VAR_NAME.capitalize()) {
            varName = "${VAR_NAME.charAt(0).toLowerCase()}${VAR_NAME.substring(1)}"
        } else {
            varName = VAR_NAME
        }
        return varName
    }


    // ******************** JAVA **********************************************
    private String javaTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final COMPONENT_TYPE TYPE, final HashMap<String, String> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/java.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        StringBuilder imageDeclaration = new StringBuilder()
        StringBuilder imageInitialization = new StringBuilder()
        StringBuilder imageCreation = new StringBuilder()
        StringBuilder drawImage = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName)) {
                String varName = createVarName(layerName)
                imageDeclaration.append("    private BufferedImage         ${varName}_Image;\n")
                imageInitialization.append("        ${varName}_Image = createImage(INNER_BOUNDS.width, INNER_BOUNDS.height, Transparency.TRANSLUCENT);\n")
                imageCreation.append("        if (${varName}_Image != null) {\n")
                imageCreation.append("            ${varName}_Image.flush();\n")
                imageCreation.append("        }\n")
                imageCreation.append("        ${varName}_Image = create_${layerName}_Image(WIDTH, HEIGHT);\n")
                drawImage.append("        G2.drawImage(${varName}_Image, 0, 0, null);\n")
            }
        }

        replaceAll(codeToExport, "\$componentImport", TYPE.IMPORT_STATEMENT)
        replaceAll(codeToExport, "\$componentType", TYPE.CODE)
        if (TYPE == COMPONENT_TYPE.TOPCOMPONENT) {
            replaceAll(codeToExport, "\$topComponentConstructor", "        setDisplayName(\"\$className\");")
        } else {
            replaceAll(codeToExport, "\$topComponentConstructor", "")
        }
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$minimumWidth", WIDTH)
        replaceAll(codeToExport, "\$minimumHeight", HEIGHT)
        replaceAll(codeToExport, "\$imageDeclaration", imageDeclaration.toString())
        replaceAll(codeToExport, "\$imageInitialization", imageInitialization.toString())
        replaceAll(codeToExport, "\$imageCreation", imageCreation.toString())
        replaceAll(codeToExport, "\$drawImage", drawImage.toString())
        replaceAll(codeToExport, "\$propertyDeclaration", javaPropertyDeclaration(PROPERTIES))
        replaceAll(codeToExport, "\$propertyInitialization", javaPropertyInitialization(PROPERTIES))
        replaceAll(codeToExport, "\$propertyGetterSetter", javaPropertyGetterSetter(PROPERTIES))

        replaceAll(codeToExport, "\$creationMethods", code(layerMap, LANGUAGE))

        return codeToExport.toString()
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
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);\n")
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
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        return codeToExport.toString()
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

    private String javaPropertyDeclaration(final HashMap<String, String> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            PROPERTY_CODE.append("    public static final String    ").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY = ").append("\"").append(PROPERTY_NAME.toUpperCase()).append("\";\n")
        }
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("double")) {
                PROPERTY_CODE.append("    private double                ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("boolean")) {
                PROPERTY_CODE.append("    private boolean               ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("int")) {
                PROPERTY_CODE.append("    private int                   ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("long")) {
                PROPERTY_CODE.append("    private long                  ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("string")) {
                PROPERTY_CODE.append("    private String                ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("object")) {
                PROPERTY_CODE.append("    private Object                ").append(PROPERTY_NAME).append(";\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaPropertyInitialization(final HashMap<String, String> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("double")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = 0.0;\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("boolean")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = false;\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("int")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = 0;\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("long")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = 0l;\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("string")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = \"\";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("object")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = new Object();\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaPropertyGetterSetter(final HashMap<String, String> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("double")) {
                PROPERTY_CODE.append("    ").append("public final double get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final double ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        double old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.fireProperty(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("boolean")) {
                PROPERTY_CODE.append("    ").append("public final boolean get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final boolean ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        double old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.fireProperty(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("int")) {
                PROPERTY_CODE.append("    ").append("public final int get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final int ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        double old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.fireProperty(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("long")) {
                PROPERTY_CODE.append("    ").append("public final long get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final long ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        double old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.fireProperty(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("string")) {
                PROPERTY_CODE.append("    ").append("public final String get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final String ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        double old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.fireProperty(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("object")) {
                PROPERTY_CODE.append("    ").append("public final Object get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final Object ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        double old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.fireProperty(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            }
        }
        return PROPERTY_CODE.toString()
    }


    // ******************** JAVA FX *******************************************
    private String javaFxSkinTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_skin.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        StringBuilder groupDeclaration = new StringBuilder()
        StringBuilder groupInitialization = new StringBuilder()
        StringBuilder groupUpdate = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName)) {
                String varName = createVarName(layerName)
                groupDeclaration.append("    private Group        ${varName};\n")
                groupInitialization.append("        ${varName} = new Group();\n")
                groupUpdate.append("        draw${layerName}();\n")
            }
        }

        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$groupDeclaration", groupDeclaration.toString())
        replaceAll(codeToExport, "\$groupInitialization", groupInitialization.toString())
        replaceAll(codeToExport, "\$groupUpdate", groupUpdate.toString())
        replaceAll(codeToExport, "\$drawingCode", code(layerMap, LANGUAGE))
        if (allLayers.length() > 31) {
            allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        }
        replaceAll(codeToExport, "\$layerList", allLayers.toString())

        return codeToExport.toString()
    }

    private String javaFxControlTemplate(final String CLASS_NAME, final HashMap<String, String> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_control.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$propertyDeclaration", javaFxPropertyDeclaration(PROPERTIES))
        replaceAll(codeToExport, "\$propertyInitialization", javaFxPropertyInitialization(PROPERTIES))
        replaceAll(codeToExport, "\$propertyGetterSetter", javaFxPropertyGetterSetter(PROPERTIES))
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$styleClass", CLASS_NAME.toLowerCase())
        return codeToExport.toString()
    }

    private String javaFxCssTemplate(final String CLASS_NAME, Map<String, List<FxgElement>> layerMap) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_css.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", packageInfo)
        replaceAll(codeToExport, "\$styleClass", CLASS_NAME.toLowerCase())
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$fillAndStrokeDefinitions", cssCode(layerMap))
        return codeToExport.toString()
    }

    private String javaFxBehaviorTemplate(final String CLASS_NAME) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_behavior.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        return codeToExport.toString()
    }

    private String javaFxTestTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafxtest.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)
        return codeToExport.toString()
    }

    private String javaFxLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        String lowerLayerName = createVarName(LAYER_NAME)
        layerCode.append("\n")
        layerCode.append("    public final void draw${LAYER_NAME}() {\n")
        layerCode.append("        final double SIZE = control.getPrefWidth() < control.getPrefHeight() ? control.getPrefWidth() : control.getPrefHeight();\n")
        layerCode.append("        final double WIDTH = square ? SIZE : control.getPrefWidth();\n")
        layerCode.append("        final double HEIGHT = square ? SIZE : control.getPrefHeight();\n")
        layerCode.append("        ${lowerLayerName}.getChildren().clear();\n")
        layerCode.append("        final Shape IBOUNDS = new Rectangle(0, 0, WIDTH, HEIGHT);\n")
        layerCode.append("        IBOUNDS.setOpacity(0.0);\n")
        layerCode.append("        IBOUNDS.setStroke(null);\n")
        layerCode.append("        ${lowerLayerName}.getChildren().add(IBOUNDS);\n")
        return layerCode.toString()
    }

    private String javaFxLayerMethodStop(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private void javaFxSplitLayer(String layerName, int splitNumber, StringBuilder code, StringBuilder allElements) {
        String varName = createVarName(layerName)
        if (splitNumber == 1) {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${varName}.getChildren().addAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${varName}, WIDTH, HEIGHT);\n\n")
            code.append("        return ${varName};\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(Group ${varName}, final int WIDTH, final int HEIGHT) {\n")
        } else {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${varName}.getChildren().addAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${varName}, WIDTH, HEIGHT);\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(Group ${varName}, final int WIDTH, final int HEIGHT) {\n")
        }
    }

    private String javaFxPropertyDeclaration(final HashMap<String, String> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("double")) {
                PROPERTY_CODE.append("    private DoubleProperty      ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("boolean")) {
                PROPERTY_CODE.append("    private BooleanProperty     ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("int")) {
                PROPERTY_CODE.append("    private IntegerProperty     ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("long")) {
                PROPERTY_CODE.append("    private LongProperty        ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("string")) {
                PROPERTY_CODE.append("    private StringProperty      ").append(PROPERTY_NAME).append(";\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("object")) {
                PROPERTY_CODE.append("    private ObjectProperty      ").append(PROPERTY_NAME).append(";\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertyInitialization(final HashMap<String, String> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        int maxLength
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            maxLength = Math.max(PROPERTY_NAME.length(), maxLength)
        }
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("double")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleDoubleProperty(0.0);\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("boolean")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleBooleanProperty(false);\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("int")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleIntegerProperty(0);\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("long")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleLongProperty(0l);\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("string")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleStringProperty(\"\");\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("object")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleObjectProperty();\n")
            }
        }
        PROPERTY_CODE.append("        square")
        final int SPACER = maxLength == 0 ? 0 : 6;
        appendBlanks(PROPERTY_CODE, (maxLength - SPACER))
        PROPERTY_CODE.append(" = false;\n")
        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertyGetterSetter(final HashMap<String, String> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("double")) {
                PROPERTY_CODE.append("    ").append("public final double get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final double ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final DoubleProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("boolean")) {
                PROPERTY_CODE.append("    ").append("public final boolean get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final boolean ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final BooleanProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("int")) {
                PROPERTY_CODE.append("    ").append("public final int get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final int ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final IntegerProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("long")) {
                PROPERTY_CODE.append("    ").append("public final long get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final long ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final LongProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("string")) {
                PROPERTY_CODE.append("    ").append("public final String get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final String ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final StringProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            }
            if (PROPERTIES.get(PROPERTY_NAME).toLowerCase().equals("object")) {
                PROPERTY_CODE.append("    ").append("public final Object get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final Object ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final ObjectProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            }
        }
        return PROPERTY_CODE.toString()
    }


    // ******************** GWT ***********************************************
    private String gwtTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/gwt.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        StringBuilder drawImagesToContext = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName)){
                drawImagesToContext.append("        draw_${layerName}_Image(context, CANVAS_WIDTH, CANVAS_HEIGHT);\n\n")
            }
        }

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$originalWidth", WIDTH)
        replaceAll(codeToExport, "\$originalHeight", HEIGHT)
        replaceAll(codeToExport, "\$drawImagesToContext", drawImagesToContext.toString())
        replaceAll(codeToExport, "\$creationMethods", code(layerMap, LANGUAGE))

        return codeToExport.toString()
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


    // ******************** CANVAS ********************************************
    private String canvasTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/canvas.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

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

        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$createBuffers", createBuffers.toString())
        replaceAll(codeToExport, "\$drawImagesToBuffer", drawImagesToBuffer.toString())
        replaceAll(codeToExport, "\$drawImagesToCanvas", drawImagesToCanvas.toString())
        replaceAll(codeToExport, "\$creationMethods", code(layerMap, LANGUAGE))

        return codeToExport.toString()
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
        StringBuilder codeToExport = new StringBuilder(template.text)

        replaceAll(codeToExport, "\$jsFileName", CLASS_NAME + ".js")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)

        return codeToExport.toString()
    }


    // ******************** GROOVY FX *****************************************
    private String groovyFxTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/groovyfx.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        StringBuilder groupDeclaration = new StringBuilder()
        StringBuilder groupInitialization = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName)) {
                String varName = createVarName(layerName)
                groupDeclaration.append("    private Group ${varName}\n")
                groupInitialization.append("        ${varName} = create_${layerName}_Layer(imageWidth, imageHeight)\n")
            }
        }

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$groupDeclaration", groupDeclaration.toString())
        replaceAll(codeToExport, "\$groupInitialization", groupInitialization.toString())
        replaceAll(codeToExport, "\$drawingCode", code(layerMap, LANGUAGE))
        if (allLayers.length() > 31) {
            allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        }
        replaceAll(codeToExport, "\$layerList", allLayers.toString())

        return codeToExport.toString()
    }

    private String groovyFxLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("\n")
        layerCode.append("    public final Group create_${LAYER_NAME}_Layer(imageWidth, imageHeight) {\n")
        layerCode.append("        def ${LAYER_NAME.charAt(0).toLowerCase()}${LAYER_NAME.substring(1)} = new Group()\n")
        return layerCode.toString()
    }

    private String groovyFxLayerMethodStop(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private String groovyFxTestTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/groovyfxtest.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)

        return codeToExport.toString()
    }

    private void groovyFxSplitLayer(String layerName, int splitNumber, StringBuilder code, StringBuilder allElements) {
        String varName = createVarName(layerName)
        if (splitNumber == 1) {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${layerName}.children.addAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${varName}, imageWidth, imageHeight)\n\n")
            code.append("        return ${varName};\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(def ${varName}, imageWidth, imageHeight) {\n")
        } else {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${varName}.children.addAll(")
            code.append(allElements.toString())
            code.append(")\n\n")
            allElements.length = 0

            code.append("        addSplit_${layerName}_${splitNumber}(${varName}, imageWidth, imageHeight)\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit_${layerName}_${splitNumber}(def ${varName}, imageWidth, imageHeight) {\n")
        }
    }


    // ******************** ANDROID *******************************************
    private String androidTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/android.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

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

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$minimumWidth", WIDTH)
        replaceAll(codeToExport, "\$minimumHeight", HEIGHT)
        replaceAll(codeToExport, "\$imageDeclaration", imageDeclaration.toString())
        //replaceAll(codeToExport, "\$imageInitialization", imageInitialization.toString())
        replaceAll(codeToExport, "\$resizeImagesSquare", resizeImagesSquare.toString())
        replaceAll(codeToExport, "\$resizeImages", resizeImages.toString())
        replaceAll(codeToExport, "\$imageCreation", imageCreation.toString())
        replaceAll(codeToExport, "\$drawImage", drawImage.toString())
        replaceAll(codeToExport, "\$creationMethods", code(layerMap, LANGUAGE))

       return codeToExport.toString()
    }

    private String androidLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    public Bitmap create_${LAYER_NAME}_Image(int imageWidth, int imageHeight) {\n")
        layerCode.append("        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);\n")
        layerCode.append("        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);\n")
        layerCode.append("        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);\n")
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
        StringBuilder codeToExport = new StringBuilder(template.text)

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)

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


    // ******************** VAADIN ********************************************



    // ******************** CODE **********************************************
    private String code(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        allLayers.length = 0
        allElements.length = 0
        String varName
        layerMap.keySet().each {String layerName->
            if (layerSelection.contains(layerName)) {
                splitNumber = 0
                int shapeIndex = 0
                varName = createVarName(layerName)
                // add language dependend method heads
                switch(LANGUAGE) {
                    case Language.JAVA: code.append(javaLayerMethodStart(layerName))
                        break
                    case Language.JAVAFX: code.append(javaFxLayerMethodStart(layerName))
                        break
                    case Language.GWT: code.append(gwtLayerMethodStart(layerName))
                        break
                    case Language.CANVAS: code.append(canvasLayerMethodStart(layerName))
                        break
                    case Language.GROOVYFX: code.append(groovyFxLayerMethodStart(layerName))
                        break
                    case Language.ANDROID: code.append(androidLayerMethodStart(layerName))
                        break
                }

                // main translation routine
                layerMap[layerName].each {FxgElement element ->
                    shapeIndex += 1
                    code.append(element.shape.translateTo(LANGUAGE, shapeIndex))
                    if (LANGUAGE == Language.JAVAFX || LANGUAGE == LANGUAGE.GROOVYFX){
                        allElements.append("${layerName.toUpperCase()}_${element.shape.shapeName.toUpperCase()}_${shapeIndex}").append(",\n")
                        for(def n = 0 ; n < layerName.length() + 30 ; n+=1) {
                            allElements.append(" ")
                        }
                    }

                    // split methods if they become too long
                    splitCounter = code.length()
                    if (splitCounter.compareTo(nextSplit) > 0) {
                        nextSplit = splitCounter + 50000
                        splitCounter = 0
                        splitNumber += 1

                        if (LANGUAGE == Language.JAVA) {
                            javaSplitLayer(layerName, splitNumber, code)
                        }
                        if (LANGUAGE == Language.JAVAFX) {
                            javaFxSplitLayer(layerName, splitNumber, code, allElements)
                        }
                        if (LANGUAGE == Language.GROOVYFX) {
                            groovyFxSplitLayer(layerName, splitNumber, code, allElements)
                        }
                        if (LANGUAGE == Language.GWT) {
                            gwtSplitLayer(layerName, splitNumber, code)
                        }
                        if (LANGUAGE == Language.ANDROID) {
                            androidSplitLayer(layerName, splitNumber, code)
                        }
                    }
                }

                // add language dependend method end
                switch(LANGUAGE) {
                    case Language.JAVA:
                        if (splitNumber > 0) {
                            code.append("    }\n\n")
                        } else {
                            code.append(javaLayerMethodStop())
                        }
                        break
                    case Language.JAVAFX:
                        if (allElements.length() > layerName.length() + 32) {
                            allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
                        }
                        code.append("        ${varName}.getChildren().addAll(")
                        code.append(allElements.toString())
                        code.append(");\n")
                        allElements.length = 0
                        code.append(javaFxLayerMethodStop(layerName))
                        allLayers.append(createVarName(layerName)).append(",\n                             ")
                        break
                    case Language.GWT:
                        code.append(gwtLayerMethodStop())
                        break
                    case Language.CANVAS:
                        code.append(canvasLayerMethodStop())
                        break
                    case Language.GROOVYFX:
                        if (allElements.length() > layerName.length() + 32) {
                            allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
                        }
                        code.append("        ${varName}.children.addAll(")
                        code.append(allElements.toString())
                        code.append(")\n")
                        allElements.length = 0
                        if (splitNumber == 0) {
                            code.append("        return ${varName}\n")
                        }
                        code.append(javaFxLayerMethodStop(layerName))
                        allLayers.append(createVarName(layerName)).append(",\n                             ")
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

    private String cssCode(Map<String, List<FxgElement>> layerMap) {
        StringBuilder cssCode = new StringBuilder()
        layerMap.keySet().each {String layerName->
            if (layerSelection.contains(layerName)) {
                int shapeIndex = 0
                layerMap[layerName].each {FxgElement element ->
                    shapeIndex += 1
                    // create css code
                    String name = "${layerName.toUpperCase()}-${element.shape.shapeName.toUpperCase()}-${shapeIndex}"
                    if (element.shape.filled) {
                        cssCode.append(element.shape.createCssFill(name))
                    }
                    //if (element.shape.stroked) {
                    //    cssCode.append(element.shape.createCssStroke(name))
                    //}
                }
            }
        }
        return cssCode.toString()
    }


    // ******************** OUTPUT ********************************************
    private void writeToFile(final String FILE_NAME, String codeToExport) {
        new File("$FILE_NAME").withWriter { out ->
            out.println codeToExport
    }
}


    // ******************** OPTIMIZE OUTPUT ***********************************
    private String makeNicer(final StringBuilder CODE, final Language LANGUAGE) {
        /* TEMPLATE ENGINE
        String template = '${width}_${height}'
        def width = 200
        def height = 1000

        def engine = new SimpleTemplateEngine()
        def binding = [width:"${width}", height:"${height}"]
        def result = engine.createTemplate(template).make(binding) // in the template, replace values of one and two from the binding

        println result.toString()

        */

        switch (LANGUAGE) {
            case Language.JAVA:
                replaceAll(CODE, "0.0 * IMAGE_WIDTH", "0.0")
                replaceAll(CODE, "0.0 * IMAGE_WIDTH", "0.0")
                replaceAll(CODE, "0.0 * IMAGE_HEIGHT", "0.0")
                replaceAll(CODE, "0.0 * SIZE", "0.0")
                replaceAll(CODE, "1.0 * IMAGE_WIDTH", "IMAGE_WIDTH")
                replaceAll(CODE, "1.0 * IMAGE_HEIGHT", "IMAGE_HEIGHT")
                replaceAll(CODE, "1.0 * SIZE", "SIZE")
                replaceAll(CODE, "new Color(0, 0, 0)", "Color.BLACK")
                replaceAll(CODE, "new Color(0.0f, 0.0f, 0.0f, 1f)", "Color.BLACK")
                replaceAll(CODE, "new Color(0.0f, 0.0f, 0.0f, 1.0f)", "Color.BLACK")
                replaceAll(CODE, "new Color(0f, 0f, 0f, 1f)", "Color.BLACK")
                replaceAll(CODE, "new Color(1, 1, 1)", "Color.WHITE")
                replaceAll(CODE, "new Color(1.0f, 1.0f, 1.0f, 1f)", "Color.WHITE")
                replaceAll(CODE, "new Color(1.0f, 1.0f, 1.0f, 1.0f)", "Color.WHITE")
                replaceAll(CODE, "new Color(1f, 1f, 1f, 1f)", "Color.WHITE")
                replaceAll(CODE, "new Color(1, 0, 0)", "Color.RED")
                replaceAll(CODE, "new Color(1.0f, 0.0f, 0.0f, 1f)", "Color.RED")
                replaceAll(CODE, "new Color(1.0f, 0.0f, 0.0f, 1.0f)", "Color.RED")
                replaceAll(CODE, "new Color(1f, 0f, 0f, 1f)", "Color.RED")
                replaceAll(CODE, "new Color(0, 1, 0)", "Color.GREEN")
                replaceAll(CODE, "new Color(0.0f, 1.0f, 0.0f, 1f)", "Color.GREEN")
                replaceAll(CODE, "new Color(0.0f, 1.0f, 0.0f, 1.0f)", "Color.GREEN")
                replaceAll(CODE, "new Color(0f, 1f, 0f, 1f)", "Color.GREEN")
                replaceAll(CODE, "new Color(0, 0, 1)", "Color.BLUE")
                replaceAll(CODE, "new Color(0.0f, 0.0f, 1.0f, 1f)", "Color.BLUE")
                replaceAll(CODE, "new Color(0.0f, 0.0f, 1.0f, 1.0f)", "Color.BLUE")
                replaceAll(CODE, "new Color(0f, 0f, 1f, 1f)", "Color.BLUE")
                replaceAll(CODE, "new Color(1, 1, 0)", "Color.YELLOW")
                replaceAll(CODE, "new Color(1.0f, 1.0f, 0.0f, 1f)", "Color.YELLOW")
                replaceAll(CODE, "new Color(1.0f, 1.0f, 0.0f, 1.0f)", "Color.YELLOW")
                replaceAll(CODE, "new Color(1f, 1f, 0f, 1f)", "Color.YELLOW")
                replaceAll(CODE, "new Color(0, 1, 1)", "Color.CYAN")
                replaceAll(CODE, "new Color(0.0f, 1.0f, 1.0f, 1f)", "Color.CYAN")
                replaceAll(CODE, "new Color(0.0f, 1.0f, 1.0f, 1.0f)", "Color.CYAN")
                replaceAll(CODE, "new Color(0f, 1f, 1f, 1f)", "Color.CYAN")
                replaceAll(CODE, "new Color(1, 0, 1)", "Color.MAGENTA")
                replaceAll(CODE, "new Color(1.0f, 0.0f, 1.0f, 1f)", "Color.MAGENTA")
                replaceAll(CODE, "new Color(1.0f, 0.0f, 1.0f, 1.0f)", "Color.MAGENTA")
                replaceAll(CODE, "new Color(1f, 0f, 1f, 1f)", "Color.MAGENTA")
                break
            case Language.JAVAFX:
                replaceAll(CODE, "0.0 * WIDTH", "0.0")
                replaceAll(CODE, "0.0 * HEIGHT", "0.0")
                replaceAll(CODE, "0.0 * SIZE", "0.0")
                replaceAll(CODE, "1.0 * WIDTH", "WIDTH")
                replaceAll(CODE, "1.0 * HEIGHT", "HEIGHT")
                replaceAll(CODE, "1.0 * SIZE", "SIZE")
                replaceAll(CODE, "Color.color(0, 0, 0, 1)", "Color.BLACK")
                replaceAll(CODE, "Color.color(0.0, 0.0, 0.0, 1)", "Color.BLACK")
                replaceAll(CODE, "Color.color(0.0, 0.0, 0.0, 1.0)", "Color.BLACK")
                replaceAll(CODE, "Color.color(1, 1, 1, 1)", "Color.WHITE")
                replaceAll(CODE, "Color.color(1.0, 1.0, 1.0, 1)", "Color.WHITE")
                replaceAll(CODE, "Color.color(1.0, 1.0, 1.0, 1.0)", "Color.WHITE")
                replaceAll(CODE, "Color.color(1, 0, 0, 1)", "Color.RED")
                replaceAll(CODE, "Color.color(1.0, 0.0, 0.0, 1)", "Color.RED")
                replaceAll(CODE, "Color.color(1.0, 0.0, 0.0, 1.0)", "Color.RED")
                replaceAll(CODE, "Color.color(0, 1, 0, 1)", "Color.LIME")
                replaceAll(CODE, "Color.color(0.0, 1.0, 0.0, 1)", "Color.LIME")
                replaceAll(CODE, "Color.color(0.0, 1.0, 0.0, 1.0)", "Color.LIME")
                replaceAll(CODE, "Color.color(0, 0, 1, 1)", "Color.BLUE")
                replaceAll(CODE, "Color.color(0.0, 0.0, 1.0, 1)", "Color.BLUE")
                replaceAll(CODE, "Color.color(0.0, 0.0, 1.0, 1.0)", "Color.BLUE")
                replaceAll(CODE, "Color.color(1, 1, 0, 1)", "Color.YELLOW")
                replaceAll(CODE, "Color.color(1.0, 1.0, 0.0, 1)", "Color.YELLOW")
                replaceAll(CODE, "Color.color(1.0, 1.0, 0.0, 1.0)", "Color.YELLOW")
                replaceAll(CODE, "Color.color(0, 1, 1, 1)", "Color.CYAN")
                replaceAll(CODE, "Color.color(0.0, 1.0, 1.0, 1)", "Color.CYAN")
                replaceAll(CODE, "Color.color(0.0, 1.0, 1.0, 1.0)", "Color.CYAN")
                replaceAll(CODE, "Color.color(1, 0, 1, 1)", "Color.MAGENTA")
                replaceAll(CODE, "Color.color(1.0, 0.0, 1.0, 1)", "Color.MAGENTA")
                replaceAll(CODE, "Color.color(1.0, 0.0, 1.0, 1.0)", "Color.MAGENTA")
                break
        }
        replaceAll(CODE, "00000000000001", "")
        // replace shape name prefixes like E_ and RRn_m_
        replaceAll(CODE, "_E_", "_")
        final Pattern PATTERN = Pattern.compile(/_?RR[0-9]+_([0-9]+_)?/)
        replaceAll(CODE, PATTERN, '_')

        return CODE.toString()
    }


    // ******************** REPLACEMENT METHODS *******************************
    private static void replaceAll(final StringBuilder TEXT, final String SEARCH, final String REPLACE) {
        int index = TEXT.indexOf(SEARCH)
        while (index != -1) {
            TEXT.replace(index, index + SEARCH.length(), REPLACE)
            index += REPLACE.length()
            index = TEXT.indexOf(SEARCH, index)
        }
    }

    private static void replaceAll(final StringBuilder TEXT, final Pattern PATTERN, final String REPLACE) {
        final Matcher MATCHER = PATTERN.matcher(TEXT)
        while (MATCHER.find()) {
            TEXT.replace(MATCHER.start(), MATCHER.end(), REPLACE)
            MATCHER.reset()
        }
    }

    private static void appendBlanks(final StringBuilder TEXT, final int NO_TO_APPEND) {
        for (int i = 0 ; i < NO_TO_APPEND ; i++) {
            TEXT.append(" ")
        }
    }

    // ******************** TRANSLATION EVENT LISTENER ************************
    public void addTranslationListener(TranslationListener listener) {
        eventListenerList.add(TranslationListener.class, listener)
    }

    public void removeTranslationListener(TranslationListener listener) {
        eventListenerList.remove(TranslationListener.class, listener)
    }

    protected void fireTranslationEvent(TranslationEvent event) {
        Object[] listeners = eventListenerList.getListenerList()
    int max = listeners.length
    for (int i = 0 ; i < max ; i++) {
      if ( listeners[i] == TranslationListener.class ) {
        ((TranslationListener) listeners[i + 1]).translationEventPerformed(event)
      }
    }
  }
}
