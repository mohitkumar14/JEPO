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
 *    Impurity.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.m5;

import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * Class for handling the impurity values when spliting the instances
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public final class Impurity
  implements RevisionHandler {
  
  float n;                   // number of total instances 
  int attr;                   // splitting attribute 
  float nl;                  // number of instances in the left group 
  float nr;                  // number of instances in the right group 
  float sl;                  // sum of the left group  
  float sr;                  // sum of the right group 
  float s2l;                 // squared sum of the left group 
  float s2r;                 // squared sum of the right group 
  float sdl;                 // standard deviation of the left group 
  float sdr;                 // standard deviation of the right group 
  float vl;                  // variance of the left group 
  float vr;                  // variance of the right group 
  float sd;                  // overall standard deviation 
  float va;                  // overall variance 

  float impurity;            // impurity value;
  int order;                  // order = 1, variance; order = 2, standard deviation; order = 3, the cubic root of the variance;  
                              // order = k, the k-th order root of the variance

  /**
   * Constructs an Impurity object containing the impurity values of partitioning the instances using an attribute
   * @param partition the index of the last instance in the left subset
   * @param attribute the attribute used in partitioning
   * @param inst instances
   * @param k the order of the impurity; =1, the variance; =2, the stardard deviation; =k, the k-th order root of the variance
   */
  public Impurity(int partition,int attribute,Instances inst,int k){

    Values values = new Values(0,inst.numInstances()-1,inst.classIndex(),inst);
    attr = attribute;
    n   = inst.numInstances();
    sd  = values.sd; 
    va  = values.va;

    values = new Values(0,partition,inst.classIndex(),inst);
    nl  = partition + 1;
    sl  = values.sum;
    s2l = values.sqrSum;

    values = new Values(partition+1,inst.numInstances()-1,inst.classIndex(),inst);
    nr  = inst.numInstances() - partition -1;
    sr  = values.sum;
    s2r = values.sqrSum;

    order = k;
    this.incremental(0,0);
  }

  /**
   * Converts an Impurity object to a string
   * @return the converted string
   */
  public final String  toString() {
    
    StringBuffer text = new StringBuffer();
    
    text.append("Print impurity values:\n");
    text.append("    Number of total instances:\t").append(n).append("\n");
    text.append("    Splitting attribute:\t\t").append(attr).append("\n");
    text.append("    Number of the instances in the left:\t").append(nl).append("\n");
    text.append("    Number of the instances in the right:\t").append(nr).append("\n");
    text.append("    Sum of the left:\t\t\t").append(sl).append("\n");
    text.append("    Sum of the right:\t\t\t").append(sr).append("\n");
    text.append("    Squared sum of the left:\t\t").append(s2l).append("\n"); 
    text.append("    Squared sum of the right:\t\t").append(s2r).append("\n");
    text.append("    Standard deviation of the left:\t").append(sdl).append("\n");
    text.append("    Standard deviation of the right:\t").append(sdr).append("\n");
    text.append("    Variance of the left:\t\t").append(vr).append("\n");
    text.append("    Variance of the right:\t\t").append(vr).append("\n");
    text.append("    Overall standard deviation:\t\t").append(sd).append("\n");
    text.append("    Overall variance:\t\t\t").append(va).append("\n");
    text.append("    Impurity (order ").append(order).append("):\t\t").append(impurity).append("\n");

    return text.toString();
  }
  
  /**
   * Incrementally computes the impurirty values
   * @param value the incremental value 
   * @param type if type=1, value will be added to the left subset; type=-1, to the right subset; type=0, initializes
   */
  public final void  incremental(float value,int type){
    float y=0.f,yl=0.f,yr=0.f;

    switch(type){
    case 1:
      nl += 1;
      nr -= 1;
      sl += value;
      sr -= value;
      s2l += value*value;
      s2r -= value*value;
      break;
    case -1:
      nl -= 1;
      nr += 1;
      sl -= value;
      sr += value;
      s2l -= value*value;
      s2r += value*value;
      break;
    case 0:
      break;
    default: System.err.println("wrong type in Impurity.incremental().");
    }

    if(nl<=0.0){
      vl=0.0f;
      sdl=0.0f;
    }
    else {
      vl = (nl*s2l-sl*sl)/((float)nl*((float)nl));
      vl = Math.abs(vl);
      sdl = (float) Math.sqrt(vl);
    }
    if(nr<=0.0){
      vr=0.0f;
      sdr=0.0f;
    }
    else {
      vr = (nr*s2r-sr*sr)/((float)nr*((float)nr));
      vr = Math.abs(vr);
      sdr = (float) Math.sqrt(vr);
    }

    if(order <= 0)System.err.println("Impurity order less than zero in Impurity.incremental()");
    else if(order == 1) {
      y = va; yl = vl; yr = vr;
    } else {
      y  = (float) Math.pow(va,1./order);
      yl = (float) Math.pow(vl,1./order);
      yr = (float) Math.pow(vr,1./order);
    }

    if(nl<=0.0 || nr<=0.0)
      impurity = 0.0f;
    else { 
      impurity = y - ((float)nl/(float)n)*yl - ((float)nr/(float)n)*yr;
    }
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
