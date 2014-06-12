/*
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.sun.pdfview.pattern;

import java.io.IOException;
import java.util.Map;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.util.Log;
import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.PDFRenderer;

/**
 * A type 2 (tiling) pattern
 */
public class PatternType2 extends PDFPattern {
	private static final String TAG = "ANDPDF.patterntype2";  
    /** the shader */
    public PDFShader shader;
        
    /** Creates a new instance of PatternType2 */
    public PatternType2() {
        super(2);
    }
    
    /**
     * Parse the pattern from the PDFObject
     *
     * Note the resources passed in are ignored...
     */
    @Override
	protected void parse(PDFObject patternObj, Map<?, ?> rsrc) throws IOException
    {
        this.shader = PDFShader.getShader(patternObj.getDictRef("Shading"), rsrc);        
    }
    
    /** 
     * Create a PDFPaint from this pattern and set of components.  
     * This creates a buffered image of this pattern using
     * the given paint, then uses that image to create the correct 
     * TexturePaint to use in the PDFPaint.
     *
     * @param basePaint the base paint to use, or null if not needed
     */
    @Override
	public PDFPaint getPaint(PDFPaint basePaint) {
    	return new TilingPatternPaint(shader.getPaint().getPaint(), this);
    }
    
    /** 
     * This class overrides PDFPaint to paint in the pattern coordinate space
     */
    static class TilingPatternPaint extends PDFPaint {
        /** the pattern to paint */
        private PatternType2 pattern;
        
        /** Create a tiling pattern paint */
        public TilingPatternPaint(Paint paint, PatternType2 pattern) {
            super (paint.getColor());
            
            this.pattern = pattern;
        }
        
        /**
         * fill a path with the paint, and record the dirty area.
         * @param state the current graphics state
         * @param g the graphics into which to draw
         * @param s the path to fill
         * @param drawn a RectF into which the dirty area (area drawn)
         * will be added.
         */
        @Override
		public RectF fill(PDFRenderer state, Canvas g, Path s) {
			// first transform s into device space
			Matrix at = g.getMatrix();//getTransform();
			//Shape xformed = s.createTransformedShape(at);
			Path xformed = new Path();
	        s.transform(at, xformed); 
	        
            // push the graphics state so we can restore it
            state.push();
            
            // set the transform to be the inital transform concatentated
            // with the pattern matrix
            state.setTransform(state.getInitialTransform());
            state.transform(this.pattern.getTransform());
            
            // now figure out where the shape should be
            try {
            	Matrix inverse = new Matrix();
                boolean result = state.getTransform().invert(inverse);//.createInverse();
            } catch (Exception nte) {
                // oh well (?)
            	Log.e(TAG,"Exception inverting matrix, and now??? "+ nte.getMessage());
            }
            //xformed = at.createTransformedShape(xformed);
            xformed.transform(at, xformed); //xformed.transform(at);
            
            if (pattern.shader.getBackground() != null) {
            	PorterDuffXfermode pdxfer = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);//TODO
                //g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            	Paint temp = pattern.shader.getBackground().getPaint();
            	temp.setXfermode(pdxfer);
            	g.drawPath(xformed, temp);
                //g.setPaint(pattern.shader.getBackground().getPaint());
                //g.fill(xformed);            	
            }
            // set the paint and draw the xformed shape
            PorterDuffXfermode pdxfer2 = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
            //g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            //g.setPaint(getPaint());
            //g.fill(xformed);
            Paint tmp2 = getPaint();
            @SuppressWarnings("unused")
			Xfermode backupXfermode = tmp2.getXfermode();
            tmp2.setXfermode(pdxfer2);
            g.drawPath(xformed,tmp2);
            
            
            
            // restore the graphics state
            state.pop();
            
            // return the area changed
            final RectF bounds = new RectF();
            final RectF result = new RectF();
            s.computeBounds(bounds, false);
            g.getMatrix().mapRect(result, bounds);
            return bounds;
            //return s.createTransformedShape(g.getTransform()).getBounds2D();
        }
    }

}