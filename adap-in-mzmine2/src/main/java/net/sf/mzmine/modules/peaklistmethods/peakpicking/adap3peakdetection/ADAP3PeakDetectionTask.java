/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3peakdetection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionParameters.AUTO_REMOVE;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionParameters.SUFFIX;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionTask;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3PeakDetectionTask extends AbstractTask {
    
    private static final Logger LOG = Logger.getLogger(DeconvolutionTask.class
            .getName());
    
    // Peak lists.
    private final MZmineProject project;
    private final PeakList originalPeakList;
    private PeakList newPeakList;

    // User parameters
    private final ParameterSet parameters;
    private final int peakSpan;
    private final int valleySpan;
    
    // Counters.
    private int processedRows;
    private int totalRows;
    
    private RSessionWrapper rSession;
    
    public ADAP3PeakDetectionTask(final MZmineProject project, 
            final PeakList list, final ParameterSet parameterSet) 
    {
        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        
        processedRows = 0;
        totalRows = 0;
        
        peakSpan = parameters.getParameter(
                    ADAP3PeakDetectionParameters.PEAK_SPAN).getValue();
        valleySpan = parameters.getParameter(
                    ADAP3PeakDetectionParameters.VALLEY_SPAN).getValue();
    }
    
    @Override
    public String getTaskDescription() {
        return "Peak recognition on " + originalPeakList;
    }
    
    @Override
    public double getFinishedPercentage() {
        return totalRows == 0 ? 0.0 : (double) processedRows
                / (double) totalRows;
    }
    
    @Override
    public void run() {
        if (!isCanceled()) {
            String errorMsg = null;

            setStatus(TaskStatus.PROCESSING);
            LOG.info("Started ADAP-3 Peak Detection on " + originalPeakList);

            // Check raw data files.
            if (originalPeakList.getNumberOfRawDataFiles() > 1) {

                setStatus(TaskStatus.ERROR);
                setErrorMessage("Peak Detection can only be performed on peak lists with a single raw data file");

            } else {
                
                //String[] rPackages = new String[] {"splus2R", "ifultools", "wmtsa"};
                //String[] rPackageVersions = new String[] {"1.2-1", "2.0-4", "2.0-2"};
                String[] rPackages = new String[] {"adap.peak.detection"};
                String[] rPackageVersions = new String[] {"0.0.1"};
                
                this.rSession = new RSessionWrapper(originalPeakList.getName(), 
                        rPackages, rPackageVersions);
                
                try {
                    
                    newPeakList = resolvePeaks(originalPeakList, rSession);
                    
                    
                    if (!isCanceled()) {

                        // Add new peaklist to the project.
                        project.addPeakList(newPeakList);

                        // Add quality parameters to peaks
                        QualityParameters.calculateQualityParameters(newPeakList);

                        // Remove the original peaklist if requested.
                        if (parameters.getParameter(AUTO_REMOVE).getValue()) {
                            project.removePeakList(originalPeakList);
                        }

                        setStatus(TaskStatus.FINISHED);
                        LOG.info("Finished peak recognition on "
                                + originalPeakList);
                    }
                    // Turn off R instance.
                    if (this.rSession != null)
                        this.rSession.close(false);
                    
                } catch (RSessionWrapperException e) {
                    errorMsg = "'R computing error' during peak detection. \n"
                            + e.getMessage();
                } catch (Exception e) {
                    errorMsg = "'Unknown error' during peak detection. \n"
                            + e.getMessage();
                } catch (Throwable t) {

                    setStatus(TaskStatus.ERROR);
                    setErrorMessage(t.getMessage());
                    LOG.log(Level.SEVERE, "Peak deconvolution error", t);
                }

                // Turn off R instance, once task ended UNgracefully.
                try {
                    if (this.rSession != null && !isCanceled())
                        rSession.close(isCanceled());
                } catch (RSessionWrapperException e) {
                    if (!isCanceled()) {
                        // Do not override potential previous error message.
                        if (errorMsg == null) {
                            errorMsg = e.getMessage();
                        }
                    } else {
                        // User canceled: Silent.
                    }
                }

                // Report error.
                if (errorMsg != null) {
                    setErrorMessage(errorMsg);
                    setStatus(TaskStatus.ERROR);
                }
            }
        }
    }
    
    private PeakList resolvePeaks (final PeakList peakList,
            RSessionWrapper rSession) throws RSessionWrapperException 
    {
        final RawDataFile dataFile = peakList.getRawDataFile(0);
        
        // Create new peak list.
        final PeakList resolvedPeakList = new SimplePeakList(peakList + " "
                + parameters.getParameter(SUFFIX).getValue(), dataFile);
        
        // Load previous applied methods.
        for (final PeakList.PeakListAppliedMethod method : 
                peakList.getAppliedMethods()) 
        {
            resolvedPeakList.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peak list.
        resolvedPeakList
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        "Peak detection by ADAP-3", parameters));
        
        // ---------------------------------------------------------
        // Build two vectors with all intensities and all m/z-values
        // ---------------------------------------------------------
        
        final Feature[] chromatograms = peakList.getPeaks(dataFile);
        final int chromatogramCount = chromatograms.length;
        
        if (chromatogramCount == 0)
            throw new IllegalArgumentException("The peak list is empty");
        
        // Get the list of unique scan numbers
        HashSet<Integer> uniqueScans = new HashSet ();
        
        for (Feature chromatogram : chromatograms)
            for (int scanNumber : chromatogram.getScanNumbers())
                uniqueScans.add(scanNumber);
        
        ArrayList<Integer> sortedScans = new ArrayList (uniqueScans);
        java.util.Collections.sort(sortedScans);
        
        int scanCount = sortedScans.size();
        
        // Build m/z- and intensity-vectors 
        double[] intensities = new double[scanCount * chromatogramCount];        
        double[] mzValues = new double[chromatogramCount];
        
        for (int i = 0; i < chromatogramCount; ++i) {
            Feature chromatogram = chromatograms[i];
            mzValues[i] = chromatogram.getMZ();
            
            for (int j = 0; j < scanCount; ++j) {
                int scanNumber = sortedScans.get(j);
                DataPoint dataPoint =  chromatogram.getDataPoint(scanNumber);
                if (dataPoint != null)
                    intensities[i * scanCount + j] = dataPoint.getIntensity();
            }
        }
        
        // ------------------------
        // Call R-function getPeaks
        // ------------------------
        
        rSession.open();
        
        // Load ADAP-3
        //String rPath = "/Users/aleksandrsmirnov/Projects/adap-gc_3";
        //rSession.assign("codeDir", rPath);
        //rSession.eval("source(paste('" + rPath + "', 'pipeline.r', sep='/'))");

        // Parameters
        rSession.eval("params <- list()");
        rSession.eval("params$nNode <- 1");
        rSession.eval("params$WorkDir <- './'");
        rSession.eval("params$Peak_span <- " + peakSpan);
        rSession.eval("params$Valley_span <- " + valleySpan);

        // Intensities
        rSession.assign("intVec", intensities);
        rSession.assign("mzVec", mzValues);
        
        int peakID = 1;
        processedRows = 0;
        totalRows = chromatogramCount;
        
        for (int i = 0; i < chromatogramCount; ++i) {
            //List<ResolvedPeak> resolvedPeaks = new ArrayList();
            
            Feature chromatogram = chromatograms[i];
            
            rSession.assign("mz", chromatogram.getMZ());
            rSession.eval("PeakList <- getPeaks(intVec, params, mz, mzVec)");
            
            
            // -------------------------
            // Read peak picking results
            // -------------------------
            
            final Object leftIndexObject, rightIndexObject, indexObject, 
                    sharpnessObject, signalToNoiseRatioObject, 
                    isSharedObject, offsetObject;
            
            try {
                leftIndexObject = rSession.collect("PeakList$Lbound");
                rightIndexObject = rSession.collect("PeakList$Rbound");
                indexObject = rSession.collect("PeakList$pkInd");
                sharpnessObject = rSession.collect("PeakList$shrp");
                signalToNoiseRatioObject = rSession.collect("PeakList$StN");
                isSharedObject = rSession.collect("PeakList$isShared");
                offsetObject = rSession.collect("PeakList$offset");
            } catch (Exception e) {
                LOG.info("No peaks found for m/z = " + chromatogram.getMZ());
                ++processedRows;
                continue;
            }
            
            //LOG.info(chromatogram.getMZ() + " : " + leftIndexObject + " " + rightIndexObject);
            
            double[] leftIndex = toDoubleArray(leftIndexObject);
            double[] rightIndex = toDoubleArray(rightIndexObject);
            double[] peakIndex = toDoubleArray(indexObject);
            double[] sharpness = toDoubleArray(sharpnessObject);
            double[] signalToNoiseRatio = toDoubleArray(signalToNoiseRatioObject);
            double[] isShared = toDoubleArray(isSharedObject);
            double[] offset = toDoubleArray(offsetObject);
            
            
            if (leftIndex == null || rightIndex == null || peakIndex == null
                    || sharpness == null || signalToNoiseRatio == null
                    || isShared == null || offset == null) 
            {
                LOG.info("No peaks found for m/z = " + chromatogram.getMZ());
                ++processedRows;
                continue;
            }
            
            int length = java.lang.Integer.min(
                    leftIndex.length, rightIndex.length);

            // --------------------------
            // Create MZmine peak objects
            // --------------------------
            
            
            for (int j = 0; j < length; ++j) {
                int left = (int) leftIndex[j];
                int right = (int) rightIndex[j];

                ResolvedPeak peak = new ResolvedPeak(chromatogram, left, right);
                
                //peak.setIndex((int) peakIndex[j]);
                //peak.setSharpness(sharpness[j]);
                //peak.setSignalToNoiseRatio(signalToNoiseRatio[j]);
                //peak.setSharedStatus(1.0 == isShared[j]);
                //peak.setOffset((int) offset[j]);
                
                //SimplePeakIdentity identity = new SimplePeakIdentity(peak.toString());
                SimplePeakIdentity identity = 
                        new SimplePeakIdentity("Single Peak");
                
                identity.setPropertyValue("index", 
                        Integer.toString((int) peakIndex[j]));
                identity.setPropertyValue("sharpness", 
                        Double.toString(sharpness[j]));
                identity.setPropertyValue("signalToNoiseRatio", 
                        Double.toString(signalToNoiseRatio[j]));
                identity.setPropertyValue("isShared", 
                        Boolean.toString(1.0 == isShared[j]));
                identity.setPropertyValue("offset", 
                        Integer.toString((int) offset[j]));
                
                SimplePeakListRow row = new SimplePeakListRow(peakID++);
                             
                row.addPeak(dataFile, peak);
                row.addPeakIdentity(identity, true);
                
                resolvedPeakList.addRow(row);
            }

            ++processedRows;
        }
        
        return resolvedPeakList;
    }
    
    @Override
    public void cancel() {

        super.cancel();
        // Turn off R instance, if already existing.
        try {
            if (this.rSession != null)
                this.rSession.close(true);
        } catch (RSessionWrapperException e) {
            // Silent, always...
        }
    }
    
    double[] toDoubleArray(Object o) {
        double[] result = null;
        
        if (o instanceof double[])
                result = (double[]) o;
        
        else if (o instanceof Double) 
        {
            result = new double[1];
            result[0] = (double) o;
        }
        
        return result;
    }
}
