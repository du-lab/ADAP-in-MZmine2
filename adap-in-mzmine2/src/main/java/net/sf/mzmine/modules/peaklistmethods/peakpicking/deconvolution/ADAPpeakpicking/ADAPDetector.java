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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking;

//import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.INTEGRATION_METHOD;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking.ADAPDetectorParameters.PEAK_DURATION;
//import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.PEAK_SCALES;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking.ADAPDetectorParameters.SN_THRESHOLD;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking.ADAPDetectorParameters.MIN_FEAT_HEIGHT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
//import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.PeakIntegrationMethod;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import com.google.common.collect.Range;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking.UseMsconvertCWT.tryCallingCppFindPeaks;
 

/**
 * Use XCMS findPeaks.centWave to identify peaks.
 */
public class ADAPDetector implements PeakResolver {

    // Logger.
    private static final Logger LOG = Logger.getLogger(ADAPDetector.class
            .getName());

    // Name.
    private static final String NAME = "Wavelets (ADAP)";

    // Minutes <-> seconds.
    private static final double SECONDS_PER_MINUTE = 60.0;


    @Nonnull
    @Override
    public String getName() {

        return NAME;
    }

    @Nonnull
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {

        return ADAPDetectorParameters.class;
    }

    @Override
    public String[] getRequiredRPackagesVersions() {
        return null;
    }
        @Override
    public boolean getRequiresR() {
        return false;
    }

    @Override
    public String[] getRequiredRPackages() {
        return null;
    }

    @Override
    public Feature[] resolvePeaks(final Feature chromatogram,
            final ParameterSet parameters,
            RSessionWrapper rSession) throws RSessionWrapperException {
        
        int scanNumbers[] = chromatogram.getScanNumbers();
        final int scanCount = scanNumbers.length;
        double retentionTimes[] = new double[scanCount];
        double intensities[] = new double[scanCount];
        RawDataFile dataFile = chromatogram.getDataFile();
        for (int i = 0; i < scanCount; i++) {
            final int scanNum = scanNumbers[i];
            retentionTimes[i] = dataFile.getScan(scanNum).getRetentionTime();
            DataPoint dp = chromatogram.getDataPoint(scanNum);
            if (dp != null)
                intensities[i] = dp.getIntensity();
            else
                intensities[i] = 0.0;
        }
        
        // Call findPeaks.centWave.
        double[][] boundsMatrix = null;
        
        final Range<Double> peakDuration = parameters.getParameter(
        PEAK_DURATION).getValue();

        //                parameters.getParameter(PEAK_SCALES).getValue(), 
        //                parameters.getParameter(INTEGRATION_METHOD).getValue(),
        boundsMatrix = ADAPPeakPicking(rSession, retentionTimes, intensities,
                chromatogram.getMZ(), 
                parameters.getParameter(SN_THRESHOLD).getValue(), 
                parameters.getParameter(MIN_FEAT_HEIGHT).getValue(),
                        peakDuration);

        final List<ResolvedPeak> resolvedPeaks;

        if ((boundsMatrix == null)||
                ((((int)boundsMatrix[0][0]==0)&&((int)boundsMatrix[1][0]==0))&&(boundsMatrix[0].length==1))){
            resolvedPeaks = new ArrayList<ResolvedPeak>(0);

        } else {

            LOG.finest("Processing peak matrix...");



            // Process peak matrix.
            resolvedPeaks = new ArrayList<ResolvedPeak>(boundsMatrix[0].length);

            // The old way could detect the same peak more than once if the wavlet scales were too large.
            // If the left bounds were the same and there was a null point before the right bounds it would
            //make the same peak twice.
            // To avoid the above see if the peak duration range is met before going into
            // the loop
            
            //for (final double[] peakRow : peakMatrix) {
            for (int i = 0; i < boundsMatrix[0].length;i++) {

                // Get peak start and end.
                //final int peakLeft = findRTIndex(retentionTimes, boundsMatrix[0][i]);
                //final int peakRight = findRTIndex(retentionTimes, boundsMatrix[1][i]);
                final int peakLeft = (int) boundsMatrix[0][i];
                final int peakRight = (int) boundsMatrix[1][i];
                if ((peakLeft==0)&&(peakRight==0)){
                    continue;
                }
                
                // The old way could detect the same peak more than once if the wavlet scales were too large.
                // If the left bounds were the same and there was a null point before the right bounds it would
                //make the same peak twice.
                // To avoid the above see if the peak duration range is met before going into
                // the loop
                double retentionTimeRight = retentionTimes[peakRight];
                double retentionTimeLeft = retentionTimes[peakLeft];
                if(! peakDuration.contains(retentionTimeRight- retentionTimeLeft))
                {
                    continue;
                }
                resolvedPeaks.add(new ResolvedPeak(chromatogram,peakLeft, peakRight));


//                // Partition into sections bounded by null data points, creating
//                // a peak for each.
//                for (int start = peakLeft; start < peakRight; start++) {
//
//                    if (chromatogram.getDataPoint(scanNumbers[start]) != null) {
//
//                        int end = start;
//                        
//                        while (end < peakRight
//                                && chromatogram
//                                        .getDataPoint(scanNumbers[end + 1]) != null) {
//
//                            end++;
//                        }
//                       
//
//                        if ((end > start)&&( peakDuration.contains(retentionTimes[end]- retentionTimes[start]))) {
//                       
//
//                            resolvedPeaks.add(new ResolvedPeak(chromatogram,
//                                    start, end));
//                        }
//
//                        start = end;
//                    }
//                }
            }
        }

        return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
    }

