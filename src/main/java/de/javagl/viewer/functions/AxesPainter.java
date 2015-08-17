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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import de.javagl.geom.AffineTransforms;
import de.javagl.geom.Lines;
import de.javagl.geom.Rectangles;
import de.javagl.viewer.Painter;

/**
 * Implementation of the {@link Painter} interface that paints labeled
 * coordinate axes
 */
class AxesPainter implements Painter
{
    /**
     * The font that will be used for the labels
     */
    private final Font font = new Font("Dialog", Font.PLAIN, 9);
    
    /**
     * The stroke for the axes
     */
    private final Stroke stroke = new BasicStroke(1.0f);

    /**
     * The color of the axes
     */
    private final Color axesColor = Color.GRAY;
    
    /**
     * The size of the tick marks on the screen
     */
    private final double tickSizeScreen = 5;
    
    /**
     * The minimum distance that two ticks should have on the screen
     */
    private final double minScreenTickDistanceX = 30;

    /**
     * The minimum distance that two ticks should have on the screen
     */
    private final double minScreenTickDistanceY = 20;
    
    /**
     * Whether the "minScreenTickDistanceX" should be adjusted
     * based on the strings that are printed for the labels
     */
    private final boolean adjustForStringLengths = true;
    
    /**
     * Whether labels are printed at the axis ticks
     */
    private final boolean printLabels = true;
    
    /**
     * Whether a grid should be painted in the background
     */
    private final boolean paintGrid = true;
    
    /**
     * The color for the grid
     */
    private final Color gridColor = new Color(240,240,240);
    
    /**
     * A line object, used internally in various methods
     */
    private final Line2D.Double line = new Line2D.Double();
    
    /**
     * The world-to-screen transform that was used for the previous
     * {@link #paint} call
     */
    private AffineTransform previousWorldToScreen = new AffineTransform();
    
    /**
     * The width that was passed in to the previous {@link #paint} call
     */
    private double previousWidth = -1;

    /**
     * The height that was passed in to the previous {@link #paint} call
     */
    private double previousHeight = -1;

    /**
     * The tick positions of the x-axis, in world coordinates
     */
    private double worldTicksX[];

    /**
     * The label format for the x-axis. May be <code>null</code>
     * if no labels should be painted
     */
    private String labelFormatX;
    
    /**
     * The tick positions of the y-axis, in world coordinates
     */
    private double worldTicksY[];

    /**
     * The label format for the y-axis. May be <code>null</code>
     * if no labels should be painted
     */
    private String labelFormatY;
        
    /**
     * The bounds of the currently visible area, in world coordinates
     */
    private Rectangle2D worldBounds = new Rectangle2D.Double();
    
    
    /**
     * Computes the distance that two ticks on the x-axis should have
     * in world coordinates. This will be the given screen tick distance,
     * converted to world coordinates and snapped to a "nice" value.
     * 
     * @param worldToScreen The world-to-screen transform
     * @param minScreenTickDistanceX  The minimum distance of two ticks
     * in screen coordinates
     * @return The tick distance in world coordinates
     */
    private static double computeWorldTickDistanceX(
        AffineTransform worldToScreen, double minScreenTickDistanceX)
    {
        double unitLengthScreenX =
            AffineTransforms.computeDistanceX(worldToScreen, 1.0);
        double minWorldTickDistanceX =
            minScreenTickDistanceX / unitLengthScreenX;
        double worldTickDistanceX =
            Axes.computeSnappedUpValue(minWorldTickDistanceX);
        return worldTickDistanceX;
    }
    
    
    /**
     * Computes the distance that two ticks on the x-axis should have
     * in world coordinates, adjusted to make sure that the distance
     * between the ticks in screen coordinates will be larger than
     * the bounds of a tick label.
     * 
     * @param g The graphics that would be used for painting 
     * @param worldToScreen The world-to-screen transform
     * @param worldMinX The minimum value for the x-axis 
     * @param worldMaxX The maximum value for the x-axis
     * @param worldTickDistanceX The (non-adjusted) world tick distance 
     * @return The adjusted tick distance in world coordinates
     */
    private double computeAdjustedWorldTickDistanceX(Graphics g,
        AffineTransform worldToScreen, double worldMinX, double worldMaxX,
        double worldTickDistanceX)
    {
        String labelFormatX = Axes.formatStringFor(worldTickDistanceX);

        int nMin = (int) (worldMinX / worldTickDistanceX);
        int nMax = (int) (worldMaxX / worldTickDistanceX) + 1;
        double worldMinTickX = nMin * worldTickDistanceX;
        double worldMaxTickX = nMax * worldTickDistanceX;

        FontMetrics fontMetrics = g.getFontMetrics(font);
        String stringMin = String.format(labelFormatX, worldMinTickX);
        Rectangle2D bMin = fontMetrics.getStringBounds(stringMin, g);
        String stringMax = String.format(labelFormatX, worldMaxTickX);
        Rectangle2D bMax = fontMetrics.getStringBounds(stringMax, g);
        double maxStringWidth =
            Math.max(bMin.getWidth(), bMax.getWidth()) * 1.05;

        if (maxStringWidth > minScreenTickDistanceX)
        {
            return computeWorldTickDistanceX(worldToScreen, maxStringWidth);
        }
        return worldTickDistanceX;
    }    
    
