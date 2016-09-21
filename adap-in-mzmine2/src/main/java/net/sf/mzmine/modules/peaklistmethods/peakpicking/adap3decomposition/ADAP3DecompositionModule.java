/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decomposition;

import java.util.Collection;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionModule implements MZmineProcessingModule {
    
    private static final String MODULE_NAME = "ADAP3 Peak Decomposition";
    private static final String MODULE_DESCRIPTION = "This method analyses "
            + "decomposes shared peaks and combines those with similar shapes";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }
    
    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKLISTPICKING;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return ADAP3DecompositionParameters.class;
    }
    
    @Override @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks)
    {
        PeakList[] peakLists = parameters
                .getParameter(ADAP3DecompositionParameters.PEAK_LISTS).getValue()
                .getMatchingPeakLists();
        
        for (PeakList peakList : peakLists) {
            Task newTask = new ADAP3DecompositionTask(project, peakList,
                    parameters);
            tasks.add(newTask);
        }
        
        return ExitCode.OK;
    }
}