/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3peakdetection;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3PeakDetectionParameters extends SimpleParameterSet {
    
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
    
    public static final IntegerParameter PEAK_SPAN =
            new IntegerParameter("Peak Span", "Peak Detection Window Size", 11);
    
    public static final IntegerParameter VALLEY_SPAN =
            new IntegerParameter("Valley Span", 
                    "Boundary Detection Window Size", 9);
    
    public static final StringParameter SUFFIX = new StringParameter("Suffix",
	    "This string is added to peak list name as suffix", "ADAP-3 Peak Detection");
    
    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, original chromatogram will be removed and only the deconvolved version remains");
     
    public ADAP3PeakDetectionParameters() {
        super(new Parameter[] {PEAK_LISTS, PEAK_SPAN, VALLEY_SPAN, SUFFIX, 
            AUTO_REMOVE});
    }
}