    /**
     * Computes the distance that two ticks on the x-axis should have
     * in world coordinates. This will be the given screen tick distance,
     * converted to world coordinates and snapped to a "nice" value.
     * 
     * @param worldToScreen The world-to-screen transform
     * @param minScreenTickDistanceY The minimum distance of two ticks
     * in screen coordinates
     * @return The tick distance in world coordinates
     */
    private static double computeWorldTickDistanceY(
        AffineTransform worldToScreen, double minScreenTickDistanceY)
    {
        double unitLengthScreenY =
            AffineTransforms.computeDistanceY(worldToScreen, 1.0);
        double minWorldTickDistanceY =
            minScreenTickDistanceY / unitLengthScreenY;
        double worldTickDistanceY =
            Axes.computeSnappedUpValue(minWorldTickDistanceY);
        return worldTickDistanceY;
    }
    
    
    /**
     * Validate the data that is used for painting the axes and the grid: 
     * If the world-to-screen transform or the width or height changed 
     * since the previous call, the data will be updated
     * 
     * @param g The graphics that would be used for painting
     * @param worldToScreen The world-to-screen transform
     * @param w The width of the painting area, in screen coordinates
     * @param h The height of the painting area, in screen coordinates
     */
    private void validateAxes(Graphics g, AffineTransform worldToScreen, 
        double w, double h)
    {
        if (w != previousWidth || h != previousHeight || 
            !worldToScreen.equals(previousWorldToScreen))
        {
            Rectangle2D screenBounds = new Rectangle2D.Double(0, 0, w, h);
            Rectangles.computeBounds(
                AffineTransforms.invert(worldToScreen, null), 
                screenBounds, worldBounds);
            double worldMinX = worldBounds.getMinX();
            double worldMaxX = worldBounds.getMaxX();
            double worldMinY = worldBounds.getMinY();
            double worldMaxY = worldBounds.getMaxY();

            updateX(g, worldToScreen, worldMinX, worldMaxX);
            updateY(worldToScreen, worldMinY, worldMaxY);

            previousWorldToScreen.setTransform(worldToScreen);
            previousWidth = w;
            previousHeight = h;
        }
    }
    
    /**
     * Update the data that is used internally for painting the x-axis, 
     * namely the {@link #worldTicksX} and the {@link #labelFormatX}
     * 
     * @param g The graphics
     * @param worldToScreen The world-to-screen transform
     * @param worldMinX The minimum world coordinate for the axis
     * @param worldMaxX The maximum world coordinate for the axis
     */
    private void updateX(Graphics g, AffineTransform worldToScreen, 
        double worldMinX, double worldMaxX)
    {
        double worldTickDistanceX = 
            computeWorldTickDistanceX(worldToScreen, minScreenTickDistanceX);
        if (printLabels && adjustForStringLengths)
        {
            worldTickDistanceX = 
                computeAdjustedWorldTickDistanceX(
                    g, worldToScreen, worldMinX, worldMaxX, 
                    worldTickDistanceX);
        }
        worldTicksX = Axes.computeWorldTicks(
            worldMinX, worldMaxX, worldTickDistanceX);
        labelFormatX =  null;
        if (printLabels)
        {
            labelFormatX = Axes.formatStringFor(worldTickDistanceX);
        }
    }

    /**
     * Update the data that is used internally for painting the y-axis, 
     * namely the {@link #worldTicksY} and the {@link #labelFormatY}
     * 
     * @param worldToScreen The world-to-screen transform
     * @param worldMinY The minimum world coordinate for the axis
     * @param worldMaxY The maximum world coordinate for the axis
     */
    private void updateY(AffineTransform worldToScreen, 
        double worldMinY, double worldMaxY)
    {
        double worldTickDistanceY = 
            computeWorldTickDistanceY(worldToScreen, minScreenTickDistanceY);
        worldTicksY = Axes.computeWorldTicks(
            worldMinY, worldMaxY, worldTickDistanceY);
        labelFormatY =  null;
        if (printLabels)
        {
            labelFormatY = Axes.formatStringFor(worldTickDistanceY);
        }
    }
    

