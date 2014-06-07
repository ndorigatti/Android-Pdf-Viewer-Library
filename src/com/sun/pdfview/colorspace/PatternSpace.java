/*
 * $Id: PatternSpace.java,v 1.2 2007-12-20 18:33:34 rbair Exp $
 *
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

package com.sun.pdfview.colorspace;

import java.io.IOException;
import java.util.Map;
import android.graphics.Color;
import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.pattern.PDFPattern;

/**
 * A PatternSpace fills with a pattern, the name of which is 
 * specified in the call to getPaint().  This pattern is
 * read from the resources of the current page.  The pattern space
 * may also have a base color space which the pattern is defined in.
 */
public class PatternSpace extends PDFColorSpace {
    private PDFColorSpace base;
    
    public PatternSpace() {
    }
    
    /**
     * Create a pattern space with the given color space as a base
     */
	public PatternSpace(PDFColorSpace base) {
		this.base = base;
	}

    /**
     * Get the base color space
     */
    public PDFColorSpace getBase() {
        return base;
    }
    
    /**
     * Get the number of components we want
     */
	@Override
	public int getNumComponents() {
		if (base == null) {
			return 0;
		} else {
			return base.getNumComponents();
		}
	}

    /**
     * get the PDFPaint representing the color described by the
     * given color components
     * @param components the color components corresponding to the given
     * colorspace
     * @return a PDFPaint object representing the closest Color to the
     * given components.
     */
	@Override
	public PDFPaint getPaint(float[] components) {
		throw new IllegalArgumentException("Pattern spaces require a pattern " + "name!");
	}
    
    /**
     * Get the paint representing a pattern, optionally with the given
     * base paint.
     *
     * @param patternObj the pattern to render
     * @param components the components of the base paint
     */
	public PDFPaint getPaint(PDFObject patternObj, float[] components, Map<?, ?> resources) throws IOException {
		PDFPaint basePaint = null;

		if (getBase() != null) {
			basePaint = getBase().getPaint(components);
		}

		PDFPattern pattern = (PDFPattern) patternObj.getCache();
		if (pattern == null) {
			pattern = PDFPattern.getPattern(patternObj, resources);
			patternObj.setCache(pattern);
		}

		return pattern.getPaint(basePaint);
	}

	@Override
	public int getType() {
		return COLORSPACE_PATTERN;
	}

	@Override
	public String getName() {
		return "Pattern";
	}

    @Override public int toColor(float[] fcomp) {//FIXME check
    	return Color.rgb((int)(fcomp[0]*255),(int)(fcomp[1]*255),(int)(fcomp[2]*255));
    }

    @Override public int toColor(int[] icomp) {//FIXME check
    	return Color.rgb(icomp[0],icomp[1],icomp[2]);
    }

}
