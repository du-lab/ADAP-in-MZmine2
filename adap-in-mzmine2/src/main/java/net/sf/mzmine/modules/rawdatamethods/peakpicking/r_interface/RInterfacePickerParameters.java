/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.rawdatamethods.peakpicking.r_interface;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class RInterfacePickerParameters extends SimpleParameterSet {
    
    public static final RawDataFilesParameter dataFiles = 
            new RawDataFilesParameter();
    
    public static final ScanSelectionParameter scanSelection = 
            new ScanSelectionParameter(new ScanSelection(1));
    
    public static final IntegerParameter peakSpan =
            new IntegerParameter("Peak Span", "Peak Detection Window Size");
    
    public static final IntegerParameter valleySpan =
            new IntegerParameter("Valley Span", 
                    "Boundary Detection Window Size");
    
    public RInterfacePickerParameters() {
        super(new Parameter[] {dataFiles, scanSelection, peakSpan, valleySpan});
    }
}
