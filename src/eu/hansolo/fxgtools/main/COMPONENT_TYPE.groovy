package eu.hansolo.fxgtools.main

/**
 * Author: hansolo
 * Date  : 19.09.11
 * Time  : 12:27
 */
enum COMPONENT_TYPE {
    JCOMPONENT("import javax.swing.JComponent;", "JComponent"),
    JPANEL("import javax.swing.JPanel;", "JPanel"),
    TOPCOMPONENT("import org.openide.windows.TopComponent;\nimport org.openide.util.NbBundle;\nimport org.netbeans.api.settings.ConvertAsProperties;\nimport org.openide.awt.ActionID;\nimport org.openide.awt.ActionReference;\n\n@TopComponent.Description(preferredID = \"\$className\", persistenceType = TopComponent.PERSISTENCE_ALWAYS)\n@TopComponent.Registration(mode = \"editor\", openAtStartup = true)\n@ActionID(category = \"Window\", id = \"\$className\")\n@ActionReference(path = \"Menu/Window\")\n@TopComponent.OpenActionRegistration(displayName = \"\$className\", preferredID = \"\$className\")", "TopComponent");

    String IMPORT_STATEMENT
    String CODE

    private COMPONENT_TYPE(final String IMPORT_STATEMENT, final String CODE) {
        this.IMPORT_STATEMENT = IMPORT_STATEMENT;
        this.CODE = CODE;
    }
}
