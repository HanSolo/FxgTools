package fxg

import java.awt.Stroke
import java.awt.Color

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 09:20
 * To change this template use File | Settings | File Templates.
 */
class FxgStroke {
    String name
    Color color
    Stroke stroke

    String getHexColor() {
        return Integer.toHexString((int) (color).getRGB() & 0x00ffffff)
    }
}
