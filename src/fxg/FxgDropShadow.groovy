package fxg

import java.awt.Color

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 13:40
 * To change this template use File | Settings | File Templates.
 */
class FxgDropShadow extends FxgFilter{
    FxgFilterType type = FxgFilterType.DROP_SHADOW
    boolean inner
    int angle
    int distance
    double alpha
    double blurX
    double blurY
    Color color
}
