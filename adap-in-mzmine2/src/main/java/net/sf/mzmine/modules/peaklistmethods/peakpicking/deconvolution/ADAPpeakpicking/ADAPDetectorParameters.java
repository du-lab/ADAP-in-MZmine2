/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking;

import java.awt.Window;
import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

/**
 * Parameters used by CentWaveDetector.
 */
public class ADAPDetectorParameters extends SimpleParameterSet {

//    /**
//     * Peak integration methods.
//     */
//    public enum PeakIntegrationMethod {
//
//	UseSmoothedData("Use smoothed data", 1), UseRawData("Use raw data", 2);
//
//	private final String name;
//	private final int index;
//
//	/**
//	 * Create the method.
//	 *
//	 * @param aName
//	 *            name
//	 * @param anIndex
//	 *            index (as used by findPeaks.centWave)
//	 */
//	PeakIntegrationMethod(final String aName, final int anIndex) {
//
//	    name = aName;
//	    index = anIndex;
//	}
//
//	@Override
//	public String toString() {
//
//	    return name;
//	}
//
//	public int getIndex() {
//
//	    return index;
//	}
//    }

    public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
	    "Peak duration range", "Range of acceptable peak lengths",
	    MZmineCore.getConfiguration().getRTFormat(),
	    Range.closed(0.0, 10.0));

//    public static final DoubleRangeParameter PEAK_SCALES = new DoubleRangeParameter(
//	    "Wavelet scales",
//	    "Range wavelet widths (smallest, largest) in minutes", MZmineCore
//		    .getConfiguration().getRTFormat(), Range.closed(0.25, 5.0));
    

    public static final DoubleParameter SN_THRESHOLD = new DoubleParameter(
	    "S/N threshold", "Signal to noise ratio threshold",
	    NumberFormat.getNumberInstance(), 10.0, 0.0, null);
    
    public static final DoubleParameter MIN_FEAT_HEIGHT = new DoubleParameter(
        "min feature height", "Minimum height of a feature. Should be the same, or similar to, the value - min start intensity - "
                + "set in the chromatogram building.",
        NumberFormat.getNumberInstance(), 10.0, 0.0, null);

//    public static final ComboParameter<PeakIntegrationMethod> INTEGRATION_METHOD = new ComboParameter<PeakIntegrationMethod>(
//	    "Peak integration method",
//	    "Method used to determine RT extents of detected peaks",
//	    PeakIntegrationMethod.values(),
//	    PeakIntegrationMethod.UseSmoothedData);

    public ADAPDetectorParameters() {

	super(new Parameter[] { SN_THRESHOLD, MIN_FEAT_HEIGHT, PEAK_DURATION, });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

	final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
		parent, valueCheckRequired, this, ADAPDetector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
