package eu.hansolo.fxgtools.main

import static javax.swing.WindowConstants.EXIT_ON_CLOSE

import javax.swing.JFrame
import javax.swing.JPanel
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import eu.hansolo.fxgtools.fxg.FxgElement
import eu.hansolo.fxgtools.fxg.Language



fxgFile = '/Volumes/Macintosh HD/Users/hansolo/Desktop/InSync/Java Apps/FXG Converter/fxg files/ForGerrit.fxg'
fxg = new XmlParser().parse(new File(fxgFile))
selectedLayer = 'Layer_1'
keepAspect = true
width = 640
height = 640

parser = new FxgParser()
Map<String, BufferedImage> allLayerImages = parser.parse(fxgFile, width, height, keepAspect)

//Map<String, List<FxgElement>> layerMap = parser.getElements(fxg)
//translator = new FxgTranslator()

//translator.translate("Test.java", layerMap, Language.JAVA, String.valueOf((int)parser.originalWidth), String.valueOf((int)parser.originalHeight), true)

String replaced = "FOREGROUND_1_RR6_0_INDICATOR_FRAME_2_2.setStroke(null);"
//def matcher = (replaced =~ /_?RR[0-9]+_([0-9]+_)?/)
//replaced = matcher.replaceAll("_")
replaced = replaced.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
System.out.println replaced


JFrame frame = new JFrame();
frame.setTitle("Groovy FXG-Parser")
frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
JPanel panel = new JPanel() {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g)
        Graphics2D g2 = (Graphics2D) g.create()

        for (BufferedImage image : allLayerImages.values()) {
            g2.drawImage(image, 0, 0, null)
        }

        //g2.drawImage(singleLayerImage, 0, 0, null)
        g2.dispose()
    }
}

frame.add(panel)
frame.setSize(width, height + 22)
frame.setLocationRelativeTo(null)
frame.setVisible(true)

