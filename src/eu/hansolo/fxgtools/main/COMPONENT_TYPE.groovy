package eu.hansolo.fxgtools.main

/**
 * Author: hansolo
 * Date  : 19.09.11
 * Time  : 12:27
 */
enum COMPONENT_TYPE {
    JCOMPONENT("import javax.swing.JComponent;", "JComponent"),
    JPANEL("import javax.swing.JPanel", "JPanel"),
    TOPCOMPONENT("import org.openide.windows.TopComponent;", "TopComponent");

    String IMPORT_STATEMENT
    String CODE

    private COMPONENT_TYPE(final String IMPORT_STATEMENT, final String CODE) {
        this.IMPORT_STATEMENT = IMPORT_STATEMENT;
        this.CODE = CODE;
    }
}
