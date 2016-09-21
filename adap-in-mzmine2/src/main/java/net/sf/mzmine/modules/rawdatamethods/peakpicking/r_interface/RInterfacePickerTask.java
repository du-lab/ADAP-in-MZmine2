/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.rawdatamethods.peakpicking.r_interface;

import java.util.logging.Logger;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;

import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

/**
 *
 * @author aleksandrsmirnov
 */
public class RInterfacePickerTask extends AbstractTask {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private final MZmineProject project;
    private final RawDataFile dataFile;
    private final ScanSelection scanSelection;
    private final int peakSpan; 
    private final int valleySpan;
    
    public RInterfacePickerTask(MZmineProject project, RawDataFile dataFile,
            ParameterSet parameters) {
        this.project = project;
        this.dataFile = dataFile;
        this.scanSelection = parameters
                .getParameter(RInterfacePickerParameters.scanSelection)
                .getValue();
        this.peakSpan = parameters.getParameter(
                    RInterfacePickerParameters.peakSpan).getValue();
        this.valleySpan = parameters.getParameter(
                    RInterfacePickerParameters.valleySpan).getValue();
    }
    
    @Override
    public String getTaskDescription() {
        return "Running R-code to find peaks in " + dataFile;
    }
    
    @Override
    public double getFinishedPercentage() {
        return 0.0;
    }
    
    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        
        Scan selectedScans[] = scanSelection.getMatchingScans(dataFile);
        int length = selectedScans.length;
        
        if (length > 0) {
            
            double[] intensities = new double[length];
            for (int i = 0; i < length; ++i)
                intensities[i] = selectedScans[i].getTIC();
            
            String[] rPackages = new String[] {"splus2R", "ifultools", "wmtsa"};
            String[] rPackageVersions = new String[] {"1.2-1", "2.0-4", "2.0-2"};

            RSessionWrapper rSession = new RSessionWrapper(project.toString(), rPackages, rPackageVersions);
            try {
                rSession.open();
                
                // Load ADAP-3
                String rPath = "/Users/aleksandrsmirnov/Projects/adap-gc_3";
                rSession.assign("codeDir", rPath);
                rSession.eval("source(paste('" + rPath + "', 'pipeline.r', sep='/'))");
                
                // Parameters
                rSession.eval("params <- list()");
                rSession.eval("params$nNode <- 1");
                rSession.eval("params$WorkDir <- './'");
                rSession.eval("params$Peak_span <- " + peakSpan);
                rSession.eval("params$Valley_span <- " + valleySpan);
                
                // Intensities
                rSession.assign("vecInt", intensities);
                
                rSession.eval("PeakList <- getPeaks(vecInt, params)");
                final Object leftIndex = (double[]) rSession
                        .collect("PeakList$lboundInd");
                final Object rightIndex = (double[]) rSession
                        .collect("PeakList$rboundInd");

                logger.info("OK");
                
            } catch (RSessionWrapperException e) {
                logger.severe(e.getMessage());
                // Do something
            }
        }
        
        setStatus(TaskStatus.FINISHED);
    }
}
