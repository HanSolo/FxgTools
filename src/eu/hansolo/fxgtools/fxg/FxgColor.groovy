package eu.hansolo.fxgtools.fxg

import java.awt.Color

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:02
 * To change this template use File | Settings | File Templates.
 */
class FxgColor extends FxgFill {
    FxgFillType type     = FxgFillType.SOLID_COLOR
    String      hexColor
    float       alpha
    Color       color
}
