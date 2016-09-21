/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package net.sf.mzmine.modules.masslistmethods.ADAPchromatogrambuilder;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Ticker;
import com.google.common.base.Stopwatch;
import com.google.common.collect.TreeRangeSet;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

// Owen Edit
import net.sf.mzmine.util.DataPointSorter;
import com.google.common.collect.Range;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.lang.*;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D1;
import ucar.ma2.InvalidRangeException;
//import ucar.ma2.*;
import ucar.ma2.DataType;
import java.io.IOException;
// End Edit

import ucar.nc2.Attribute;


public class ChromatogramBuilderTask extends AbstractTask {

    private TreeMap<Double,Chromatogram> mzToChromMap = new TreeMap<Double,Chromatogram>();

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineProject project;
    private RawDataFile dataFile;

    // scan counter
    private int processedPoints = 0, totalPoints;
    private ScanSelection scanSelection;
    private int newPeakID = 1;
    private Scan[] scans;

    // User parameters
    private String suffix, massListName;
    private MZTolerance mzTolerance;
    private double minimumHeight;
    private int minimumScanSpan;
    // Owen added User parameers;
    private double noise;
    private double minIntensityForStartChrom;

    private SimplePeakList newPeakList;


    /**
     * @param dataFile
     * @param parameters
     */
    public ChromatogramBuilderTask(MZmineProject project, RawDataFile dataFile,
            ParameterSet parameters) {



        this.project = project;
        this.dataFile = dataFile;
        this.scanSelection = parameters
                .getParameter(ChromatogramBuilderParameters.scanSelection)
                .getValue();
        this.massListName = parameters
                .getParameter(ChromatogramBuilderParameters.massList)
                .getValue();

        this.mzTolerance = parameters
                .getParameter(ChromatogramBuilderParameters.mzTolerance)
                .getValue();
        this.minimumScanSpan = parameters
                .getParameter(ChromatogramBuilderParameters.minimumScanSpan)
                .getValue().intValue();
        //this.minimumHeight = parameters
        //        .getParameter(ChromatogramBuilderParameters.minimumHeight)
        //        .getValue();

        this.suffix = parameters
                .getParameter(ChromatogramBuilderParameters.suffix).getValue();

        // Owen added parameters
        this.noise = parameters
                .getParameter(ChromatogramBuilderParameters.noise)
                .getValue();
        this.minIntensityForStartChrom = parameters
                .getParameter(ChromatogramBuilderParameters.startIntensity)
                .getValue();


    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Detecting chromatograms in " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalPoints == 0)
            return 0;
        else
            return (double) processedPoints/ totalPoints;
    }

    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {
        boolean writeChromCDF = true;

        setStatus(TaskStatus.PROCESSING);

        logger.info("Started chromatogram builder on " + dataFile);

        scans = scanSelection.getMatchingScans(dataFile);
        int allScanNumbers[] = scanSelection.getMatchingScanNumbers(dataFile);

        List<Double> rtListForChromCDF = new ArrayList<Double>();

        // Check if the scans are properly ordered by RT
        double prevRT = Double.NEGATIVE_INFINITY;
        for (Scan s : scans) {

            if (writeChromCDF){
                rtListForChromCDF.add(s.getRetentionTime());
            }

            if (s.getRetentionTime() < prevRT) {
                setStatus(TaskStatus.ERROR);
                final String msg = "Retention time of scan #"
                        + s.getScanNumber()
                        + " is smaller then the retention time of the previous scan."
                        + " Please make sure you only use scans with increasing retention times."
                        + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
                setErrorMessage(msg);
                return;
            }
            prevRT = s.getRetentionTime();
        }


        // Create new peak list
        newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

        // make a list of all the data points
        // sort data points by intensity
        // loop through list 
        //      add data point to chromatogrm or make new one
        //      update mz avg and other stuff
        // 

        
        // make a list of all the data points
        List<ExpandedDataPoint> allMzValues = new ArrayList<ExpandedDataPoint>();

        for (Scan scan : scans) {
            if (isCanceled())
                return;

            MassList massList = scan.getMassList(massListName);
            if (massList == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
                        + " does not have a mass list " + massListName);
                return;
            }

            DataPoint mzValues[] = massList.getDataPoints();

            if (mzValues == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Mass list " + massListName
                        + " does not contain m/z values for scan #"
                        + scan.getScanNumber() + " of file " + dataFile);
                return;
            }

