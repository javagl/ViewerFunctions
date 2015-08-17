/*
 * www.javagl.de - Viewer - Functions
 *
 * Copyright (c) 2013-2015 Marco Hutter - http://www.javagl.de
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package de.javagl.viewer.functions;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

import de.javagl.geom.Points;
import de.javagl.viewer.Painter;
import de.javagl.viewer.Viewer;

// Note: This is not officially used or supported yet, but will be
// an alternative to the default AxesPainter in the FunctionPanel

/**
 * An implementation of the {@link Painter} interface that paints coordinate
 * axis (using an {@link AxesPainter}) at fixed screen coordinates, regardless
 * of the zoom and translation of the {@link Viewer}.
 */
class ScreenFixedAxesPainter implements Painter
{
    /**
     * The component to which the screen coordinates refer. This is usually 
     * the {@link Viewer} that this painter is added to - particularly, a 
     * {@link FunctionPanel} 
     */
    private final JComponent functionPanel;

    /**
     * The delegate {@link AxesPainter}
     */
    private final AxesPainter axesPainter = new AxesPainter();

    /**
     * The insets for the x-axis
     */
    private final Insets xInsets;

    /**
     * The insets for the y-axis
     */
    private final Insets yInsets;
    
    /**
     * Creates a new painter
     * 
     * @param functionPanel The component to which the margin refers
     * @param topX The top offset of the x-axis. If this is negative, 
     * then the bottom offset will be used.
     * @param leftX The left offset of the x-axis 
     * @param bottomX The bottom offset of the x-axis. If this is negative, 
     * then the top offset will be used.
     * @param rightX The right offset of the x-axis
     * @param topY The top offset of the y-axis 
     * @param leftY The left offset of the y-axis. If this is negative, 
     * then the right offset will be used.
     * @param bottomY The bottom offset of the y-axis
     * @param rightY The right offset of the y-axis. If this is negative, 
     * then the left offset will be used.
     */
    ScreenFixedAxesPainter(JComponent functionPanel, 
        int topX, int leftX, int bottomX, int rightX, 
        int topY, int leftY, int bottomY, int rightY) 
    {
        this.functionPanel = functionPanel;
        this.xInsets = new Insets(topX, leftX, bottomX, rightX);
        this.yInsets = new Insets(topY, leftY, bottomY, rightY);
         
    }
    
    @Override
    public void paint(Graphics2D g, AffineTransform worldToScreen, 
        double w, double h)
    {
        int sw = functionPanel.getWidth();
        int sh = functionPanel.getHeight(); 
        
        int xy = xInsets.top < 0 ? sh - xInsets.bottom : xInsets.top; 
        Point2D pxMin = new Point2D.Double(xInsets.left, xy);
        Point2D pxMax = new Point2D.Double(sw-xInsets.right, xy);
        Points.inverseTransform(worldToScreen, pxMin, pxMin);
        Points.inverseTransform(worldToScreen, pxMax, pxMax);

        int yx = yInsets.left < 0 ? sw - yInsets.right : yInsets.left; 
        Point2D pyMin = new Point2D.Double(yx, sh-yInsets.bottom);
        Point2D pyMax = new Point2D.Double(yx, yInsets.top);
        Points.inverseTransform(worldToScreen, pyMin, pyMin);
        Points.inverseTransform(worldToScreen, pyMax, pyMax);

        axesPainter.paintGrid(g, worldToScreen, w, h);
        axesPainter.paintX(g, worldToScreen, w, h, 
            pxMin.getX(), pxMax.getX(), pxMin.getY());
        axesPainter.paintY(g, worldToScreen, w, h, 
            pyMin.getY(), pyMax.getY(), pyMin.getX());
    }
}
