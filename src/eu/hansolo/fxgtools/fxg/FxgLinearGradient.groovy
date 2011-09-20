package eu.hansolo.fxgtools.fxg

import java.awt.LinearGradientPaint
import java.awt.Color
import java.awt.geom.Point2D

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:03
 * To change this template use File | Settings | File Templates.
 */
class FxgLinearGradient extends FxgFill {
    FxgFillType type = FxgFillType.LINEAR_GRADIENT
    Point2D start
    Point2D stop
    float[] fractions
    Color[] colors

    LinearGradientPaint getLinearGradient() {
        return new LinearGradientPaint(start, stop, fractions, colors)
    }
}
