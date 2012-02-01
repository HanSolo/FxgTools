package eu.hansolo.fxgtools.fxg

import java.awt.RadialGradientPaint
import java.awt.Color
import java.awt.geom.Point2D

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:04
 * To change this template use File | Settings | File Templates.
 */
class FxgRadialGradient extends FxgFill {
    FxgFillType type      = FxgFillType.RADIAL_GRADIENT
    Point2D     center
    float       radius
    float[]     fractions
    Color[]     colors

    RadialGradientPaint getRadialGradient() {
        return new RadialGradientPaint(center, radius, fractions, colors)
    }
}
