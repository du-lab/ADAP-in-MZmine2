/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.alignment.adap3;

import adap.common.algorithms.machineleanring.OptimizationParameters;
import adap.datamodel.Component;
import adap.datamodel.Peak;
import adap.datamodel.PeakInfo;
import adap.datamodel.Project;
import adap.datamodel.ReferenceComponent;
import adap.datamodel.Sample;
import adap.workflow.AlignmentParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerTask extends AbstractTask {
    
    private static final Logger LOG = Logger.getLogger(
            ADAP3AlignerTask.class.getName());
    
    private final MZmineProject project;
    private final ParameterSet parameters;
    
    private final PeakList[] peakLists;
    
    private final String peakListName;
    
    private Project alignment;
    
    public ADAP3AlignerTask(MZmineProject project, ParameterSet parameters) {
        
        this.project = project;
        this.parameters = parameters;
        
        this.peakLists = parameters.getParameter(
                ADAP3AlignerParameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();
        
        this.peakListName = parameters.getParameter(
                ADAP3AlignerParameters.NEW_PEAK_LIST_NAME).getValue();
        
        this.alignment = new Project();
    }
    
    @Override
    public String getTaskDescription() {
        return "ADAP3 Aligner, " + peakListName + " (" + peakLists.length
                + " peak lists)";
    }

    @Override
    public double getFinishedPercentage() {
        return alignment.getProcessedPercent();
    }
    
    @Override
    public void run() {
        
        if (!isCanceled()) {
            String errorMsg = null;
            
            setStatus(TaskStatus.PROCESSING);
            LOG.info("Started ADAP-3 Peak Alignment");
            
            try {
                PeakList peakList = alignPeaks(peakLists);

                if (!isCanceled()) {
                    project.addPeakList(peakList);

                    QualityParameters.calculateQualityParameters(peakList);

                    setStatus(TaskStatus.FINISHED);
                    LOG.info("Finished ADAP-3 Peak Alignment");
                }
            } catch (IllegalArgumentException e) {
                errorMsg = "Incorrect Peak Lists:\n" + e.getMessage();
            } catch (Exception e) {
                errorMsg = "'Unknown error' during alignment. \n"
                            + e.getMessage();
            } catch (Throwable t) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage(t.getMessage());
                LOG.log(Level.SEVERE, "ADAP-3 Alignment error", t);
            }
            
            // Report error
            if (errorMsg != null) {
                setErrorMessage(errorMsg);
                setStatus(TaskStatus.ERROR);
            }
        }
    }
    
    private PeakList alignPeaks(PeakList[] peakLists) {
        
        // Collect all data files

        List <RawDataFile> allDataFiles = new ArrayList <> (peakLists.length);
        
        for (final PeakList peakList : peakLists) {
            RawDataFile[] dataFiles = peakList.getRawDataFiles();
            if (dataFiles.length != 1)
                throw new IllegalArgumentException("Found more then one data "
                        + "file in some of the peaks lists");
        
            allDataFiles.add(dataFiles[0]);
        }
        
        // Perform alignment
        
        for (final PeakList peakList : peakLists) {
            
            Sample sample = new Sample(peakList.hashCode());
            
            for (final PeakListRow row : peakList.getRows())
                sample.addComponent(getComponent(row));
            
            alignment.addSample(sample);
        }
        
        process(alignment);
        
        // Create new peak list
        final PeakList alignedPeakList = new SimplePeakList(peakListName,
                allDataFiles.toArray(new RawDataFile[allDataFiles.size()]));
        
        int rowID = 0;
        
        List <ReferenceComponent> alignedComponents = alignment.getComponents();
        
        Collections.sort(alignedComponents);
        
        for (final ReferenceComponent component : alignedComponents) {
            final Peak peak = component.getBestPeak();
            
            PeakListRow refRow = findRow(peakLists, component.getSampleID(), 
                    peak.getInfo().peakID);
            
            PeakListRow newRow = new SimplePeakListRow(++rowID);
            
            newRow.addPeak(refRow.getRawDataFiles()[0], refRow.getBestPeak());
            
            for (int i = 0; i < component.size(); ++i) {
                PeakListRow row = findRow(peakLists, 
                        component.getSampleID(i), 
                        component.getComponent(i).getBestPeak().getInfo().peakID);
                
                newRow.addPeak(row.getRawDataFiles()[0], row.getBestPeak());
            }
            
            SimplePeakIdentity identity = new SimplePeakIdentity(
                    new Hashtable <> (refRow.getPreferredPeakIdentity()
                            .getAllProperties()));
            
            String strScore = Double.toString(component.getScore());
            
            identity.setPropertyValue("Name", "Aligned Peaks (score=" + strScore + ")");
            identity.setPropertyValue("Alignment Score", strScore);
            
            newRow.addPeakIdentity(identity, true);
            
            alignedPeakList.addRow(newRow);
            
        }
        
        return alignedPeakList;
    }
    
    private Component getComponent(final PeakListRow row) {
        final PeakIdentity identity = row.getPreferredPeakIdentity();
        
        if (row.getNumberOfPeaks() == 0)
            throw new IllegalArgumentException("No peaks found");
        
        // Read Spectrum information        
        NavigableMap <Double, Double> spectrum = new TreeMap <> ();
        for (DataPoint dataPoint : row.getBestIsotopePattern().getDataPoints())
            spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());
        
        // Read Chromatogram
        final Feature peak = row.getBestPeak();
        final RawDataFile dataFile = peak.getDataFile();
        
        NavigableMap <Double, Double> chromatogram = new TreeMap <> ();
        
        for (final int scan : peak.getScanNumbers()) {
            final DataPoint dataPoint = peak.getDataPoint(scan);
            if (dataPoint != null)
                chromatogram.put(dataFile.getScan(scan).getRetentionTime(), 
                        dataPoint.getIntensity());
        }
        
        return new Component(null, 
                new Peak(chromatogram, new PeakInfo()
                        .mzValue(peak.getMZ())
                        .peakID(row.hashCode())),
                spectrum, null);
    }
    
    private void process(Project alignment) 
    {
        AlignmentParameters params = new AlignmentParameters()
                .sampleCountRatio(parameters.getParameter(
                        ADAP3AlignerParameters.SAMPLE_COUNT_RATIO).getValue())
                .retTimeRange(parameters.getParameter(
                        ADAP3AlignerParameters.RET_TIME_RANGE).getValue())
                .mzRange(parameters.getParameter(
                        ADAP3AlignerParameters.MZ_RANGE).getValue())
                .scoreTolerance(parameters.getParameter(
                        ADAP3AlignerParameters.SCORE_TOLERANCE).getValue())
                .scoreWeight(parameters.getParameter(
                        ADAP3AlignerParameters.SCORE_WEIGHT).getValue())
                .maxShift(parameters.getParameter(
                        ADAP3AlignerParameters.MAX_SHIFT).getValue());
        
        ParameterSet optim = parameters.getParameter(
                        ADAP3AlignerParameters.OPTIM_PARAMS).getValue();
        
        params.optimizationParameters = new OptimizationParameters()
                .gradientTolerance(optim.getParameter(
                        ADAP3AlignerOptimizationParameters.GRADIENT_TOLERANCE).getValue())
                .alpha(optim.getParameter(
                        ADAP3AlignerOptimizationParameters.ALPHA).getValue())
                .maxIterationCount(optim.getParameter(
                        ADAP3AlignerOptimizationParameters.MAX_ITERATION).getValue())
                .verbose(optim.getParameter(
                        ADAP3AlignerOptimizationParameters.VERBOSE).getValue());
        
        alignment.alignSamples(params);
    }
    
    private PeakListRow findRow(final PeakList[] peakLists, 
            final int listID, final int peakID)
    {
        // Find peak list

        PeakList peakList = null;            
        for (final PeakList list : peakLists)
            if (listID == list.hashCode()) {
                peakList = list;
                break;
            }

        if (peakList == null) return null;
        
        // Find row
        
        PeakListRow row = null;
        for (final PeakListRow r : peakList.getRows())
            if (peakID == r.hashCode()) {
                row = r;
                break;
            }
        
        return row;
    }
}