    @Override
    public void paint(Graphics2D g, AffineTransform worldToScreen, 
        double w, double h)
    {
        validateAxes(g, worldToScreen, w, h);
        
        g.setStroke(stroke);
        if (paintGrid)
        {
            g.setColor(gridColor);
            paintInternalGrid(g, worldToScreen);
        }
        g.setColor(axesColor);
        double worldMinX = worldBounds.getMinX();
        double worldMaxX = worldBounds.getMaxX();
        double worldMinY = worldBounds.getMinY();
        double worldMaxY = worldBounds.getMaxY();
        paintInternalX(g, worldToScreen, worldMinX, worldMaxX, 0.0);
        paintInternalY(g, worldToScreen, worldMinY, worldMaxY, 0.0);
    }

    /**
     * Paint the specified representation of the x-axis into the given 
     * graphics context using the given world-to-screen transform.
     *  
     * @param g The graphics to paint to
     * @param worldToScreen The world-to-screen transform
     * @param w The width of the painting area, in screen coordinates
     * @param h The height of the painting area, in screen coordinates
     * @param worldMinX The minimum world coordinate of the axis
     * @param worldMaxX The maximum world coordinate of the axis
     * @param worldY The world coordinate at which the axis should be painted
     */
    public void paintX(Graphics2D g, AffineTransform worldToScreen,
        double w, double h, double worldMinX, double worldMaxX, double worldY)
    {
        validateAxes(g, worldToScreen, w, h);
        g.setColor(axesColor);
        paintInternalX(g, worldToScreen, worldMinX, worldMaxX, worldY);
    }
    
    /**
     * Paint the x-axis after it has been made sure that the
     * {@link #worldTicksX} and {@link #labelFormatX} are up to date
     *  
     * @param g The graphics to paint to
     * @param worldToScreen The world-to-screen transform
     * @param worldMinX The minimum world coordinate of the axis
     * @param worldMaxX The maximum world coordinate of the axis
     * @param worldY The world coordinate at which the axis should be painted
     */
    private void paintInternalX(
        Graphics2D g, AffineTransform worldToScreen, 
        double worldMinX, double worldMaxX, double worldY)
    {
        line.setLine(worldMinX,worldY,worldMaxX,worldY);
        Lines.transform(worldToScreen, line, line);
        g.draw(line);
        g.setFont(font);
        for (int i=0; i<worldTicksX.length; i++)
        {
            double worldTickX = worldTicksX[i];
            if (worldTickX >= worldMinX && worldTickX <= worldMaxX)
            {
                paintTickX(g, worldToScreen, worldTickX, worldY, labelFormatX);
            }
        }
    }

    /**
     * Paint the specified representation of the y-axis into the given 
     * graphics context using the given world-to-screen transform.
     *  
     * @param g The graphics to paint to
     * @param worldToScreen The world-to-screen transform
     * @param w The width of the painting area, in screen coordinates
     * @param h The height of the painting area, in screen coordinates
     * @param worldMinY The minimum world coordinate of the axis
     * @param worldMaxY The maximum world coordinate of the axis
     * @param worldX The world coordinate at which the axis should be painted
     */
    public void paintY(Graphics2D g, AffineTransform worldToScreen,
        double w, double h, double worldMinY, double worldMaxY, double worldX)
    {
        validateAxes(g, worldToScreen, w, h);
        g.setColor(axesColor);
        paintInternalY(g, worldToScreen, worldMinY, worldMaxY, worldX);
    }
    
    /**
     * Paint the y-axis after it has been made sure that the
     * {@link #worldTicksY} and {@link #labelFormatY} are up to date
     *  
     * @param g The graphics to paint to
     * @param worldToScreen The world-to-screen transform
     * @param worldMinY The minimum world coordinate of the axis
     * @param worldMaxY The maximum world coordinate of the axis
     * @param worldX The world coordinate at which the axis should be painted
     */
    private void paintInternalY(
        Graphics2D g, AffineTransform worldToScreen, 
        double worldMinY, double worldMaxY, double worldX)
    {
        line.setLine(worldX,worldMinY,worldX,worldMaxY);
        Lines.transform(worldToScreen, line, line);
        g.draw(line);
        g.setFont(font);
        for (int i=0; i<worldTicksY.length; i++)
        {
            double worldTickY = worldTicksY[i];
            if (worldTickY >= worldMinY && worldTickY <= worldMaxY)
            {
                paintTickY(g, worldToScreen, worldX, worldTickY, labelFormatY);
            }
        }
    }
    
