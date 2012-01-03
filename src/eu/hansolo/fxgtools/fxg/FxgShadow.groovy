package eu.hansolo.fxgtools.fxg

import java.awt.Color
import java.awt.geom.Point2D

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 13:40
 * To change this template use File | Settings | File Templates.
 */
class FxgShadow extends FxgFilter{
    FxgFilterType type = FxgFilterType.SHADOW
    boolean inner
    int angle
    int distance
    double alpha
    double blurX
    double blurY
    Color color

    Point2D getOffset() {
        return new Point2D.Double(distance * Math.cos(Math.toRadians(-angle)), distance * Math.sin(Math.toRadians(angle)))
    }
}