    private static int findRTIndex(final double[] rtMinutes, final double rtSec) {

        final int i = Arrays
                .binarySearch(rtMinutes, rtSec / SECONDS_PER_MINUTE);
        return i >= 0 ? i : -i - 2;
    }

    /**
     *
     * 
     * @param scanTime
     *            retention times (for each scan).
     * @param intensity
     *            intensity values (for each scan).
     * @param mz
     *            fixed m/z value for EIC.
     * @param snrThreshold
     *            signal:noise ratio threshold.
     * @param peakWidth
     *            peak width range.
     * @param integrationMethod
     *            integration method.
     * @return a matrix with a row for each detected peak.
     * @throws RSessionWrapperException
     */


   private double[][] ADAPPeakPicking(RSessionWrapper rSession,
            final double[] scanTime, final double[] intensity, final double mz,
            final double snrThreshold,
            final double minimumFeatHeight,
            final Range<Double> peakWidth)
            throws RSessionWrapperException {
       


        LOG.finest("Detecting peaks.");

        final int[][] newPeaks;
        newPeaks =  tryCallingCppFindPeaks(intensity,scanTime,snrThreshold,peakWidth.lowerEndpoint());
        

        // Check and make sure the given features abolute height is large enough
        List <Double> doneLBound = new ArrayList<Double>();
        List <Double> doneRBound = new ArrayList<Double>();
        
        for (int i = 0; i<intensity.length; i++){
            int curLBound = newPeaks[0][i];
            int curRBound = newPeaks[1][i];
            if ((curLBound==0)&&(curRBound==0)){
                doneLBound.add(new Double(0));
                doneRBound.add(new Double(0));
            }
            else{
                double highestPoint = 0;
                System.out.println("left Bound: ");
                System.out.println(curLBound);
                System.out.println("right Bound: ");
                System.out.println(curRBound);
                for(int alpha = curLBound; alpha<curRBound; alpha++){
                    if (intensity[alpha]>=highestPoint){
                        highestPoint = intensity[alpha];
                    }
                }
                if(highestPoint>= minimumFeatHeight){
                    doneLBound.add(new Double(curLBound));
                    doneRBound.add(new Double(curRBound));
                }
            }
            
        }
        
        System.out.println("peakWidth.lowerEndpoint()");
        System.out.println(peakWidth.lowerEndpoint());
        
        final double[][] donePeaks = new double[2][doneLBound.size()];
        for (int i = 0; i<doneLBound.size(); i++){
            donePeaks[0][i] = doneLBound.get(i);
            donePeaks[1][i] = doneRBound.get(i);   
        }
        return donePeaks;



    }
}