            for (DataPoint mzPeak : mzValues){
                ExpandedDataPoint curDatP = new ExpandedDataPoint(mzPeak, scan.getScanNumber());
                allMzValues.add(curDatP );
                //corespondingScanNum.add(scan.getScanNumber());
            }

        }

        //Integer[] simpleCorespondingScanNums = new Integer[corespondingScanNum.size()];
        //corespondingScanNum.toArray(simpleCorespondingScanNums );

        ExpandedDataPoint[] simpleAllMzVals = new ExpandedDataPoint[allMzValues.size()];
        allMzValues.toArray(simpleAllMzVals);

        // sort data points by intensity
        Arrays.sort(simpleAllMzVals, new DataPointSorter(SortingProperty.Intensity,
                SortingDirection.Descending));


        //Set<Chromatogram> buildingChromatograms;
        //buildingChromatograms = new LinkedHashSet<Chromatogram>();



        double maxIntensity = simpleAllMzVals[0].getIntensity();

        // count starts at 1 since we already have added one with a single point.

        //Stopwatch stopwatch = Stopwatch.createUnstarted(); 
        // stopwatch2 = Stopwatch.createUnstarted(); 
        //Stopwatch stopwatch3 = Stopwatch.createUnstarted(); 


        totalPoints = simpleAllMzVals.length;

        for (ExpandedDataPoint mzPeak : simpleAllMzVals){


            if (mzPeak.getIntensity()<=noise){
                //System.out.println("continue here 1");
                continue;
            }
            if (mzPeak==null){
                //System.out.println("null Peak");
                continue;
            }


            Double currentMZ = new Double(mzPeak.getMZ());


            
            boolean makeNewChrom = false;
            boolean checkFloorChrom = false;
            boolean checkCeilingChrom = false;


            // find the floor and celing key values in the map given an mz value. 
            // check each to see whick is closer
            Double ceilingMZ = mzToChromMap.ceilingKey(currentMZ);
            Double floorMZ = mzToChromMap.floorKey(currentMZ);
            Double ToAddToMZ = new Double(-1);



            if ((ceilingMZ==null)&&(floorMZ == null)){
                System.out.println("ceiling and floor are null");
                makeNewChrom = true;
            }
            else if (ceilingMZ == null){
                checkFloorChrom = true;
            }
            else if (floorMZ == null){
                checkCeilingChrom = true;
            }
            else if (floorMZ == ceilingMZ){
                checkCeilingChrom = true;
            }
            else {
                checkCeilingChrom = true;
                checkFloorChrom = true;
            }

            boolean inCeiling = false;
            boolean inFloor = false;
            if (checkCeilingChrom){


                //stopwatch.start();

                Chromatogram curChrom = mzToChromMap.get(ceilingMZ);
                Range<Double> toleranceRange =  mzTolerance.getToleranceRange(curChrom.getHighPointMZ());

                if (toleranceRange.contains(mzPeak.getMZ())){

                    curChrom.addMzPeak(mzPeak.getScanNumber(),mzPeak);
                    mzToChromMap.put(ceilingMZ,curChrom); 
                    inCeiling = true;
                }

                //stopwatch.stop();
            }
            if (checkFloorChrom){


                //stopwatch.start();
                Chromatogram curChrom = mzToChromMap.get(floorMZ);
                Range<Double> toleranceRange =  mzTolerance.getToleranceRange(curChrom.getHighPointMZ());

                if (toleranceRange.contains(mzPeak.getMZ())){

                    curChrom.addMzPeak(mzPeak.getScanNumber(),mzPeak);
                    mzToChromMap.put(floorMZ,curChrom); 
                    inFloor = true;
                }

                //stopwatch.stop();
            }
            if ((!inFloor)&&(!inCeiling)){
                makeNewChrom = true;
            }


            if (makeNewChrom){

                // See if the curent data point is large enough to warrent making a new
                // chromatogram.
                if (mzPeak.getIntensity()>(minIntensityForStartChrom)){

                   // stopwatch2.start();

                    Chromatogram newChrom = new Chromatogram(dataFile, allScanNumbers);

                    newChrom.addMzPeak(mzPeak.getScanNumber(),mzPeak);

                    newChrom.setHighPointMZ(mzPeak.getMZ());


                    mzToChromMap.put(currentMZ,newChrom);


                   // stopwatch2.stop();
                }
            }


            processedPoints+=1;



        }

        //System.out.println("search chroms (ms): " +  stopwatch.elapsed(TimeUnit.MILLISECONDS));
        //System.out.println("making new chrom (ms): " +  stopwatch2.elapsed(TimeUnit.MILLISECONDS));

        // finish chromatograms
        Iterator<Double> MzIterator = mzToChromMap.keySet().iterator();

        List<Chromatogram> buildingChromatograms = new ArrayList<Chromatogram>();

        List<Double> keyMZListForCDFWrite = new ArrayList<Double>();
        List<Double> intensityListForCDFWrite = new ArrayList<Double>();
        while (MzIterator.hasNext()) {

            Double curMZKey = MzIterator.next();

            Chromatogram chromatogram = mzToChromMap.get(curMZKey);

            if (writeChromCDF){
                keyMZListForCDFWrite.add(curMZKey);
                List<Double> curChromIntensities = new ArrayList<Double>();
                curChromIntensities = chromatogram.getIntensitiesForCDFOut();
                intensityListForCDFWrite.addAll(curChromIntensities);
            }

            chromatogram.finishChromatogram();

            // Remove chromatograms smaller then minimum height
            // And remove chromatograms who dont have a certian number of continous points above the
            // noise level.
            double numberOfContinuousPointsAboveNoise = chromatogram.findNumberOfContinuousPointsAboveNoise(noise);
            if (chromatogram.getHeight() < minimumHeight){
                continue;
            }
            else if (numberOfContinuousPointsAboveNoise < minimumScanSpan) {
                System.out.println("skipping chromatogram because it does not meet the min point scan requirements");
                System.out.println("curMZKey");
                System.out.println(curMZKey);
                continue;
            }
            else{
                buildingChromatograms.add(chromatogram);
            }

        }

        Chromatogram[] chromatograms = buildingChromatograms.toArray(new Chromatogram[0]);
       

        // Sort the final chromatograms by m/z
        Arrays.sort(chromatograms,
                new PeakSorter(SortingProperty.MZ, SortingDirection.Ascending));


        // Add the chromatograms to the new peak list
        for (Feature finishedPeak : chromatograms) {
            SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
            newPeakID++;
            newRow.addPeak(dataFile, finishedPeak);
            newPeakList.addRow(newRow);

            finishedPeak.outputChromToFile();
        }

        // Add new peaklist to the project
        project.addPeakList(newPeakList);

        // Add quality parameters to peaks
        QualityParameters.calculateQualityParameters(newPeakList);

        setStatus(TaskStatus.FINISHED);


        if (writeChromCDF){

            NetcdfFileWriter writer = null;

            try{
                String fileName = dataFile.getName();

                String fileNameNoExtention = fileName.split("\\.")[0];

                

                writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileNameNoExtention+".CDF" , null);
                // each chromatogram will use a single mass value so this must have lenght equal to the
                // number of chromatograms -> just the masss keys i.e. hights point's mz in chromatogram
                Dimension dim_massValues = writer.addDimension(null, "mzVec", keyMZListForCDFWrite.size());
                // Full lenght chromatograms so this is just the full list of alll the scans RT vals
                Dimension dim_rtValues = writer.addDimension(null, "scan_acquisition_time", rtListForChromCDF.size());
                // inesity profiles for all chromatograms one after the other. Full RT length so use the
                // length of the above array to parse them corectly
                Dimension dim_intensityValues = writer.addDimension(null, "intVec", intensityListForCDFWrite.size());

                List<Dimension> dims = new ArrayList<>();
                dims.add(dim_massValues);
                dims.add(dim_rtValues );
                dims.add(dim_intensityValues);

                // make the variables that contain the actual data I think.
                Variable var_massValues = writer.addVariable(null, "mzVec", DataType.DOUBLE, "mzVec");
                Variable var_rtValues       = writer.addVariable(null, "scan_acquisition_time", DataType.DOUBLE, "scan_acquisition_time");
                Variable var_intensityValues= writer.addVariable(null, "intVec", DataType.DOUBLE, "intVec");

                // create file
                writer.create();

                ArrayDouble.D1  arr_massValues = new ArrayDouble.D1(dim_massValues     .getLength()); 
                ArrayDouble.D1  arr_rtValues  = new ArrayDouble.D1(dim_rtValues       .getLength()); 
                ArrayDouble.D1  arr_intensityValues= new ArrayDouble.D1(dim_intensityValues.getLength()); 

                for (int i = 0; i<keyMZListForCDFWrite.size();i++){
                    arr_massValues.set(i,keyMZListForCDFWrite.get(i));
                }
                for (int i = 0; i<rtListForChromCDF.size();i++){
                    arr_rtValues.set(i,rtListForChromCDF.get(i));
                }
                for (int i = 0; i<keyMZListForCDFWrite.size();i++){
                    arr_intensityValues.set(i,intensityListForCDFWrite.get(i));
                }
                writer.write(var_massValues     ,arr_massValues     );
                writer.write(var_rtValues       ,arr_rtValues       );
                writer.write(var_intensityValues,arr_intensityValues);
            }
            
            catch (IOException e){
                e.printStackTrace();
            }
            catch (InvalidRangeException e) {
            e.printStackTrace();
            } 

            finally {
                if (null != writer){
                  try {
                    writer.close();
                  } 
                  catch (IOException ioe) {
                    ioe.printStackTrace();
                  }
                }
            }
        }
        
        System.out.println("System.getProperty(java.class.path)");
        System.out.println(System.getProperty("java.class.path"));
        
        logger.info("Finished chromatogram builder on " + dataFile);
    }

}
