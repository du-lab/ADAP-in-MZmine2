/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3peakdetection;

import java.util.Collection;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionParameters;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3PeakDetectionModule implements MZmineProcessingModule {
    
    private static final String MODULE_NAME = "ADAP3 Peak Detection";
    private static final String MODULE_DESCRIPTION = "This module uses ADAP-3 pipeline to separate each chromatogram into individual peaks";
    
    @Nonnull @Override
    public String getName() {
        return MODULE_NAME;
    }
    
    @Nonnull @Override
    public String getDescription() {
        return MODULE_DESCRIPTION;
    }
    
    @Nonnull @Override
    public MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKLISTPICKING;
    }
    
    @Nonnull @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
        return ADAP3PeakDetectionParameters.class;
    }
    
    @Nonnull @Override
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull final ParameterSet parameters,
            @Nonnull final Collection<Task> tasks) {
        
         for (final PeakList peakList : parameters
                .getParameter(DeconvolutionParameters.PEAK_LISTS).getValue()
                .getMatchingPeakLists()) {

            tasks.add(new ADAP3PeakDetectionTask(project, peakList, parameters));
        }
        
        return ExitCode.OK;
    }
}
