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
 *    YongSplitInfo.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.m5;

import java.io.Serializable;

import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Stores split information.
 * 
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 11269 $
 */
public final class YongSplitInfo implements Cloneable, Serializable,
  SplitEvaluate, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = 1864267581079767881L;

  private int number; // number of total instances
  private int first; // first instance index
  private int last; // last instance index
  private int position; // position of maximum impurity reduction
  private float maxImpurity; // maximum impurity reduction
  private float leftAve; // left average class value
  private float rightAve; // right average class value
  private int splitAttr; // spliting attribute
  private float splitValue; // splitting value

  /**
   * Constructs an object which contains the split information
   * 
   * @param low the index of the first instance
   * @param high the index of the last instance
   * @param attr an attribute
   */
  public YongSplitInfo(int low, int high, int attr) {
    number = high - low + 1;
    first = low;
    last = high;
    position = -1;
    maxImpurity = -1.e20f;
    splitAttr = attr; // attr < 0 is an empty object
    splitValue = 0.0f;
  }

  /**
   * Makes a copy of this SplitInfo object
   */
  @Override
  public final SplitEvaluate copy() throws Exception {

    YongSplitInfo s = (YongSplitInfo) this.clone();

    return s;
  }

  /**
   * Resets the object of split information
   * 
   * @param low the index of the first instance
   * @param high the index of the last instance
   * @param attr the attribute
   */
  public final void initialize(int low, int high, int attr) {

    number = high - low + 1;
    first = low;
    last = high;
    position = -1;
    maxImpurity = -1.e20f;
    splitAttr = attr;
    splitValue = 0.0f;
  }

  /**
   * Converts the spliting information to string
   * 
   * @param inst the instances
   */
  public final String toString(Instances inst) {

    StringBuffer text = new StringBuffer();

    text.append("Print SplitInfo:\n");
    text.append("    Instances:\t\t").append(number).append(" (").append(first).append("-").append(position
    		).append(",").append((position + 1)).append("-").append(last).append(")\n");
    text.append("    Maximum Impurity Reduction:\t")
    .append(Utils.doubleToString(maxImpurity, 1, 4)).append("\n");
    text.append("    Left average:\t").append(leftAve).append("\n");
    text.append("    Right average:\t").append(rightAve).append("\n");
    if (maxImpurity > 0.0) {
      text.append("    Splitting function:\t")
      .append(inst.attribute(splitAttr).name()).append(" = ").append(splitValue).append("\n");
    } else {
      text.append("    Splitting function:\tnull\n");
    }

    return text.toString();
  }

  /**
   * Finds the best splitting point for an attribute in the instances
   * 
   * @param attr the splitting attribute
   * @param inst the instances
   * @exception Exception if something goes wrong
   */
  @Override
  public final void attrSplit(int attr, Instances inst) throws Exception {
    int i, len, part;
    Impurity imp;

    int low = 0;
    int high = inst.numInstances() - 1;
    this.initialize(low, high, attr);
    if (number < 4) {
      return;
    }

    if((high - low + 1) < 5)
    	len = 1;
    else
    	len = (high - low + 1) / 5;

    position = low;

    part = low + len - 1;
    imp = new Impurity(part, attr, inst, 5);

    for (i = low + len; i <= high - len - 1; i++) {

      imp.incremental(inst.instance(i).classValue(), 1);

      if (Utils.eq(inst.instance(i + 1).value(attr),
        inst.instance(i).value(attr)) == false) {
        if (imp.impurity > maxImpurity) {
          maxImpurity = imp.impurity;
          splitValue = (inst.instance(i).value(attr) + inst.instance(i + 1)
            .value(attr)) * 0.5f;
          leftAve = imp.sl / imp.nl;
          rightAve = imp.sr / imp.nr;
          position = i;
        }
      }
    }
  }

  /**
   * Returns the impurity of this split
   * 
   * @return the impurity of this split
   */
  @Override
  public float maxImpurity() {
    return maxImpurity;
  }

  /**
   * Returns the attribute used in this split
   * 
   * @return the attribute used in this split
   */
  @Override
  public int splitAttr() {
    return splitAttr;
  }

  /**
   * Returns the position of the split in the sorted values. -1 indicates that a
   * split could not be found.
   * 
   * @return an <code>int</code> value
   */
  @Override
  public int position() {
    return position;
  }

  /**
   * Returns the split value
   * 
   * @return the split value
   */
  @Override
  public float splitValue() {
    return splitValue;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11269 $");
  }
}
