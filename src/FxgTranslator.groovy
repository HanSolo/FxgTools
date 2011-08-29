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
        final String USER_HOME = System.getProperties().getProperty("user.home")
        final String EXPORT_FILE_NAME = USER_HOME + File.separator + "Desktop" + File.separator + FILE_NAME

        StringBuilder codeToExport = new StringBuilder();

        // Export the header of the language specific template
        switch(TO_LANGUAGE) {
            case Language.JAVA:     codeToExport.append(javaTemplateHeader())
                                    break;
            case Language.JAVAFX:   codeToExport.append(javaFxTemplateHeader())
                                    break;
            case Language.GWT:      codeToExport.append(gwtTemplateHeader())
                                    break;
            case Language.CANVAS:   codeToExport.append(canvasTemplateHeader())
                                    break;
            default: throw new Exception("Language not supported...")
        }

        // Export the language specific graphics code
        codeToExport.append(code(layerMap, TO_LANGUAGE))

        // Export the rest of the language specific template
        switch(TO_LANGUAGE) {
            case Language.JAVA:     codeToExport.append(javaTemplateFooter())
                                    break;
            case Language.JAVAFX:   codeToExport.append(javaFxTemplateFooter())
                                    break;
            case Language.GWT:      codeToExport.append(gwtTemplateFooter())
                                    break;
            case Language.CANVAS:   codeToExport.append(canvasTemplateFooter())
                                    break;
        }

        writeToFile(EXPORT_FILE_NAME, codeToExport.toString())
    }

    // JAVA
    private String javaTemplateHeader() {
        return "JavaHeader\n"
    }

    private String javaTemplateFooter() {
        return "JavaFooter\n"
    }

    // JAVAFX
    private String javaFxTemplateHeader() {
        return "JavaFxHeader\n"
    }

    private String javaFxTemplateFooter() {
        return "JavaFxFooter\n"
    }

    // GWT
    private String gwtTemplateHeader() {
        return "GwtHeader\n"
    }

    private String gwtTemplateFooter() {
        return "GwtFooter\n"
    }

    // CANVAS
    private String canvasTemplateHeader() {
        return "CanvasHeader\n"
    }

    private String canvasTemplateFooter() {
        return "CanvasFooter\n"
    }

    // CODE
    private String code(Map<String, List<FxgElement>> layerMap, final Language TO_LANGUAGE) {
        StringBuilder code = new StringBuilder()
        for (String layer : layerMap.keySet()) {
            for (FxgElement element : layerMap[layer]) {
                code.append(element.shape.translateTo(TO_LANGUAGE))
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
