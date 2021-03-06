package eu.hansolo.fxgtools.main

import eu.hansolo.fxgtools.fxg.FxgElement
import eu.hansolo.fxgtools.fxg.FxgVariable
import eu.hansolo.fxgtools.fxg.Language

import java.util.regex.Matcher
import java.util.regex.Pattern
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
    private static final Pattern CANVAS_PATTERN = Pattern.compile("_?(canvas)", Pattern.CASE_INSENSITIVE)
    private StringBuilder     allLayers         = new StringBuilder()
    private StringBuilder     allElements       = new StringBuilder()
    private int               splitCounter      = 0
    private int               nextSplit         = 70000
    private int               splitNumber       = 0
    private String            packageInfo       = "eu.hansolo.fx"
    private List<String>      layerSelection    = []
    private HashSet<String>   nameSet           = []
    private HashSet<String>   groupNameSet      = []
    private HashSet<String>   cssNameSet        = []


    // ******************** Translate given elements to given language ********
    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE) {
        return translate(FILE_NAME, layerMap, LANGUAGE, WIDTH, HEIGHT, EXPORT_TO_FILE, COMPONENT_TYPE.JCOMPONENT, "", new HashMap<String, FxgVariable>())
    }

    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE, final HashMap<String, FxgVariable> PROPERTIES) {
        return translate(FILE_NAME, layerMap, LANGUAGE, WIDTH, HEIGHT, EXPORT_TO_FILE, COMPONENT_TYPE.JCOMPONENT, "", PROPERTIES)
    }

    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE, final COMPONENT_TYPE TYPE, final String NAME_PREFIX, final HashMap<String, FxgVariable> PROPERTIES) {
        fireTranslationEvent(new TranslationEvent(this, TranslationState.RUNNING))
        final String CLASS_NAME = (FILE_NAME.contains(".") ? (FILE_NAME.substring(0, FILE_NAME.lastIndexOf('.')) + NAME_PREFIX) : (FILE_NAME + NAME_PREFIX)).capitalize()
        final String USER_HOME = System.properties.getProperty('user.home')
        StringBuilder desktopPath = new StringBuilder(USER_HOME).append(File.separator).append('Desktop').append(File.separator)
        StringBuilder exportFileName = new StringBuilder(desktopPath).append(CLASS_NAME)
        if (layerSelection.isEmpty()) {
            layerSelection.addAll(layerMap.keySet())
        }

        splitCounter = 0
        nextSplit = 70000

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
                    writeToFile(desktopPath.append('Demo.java').toString(), javaFxDemoTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                    writeToFile(path + ("${CLASS_NAME}.java").toString(), javaFxControlTemplate(CLASS_NAME, Double.parseDouble(WIDTH), Double.parseDouble(HEIGHT), PROPERTIES))
                    writeToFile(path + ("${CLASS_NAME}Behavior.java").toString(), javaFxBehaviorTemplate(CLASS_NAME))
                    writeToFile(path + ("${CLASS_NAME.toLowerCase()}.css").toString(), makeCssNicer(javaFxCssTemplate(CLASS_NAME, layerMap, PROPERTIES)))
                    writeToFile(path + ("${CLASS_NAME}Builder.java").toString(), javaFxBuilderTemplate(CLASS_NAME, PROPERTIES))
                }
                codeToExport.append(javaFxSkinTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE, PROPERTIES))
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
        String result = createCode(layerMap, LANGUAGE)
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

    private String createVarName(final String LAYER_NAME) {
        String varName
        if (LAYER_NAME.contains("_")) {
            String[] varNameParts = LAYER_NAME.toLowerCase().split("_")
            StringBuilder output = new StringBuilder()
            varNameParts.each {output.append(it.capitalize())}
            varName = output.substring(0,1).toLowerCase() + output.substring(1)
        } else if (LAYER_NAME == LAYER_NAME.toUpperCase()) {
            varName = LAYER_NAME.toLowerCase()
        } else if (LAYER_NAME == LAYER_NAME.capitalize()) {
            varName = "${LAYER_NAME.charAt(0).toLowerCase()}${LAYER_NAME.substring(1)}"
        } else {
            varName = LAYER_NAME
        }
        return varName
    }


    // ******************** JAVA **********************************************
    private String javaTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final COMPONENT_TYPE TYPE, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/java.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        StringBuilder imageDeclaration = new StringBuilder()
        StringBuilder imageInitialization = new StringBuilder()
        StringBuilder imageCreation = new StringBuilder()
        StringBuilder drawImage = new StringBuilder()

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                String varName = createVarName(layerName)
                imageDeclaration.append("    private BufferedImage         ${varName}Image;\n")
                imageInitialization.append("        ${varName}Image = createImage(INNER_BOUNDS.width, INNER_BOUNDS.height, Transparency.TRANSLUCENT);\n")
                imageCreation.append("        if (${varName}Image != null) {\n")
                imageCreation.append("            ${varName}Image.flush();\n")
                imageCreation.append("        }\n")
                imageCreation.append("        ${varName}Image = create${layerName}Image(WIDTH, HEIGHT);\n")
                drawImage.append("        G2.drawImage(${varName}Image, 0, 0, null);\n")
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

        replaceAll(codeToExport, "\$creationMethods", createCode(layerMap, LANGUAGE))

        return codeToExport.toString()
    }

    private String javaLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    public BufferedImage create${LAYER_NAME}Image(final int WIDTH, final int HEIGHT) {\n")
        layerCode.append("        if (WIDTH <= 0 || HEIGHT <= 0) {\n")
        layerCode.append("            return createImage(1, 1, Transparency.TRANSLUCENT);\n")
        layerCode.append("        }\n")
        layerCode.append("        final BufferedImage IMAGE = createImage(WIDTH, HEIGHT, Transparency.TRANSLUCENT);\n")
        layerCode.append("        final Graphics2D    G2    = IMAGE.createGraphics();\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);\n")
        layerCode.append("        G2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);\n")
        layerCode.append("\n")
        layerCode.append("        final int IMAGE_WIDTH  = IMAGE.getWidth();\n")
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
            code.append("        addSplit${layerName}${splitNumber}(G2, IMAGE_WIDTH, IMAGE_HEIGHT);\n\n")
            code.append("        G2.dispose();\n\n")
            code.append("        return IMAGE;\n")
            code.append("    }\n\n")
            code.append("    private void addSplit${layerName}${splitNumber}(final Graphics2D G2, final int IMAGE_WIDTH, final int IMAGE_HEIGHT) {\n")
        } else {
            code.append("        addSplit${layerName}${splitNumber}(G2, IMAGE_WIDTH, IMAGE_HEIGHT);\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit${layerName}${splitNumber}(final Graphics2D G2, final int IMAGE_WIDTH, final int IMAGE_HEIGHT) {\n")
        }
    }

    private String javaPropertyDeclaration(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            PROPERTY_CODE.append("    public static final String    ").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY = ").append("\"").append(PROPERTY_NAME.toUpperCase()).append("\";\n")
        }
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("    private double                ").append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("    private boolean               ").append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("    private int                   ").append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("    private long                  ").append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("    private String                ").append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("    private Object                ").append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("color")) {
                PROPERTY_CODE.append("    private Color                 ").append(PROPERTY_NAME).append(";\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaPropertyInitialization(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ${PROPERTIES.get(PROPERTY_NAME).defaultValue};\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ${PROPERTIES.get(PROPERTY_NAME).defaultValue};\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ${PROPERTIES.get(PROPERTY_NAME).defaultValue};\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ${PROPERTIES.get(PROPERTY_NAME).defaultValue};\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ${PROPERTIES.get(PROPERTY_NAME).defaultValue};\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ${PROPERTIES.get(PROPERTY_NAME).defaultValue};\n")
            } else if (TYPE.equals("color")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = new Color(0x${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaPropertyGetterSetter(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("    ").append("public final double get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final double ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        double old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.firePropertyChange(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("    ").append("public final boolean is").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final boolean ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        boolean old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.firePropertyChange(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("    ").append("public final int get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final int ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        int old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.firePropertyChange(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("    ").append("public final long get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final long ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        long old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.firePropertyChange(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("    ").append("public final String get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final String ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        String old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.firePropertyChange(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("    ").append("public final Object get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final Object ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        Object old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.firePropertyChange(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("color")) {
                PROPERTY_CODE.append("    ").append("public final Color get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final Color ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        Color old").append(PROPERTY_NAME.capitalize()).append(" = ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(" = ").append(PROPERTY_NAME.toUpperCase()).append(";\n")
                PROPERTY_CODE.append("        ").append("propertySupport.firePropertyChange(").append(PROPERTY_NAME.toUpperCase()).append("_PROPERTY, old").append(PROPERTY_NAME.capitalize()).append(", ").append(PROPERTY_NAME).append(");\n")
                PROPERTY_CODE.append("        //repaint(INNER_BOUNDS);\n")
                PROPERTY_CODE.append("    }\n\n")
            }
        }
        return PROPERTY_CODE.toString()
    }


    // ******************** JAVA FX *******************************************
    private String javaFxSkinTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_skin.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        StringBuilder groupDeclaration    = new StringBuilder()
        StringBuilder groupInitialization = new StringBuilder()
        StringBuilder groupUpdate         = new StringBuilder()

        int maxLength = 11
        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                maxLength = Math.max(createVarName(layerName).length(), maxLength)
            }
        }

        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties") && !layerName.toLowerCase().endsWith("canvas")) {
                String varName = createVarName(layerName)
                groupDeclaration.append("    private Group        ${varName};\n")
                groupInitialization.append("        ${varName}")
                appendBlanks(groupInitialization, (maxLength - varName.length()))
                groupInitialization.append(" = new Group();\n")
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
        replaceAll(codeToExport, "\$registerListeners", javaFxRegisterListeners(PROPERTIES))
        replaceAll(codeToExport, "\$handlePropertyChanges", javaFxHandlePropertyChanges(PROPERTIES))
        replaceAll(codeToExport, "\$drawingCode", createCode(layerMap, LANGUAGE))
        replaceAll(allLayers, "_Canvas", "")
        if (allLayers.length() > 31) {
            allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        }
        replaceAll(codeToExport, "\$layerList", allLayers.toString())

        return codeToExport.toString()
    }

    private String javaFxControlTemplate(final String CLASS_NAME, final double WIDTH, final double HEIGHT, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_control.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$propertyDeclaration", javaFxPropertyDeclaration(PROPERTIES))
        replaceAll(codeToExport, "\$propertyInitialization", javaFxPropertyInitialization(PROPERTIES))
        replaceAll(codeToExport, "\$propertyGetterSetter", javaFxPropertyGetterSetter(PROPERTIES))
        replaceAll(codeToExport, "\$prefSizeCalculation", javaFxPrefSizeCalculation(WIDTH, HEIGHT))
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$styleClass", CLASS_NAME.toLowerCase())
        return codeToExport.toString()
    }

    private String javaFxBuilderTemplate(final String CLASS_NAME, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_builder.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$propertySetter", javaFxPropertySetter(CLASS_NAME, PROPERTIES))
        replaceAll(codeToExport, "\$buildMethod", javaFxBuildMethod(CLASS_NAME, PROPERTIES))
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        return codeToExport.toString()
    }

    private StringBuilder javaFxCssTemplate(final String CLASS_NAME, Map<String, List<FxgElement>> layerMap, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_css.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", packageInfo)
        replaceAll(codeToExport, "\$styleClass", CLASS_NAME.toLowerCase())
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$fillAndStrokeDefinitions", cssCode(layerMap))
        replaceAll(codeToExport, "\$colorDefinitions", cssColors(PROPERTIES))
        return codeToExport
    }

    private String javaFxBehaviorTemplate(final String CLASS_NAME) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_behavior.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        return codeToExport.toString()
    }

    private String javaFxDemoTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_demo.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)
        return codeToExport.toString()
    }

    private String javaFxLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        //String lowerLayerName = createVarName(LAYER_NAME)
        layerCode.append("\n")
        layerCode.append("    public final void draw${LAYER_NAME.capitalize()}() {\n")
        layerCode.append("        final double SIZE   = control.getPrefWidth() < control.getPrefHeight() ? control.getPrefWidth() : control.getPrefHeight();\n")
        layerCode.append("        final double WIDTH  = square ? SIZE : control.getPrefWidth();\n")
        layerCode.append("        final double HEIGHT = square ? SIZE : control.getPrefHeight();\n\n")
        layerCode.append("        final Shape IBOUNDS = new Rectangle(0, 0, WIDTH, HEIGHT);\n")
        layerCode.append("        IBOUNDS.setOpacity(0.0);\n")
        return layerCode.toString()
    }

    private String javaFxLayerMethodStop(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private String javaFxCanvasLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        String layerName = LAYER_NAME.replaceAll(CANVAS_PATTERN, "")
        layerCode.append("\n")
        layerCode.append("    public final Canvas draw${layerName.capitalize()}(final double WIDTH, final double HEIGHT) {\n")
        layerCode.append("        final Canvas          CANVAS = new Canvas(WIDTH, HEIGHT);\n")
        layerCode.append("        final GraphicsContext CTX    = CANVAS.getGraphicsContext2D();\n");
        return layerCode.toString()
    }

    private String javaFxCanvasLayerMethodStop() {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("\n")
        layerCode.append("        return CANVAS;\n")
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private void javaFxSplitLayer(String layerName, int splitNumber, StringBuilder code, StringBuilder allElements) {
        String varName = createVarName(layerName)
        if (splitNumber == 1) {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${varName}.getChildren().setAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        continue${layerName}${splitNumber}(${varName}, WIDTH, HEIGHT);\n\n")
            //createCode.append("        return ${varName};\n")
            code.append("    }\n\n")
            code.append("    private void continue${layerName}${splitNumber}(Group ${varName}, final double WIDTH, final double HEIGHT) {\n")
        } else {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${varName}.getChildren().setAll(")
            code.append(allElements.toString())
            code.append(");\n\n")
            allElements.length = 0

            code.append("        continue${layerName}${splitNumber}(${varName}, WIDTH, HEIGHT);\n\n")
            code.append("    }\n\n")
            code.append("    private void continue${layerName}${splitNumber}(Group ${varName}, final sdouble WIDTH, final double HEIGHT) {\n")
        }
    }

    private String javaFxPropertyDeclaration(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        int maxLength = -1
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (!TYPE.equals("double") && !TYPE.equals("boolean") && !TYPE.equals("int") && !TYPE.equals("long") &&
                !TYPE.equals("string") && !TYPE.equals("object")) {
                maxLength = Math.max(TYPE.length(), maxLength)
            }
        }

        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("    private DoubleProperty ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("    private BooleanProperty")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("    private IntegerProperty")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("    private LongProperty   ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("    private StringProperty ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("    private ObjectProperty ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else {
                PROPERTY_CODE.append("    private ObjectProperty<${PROPERTIES.get(PROPERTY_NAME).type}> ").append(PROPERTY_NAME).append(";\n")
            }
        }
        PROPERTY_CODE.append("    private boolean        ")
        appendBlanks(PROPERTY_CODE, (maxLength + 2))
        PROPERTY_CODE.append("square;\n")
        PROPERTY_CODE.append("    private boolean        ")
        appendBlanks(PROPERTY_CODE, (maxLength + 2))
        PROPERTY_CODE.append("keepAspect;\n")
        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertyInitialization(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        int maxLength = 10
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            maxLength = Math.max(PROPERTY_NAME.length(), maxLength)
        }
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleDoubleProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleBooleanProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleIntegerProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleLongProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleStringProperty(\"${PROPERTIES.get(PROPERTY_NAME).defaultValue}\");\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleObjectProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                String defaultValue
                if (TYPE.equals("color")) {
                    defaultValue = "Color.web(\"#${PROPERTIES.get(PROPERTY_NAME).defaultValue}\")"
                } else {
                    defaultValue = PROPERTIES.get(PROPERTY_NAME).defaultValue
                }
                PROPERTY_CODE.append(" = new SimpleObjectProperty<${PROPERTIES.get(PROPERTY_NAME).type}>(${defaultValue});\n")
            }
        }
        PROPERTY_CODE.append("        square")
        int spacer = maxLength == 0 ? 0 : 6;
        appendBlanks(PROPERTY_CODE, (maxLength - spacer))
        PROPERTY_CODE.append(" = false;\n")

        PROPERTY_CODE.append("        keepAspect")
        spacer = maxLength == 0 ? 0 : 10;
        appendBlanks(PROPERTY_CODE, (maxLength - spacer))
        PROPERTY_CODE.append(" = false;\n")

        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertyGetterSetter(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("    ").append("public final double get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final double ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final DoubleProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("    ").append("public final boolean is").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final boolean ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final BooleanProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("    ").append("public final int get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final int ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final IntegerProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("    ").append("public final long get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final long ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final LongProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("    ").append("public final String get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final String ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final StringProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("    ").append("public final Object get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final Object ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final ObjectProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            } else {
                final String ORIGINAL_TYPE = PROPERTIES.get(PROPERTY_NAME).type
                PROPERTY_CODE.append("    ").append("public final ${ORIGINAL_TYPE} get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final ${ORIGINAL_TYPE} ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final ObjectProperty<${ORIGINAL_TYPE}> ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertySetter(final String CLASS_NAME, final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final double ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleDoubleProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final boolean ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleBooleanProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final int ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleIntegerProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final long ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleLongProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final String ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleStringProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final Object ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleObjectProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else {
                final String ORIGINAL_TYPE = PROPERTIES.get(PROPERTY_NAME).type
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final ${ORIGINAL_TYPE} ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleObjectProperty<${ORIGINAL_TYPE}>(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaFxBuildMethod(final String CLASS_NAME, final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder BUILD_CODE = new StringBuilder()
        BUILD_CODE.append("        for (String key : properties.keySet()) {\n")
        boolean first = true
        if (PROPERTIES.keySet().isEmpty()) {
            BUILD_CODE.append("            if (\"\".equals(key)) {\n\n")
        } else {
            PROPERTIES.keySet().each{String PROPERTY_NAME->
                final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
                if (first) {
                    BUILD_CODE.append("            if (")
                } else {
                    BUILD_CODE.append("            } else if(")
                }
                BUILD_CODE.append("\"").append(PROPERTY_NAME).append("\".equals(key)) {\n")
                if (TYPE.equals("double")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((DoubleProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("boolean")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((BooleanProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("int")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((IntegerProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("long")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((LongProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("string")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((StringProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("object")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((ObjectProperty) properties.get(key)).get());\n")
                } else {
                    final String ORIGINAL_TYPE = PROPERTIES.get(PROPERTY_NAME).type
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((ObjectProperty<${ORIGINAL_TYPE}>) properties.get(key)).get());\n")
                }
                first = false
            }
        }
        BUILD_CODE.append("            } else if(\"prefWidth\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setPrefWidth(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            } else if(\"prefHeight\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setPrefHeight(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            } else if (\"layoutX\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setLayoutX(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            } else if (\"layoutY\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setLayoutY(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            }\n")
        BUILD_CODE.append("        }\n")
        return BUILD_CODE.toString()
    }

    private String javaFxPrefSizeCalculation(final double WIDTH, final double HEIGHT) {
        StringBuilder PREF_SIZE_CODE = new StringBuilder()
        PREF_SIZE_CODE.append("        double prefHeight = WIDTH < (HEIGHT * ${WIDTH / HEIGHT}) ? (WIDTH * ${HEIGHT / WIDTH}) : HEIGHT;\n")
        PREF_SIZE_CODE.append("        double prefWidth = prefHeight * ${WIDTH / HEIGHT};\n")
        return PREF_SIZE_CODE.toString()
    }

    private String javaFxRegisterListeners(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTY_CODE.append("        // Register listeners\n")
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            PROPERTY_CODE.append("        ").append("registerChangeListener(control.")
            PROPERTY_CODE.append("${PROPERTY_NAME}Property(), \"${PROPERTY_NAME.toUpperCase()}\");\n")
        }
        return PROPERTY_CODE.toString()
    }

    private String javaFxHandlePropertyChanges(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().eachWithIndex{String PROPERTY_NAME, int index->
            /*
            if (index == 0) {
                PROPERTY_CODE.append("        if (").append("\"${PROPERTY_NAME.toUpperCase()}\".equals(PROPERTY)) {\n")
                PROPERTY_CODE.append("            // React to ${PROPERTY_NAME} property change here\n")
                PROPERTY_CODE.append("        }")
                if (PROPERTIES.size() == 1) {
                    PROPERTY_CODE.append(";")
                }
            } else {
                PROPERTY_CODE.append(" else if (").append("\"${PROPERTY_NAME.toUpperCase()}\".equals(PROPERTY)) {\n")
                PROPERTY_CODE.append("            // React to ${PROPERTY_NAME} property change here\n")
                PROPERTY_CODE.append("        }")
                if (index == PROPERTIES.size()) {
                    PROPERTY_CODE.append(";")
                }
            }
            */

            if (index > 0) {
                PROPERTY_CODE.append(" else if (").append("\"${PROPERTY_NAME.toUpperCase()}\".equals(PROPERTY)) {\n")
                PROPERTY_CODE.append("            // React to ${PROPERTY_NAME} property change here\n")
                PROPERTY_CODE.append("        }")
                if (index == PROPERTIES.size()) {
                    PROPERTY_CODE.append(";")
                }
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
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")){
                drawImagesToContext.append("        draw${layerName}Image(context, CANVAS_WIDTH, CANVAS_HEIGHT);\n\n")
            }
        }

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$originalWidth", WIDTH)
        replaceAll(codeToExport, "\$originalHeight", HEIGHT)
        replaceAll(codeToExport, "\$drawImagesToContext", drawImagesToContext.toString())
        replaceAll(codeToExport, "\$creationMethods", createCode(layerMap, LANGUAGE))

        return codeToExport.toString()
    }

    private String gwtLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    public void draw${LAYER_NAME}Image(Context2d ctx, int imageWidth, int imageHeight) {\n")
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
            code.append("        addSplit${layerName}${splitNumber}(ctx, imageWidth, imageHeight);\n\n")
            code.append("        ctx.restore();\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit${layerName}${splitNumber}(Context2d ctx, int imageWidth, int imageHeight) {\n")
        } else {
            code.append("        addSplit${layerName}${splitNumber}(ctx, imageWidth, imageHeight);\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit${layerName}${splitNumber}(Context2d ctx, int imageWidth, int imageHeight) {\n")
        }
    }


    // ******************** CANVAS ********************************************
    private String canvasTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/canvas.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        StringBuilder createBuffers = new StringBuilder()
        StringBuilder drawImagesToBuffer = new StringBuilder()
        StringBuilder drawImagesToCanvas = new StringBuilder()
        StringBuilder clearBuffers = new StringBuilder()
        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                createBuffers.append("    var ${layerName}Buffer = document.createElement('canvas');\n")
                createBuffers.append("    ${layerName}Buffer.width = imageWidth;\n")
                createBuffers.append("    ${layerName}Buffer.height = imageHeight;\n")
                createBuffers.append("    var ${layerName}Ctx = ${layerName}Buffer.getContext('2d');\n")
                drawImagesToBuffer.append("        draw${layerName}Image(${layerName}Ctx);\n")
                drawImagesToCanvas.append("        mainCtx.drawImage(${layerName}Buffer, 0, 0);\n")
                clearBuffers.append("        ${layerName}Ctx.clearRect(0, 0, this.width, this.height);\n")
            }
        }

        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$createBuffers", createBuffers.toString())
        replaceAll(codeToExport, "\$drawImagesToBuffer", drawImagesToBuffer.toString())
        replaceAll(codeToExport, "\$drawImagesToCanvas", drawImagesToCanvas.toString())
        replaceAll(codeToExport, "\$creationMethods", createCode(layerMap, LANGUAGE))
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)
        replaceAll(codeToExport, "\$clearBuffers", clearBuffers.toString())

        return codeToExport.toString()
    }

    private String canvasLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    var draw${LAYER_NAME}Image = function(ctx) {\n")
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
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                String varName = createVarName(layerName)
                groupDeclaration.append("    private Group ${varName}\n")
                groupInitialization.append("        ${varName} = create${layerName}Layer(imageWidth, imageHeight)\n")
            }
        }

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$groupDeclaration", groupDeclaration.toString())
        replaceAll(codeToExport, "\$groupInitialization", groupInitialization.toString())
        replaceAll(codeToExport, "\$drawingCode", createCode(layerMap, LANGUAGE))
        if (allLayers.length() > 31) {
            allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        }
        replaceAll(codeToExport, "\$layerList", allLayers.toString())

        return codeToExport.toString()
    }

    private String groovyFxLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("\n")
        layerCode.append("    public final Group create${LAYER_NAME}Layer(imageWidth, imageHeight) {\n")
        layerCode.append("        def ${LAYER_NAME.charAt(0).toLowerCase()}${LAYER_NAME.substring(1)} = new Group()\n")
        return layerCode.toString()
    }

    private String groovyFxLayerMethodStop(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private String groovyFxTestTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/groovyfx_demo.txt')
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

            code.append("        addSplit${layerName}${splitNumber}(${varName}, imageWidth, imageHeight)\n\n")
            code.append("        return ${varName};\n")
            code.append("    }\n\n")
            code.append("    private void addSplit${layerName}${splitNumber}(def ${varName}, imageWidth, imageHeight) {\n")
        } else {
            if (allElements.length() > layerName.length() + 32) {
                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
            }
            code.append("        ${varName}.children.addAll(")
            code.append(allElements.toString())
            code.append(")\n\n")
            allElements.length = 0

            code.append("        addSplit${layerName}${splitNumber}(${varName}, imageWidth, imageHeight)\n\n")
            code.append("    }\n\n")
            code.append("    private void addSplit${layerName}${splitNumber}(def ${varName}, imageWidth, imageHeight) {\n")
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
           if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
               imageDeclaration.append("    private Bitmap ${layerName}Image;\n")
               //imageInitialization.append("        ${layerName}Image = Bitmap.createBitmap(${WIDTH}, ${HEIGHT}, Bitmap.Config.ARGB_8888);\n")
               resizeImagesSquare.append("            ${layerName}Image = create${layerName}Image(size, size);\n")
               resizeImages.append("            ${layerName}Image = create${layerName}Image(width, height);\n")
               imageCreation.append("        ${layerName}Image = create${layerName}Image(imageWidth, imageHeight);\n")
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
        replaceAll(codeToExport, "\$creationMethods", createCode(layerMap, LANGUAGE))

       return codeToExport.toString()
    }

    private String androidLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    public Bitmap create${LAYER_NAME}Image(int imageWidth, int imageHeight) {\n")
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
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/android_demo.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)

        return codeToExport
    }

    private void androidSplitLayer(String layerName, int splitNumber, StringBuilder code) {
           if (splitNumber == 1) {
               code.append("        addSplit${layerName}${splitNumber}(canvas, paint, stroke,  imageWidth, imageHeight);\n\n")
               code.append("        return image;\n")
               code.append("    }\n\n")
               code.append("    private void addSplit${layerName}${splitNumber}(Canvas canvas, Paint paint, Paint stroke, int imageWidth, int imageHeight) {\n")
           } else {
               code.append("        addSplit${layerName}${splitNumber}(canvas, paint, stroke, imageWidth, imageHeight);\n\n")
               code.append("    }\n\n")
               code.append("    private void addSplit${layerName}${splitNumber}(Canvas canvas, Paint paint, Paint stroke, int imageWidth, int imageHeight) {\n")
           }
       }


    // ******************** CREATE CODE ***************************************
    private String createCode(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        allLayers.length = 0
        allElements.length = 0
        String varName
        layerMap.keySet().each {String layerName->
            groupNameSet.clear()
            nameSet.clear();
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                splitNumber = 0
                int shapeIndex = 0
                varName = createVarName(layerName)
                // add language dependend method heads
                switch(LANGUAGE) {
                    case Language.JAVA         : code.append(javaLayerMethodStart(layerName))
                        break
                    case Language.JAVAFX       :
                        if (layerName.toLowerCase().endsWith("canvas")) {
                            code.append(javaFxCanvasLayerMethodStart(layerName))
                        } else {
                            code.append(javaFxLayerMethodStart(layerName))
                        }
                        break
                    case Language.GWT          : code.append(gwtLayerMethodStart(layerName))
                        break
                    case Language.CANVAS       : code.append(canvasLayerMethodStart(layerName))
                        break
                    case Language.ANDROID      : code.append(androidLayerMethodStart(layerName))
                        break
                }

                // main translation routine
                layerMap[layerName].each {FxgElement element ->
                    shapeIndex += 1
                    if (layerName.toLowerCase().endsWith("canvas")) {
                        code.append(element.shape.translateTo(Language.JAVAFX_CANVAS, shapeIndex, nameSet))
                    } else {
                        code.append(element.shape.translateTo(LANGUAGE, shapeIndex, nameSet))
                        if (LANGUAGE == Language.JAVAFX || LANGUAGE == LANGUAGE.GROOVYFX){
                            String name = element.shape.shapeName.toUpperCase()
                            name = name.startsWith("E_") ? name.replaceFirst("E_", "") : name
                            name = name.replaceAll("_?RR[0-9]+_([0-9]+_)?", "")
                            name = name.replace("_E_", "");
                            name = name.startsWith("_") ? name.replaceFirst("_", "") : name

                            if (groupNameSet.contains(name)) {
                                allElements.append("${layerName.toUpperCase()}${element.shape.shapeName.toUpperCase()}${shapeIndex}").append(",\n")
                            } else {
                                allElements.append("${name}").append(",\n")
                                groupNameSet.add(name)
                            }

                            for(int n = 0 ; n < layerName.length() + 30 ; n+=1) {
                                allElements.append(" ")
                            }
                        }
                    }

                    // split methods if they become too long
                    splitCounter = code.length()
                    if (splitCounter.compareTo(nextSplit) > 0) {
                        nextSplit = splitCounter + 70000
                        splitCounter = 0
                        splitNumber += 1

                        if (LANGUAGE == Language.JAVA) {
                            javaSplitLayer(layerName, splitNumber, code)
                        }
                        if (LANGUAGE == Language.JAVAFX && !layerName.toLowerCase().endsWith("canvas")) {
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
                        if (layerName.toLowerCase().endsWith("canvas")) {
                            code.append(javaFxCanvasLayerMethodStop())
                            allLayers.append("draw").append(layerName.capitalize()).append("(control.getPrefWidth(), control.getPrefHeight())").append(",\n                             ")
                        } else {
                            if (allElements.length() > layerName.length() + 32) {
                                allElements.replace(allElements.length() - (layerName.length() + 32), allElements.length(), "")
                            }
                            code.append("        ${varName}.getChildren().setAll(IBOUNDS,\n")
                            for(int n = 0 ; n < layerName.length() + 30 ; n+=1) {
                                code.append(" ")
                            }
                            code.append(allElements.toString())
                            code.append(");\n")
                            allElements.length = 0
                            code.append(javaFxLayerMethodStop(layerName))
                            allLayers.append(createVarName(layerName)).append(",\n                             ")
                        }
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
            cssNameSet.clear();
            if (layerSelection.contains(layerName) && !layerName.equalsIgnoreCase("properties")) {
                int shapeIndex = 0
                layerMap[layerName].each {FxgElement element ->
                    shapeIndex += 1

                    String cssName = "${element.shape.shapeName}"
                    cssName = cssName.replaceAll("E_", '')
                    cssName = cssName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '')
                    cssName = "${layerName.toLowerCase()}-${cssName.toLowerCase()}"

                    if (cssNameSet.contains(cssName)) {
                        cssName = "${layerName.toLowerCase()}-${element.shape.shapeName.toLowerCase()}-${shapeIndex}"
                    } else {
                        cssNameSet.add(cssName)
                    }
                    cssCode.append(element.shape.createCssFillAndStroke(cssName, element.shape.filled, element.shape.stroked))
                }
            }
        }
        return cssCode.toString()
    }

    private String cssColors(final HashMap<String, FxgVariable> PROPERTIES) {
        final StringBuilder CSS_COLORS = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).type.toLowerCase().equals("color")) {
                CSS_COLORS.append("    -fx-")
                          .append(PROPERTY_NAME.toLowerCase())
                          .append(": #")
                          .append(PROPERTIES.get(PROPERTY_NAME).defaultValue)
                          .append(";\n")
            }
        }
        return CSS_COLORS.toString()
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
        final Pattern ZERO_PATTERN = Pattern.compile(/(0{8}[0-9]*)/)
        replaceAll(CODE, ZERO_PATTERN, "0")
        // replace shape name prefixes like E_ and RRn_m_
        replaceAll(CODE, "_E_", "")
        final Pattern PATTERN = Pattern.compile(/_?RR[0-9]+_([0-9]+_)?/)
        replaceAll(CODE, PATTERN, "")

        return CODE.toString()
    }

    private String makeCssNicer(final StringBuilder CSS) {
        replaceAll(CSS, "rgba(255, 255, 255, 0)", "transparent")
        replaceAll(CSS, "rgba(255, 255, 255, 0.0)", "transparent")
        replaceAll(CSS, "rgba(0, 0, 0, 0)", "transparent")
        replaceAll(CSS, "rgba(0, 0, 0, 0.0)", "transparent")
        replaceAll(CSS, "rgba(0, 0, 0, 1)", "black")
        replaceAll(CSS, "rgba(0, 0, 0, 1.0)", "black")
        replaceAll(CSS, "rgb(0, 0, 0)", "black")
        replaceAll(CSS, "rgba(255, 255, 255, 1)", "white")
        replaceAll(CSS, "rgba(255, 255, 255, 1.0)", "white")
        replaceAll(CSS, "rgb(255, 255, 255)", "white")
        replaceAll(CSS, "rgba(255, 0, 0, 1)", "red")
        replaceAll(CSS, "rgba(255, 0, 0, 1.0)", "red")
        replaceAll(CSS, "rgb(255, 0, 0)", "red")
        replaceAll(CSS, "rgba(0, 255, 0, 1)", "lime")
        replaceAll(CSS, "rgba(0, 255, 0, 1.0)", "lime")
        replaceAll(CSS, "rgb(0, 255, 0)", "lime")
        replaceAll(CSS, "rgba(0, 0, 255, 1)", "blue")
        replaceAll(CSS, "rgba(0, 0, 255, 1.0)", "blue")
        replaceAll(CSS, "rgb(0, 0, 255)", "blue")
        replaceAll(CSS, "rgba(255, 255, 0, 1)", "yellow")
        replaceAll(CSS, "rgba(255, 255, 0, 1.0)", "yellow")
        replaceAll(CSS, "rgb(255, 255, 0)", "yellow")
        replaceAll(CSS, "rgba(0, 255, 255, 1)", "cyan")
        replaceAll(CSS, "rgba(0, 255, 255, 1.0)", "cyan")
        replaceAll(CSS, "rgb(0, 255, 255)", "cyan")
        replaceAll(CSS, "rgba(255, 0, 255, 1)", "magenta")
        replaceAll(CSS, "rgba(255, 0, 255, 1.0)", "magenta")
        replaceAll(CSS, "rgb(255, 0, 255)", "magenta")

        return CSS.toString()
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
