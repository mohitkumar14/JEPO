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
 *    UnivariateNormalEstimator.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.estimators;

import java.io.Serializable;
import java.util.Random;

import weka.core.RevisionUtils;
import weka.core.Statistics;

/**
 * Simple weighted normal density estimator.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11318 $
 */
public class UnivariateNormalEstimator implements UnivariateDensityEstimator,
                                                  UnivariateIntervalEstimator,
                                                  UnivariateQuantileEstimator,
                                                  Serializable {

  /** For serialization */
  private static final long serialVersionUID = -1669009817825826548L;

  /** The weighted sum of values */
  protected float m_WeightedSum = 0;

  /** The weighted sum of squared values */
  protected float m_WeightedSumSquared = 0;

  /** The weight of the values collected so far */
  protected float m_SumOfWeights = 0;

  /** The mean value (only updated when needed) */
  protected float m_Mean = 0;

  /** The variance (only updated when needed) */
  protected float m_Variance = Float.MAX_VALUE;

  /** The minimum allowed value of the variance (default: 1.0E-6 * 1.0E-6) */
  protected float m_MinVar = 1.0E-6f * 1.0E-6f;

  /** Constant for Gaussian density */
  public static final float CONST = (float) Math.log(2 * Math.PI);

  /**
   * Returns a string describing the estimator.
   */
  public String globalInfo() {
    return "Estimates a univariate normal density.";
  }
  /**
   * Adds a value to the density estimator.
   *
   * @param value the value to add
   * @param weight the weight of the value
   */
  public void addValue(float value, float weight) {

    m_WeightedSum += value * weight;
    m_WeightedSumSquared += value * value * weight;
    m_SumOfWeights += weight;
  }

  /**
   * Updates mean and variance based on sufficient statistics.
   * Variance is set to m_MinVar if it becomes smaller than that
   * value. It is set to Double.MAX_VALUE if the sum of weights is
   * zero.
   */
  protected void updateMeanAndVariance() {
    
    // Compute mean
    m_Mean = 0;
    if (m_SumOfWeights > 0) {
      m_Mean = m_WeightedSum / m_SumOfWeights;
    }

    // Compute variance
    m_Variance = Float.MAX_VALUE;
    if (m_SumOfWeights > 0) {
      m_Variance = m_WeightedSumSquared / m_SumOfWeights - m_Mean * m_Mean; 
    }

    // Hack for case where variance is 0
    if (m_Variance <= m_MinVar) {
      m_Variance = m_MinVar;
    }
  }

  /**
   * Returns the interval for the given confidence value. 
   * 
   * @param conf the confidence value in the interval [0, 1]
   * @return the interval
   */
  public float[][] predictIntervals(float conf) {
    
    updateMeanAndVariance();

    float val = Statistics.normalInverse(1.0f - (1.0f - conf) / 2.0f);

    float[][] arr = new float[1][2];
    arr[0][1] = (float) (m_Mean + val * Math.sqrt(m_Variance));
    arr[0][0] = (float) (m_Mean - val * Math.sqrt(m_Variance));

    return arr;
  }

  /**
   * Returns the quantile for the given percentage.
   * 
   * @param percentage the percentage
   * @return the quantile
   */
  public float predictQuantile(float percentage) {
    
    updateMeanAndVariance();

    return (float) (m_Mean + Statistics.normalInverse(percentage) * Math.sqrt(m_Variance));
  }

  /**
   * Returns the natural logarithm of the density estimate at the given
   * point.
   *
   * @param value the value at which to evaluate
   * @return the natural logarithm of the density estimate at the given
   * value
   */
  public float logDensity(float value) {
    
    updateMeanAndVariance();

    // Return natural logarithm of density
    float val = (float) (-0.5f * (CONST + Math.log(m_Variance) + 
                         (value - m_Mean) * (value - m_Mean) / m_Variance)); 

    return val;
  }

  /**
   * Returns textual description of this estimator.
   */
  public String toString() {

    updateMeanAndVariance();

    return "Mean: " + m_Mean + "\t" + "Variance: " + m_Variance;
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
    UnivariateNormalEstimator e = new UnivariateNormalEstimator();

    // Output the density estimator
    System.out.println(e);

    // Monte Carlo integration
    float sum = 0;
    for (int i = 0; i < 100000; i++) {
      sum += Math.exp(e.logDensity(r.nextFloat() * 10.0f - 5.0f));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / 100000);

    // Add Gaussian values into it
    for (int i = 0; i < 100000; i++) {
      e.addValue((float) r.nextGaussian(), 1);
      e.addValue((float) (r.nextGaussian() * 2.0), 3);
    }

    // Output the density estimator
    System.out.println(e);

    // Monte Carlo integration
    sum = 0;
    for (int i = 0; i < 100000; i++) {
      sum += Math.exp(e.logDensity(r.nextFloat() * 10.0f - 5.0f));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / 100000);

    // Create density estimator
    e = new UnivariateNormalEstimator();

    // Add Gaussian values into it
    for (int i = 0; i < 100000; i++) {
      e.addValue((float) r.nextGaussian(), 1);
      e.addValue((float) (r.nextGaussian() * 2.0), 1);
      e.addValue((float) (r.nextGaussian() * 2.0), 1);
      e.addValue((float) (r.nextGaussian() * 2.0), 1);
    }

    // Output the density estimator
    System.out.println(e);

    // Monte Carlo integration
    sum = 0;
    for (int i = 0; i < 100000; i++) {
      sum += Math.exp(e.logDensity(r.nextFloat() * 10.0f - 5.0f));
    }
    System.out.println("Approximate integral: " + 10.0 * sum / 100000);

    // Create density estimator
    e = new UnivariateNormalEstimator();

    // Add Gaussian values into it
    for (int i = 0; i < 100000; i++) {
      e.addValue((float) (r.nextGaussian() * 5.0 + 3.0) , 1);
    }

    // Output the density estimator
    System.out.println(e);

    // Check interval estimates
    float[][] intervals = e.predictIntervals(0.95f);
    System.out.println("Lower: " + intervals[0][0] + " Upper: " + intervals[0][1]);
    float covered = 0;
    for (int i = 0; i < 100000; i++) {
      float val = (float) (r.nextGaussian() * 5.0 + 3.0);
      if (val >= intervals[0][0] && val <= intervals[0][1]) {
        covered++;
      }
    }
    System.out.println("Coverage: " + covered / 100000);

    intervals = e.predictIntervals(0.8f);
    System.out.println("Lower: " + intervals[0][0] + " Upper: " + intervals[0][1]);
    covered = 0;
    for (int i = 0; i < 100000; i++) {
      float val = (float) (r.nextGaussian() * 5.0 + 3.0);
      if (val >= intervals[0][0] && val <= intervals[0][1]) {
        covered++;
      }
    }
    System.out.println("Coverage: " + covered / 100000);
  }
}