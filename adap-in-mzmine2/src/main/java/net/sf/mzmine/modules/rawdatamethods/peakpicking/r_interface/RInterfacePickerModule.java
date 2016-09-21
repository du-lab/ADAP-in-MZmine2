/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.rawdatamethods.peakpicking.r_interface;

import java.util.Collection;
import javax.annotation.Nonnull;

import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 *
 * @author aleksandrsmirnov
 */
public class RInterfacePickerModule implements MZmineProcessingModule {
    private static final String MODULE_NAME = "Interface for peakpicking in R";
    private static final String MODULE_DESCRIPTION = 
            "This module runs R-code for peak picking";
    
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
        return MZmineModuleCategory.PEAKPICKING;
    }
    
    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return RInterfacePickerParameters.class;
    }
    
    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks)
    {
        final RawDataFile[] dataFiles = parameters
                .getParameter(RInterfacePickerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();   
        
        for (RawDataFile dataFile : dataFiles) {
            Task newTask = new RInterfacePickerTask(project, dataFile, 
                    parameters);
            tasks.add(newTask);
        }
        
        return ExitCode.OK;
    }
}
