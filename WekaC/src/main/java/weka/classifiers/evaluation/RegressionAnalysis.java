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
 *    RegressionAnalysis.java
 *    Copyright (C) 1999-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.matrix.Matrix;

/**
 * Analyzes linear regression model by using the Student's t-test on each
 * coefficient. Also calculates R^2 value and F-test value.
 * 
 * More information: http://en.wikipedia.org/wiki/Student's_t-test
 * http://en.wikipedia.org/wiki/Linear_regression
 * http://en.wikipedia.org/wiki/Ordinary_least_squares
 * 
 * @author Chris Meyer: cmeyer@udel.edu University of Delaware, Newark, DE, USA
 *         CISC 612: Design extension implementation
 * @version $Revision: $
 */

public class RegressionAnalysis {

  /**
   * Returns the sum of squared residuals of the simple linear regression model:
   * y = a + bx.
   * 
   * @param data (the data set)
   * @param chosen (chosen x-attribute)
   * @param slope (slope determined by simple linear regression model)
   * @param intercept (intercept determined by simple linear regression model)
   * 
   * @return sum of squared residuals
   * @throws Exception if there is a missing class value in data
   */
  public static float calculateSSR(Instances data, Attribute chosen,
    float slope, float intercept) throws Exception {
    float ssr = 0.0f;
    for (int i = 0; i < data.numInstances(); i++) {
      float yHat = slope * data.instance(i).value(chosen) + intercept;
      float resid = data.instance(i).value(data.classIndex()) - yHat;
      ssr += resid * resid;
    }
    return ssr;
  }

  /**
   * Returns the R-squared value for a linear regression model, where sum of
   * squared residuals is already calculated. This works for either a simple or
   * a multiple linear regression model.
   * 
   * @param data (the data set)
   * @param ssr (sum of squared residuals)
   * @return R^2 value
   * @throws Exception if there is a missing class value in data
   */
  public static float calculateRSquared(Instances data, float ssr)
    throws Exception {
    // calculate total sum of squares (derivation of y from mean)
    float yMean = data.meanOrMode(data.classIndex());
    float tss = 0.0f;
    for (int i = 0; i < data.numInstances(); i++) {
      tss += (data.instance(i).value(data.classIndex()) - yMean)
        * (data.instance(i).value(data.classIndex()) - yMean);
    }

    // calculate R-squared value and return
    float rsq = 1 - (ssr / tss);
    return rsq;
  }

  /**
   * Returns the adjusted R-squared value for a linear regression model. This
   * works for either a simple or a multiple linear regression model.
   * 
   * @param rsq (the model's R-squared value)
   * @param n (the number of instances in the data)
   * @param k (the number of coefficients in the model: k>=2)
   * @return the adjusted R squared value
   */
  public static float calculateAdjRSquared(float rsq, int n, int k) {
    if (n < 1 || k < 2 || n == k) {
      System.err.println("Cannot calculate Adjusted R^2.");
      return Float.NaN;
    }

    return 1 - ((1 - rsq) * (n - 1) / (n - k));
  }

  /**
   * Returns the F-statistic for a linear regression model.
   * 
   * @param rsq (the model's R-squared value)
   * @param n (the number of instances in the data)
   * @param k (the number of coefficients in the model: k>=2)
   * @return F-statistic
   */
  public static float calculateFStat(float rsq, int n, int k) {
    if (n < 1 || k < 2 || n == k) {
      System.err.println("Cannot calculate F-stat.");
      return Float.NaN;
    }

    float numerator = rsq / (k - 1);
    float denominator = (1 - rsq) / (n - k);
    return numerator / denominator;
  }

  /**
   * Returns the standard errors of slope and intercept for a simple linear
   * regression model: y = a + bx. The first element is the standard error of
   * slope, the second element is standard error of intercept.
   * 
   * @param data (the data set)
   * @param chosen (chosen x-attribute)
   * @param slope (slope determined by simple linear regression model)
   * @param intercept (intercept determined by simple linear regression model)
   * @param df (number of instances - 2)
   * 
   * @return array of standard errors of slope and intercept
   * @throws Exception if there is a missing class value in data
   */
  public static float[] calculateStdErrorOfCoef(Instances data,
    Attribute chosen, float slope, float intercept, int df) throws Exception {
    // calculate sum of squared residuals, mean squared error
    float ssr = calculateSSR(data, chosen, slope, intercept);
    float mse = ssr / df;

    /*
     * put data into 2-D array with 2 columns first column is value of chosen
     * attribute second column is constant (1's)
     */
    float[][] array = new float[data.numInstances()][2];
    for (int i = 0; i < data.numInstances(); i++) {
      array[i][0] = data.instance(i).value(chosen);
      array[i][1] = 1.0f;
    }

    /*
     * linear algebra calculation: covariance matrix = mse * (XtX)^-1 diagonal
     * of covariance matrix is square of standard error of coefficients
     */
    Matrix X = new Matrix(array);
    Matrix Xt = X.transpose();
    Matrix XtX = Xt.times(X);
    Matrix inverse = XtX.inverse();
    Matrix cov = inverse.times(mse);

    float[] result = new float[2];
    for (int i = 0; i < 2; i++) {
      result[i] = (float) Math.sqrt(cov.get(i, i));
    }

    return result;
  }

  /**
   * Returns an array of the standard errors of the coefficients in a multiple
   * linear regression. The last element in the array is the standard error of
   * the constant coefficient. The standard error array is used to calculate the
   * t-statistics.
   * 
   * @param data (the data set
   * @param selected (flags indicating variables used in the regression)
   * @param ssr (sum of squared residuals)
   * @param n (number of instances)
   * @param k (number of coefficients; includes constant)
   * 
   * @return array of standard errors of coefficients
   * @throws Exception if there is a missing class value in data
   */
  public static float[] calculateStdErrorOfCoef(Instances data,
    boolean[] selected, float ssr, int n, int k) throws Exception {
    // Construct a matrix to hold X variables
    float[][] array = new float[n][k];
    // put data into 2-D array format
    int column = 0;
    for (int j = 0; j < data.numAttributes(); j++) {
      if ((data.classIndex() != j) && (selected[j])) {
        for (int i = 0; i < n; i++) {
          array[i][column] = data.instance(i).value(j);
        }
        column++;
      }
    }

    // last column in array is constant (1's)
    for (int i = 0; i < n; i++) {
      array[i][k - 1] = 1.0f;
    }

    /*
     * linear algebra calculation: covariance matrix = mse * (XtX)^-1 diagonal
     * of covariance matrix is square of standard error of coefficients
     */
    Matrix X = new Matrix(array);
    Matrix Xt = X.transpose();
    Matrix XtX = Xt.times(X);
    Matrix inverse = XtX.inverse();

    float mse = ssr / (n - k);
    Matrix cov = inverse.times(mse);

    float[] result = new float[k];
    for (int i = 0; i < k; i++) {
      result[i] = (float) Math.sqrt(cov.get(i, i));
    }
    return result;
  }

  /**
   * Returns an array of the t-statistic of each coefficient in a multiple
   * linear regression model.
   * 
   * @param m_Coefficients (array holding the value of each coefficient)
   * @param m_StdErrorOfCoef (array holding each coefficient's standard error)
   * @param k (number of coefficients, includes constant)
   * @return array of t-statistics of coefficients
   */
  public static float[] calculateTStats(float[] m_Coefficients, float[] m_StdErrorOfCoef, int k) {
    float[] result = new float[k];
    for (int i = 0; i < k; i++) {
      result[i] = m_Coefficients[i] / m_StdErrorOfCoef[i];
    }
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: ? $");
  }
}
