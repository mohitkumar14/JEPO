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
 *    NominalPrediction.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import java.io.Serializable;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * Encapsulates an evaluatable nominal prediction: the predicted probability
 * distribution plus the actual class value.
 *
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 8034 $
 */
public class NominalPrediction
  implements Prediction, Serializable, RevisionHandler {

  /**
   * Remove this if you change this class so that serialization would be
   * affected.
   */
  static final long serialVersionUID = -8871333992740492788L;

  /** The predicted probabilities */
  private float [] m_Distribution;

  /** The actual class value */
  private float m_Actual = MISSING_VALUE;

  /** The predicted class value */
  private float m_Predicted = MISSING_VALUE;

  /** The weight assigned to this prediction */
  private float m_Weight = 1;

  /**
   * Creates the NominalPrediction object with a default weight of 1.0.
   *
   * @param actual the actual value, or MISSING_VALUE.
   * @param distribution the predicted probability distribution. Use 
   * NominalPrediction.makeDistribution() if you only know the predicted value.
   */
  public NominalPrediction(float actual, float [] distribution) {

    this(actual, distribution, 1);
  }

  /**
   * Creates the NominalPrediction object.
   *
   * @param actual the actual value, or MISSING_VALUE.
   * @param dist the predicted probability distribution. Use 
   * NominalPrediction.makeDistribution() if you only know the predicted value.
   * @param weight the weight assigned to the prediction.
   */
  public NominalPrediction(float actual, float[] dist, 
                           float weight) {

    if (dist == null) {
      throw new NullPointerException("Null distribution in NominalPrediction.");
    }
    m_Actual = actual;
    m_Distribution=new float[dist.length];
    System.arraycopy(dist, 0, m_Distribution,  
            0, dist.length);
    m_Weight = weight;
    updatePredicted();
  }

  /**
   * Gets the predicted probabilities
   * 
   * @return the predicted probabilities
   */
  public float [] distribution() { 

    return m_Distribution; 
  }

  /** 
   * Gets the actual class value.
   *
   * @return the actual class value, or MISSING_VALUE if no
   * prediction was made.  
   */
  public float actual() { 

    return m_Actual; 
  }

  /**
   * Gets the predicted class value.
   *
   * @return the predicted class value, or MISSING_VALUE if no
   * prediction was made.  
   */
  public float predicted() { 

    return m_Predicted; 
  }

  /** 
   * Gets the weight assigned to this prediction. This is typically the weight
   * of the test instance the prediction was made for.
   *
   * @return the weight assigned to this prediction.
   */
  public float weight() { 

    return m_Weight; 
  }

  /**
   * Calculates the prediction margin. This is defined as the difference
   * between the probability predicted for the actual class and the highest
   * predicted probability of the other classes.
   *
   * @return the margin for this prediction, or
   * MISSING_VALUE if either the actual or predicted value
   * is missing.  
   */
  public float margin() {

    if ((m_Actual == MISSING_VALUE) ||
        (m_Predicted == MISSING_VALUE)) {
      return MISSING_VALUE;
    }
    float probActual = m_Distribution[(int)m_Actual];
    float probNext = 0;
    for(int i = 0; i < m_Distribution.length; i++)
      if ((i != m_Actual) &&
	  (m_Distribution[i] > probNext))
	probNext = m_Distribution[i];

    return probActual - probNext;
  }

  /**
   * Convert a single prediction into a probability distribution
   * with all zero probabilities except the predicted value which
   * has probability 1.0. If no prediction was made, all probabilities
   * are zero.
   *
   * @param predictedClass the index of the predicted class, or 
   * MISSING_VALUE if no prediction was made.
   * @param numClasses the number of possible classes for this nominal
   * prediction.
   * @return the probability distribution.  
   */
  public static float [] makeDistribution(float predictedClass, 
                                           int numClasses) {

    float [] dist = new float [numClasses];
    if (predictedClass == MISSING_VALUE) {
      return dist;
    }
    dist[(int)predictedClass] = 1.0f;
    return dist;
  }
  
  /**
   * Creates a uniform probability distribution -- where each of the
   * possible classes is assigned equal probability.
   *
   * @param numClasses the number of possible classes for this nominal
   * prediction.
   * @return the probability distribution.  
   */
  public static float [] makeUniformDistribution(int numClasses) {
    
    float [] dist = new float [numClasses];
    for (int i = 0; i < numClasses; i++) {
      dist[i] = 1.0f / numClasses;
    }
    return dist;
  }
 
  /**
   * Determines the predicted class (doesn't detect multiple 
   * classifications). If no prediction was made (i.e. all zero
   * probababilities in the distribution), m_Prediction is set to
   * MISSING_VALUE.
   */
  private void updatePredicted() {

    int predictedClass = -1;
    float bestProb = 0.0f;
    for(int i = 0; i < m_Distribution.length; i++) {
      if (m_Distribution[i] > bestProb) {
        predictedClass = i;
        bestProb = m_Distribution[i];
      }
    }

    if (predictedClass != -1) {
      m_Predicted = predictedClass;
    } else {
      m_Predicted = MISSING_VALUE;
    }
  }

  /**
   * Gets a human readable representation of this prediction.
   *
   * @return a human readable representation of this prediction.
   */
  public String toString() {

    StringBuffer sb = new StringBuffer();
    sb.append("NOM: ").append(actual()).append(" ").append(predicted());
    sb.append(' ').append(weight());
    float [] dist = distribution();
    for (int i = 0; i < dist.length; i++) {
      sb.append(' ').append(dist[i]);
    }
    return sb.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }
}

