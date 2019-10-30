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
 *    ResidualSplit.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.lmt;

import weka.classifiers.trees.j48.ClassifierSplitModel;
import weka.classifiers.trees.j48.Distribution;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Helper class for logistic model trees (weka.classifiers.trees.lmt.LMT) to implement the 
 * splitting criterion based on residuals of the LogitBoost algorithm.
 * 
 * @author Niels Landwehr
 * @version $Revision: 8034 $
 */
public class ResidualSplit
  extends ClassifierSplitModel{

  /** for serialization */
  private static final long serialVersionUID = -5055883734183713525L;
  
  /**The attribute selected for the split*/
  protected Attribute m_attribute;

  /**The index of the attribute selected for the split*/
  protected int m_attIndex;

  /**Number of instances in the set*/
  protected int m_numInstances;

  /**Number of classed*/
  protected int m_numClasses;

  /**The set of instances*/
  protected Instances m_data;

  /**The Z-values (LogitBoost response) for the set of instances*/
  protected float[][] m_dataZs;

  /**The LogitBoost-weights for the set of instances*/
  protected float[][] m_dataWs; 

  /**The split point (for numeric attributes)*/
  protected float m_splitPoint;

  /**
   *Creates a split object
   *@param attIndex the index of the attribute to split on 
   */    
  public ResidualSplit(int attIndex) {	
    m_attIndex = attIndex;              
  }

  /**
   * Builds the split.
   * Needs the Z/W values of LogitBoost for the set of instances.
   */
  public void buildClassifier(Instances data, float[][] dataZs, float[][] dataWs) 
    throws Exception {

    m_numClasses = data.numClasses();	
    m_numInstances = data.numInstances();
    if (m_numInstances == 0) throw new Exception("Can't build split on 0 instances");

    //save data/Zs/Ws
    m_data = data;
    m_dataZs = dataZs;
    m_dataWs = dataWs;
    m_attribute = data.attribute(m_attIndex);

    //determine number of subsets and split point for numeric attributes
    if (m_attribute.isNominal()) {
      m_splitPoint = 0.0f;
      m_numSubsets = m_attribute.numValues();
    } else {
      getSplitPoint();
      m_numSubsets = 2;
    }
    //create distribution for data
    m_distribution = new Distribution(data, this);	
  }


  /**
   * Selects split point for numeric attribute.
   */
  protected boolean getSplitPoint() throws Exception{

    //compute possible split points
    float[] splitPoints = new float[m_numInstances];
    int numSplitPoints = 0;

    Instances sortedData = new Instances(m_data);
    sortedData.sort(sortedData.attribute(m_attIndex));

    float last, current;

    last = sortedData.instance(0).value(m_attIndex);	

    for (int i = 0; i < m_numInstances - 1; i++) {
      current = sortedData.instance(i+1).value(m_attIndex);	
      if (!Utils.eq(current, last)){
	splitPoints[numSplitPoints++] = (last + current) / 2.0f;
      }
      last = current;
    }

    //compute entropy for all split points
    float[] entropyGain = new float[numSplitPoints];

    for (int i = 0; i < numSplitPoints; i++) {
      m_splitPoint = splitPoints[i];
      entropyGain[i] = entropyGain();
    }

    //get best entropy gain
    int bestSplit = -1;
    float bestGain = -Float.MAX_VALUE;

    for (int i = 0; i < numSplitPoints; i++) {
      if (entropyGain[i] > bestGain) {
	bestGain = entropyGain[i];
	bestSplit = i;
      }
    }

    if (bestSplit < 0) return false;

    m_splitPoint = splitPoints[bestSplit];	
    return true;
  }

  /**
   * Computes entropy gain for current split.
   */
  public float entropyGain() throws Exception{

    int numSubsets;
    if (m_attribute.isNominal()) {
      numSubsets = m_attribute.numValues();
    } else {
      numSubsets = 2;
    }

    float[][][] splitDataZs = new float[numSubsets][][];
    float[][][] splitDataWs = new float[numSubsets][][];

    //determine size of the subsets
    int[] subsetSize = new int[numSubsets];
    for (int i = 0; i < m_numInstances; i++) {
      int subset = whichSubset(m_data.instance(i));
      if (subset < 0) throw new Exception("ResidualSplit: no support for splits on missing values");
      subsetSize[subset]++;
    }

    for (int i = 0; i < numSubsets; i++) {
      splitDataZs[i] = new float[subsetSize[i]][];
      splitDataWs[i] = new float[subsetSize[i]][];
    }


    int[] subsetCount = new int[numSubsets];

    //sort Zs/Ws into subsets
    for (int i = 0; i < m_numInstances; i++) {
      int subset = whichSubset(m_data.instance(i));
      splitDataZs[subset][subsetCount[subset]] = m_dataZs[i];
      splitDataWs[subset][subsetCount[subset]] = m_dataWs[i];
      subsetCount[subset]++;
    }

    //calculate entropy gain
    float entropyOrig = entropy(m_dataZs, m_dataWs);

    float entropySplit = 0.0f;

    for (int i = 0; i < numSubsets; i++) {
      entropySplit += entropy(splitDataZs[i], splitDataWs[i]);
    }

    return entropyOrig - entropySplit;
  }

  /**
   * Helper function to compute entropy from Z/W values.
   */
  protected float entropy(float[][] dataZs, float[][] dataWs){
    //method returns entropy * sumOfWeights
    float entropy = 0.0f;
    int numInstances = dataZs.length;

    for (int j = 0; j < m_numClasses; j++) {

      //compute mean for class
      float m = 0.0f;
      float sum = 0.0f;
      for (int i = 0; i < numInstances; i++) {
	m += dataZs[i][j] * dataWs[i][j];
	sum += dataWs[i][j];
      }
      m /= sum;

      //sum up entropy for class
      for (int i = 0; i < numInstances; i++) {
	entropy += dataWs[i][j] * Math.pow(dataZs[i][j] - m,2);
      }

    }

    return entropy;
  }

  /**
   * Checks if there are at least 2 subsets that contain >= minNumInstances.
   */
  public boolean checkModel(int minNumInstances){
    //checks if there are at least 2 subsets that contain >= minNumInstances
    int count = 0;
    for (int i = 0; i < m_distribution.numBags(); i++) {
      if (m_distribution.perBag(i) >= minNumInstances) count++; 
    }
    return (count >= 2);
  }

  /**
   * Returns name of splitting attribute (left side of condition).
   */
  public final String leftSide(Instances data) {

    return data.attribute(m_attIndex).name();
  }

  /**
   * Prints the condition satisfied by instances in a subset.
   */
  public final String rightSide(int index,Instances data) {

    StringBuffer text;

    text = new StringBuffer();
    if (data.attribute(m_attIndex).isNominal())
      text.append(" = ").append(data.attribute(m_attIndex).value(index));
    else
      if (index == 0)
	text.append(" <= ").append(Utils.doubleToString(m_splitPoint,6));
      else
	text.append(" > ").append(Utils.doubleToString(m_splitPoint,6));
    return text.toString();
  }

  public final int whichSubset(Instance instance) 
  throws Exception {

    if (instance.isMissing(m_attIndex))
      return -1;
    else{
      if (instance.attribute(m_attIndex).isNominal())
	return (int)instance.value(m_attIndex);
      else
	if (Utils.smOrEq(instance.value(m_attIndex),m_splitPoint))
	  return 0;
	else
	  return 1;
    }
  }    

  /** Method not in use*/
  public void buildClassifier(Instances data) {
    //method not in use
  }

  /**Method not in use*/
  public final float [] weights(Instance instance){
    //method not in use
    return null;
  } 

  /**Method not in use*/
  public final String sourceExpression(int index, Instances data) {
    //method not in use
    return "";
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