    /**
     * Paint the coordinate grid in the background
     *  
     * @param g The graphics to paint to
     * @param worldToScreen The world-to-screen transform
     * @param w The width of the painting area, in screen coordinates
     * @param h The height of the painting area, in screen coordinates
     */
    public void paintGrid(
        Graphics2D g, AffineTransform worldToScreen, double w, double h)
    {
        validateAxes(g, worldToScreen, w, h);
        g.setColor(gridColor);
        paintInternalGrid(g, worldToScreen);
    }


    /**
     * Paint the coordinate grid in the background, after it has been
     * made sure that the data for painting the grid and axes is up
     * to date 
     *  
     * @param g The graphics to paint to
     * @param worldToScreen The world-to-screen transform
     */
    private void paintInternalGrid(Graphics2D g, AffineTransform worldToScreen)
    {
        double worldMinX = worldBounds.getMinX();
        double worldMaxX = worldBounds.getMaxX();
        double worldMinY = worldBounds.getMinY();
        double worldMaxY = worldBounds.getMaxY();
        for (int i=0; i<worldTicksX.length; i++)
        {
            double worldTickX = worldTicksX[i];
            paintGridLineX(g, worldToScreen, worldTickX, 
                worldMinY, worldMaxY);
        }
        for (int i=0; i<worldTicksY.length; i++)
        {
            double worldTickY = worldTicksY[i];
            paintGridLineY(g, worldToScreen, worldTickY, 
                worldMinX, worldMaxX);
        }
    }
    
    
    
    /**
     * Paints a single grid line at the given x-coordinate 
     * 
     * @param g The graphics context
     * @param worldToScreen The world-to-screen transform
     * @param worldX The world coordinate of the grid line
     * @param worldMinY The minimum y-coordinate 
     * @param worldMaxY The maximum y-coordinate 
     */
    private void paintGridLineX(Graphics2D g, AffineTransform worldToScreen, 
        double worldX, double worldMinY, double worldMaxY)
    {
        line.setLine(worldX, worldMinY, worldX, worldMaxY);
        Lines.transform(worldToScreen, line, line);
        g.draw(line);
    }
    
    /**
     * Paints a single grid line at the given y-coordinate 
     * 
     * @param g The graphics context
     * @param worldToScreen The world-to-screen transform
     * @param worldY The world coordinate of the tick
     * @param worldMinX The minimum x-coordinate 
     * @param worldMaxX The maximum x-coordinate 
     */
    private void paintGridLineY(Graphics2D g, AffineTransform worldToScreen, 
        double worldY, double worldMinX, double worldMaxX)
    {
        line.setLine(worldMinX, worldY, worldMaxX, worldY);
        Lines.transform(worldToScreen, line, line);
        g.draw(line);
    }
    
    
    /**
     * Paints a single tick of the x-axis 
     * 
     * @param g The graphics context
     * @param worldToScreen The world-to-screen transform
     * @param worldX The x-world coordinate of the tick
     * @param worldY The y-world coordinate of the tick
     * @param labelFormat The format string for the labels. If this is
     * <code>null</code>, then no labels will be painted
     */
    private void paintTickX(Graphics2D g, AffineTransform worldToScreen, 
        double worldX, double worldY, String labelFormat)
    {
        line.setLine(worldX, worldY, worldX, worldY-1);
        Lines.transform(worldToScreen, line, line);
        Lines.scaleToLength(tickSizeScreen, line, line);
        g.draw(line);
        
        if (labelFormat != null)
        {
            String string = String.format(labelFormat, worldX);
            Rectangle2D b = g.getFontMetrics().getStringBounds(string, g);
            int sx = (int)(line.getX2() - b.getWidth() * 0.5);
            int sy = (int)(line.getY2() + b.getHeight());
            g.drawString(string, sx, sy);
        }
    }
    
    /**
     * Paints a single tick of the y-axis 
     * 
     * @param g The graphics context
     * @param worldToScreen The world-to-screen transform
     * @param worldX The x-world coordinate of the tick
     * @param worldY The y-world coordinate of the tick
     * @param labelFormat The format string for the labels. If this is
     * <code>null</code>, then no labels will be painted
     */
    private void paintTickY(Graphics2D g, AffineTransform worldToScreen, 
        double worldX, double worldY, String labelFormat)
    {
        line.setLine(worldX, worldY, worldX-1, worldY);
        Lines.transform(worldToScreen, line, line);
        Lines.scaleToLength(tickSizeScreen, line, line);
        g.draw(line);
        
        if (labelFormat != null)
        {
            String string = String.format(labelFormat, worldY);
            Rectangle2D b = g.getFontMetrics().getStringBounds(string, g);
            int sx = (int)(line.getX2() - b.getWidth() * 1.05);
            int sy = (int)(line.getY2() + b.getHeight() * 0.3);
            g.drawString(string, sx, sy);
        }
    }
}