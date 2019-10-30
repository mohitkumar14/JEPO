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
 *    UnivariateKernelEstimator.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.estimators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import weka.core.RevisionUtils;
import weka.core.Statistics;
import weka.core.Utils;

/**
 * Simple weighted kernel density estimator.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11318 $
 */
public class UnivariateKernelEstimator implements UnivariateDensityEstimator,
  UnivariateIntervalEstimator, UnivariateQuantileEstimator, Serializable {

  /** For serialization */
  private static final long serialVersionUID = -1163983347810498880L;

  /** The collection used to store the weighted values. */
  protected TreeMap<Float, Float> m_TM = new TreeMap<Float, Float>();

  /** The weighted sum of values */
  protected float m_WeightedSum = 0;

  /** The weighted sum of squared values */
  protected float m_WeightedSumSquared = 0;

  /** The weight of the values collected so far */
  protected float m_SumOfWeights = 0;

  /** The current bandwidth (only computed when needed) */
  protected float m_Width = Float.MAX_VALUE;

  /** The exponent to use in computation of bandwidth (default: -0.25) */
  protected float m_Exponent = -0.25f;

  /** The minimum allowed value of the kernel width (default: 1.0E-6) */
  protected float m_MinWidth = 1.0E-6f;

  /** Constant for Gaussian density. */
  public static final float CONST = (float) (-0.5 * Math.log(2 * Math.PI));

  /** Threshold at which further kernels are no longer added to sum. */
  protected float m_Threshold = 1.0E-6f;

  /** The number of intervals used to approximate prediction interval. */
  protected int m_NumIntervals = 1000;

  /**
   * Returns a string describing the estimator.
   */
  public String globalInfo() {
    return "Provides a univariate kernel estimator.";
  }
  /**
   * Adds a value to the density estimator.
   * 
   * @param value the value to add
   * @param weight the weight of the value
   */
  @Override
  public void addValue(float value, float weight) {

    m_WeightedSum += value * weight;
    m_WeightedSumSquared += value * value * weight;
    m_SumOfWeights += weight;
    if (m_TM.get(value) == null) {
      m_TM.put(value, weight);
    } else {
      m_TM.put(value, m_TM.get(value) + weight);
    }
  }

  /**
   * Updates bandwidth: the sample standard deviation is multiplied by the total
   * weight to the power of the given exponent.
   * 
   * If the total weight is not greater than zero, the width is set to
   * Double.MAX_VALUE. If that is not the case, but the width becomes smaller
   * than m_MinWidth, the width is set to the value of m_MinWidth.
   */
  public void updateWidth() {

    // OK, need to do some work
    if (m_SumOfWeights > 0) {

      // Compute variance for scaling
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
    } else {
      m_Width = Float.MAX_VALUE;
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
    updateWidth();

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
    updateWidth();

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
   * Computes the logarithm of x and y given the logarithms of x and y.
   * 
   * This is based on Tobias P. Mann's description in "Numerically Stable Hidden
   * Markov Implementation" (2006).
   */
  protected float logOfSum(float logOfX, float logOfY) {

    // Check for cases where log of zero is present
    if (Float.isNaN(logOfX)) {
      return logOfY;
    }
    if (Float.isNaN(logOfY)) {
      return logOfX;
    }

    // Otherwise return proper result, taken care of overflows
    if (logOfX > logOfY) {
      return (float) (logOfX + Math.log(1 + Math.exp(logOfY - logOfX)));
    } else {
      return (float) (logOfY + Math.log(1 + Math.exp(logOfX - logOfY)));
    }
  }

  /**
   * Compute running sum of density values and weights.
   */
  protected void runningSum(Set<Map.Entry<Float, Float>> c, float value,
    float[] sums) {

    // Auxiliary variables
    float offset = (float) (CONST - Math.log(m_Width));
    float logFactor = (float) (Math.log(m_Threshold) - Math.log(1 - m_Threshold));
    float logSumOfWeights = (float) Math.log(m_SumOfWeights);

    // Iterate through values
    Iterator<Map.Entry<Float, Float>> itr = c.iterator();
    while (itr.hasNext()) {
      Map.Entry<Float, Float> entry = itr.next();

      // Skip entry if weight is zero because it cannot contribute to sum
      if (entry.getValue() > 0) {
        float diff = (entry.getKey() - value) / m_Width;
        float logDensity = offset - 0.5f * diff * diff;
        float logWeight = (float) Math.log(entry.getValue());
        sums[0] = logOfSum(sums[0], logWeight + logDensity);
        sums[1] = logOfSum(sums[1], logWeight);

        // Can we stop assuming worst case?
        if (logDensity + logSumOfWeights < logOfSum(logFactor + sums[0],
          logDensity + sums[1])) {
          break;
        }
      }
    }
  }

  /**
   * Returns the natural logarithm of the density estimate at the given point.
   * 
   * @param value the value at which to evaluate
   * @return the natural logarithm of the density estimate at the given value
   */
  @Override
  public float logDensity(float value) {

    // Update the bandwidth
    updateWidth();

    // Array used to keep running sums
    float[] sums = new float[2];
    sums[0] = Float.NaN;
    sums[1] = Float.NaN;

    // Examine right-hand size of value
    runningSum(m_TM.tailMap(value, true).entrySet(), value, sums);

    // Examine left-hand size of value
    runningSum(m_TM.headMap(value, false).descendingMap().entrySet(), value,
      sums);

    // Need to normalize
    return (float) (sums[0] - Math.log(m_SumOfWeights));
  }

  /**
   * Returns textual description of this estimator.
   */
  @Override
  public String toString() {

    return "Kernel estimator with bandwidth " + m_Width + " and total weight "
      + m_SumOfWeights + " based on\n" + m_TM.toString();
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
   * Main method, used for testing this class.
   */
  public static void main(String[] args) {

    // Get random number generator initialized by system
    Random r = new Random();

    // Create density estimator
    UnivariateKernelEstimator e = new UnivariateKernelEstimator();

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
      e.addValue((float) (r.nextGaussian() * 0.25), 3);
    }

    // Monte Carlo integration
    sum = 0;
    int points = 10000;
    for (int i = 0; i < points; i++) {
      float value = r.nextFloat() * 10.0f - 5.0f;
      sum += Math.exp(e.logDensity(value));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / points);

    // Check interval estimates
    float[][] Intervals = e.predictIntervals(0.9f);

    System.out.println("Printing kernel intervals ---------------------");

    for (float[] interval : Intervals) {
      System.out.println("Left: " + interval[0] + "\t Right: " + interval[1]);
    }

    System.out
      .println("Finished kernel printing intervals ---------------------");

    float Covered = 0;
    for (int i = 0; i < 1000; i++) {
      float val = -1;
      if (r.nextFloat() < 0.25) {
        val = (float) (0.1 * r.nextGaussian() - 3.0);
      } else {
        val = (float) (r.nextGaussian() * 0.25);
      }
      for (float[] interval : Intervals) {
        if (val >= interval[0] && val <= interval[1]) {
          Covered++;
          break;
        }
      }
    }
    System.out.println("Coverage at 0.9 level for kernel intervals: " + Covered
      / 1000);

    // Compare performance to normal estimator on normally distributed data
    UnivariateKernelEstimator eKernel = new UnivariateKernelEstimator();
    UnivariateNormalEstimator eNormal = new UnivariateNormalEstimator();

    for (int j = 1; j < 5; j++) {
      float numTrain = (float) Math.pow(10, j);
      System.out.println("Number of training cases: " + numTrain);

      // Add training cases
      for (int i = 0; i < numTrain; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        eKernel.addValue(val, 1);
        eNormal.addValue(val, 1);
      }

      // Monte Carlo integration
      sum = 0;
      points = 10000;
      for (int i = 0; i < points; i++) {
        float value = r.nextFloat() * 20.0f - 10.0f;
        sum += Math.exp(eKernel.logDensity(value));
      }
      System.out.println("Approximate integral for kernel estimator: " + 20.0
        * sum / points);

      // Evaluate estimators
      float loglikelihoodKernel = 0, loglikelihoodNormal = 0;
      for (int i = 0; i < 1000; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        loglikelihoodKernel += eKernel.logDensity(val);
        loglikelihoodNormal += eNormal.logDensity(val);
      }
      System.out.println("Loglikelihood for kernel estimator: "
        + loglikelihoodKernel / 1000);
      System.out.println("Loglikelihood for normal estimator: "
        + loglikelihoodNormal / 1000);

      // Check interval estimates
      float[][] kernelIntervals = eKernel.predictIntervals(0.95f);
      float[][] normalIntervals = eNormal.predictIntervals(0.95f);

      System.out.println("Printing kernel intervals ---------------------");

      for (float[] kernelInterval : kernelIntervals) {
        System.out.println("Left: " + kernelInterval[0] + "\t Right: "
          + kernelInterval[1]);
      }

      System.out
        .println("Finished kernel printing intervals ---------------------");

      System.out.println("Printing normal intervals ---------------------");

      for (float[] normalInterval : normalIntervals) {
        System.out.println("Left: " + normalInterval[0] + "\t Right: "
          + normalInterval[1]);
      }

      System.out
        .println("Finished normal printing intervals ---------------------");

      float kernelCovered = 0;
      float normalCovered = 0;
      for (int i = 0; i < 1000; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        for (float[] kernelInterval : kernelIntervals) {
          if (val >= kernelInterval[0] && val <= kernelInterval[1]) {
            kernelCovered++;
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
      System.out.println("Coverage at 0.95 level for kernel intervals: "
        + kernelCovered / 1000);
      System.out.println("Coverage at 0.95 level for normal intervals: "
        + normalCovered / 1000);

      kernelIntervals = eKernel.predictIntervals(0.8f);
      normalIntervals = eNormal.predictIntervals(0.8f);
      kernelCovered = 0;
      normalCovered = 0;
      for (int i = 0; i < 1000; i++) {
        float val = (float) (r.nextGaussian() * 1.5 + 0.5);
        for (float[] kernelInterval : kernelIntervals) {
          if (val >= kernelInterval[0] && val <= kernelInterval[1]) {
            kernelCovered++;
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
      System.out.println("Coverage at 0.8 level for kernel intervals: "
        + kernelCovered / 1000);
      System.out.println("Coverage at 0.8 level for normal intervals: "
        + normalCovered / 1000);
    }
  }
}
