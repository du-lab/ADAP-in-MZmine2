/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.alignment.adap3;

import java.text.NumberFormat;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ParameterSetParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerParameters extends SimpleParameterSet {
    
    public static final PeakListsParameter PEAK_LISTS = 
            new PeakListsParameter();

    public static final StringParameter NEW_PEAK_LIST_NAME = 
            new StringParameter("Aligned Peak List Name", "Peak list name", "Aligned peak list");
    
    public static final DoubleParameter SAMPLE_COUNT_RATIO =
            new DoubleParameter("Samples-per-Tag Ratio (between 0 and 1)", 
                    "Determines in how many samples a peak should be found",
                    NumberFormat.getInstance(), 0.5, 0.0, 1.0);
    
    public static final DoubleParameter RET_TIME_RANGE =
            new DoubleParameter("Retention Time Range (min)",
                    "Only peaks within this range will be aligned",
                    NumberFormat.getInstance(), 0.5);
    
    public static final DoubleParameter MZ_RANGE =
            new DoubleParameter("M/z Range (Da)",
                    "Only peaks within this range will be aligned",
                    NumberFormat.getInstance(), 0.1);
    
    public static final DoubleParameter SCORE_TOLERANCE =
            new DoubleParameter("Score Tolerance (between 0 and 1)",
                    "Only peaks with the score within this tolerance will be aligned",
                    NumberFormat.getInstance(), 0.75, 0.0, 1.0);
    
    public static final DoubleParameter SCORE_WEIGHT =
            new DoubleParameter("Score Weight (between 0 and 1)",
                    "The score is calculated by the formula w * EICScore + (1 - w) * SpectrumScore, where w is this weight",
                    NumberFormat.getInstance(), 0.5, 0.0, 1.0);
    
    public static final DoubleParameter MAX_SHIFT =
            new DoubleParameter("Max Time Shift (min)",
                    "Maximum time-shift allowed for allignment",
                    NumberFormat.getInstance(), 0.5);
    
    public static final ParameterSetParameter OPTIM_PARAMS =
            new ParameterSetParameter("Optimization Parameters", 
                    "These parameters control optimization algorithm for finding "
                            + "maximum of the convolution integral",
                    new ADAP3AlignerOptimizationParameters());
    
    public ADAP3AlignerParameters() {
        super(new Parameter[] {PEAK_LISTS, SAMPLE_COUNT_RATIO, RET_TIME_RANGE,
                MZ_RANGE, SCORE_TOLERANCE, SCORE_WEIGHT, MAX_SHIFT, 
                OPTIM_PARAMS, NEW_PEAK_LIST_NAME});
    }
}
