/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decomposition;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3TICWindowDetectionParameters extends SimpleParameterSet {
    
    public static final IntegerParameter PEAK_SPAN =
            new IntegerParameter("Peak Span", "Peak Detection Window Size", 11);
    
    public static final IntegerParameter VALLEY_SPAN =
            new IntegerParameter("Valley Span", 
                    "Boundary Detection Window Size", 9);
    
    public ADAP3TICWindowDetectionParameters() {
        super(new Parameter[] {PEAK_SPAN, VALLEY_SPAN});
    }
}
