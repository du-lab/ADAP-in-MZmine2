/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decomposition;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.ParameterSetParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionParameters extends SimpleParameterSet {
    
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
    
    public static final ParameterSetParameter TIC_WINDOW =
            new ParameterSetParameter("TIC Window Detection Parameters", 
                    "Parameters for ADAP-3 TIC Window Detection", 
                    new ADAP3TICWindowDetectionParameters());
    
    public static final ParameterSetParameter EIC_DECOMPOSITION =
            new ParameterSetParameter("EIC Decomposition Parameters",
                    "Parameters for ADAP-3 Peak Decomposition",
                    new ADAP3EICDecompositionParameters());
    
    public static final ParameterSetParameter OPTIMIZATION =
            new ParameterSetParameter("Optimization Parameters",
                    "Parameters for ADAP-3 spectrum building",
                    new ADAP3OptimizationParameters());
    
    public static final StringParameter SUFFIX = new StringParameter("Suffix",
	    "This string is added to peak list name as suffix", "ADAP-3 Peak Decomposition");
    
    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, original chromatogram will be removed and only the deconvolved version remains");
    
    public ADAP3DecompositionParameters() {
	super(new Parameter[] {PEAK_LISTS, TIC_WINDOW, EIC_DECOMPOSITION,
            OPTIMIZATION, SUFFIX, AUTO_REMOVE});
    }
}
