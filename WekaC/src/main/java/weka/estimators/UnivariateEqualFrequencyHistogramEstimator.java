/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    UnivariateEqualFrequencyEstimator.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.estimators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import weka.core.RevisionUtils;
import weka.core.Statistics;
import weka.core.Utils;

/**
 * Simple histogram density estimator. Uses equal-frequency histograms based on
 * the specified number of bins (default: 10).
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11318 $
 */
public class UnivariateEqualFrequencyHistogramEstimator implements
  UnivariateDensityEstimator, UnivariateIntervalEstimator,
  UnivariateQuantileEstimator, Serializable {

  /** For serialization */
  private static final long serialVersionUID = -3180287591539683137L;

  /** The collection used to store the weighted values. */
  protected TreeMap<Float, Float> m_TM = new TreeMap<Float, Float>();

  /** The interval boundaries. */
  protected float[] m_Boundaries = null;

  /** The weight of each interval. */
  protected float[] m_Weights = null;

  /** The weighted sum of values */
  protected float m_WeightedSum = 0;

  /** The weighted sum of squared values */
  protected float m_WeightedSumSquared = 0;

  /** The total sum of weights. */
  protected float m_SumOfWeights = 0;

  /** The number of bins to use. */
  protected int m_NumBins = 10;

  /** The current bandwidth (only computed when needed) */
  protected float m_Width = Float.MAX_VALUE;

  /** The exponent to use in computation of bandwidth (default: -0.25) */
  protected float m_Exponent = -0.25f;

  /** The minimum allowed value of the kernel width (default: 1.0E-6) */
  protected float m_MinWidth = 1.0E-6f;

  /** Constant for Gaussian density. */
  public static final float CONST = (float) (-0.5f * Math.log(2 * Math.PI));

  /** The number of intervals used to approximate prediction interval. */
  protected int m_NumIntervals = 1000;

  /** Whether boundaries are updated or only weights. */
  protected boolean m_UpdateWeightsOnly = false;

  /**
   * Returns a string describing the estimator.
   */
  public String globalInfo() {
    return "Provides a univariate histogram estimator based on equal-frequency bins.";
  }
  /**
   * Gets the number of bins
   * 
   * @return the number of bins.
   */
  public int getNumBins() {

    return m_NumBins;
  }

  /**
   * Sets the number of bins
   * 
   * @param numBins the number of bins
   */
  public void setNumBins(int numBins) {

    m_NumBins = numBins;
  }

  /**
   * Triggers construction of estimator based on current data and then
   * initializes the statistics.
   */
  public void initializeStatistics() {

    updateBoundariesAndOrWeights();

    m_TM = new TreeMap<Float, Float>();
    m_WeightedSum = 0;
    m_WeightedSumSquared = 0;
    m_SumOfWeights = 0;
    m_Weights = null;
  }

  /**
   * Sets whether only weights should be udpated.
   */
  public void setUpdateWeightsOnly(boolean flag) {

    m_UpdateWeightsOnly = flag;
  }

  /**
   * Gets whether only weights should be udpated.*
   */
  public boolean getUpdateWeightsOnly() {

    return m_UpdateWeightsOnly;
  }

  /**
   * Adds a value to the density estimator.
   * 
   * @param value the value to add
   * @param weight the weight of the value
   */
  @Override
  public void addValue(float value, float weight) {

    // Add data point to collection
    m_WeightedSum += value * weight;
    m_WeightedSumSquared += value * value * weight;
    m_SumOfWeights += weight;
    if (m_TM.get(value) == null) {
      m_TM.put(value, weight);
    } else {
      m_TM.put(value, m_TM.get(value) + weight);
    }

    // Make sure estimator is updated
    if (!getUpdateWeightsOnly()) {
      m_Boundaries = null;
    }
    m_Weights = null;
  }

  /**
   * Updates the boundaries if necessary.
   */
  protected void updateBoundariesAndOrWeights() {

    // Do we need to update?
    if (m_Weights != null) {
      return;
    }

    // Update widths for cases that are out of bounds,
    // using same code as in kernel estimator

    // First, compute variance for scaling
    float mean = m_WeightedSum / m_SumOfWeights;
    float variance = m_WeightedSumSquared / m_SumOfWeights - mean * mean;
    if (variance < 0) {
      variance = 0;
    }

    // Compute kernel bandwidth
    m_Width = (float) (Math.sqrt(variance) * Math.pow(m_SumOfWeights, m_Exponent));

    if (m_Width <= m_MinWidth) {
      m_Width = m_MinWidth;
    }

    // Do we need to update weights only
    if (getUpdateWeightsOnly()) {
      updateWeightsOnly();
    } else {
      updateBoundariesAndWeights();
    }
  }

  /**
   * Updates the weights only.
   */
  protected void updateWeightsOnly() throws IllegalArgumentException {

    // Get values and keys from tree map
    Iterator<Map.Entry<Float, Float>> itr = m_TM.entrySet().iterator();
    int j = 1;
    m_Weights = new float[m_Boundaries.length - 1];
    while (itr.hasNext()) {
      Map.Entry<Float, Float> entry = itr.next();
      float value = entry.getKey();
      float weight = entry.getValue();
      if ((value < m_Boundaries[0])
        || (value > m_Boundaries[m_Boundaries.length - 1])) {
        throw new IllegalArgumentException(
          "Out-of-range value during weight update");
      }
      while (value > m_Boundaries[j]) {
        j++;
      }
      m_Weights[j - 1] += weight;
    }
  }

  /**
   * Updates the boundaries and weights.
   */
  protected void updateBoundariesAndWeights() {

    // Get values and keys from tree map
    float[] values = new float[m_TM.size()];
    float[] weights = new float[m_TM.size()];
    Iterator<Map.Entry<Float, Float>> itr = m_TM.entrySet().iterator();
    int j = 0;
    while (itr.hasNext()) {
      Map.Entry<Float, Float> entry = itr.next();
      values[j] = entry.getKey();
      weights[j] = entry.getValue();
      j++;
    }

    float freq = m_SumOfWeights / m_NumBins;
    float[] cutPoints = new float[m_NumBins - 1];
    float[] binWeights = new float[m_NumBins];
    float sumOfWeights = m_SumOfWeights;

    // Compute break points
    float weightSumSoFar = 0, lastWeightSum = 0;
    int cpindex = 0, lastIndex = -1;
    for (int i = 0; i < values.length - 1; i++) {

      // Update weight statistics
      weightSumSoFar += weights[i];
      sumOfWeights -= weights[i];

      // Have we passed the ideal size?
      if (weightSumSoFar >= freq) {

        // Is this break point worse than the last one?
        if (((freq - lastWeightSum) < (weightSumSoFar - freq))
          && (lastIndex != -1)) {
          cutPoints[cpindex] = (values[lastIndex] + values[lastIndex + 1]) / 2;
          weightSumSoFar -= lastWeightSum;
          binWeights[cpindex] = lastWeightSum;
          lastWeightSum = weightSumSoFar;
          lastIndex = i;
        } else {
          cutPoints[cpindex] = (values[i] + values[i + 1]) / 2;
          binWeights[cpindex] = weightSumSoFar;
          weightSumSoFar = 0;
          lastWeightSum = 0;
          lastIndex = -1;
        }
        cpindex++;
        freq = (sumOfWeights + weightSumSoFar)
          / ((cutPoints.length + 1) - cpindex);
      } else {
        lastIndex = i;
        lastWeightSum = weightSumSoFar;
      }
    }

    // Check whether there was another possibility for a cut point
    if ((cpindex < cutPoints.length) && (lastIndex != -1)) {
      cutPoints[cpindex] = (values[lastIndex] + values[lastIndex + 1]) / 2;
      binWeights[cpindex] = lastWeightSum;
      cpindex++;
      binWeights[cpindex] = weightSumSoFar - lastWeightSum;
    } else {
      binWeights[cpindex] = weightSumSoFar;
    }

    // Did we find any cutpoints?
    if (cpindex == 0) {
      m_Boundaries = null;
      m_Weights = null;
    } else {

      // Need to add weight of last data point to right-most bin
      binWeights[cpindex] += weights[values.length - 1];

      // Copy over boundaries and weights
      m_Boundaries = new float[cpindex + 2];
      m_Boundaries[0] = m_TM.firstKey();
      m_Boundaries[cpindex + 1] = m_TM.lastKey();
      System.arraycopy(cutPoints, 0, m_Boundaries, 1, cpindex);
      m_Weights = new float[cpindex + 1];
      System.arraycopy(binWeights, 0, m_Weights, 0, cpindex + 1);
    }
  }

  /**
   * Returns the interval for the given confidence value.
   * 
   * @param conf the confidence value in the interval [0, 1]
   * @return the interval
   */
  @Override
  public float[][] predictIntervals(float conf) {

    // Update the bandwidth
    updateBoundariesAndOrWeights();

    // Compute minimum and maximum value, and delta
    float val = Statistics.normalInverse(1.0f - (1.0f - conf) / 2);
    float min = m_TM.firstKey() - val * m_Width;
    float max = m_TM.lastKey() + val * m_Width;
    float delta = (max - min) / m_NumIntervals;

    // Create array with estimated probabilities
    float[] probabilities = new float[m_NumIntervals];
    float leftVal = (float) Math.exp(logDensity(min));
    for (int i = 0; i < m_NumIntervals; i++) {
      float rightVal = (float) Math.exp(logDensity(min + (i + 1) * delta));
      probabilities[i] = 0.5f * (leftVal + rightVal) * delta;
      leftVal = rightVal;
    }

    // Sort array based on area of bin estimates
    int[] sortedIndices = Utils.sort(probabilities);

    // Mark the intervals to use
    float sum = 0;
    boolean[] toUse = new boolean[probabilities.length];
    int k = 0;
    while ((sum < conf) && (k < toUse.length)) {
      toUse[sortedIndices[toUse.length - (k + 1)]] = true;
      sum += probabilities[sortedIndices[toUse.length - (k + 1)]];
      k++;
    }

    // Don't need probabilities anymore
    probabilities = null;

    // Create final list of intervals
    ArrayList<float[]> intervals = new ArrayList<float[]>();

    // The current interval
    float[] interval = null;

    // Iterate through kernels
    boolean haveStartedInterval = false;
    for (int i = 0; i < m_NumIntervals; i++) {

      // Should the current bin be used?
      if (toUse[i]) {

        // Do we need to create a new interval?
        if (haveStartedInterval == false) {
          haveStartedInterval = true;
          interval = new float[2];
          interval[0] = min + i * delta;
        }

        // Regardless, we should update the upper boundary
        interval[1] = min + (i + 1) * delta;
      } else {

        // We need to finalize and store the last interval
        // if necessary.
        if (haveStartedInterval) {
          haveStartedInterval = false;
          intervals.add(interval);
        }
      }
    }

    // Add last interval if there is one
    if (haveStartedInterval) {
      intervals.add(interval);
    }

    return intervals.toArray(new float[0][0]);
  }

  /**
   * Returns the quantile for the given percentage.
   * 
   * @param percentage the percentage
   * @return the quantile
   */
  @Override
  public float predictQuantile(float percentage) {

    // Update the bandwidth
    updateBoundariesAndOrWeights();

    // Compute minimum and maximum value, and delta
    float val = Statistics.normalInverse(1.0f - (1.0f - 0.95f) / 2);
    float min = m_TM.firstKey() - val * m_Width;
    float max = m_TM.lastKey() + val * m_Width;
    float delta = (max - min) / m_NumIntervals;

    float sum = 0;
    float leftVal = (float) Math.exp(logDensity(min));
    for (int i = 0; i < m_NumIntervals; i++) {
      if (sum >= percentage) {
        return min + i * delta;
      }
      float rightVal = (float) Math.exp(logDensity(min + (i + 1) * delta));
      sum += 0.5 * (leftVal + rightVal) * delta;
      leftVal = rightVal;
    }
    return max;
  }

  /**
   * Returns the natural logarithm of the density estimate at the given point.
   * 
   * @param value the value at which to evaluate
   * @return the natural logarithm of the density estimate at the given value
   */
  @Override
  public float logDensity(float value) {

    // Update boundaries if necessary
    updateBoundariesAndOrWeights();

    if (m_Boundaries == null) {
      return (float) Math.log(Float.MIN_VALUE);
    }

    // Find the bin
    int index = Arrays.binarySearch(m_Boundaries, value);

    // Is the value outside?
    if ((index == -1) || (index == -m_Boundaries.length - 1)) {

      // Use normal density outside
      float val = 0;
      if (index == -1) { // Smaller than minimum
        val = m_TM.firstKey() - value;
      } else {
        val = value - m_TM.lastKey();
      }
      return (float) ((CONST - Math.log(m_Width) - 0.5 * (val * val / (m_Width * m_Width)))
        - Math.log(m_SumOfWeights + 2));
    }

    // Is value exactly equal to right-most boundary?
    if (index == m_Boundaries.length - 1) {
      index--;
    } else {

      // Need to reverse index if necessary
      if (index < 0) {
        index = -index - 2;
      }
    }

    // Figure out of width
    float width = m_Boundaries[index + 1] - m_Boundaries[index];

    // Density compontent from smeared-out data point
    float densSmearedOut = 1.0f / ((m_SumOfWeights + 2) * (m_Boundaries[m_Boundaries.length - 1] - m_Boundaries[0]));

    // Return log of density
    if (m_Weights[index] <= 0) {

      /*
       * System.out.println(value); System.out.println(this); System.exit(1);
       */
      // Just use one smeared-out data point
      return (float) Math.log(densSmearedOut);
    } else {
      return (float) Math.log(densSmearedOut + m_Weights[index]
        / ((m_SumOfWeights + 2) * width));
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11318 $");
  }

  /**
   * Returns textual description of this estimator.
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();

    text.append("EqualFrequencyHistogram estimator\n\n"
      + "Bandwidth for out of range cases " + m_Width + ", total weight "
      + m_SumOfWeights);

    if (m_Boundaries != null) {
      text.append("\nLeft boundary\tRight boundary\tWeight\n");
      for (int i = 0; i < m_Boundaries.length - 1; i++) {
        text.append(m_Boundaries[i] + "\t" + m_Boundaries[i + 1] + "\t"
          + m_Weights[i] + "\t"
          + Math.exp(logDensity((m_Boundaries[i + 1] + m_Boundaries[i]) / 2))
          + "\n");
      }
    }

    return text.toString();
  }

  /**
   * Main method, used for testing this class.
   */
  public static void main(String[] args) {

    // Get random number generator initialized by system
    Random r = new Random();

    // Create density estimator
    UnivariateEqualFrequencyHistogramEstimator e = new UnivariateEqualFrequencyHistogramEstimator();

    // Output the density estimator
    System.out.println(e);

    // Monte Carlo integration
    float sum = 0;
    for (int i = 0; i < 1000; i++) {
      sum += Math.exp(e.logDensity(r.nextFloat() * 10.0f - 5.0f));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / 1000);

    // Add Gaussian values into it
    for (int i = 0; i < 1000; i++) {
      e.addValue((float) (0.1f * r.nextGaussian() - 3), 1);
      e.addValue((float) (r.nextGaussian() * 0.25f), 3);
    }

    // Monte Carlo integration
    sum = 0;
    int points = 10000000;
    for (int i = 0; i < points; i++) {
      float value = r.nextFloat() * 20.0f - 10.0f;
      sum += Math.exp(e.logDensity(value));
    }

    // Output the density estimator
    System.out.println(e);

    System.out.println("Approximate integral: " + 20.0 * sum / points);

    // Check interval estimates
    float[][] Intervals = e.predictIntervals(0.9f);

    System.out.println("Printing histogram intervals ---------------------");

    for (float[] interval : Intervals) {
      System.out.println("Left: " + interval[0] + "\t Right: " + interval[1]);
    }

    System.out
      .println("Finished histogram printing intervals ---------------------");

    float Covered = 0;
    for (int i = 0; i < 1000; i++) {
      float val = -1;
      if (r.nextFloat() < 0.25) {
        val = (float) (0.1f * r.nextGaussian() - 3.0f);
      } else {
        val = (float) (r.nextGaussian() * 0.25f);
      }
      for (float[] interval : Intervals) {
        if (val >= interval[0] && val <= interval[1]) {
          Covered++;
          break;
        }
      }
    }
    System.out.println("Coverage at 0.9 level for histogram intervals: "
      + Covered / 1000);

    for (int j = 1; j < 5; j++) {
      float numTrain = (float) Math.pow(10, j);
      System.out.println("Number of training cases: " + numTrain);

      // Compare performance to normal estimator on normally distributed data
      UnivariateEqualFrequencyHistogramEstimator eHistogram = new UnivariateEqualFrequencyHistogramEstimator();
      UnivariateNormalEstimator eNormal = new UnivariateNormalEstimator();

      // Add training cases
      for (int i = 0; i < numTrain; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        /*
         * if (j == 4) { System.err.println(val); }
         */
        eHistogram.addValue(val, 1);
        eNormal.addValue(val, 1);
      }

      // Monte Carlo integration
      sum = 0;
      points = 10000000;
      for (int i = 0; i < points; i++) {
        float value = r.nextFloat() * 20.0f - 10.0f;
        sum += Math.exp(eHistogram.logDensity(value));
      }
      System.out.println(eHistogram);
      System.out.println("Approximate integral for histogram estimator: "
        + 20.0 * sum / points);

      // Evaluate estimators
      float loglikelihoodHistogram = 0, loglikelihoodNormal = 0;
      for (int i = 0; i < 1000; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        loglikelihoodHistogram += eHistogram.logDensity(val);
        loglikelihoodNormal += eNormal.logDensity(val);
      }
      System.out.println("Loglikelihood for histogram estimator: "
        + loglikelihoodHistogram / 1000);
      System.out.println("Loglikelihood for normal estimator: "
        + loglikelihoodNormal / 1000);

      // Check interval estimates
      float[][] histogramIntervals = eHistogram.predictIntervals(0.95f);
      float[][] normalIntervals = eNormal.predictIntervals(0.95f);

      System.out.println("Printing histogram intervals ---------------------");

      for (float[] histogramInterval : histogramIntervals) {
        System.out.println("Left: " + histogramInterval[0] + "\t Right: "
          + histogramInterval[1]);
      }

      System.out
        .println("Finished histogram printing intervals ---------------------");

      System.out.println("Printing normal intervals ---------------------");

      for (float[] normalInterval : normalIntervals) {
        System.out.println("Left: " + normalInterval[0] + "\t Right: "
          + normalInterval[1]);
      }

      System.out
        .println("Finished normal printing intervals ---------------------");

      float histogramCovered = 0;
      float normalCovered = 0;
      for (int i = 0; i < 1000; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        for (float[] histogramInterval : histogramIntervals) {
          if (val >= histogramInterval[0] && val <= histogramInterval[1]) {
            histogramCovered++;
            break;
          }
        }
        for (float[] normalInterval : normalIntervals) {
          if (val >= normalInterval[0] && val <= normalInterval[1]) {
            normalCovered++;
            break;
          }
        }
      }
      System.out.println("Coverage at 0.95 level for histogram intervals: "
        + histogramCovered / 1000);
      System.out.println("Coverage at 0.95 level for normal intervals: "
        + normalCovered / 1000);

      histogramIntervals = eHistogram.predictIntervals(0.8f);
      normalIntervals = eNormal.predictIntervals(0.8f);
      histogramCovered = 0;
      normalCovered = 0;
      for (int i = 0; i < 1000; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        for (float[] histogramInterval : histogramIntervals) {
          if (val >= histogramInterval[0] && val <= histogramInterval[1]) {
            histogramCovered++;
            break;
          }
        }
        for (float[] normalInterval : normalIntervals) {
          if (val >= normalInterval[0] && val <= normalInterval[1]) {
            normalCovered++;
            break;
          }
        }
      }
      System.out.println("Coverage at 0.8 level for histogram intervals: "
        + histogramCovered / 1000);
      System.out.println("Coverage at 0.8 level for normal intervals: "
        + normalCovered / 1000);
    }
  }
}
