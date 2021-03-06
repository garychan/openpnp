/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components.reticle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Point;
import org.openpnp.util.HslColor;
import org.openpnp.util.Utils2D;

public class CrosshairReticle implements Reticle {
	private Color color;
	private Color complimentaryColor;
	
	public CrosshairReticle() {
		setColor(Color.red);
	}
	
	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
		complimentaryColor = new HslColor(color).getComplementary();
	}

	@Override
	public void draw(Graphics2D g2d,
			LengthUnit cameraUnitsPerPixelUnits,
			double cameraUnitsPerPixelX, 
			double cameraUnitsPerPixelY, 
			double viewPortCenterX, 
			double viewPortCenterY,
			int viewPortWidth,
			int viewPortHeight,
			double rotation) {
		
		g2d.setStroke(new BasicStroke(1f));
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		

		// TODO: Rotate a bounding box and use the calculated width and height
		// so that the lines reach the edges of the component once rotated.
		// We currently use the full width for each half width segment, so it
		// works but it is sloppy.
		
		Point p;
		g2d.setColor(color);
		p = new Point(viewPortWidth, 0);
		p = Utils2D.rotatePoint(p, rotation);
		g2d.drawLine(
				(int) viewPortCenterX, 
				(int) viewPortCenterY, 
				(int) (viewPortCenterX + p.getX()), 
				(int) (viewPortCenterY + p.getY()));
		g2d.drawLine(
				(int) viewPortCenterX, 
				(int) viewPortCenterY, 
				(int) (viewPortCenterX - p.getX()), 
				(int) (viewPortCenterY - p.getY()));
		
		p = new Point(0, viewPortHeight);
		p = Utils2D.rotatePoint(p, rotation);
		g2d.drawLine(
				(int) viewPortCenterX, 
				(int) viewPortCenterY, 
				(int) (viewPortCenterX + p.getX()), 
				(int) (viewPortCenterY + p.getY()));
		g2d.setColor(complimentaryColor);
		g2d.drawLine(
				(int) viewPortCenterX, 
				(int) viewPortCenterY, 
				(int) (viewPortCenterX - p.getX()), 
				(int) (viewPortCenterY - p.getY()));
	}
}
