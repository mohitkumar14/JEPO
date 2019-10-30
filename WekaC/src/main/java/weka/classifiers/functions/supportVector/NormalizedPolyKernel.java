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
 *    NormalizedPolyKernel.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions.supportVector;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;

/**
 <!-- globalinfo-start -->
 * The normalized polynomial kernel.<br/>
 * K(x,y) = &lt;x,y&gt;/sqrt(&lt;x,x&gt;&lt;y,y&gt;) where &lt;x,y&gt; = PolyKernel(x,y)
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Enables debugging output (if available) to be printed.
 *  (default: off)</pre>
 *
 * <pre> -C &lt;num&gt;
 *  The size of the cache (a prime number), 0 for full cache and 
 *  -1 to turn it off.
 *  (default: 250007)</pre>
 * 
 * <pre> -E &lt;num&gt;
 *  The Exponent to use.
 *  (default: 1.0)</pre>
 * 
 * <pre> -L
 *  Use lower-order terms.
 *  (default: no)</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 14512 $
 */
public class NormalizedPolyKernel 
  extends PolyKernel {

  /** for serialization */
  static final long serialVersionUID = 1248574185532130851L;

  /**
   * default constructor - does nothing
   */
  public NormalizedPolyKernel() {
    super();

    setExponent(2.0f);
  }
  
  /**
   * Creates a new <code>NormalizedPolyKernel</code> instance.
   *
   * @param dataset	the training dataset used.
   * @param cacheSize	the size of the cache (a prime number)
   * @param exponent	the exponent to use
   * @param lowerOrder	whether to use lower-order terms
   * @throws Exception	if something goes wrong
   */
  public NormalizedPolyKernel(Instances dataset, int cacheSize, 
      float exponent, boolean lowerOrder) throws Exception {
	
    super(dataset, cacheSize, exponent, lowerOrder);
  }
  
  /**
   * Returns a string describing the kernel
   * 
   * @return a description suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "The normalized polynomial kernel.\nK(x,y) = <x,y>/sqrt(<x,x><y,y>) where <x,y> = PolyKernel(x,y)";
  }
   
  /**
   * Computes the result of the kernel function for two instances.
   * If id1 == -1, eval use inst1 instead of an instance in the dataset.
   * Redefines the eval function of PolyKernel.
   *
   * @param id1 the index of the first instance in the dataset
   * @param id2 the index of the second instance in the dataset
   * @param inst1 the instance corresponding to id1 (used if id1 == -1)
   * @return the result of the kernel function
   * @throws Exception if something goes wrong
   */
  public float eval(int id1, int id2, Instance inst1) 
    throws Exception {

    float div = (float) Math.sqrt(super.eval(id1, id1, inst1) * ((m_keys != null)
                           ? super.eval(id2, id2, m_data.instance(id2))
                           : super.eval(-1, -1, m_data.instance(id2))));

    if(div != 0){      
      return super.eval(id1, id2, inst1) / div;
    } else {
      return 0;
    }
  }    
  
  /**
   * Sets the exponent value (must be different from 1.0).
   * 
   * @param value	the exponent value
   */
  public void setExponent(float value) {
    if (value != 1.0)
      super.setExponent(value);
    else
      System.out.println("A linear kernel, i.e., Exponent=1, is not possible!");
  }
  
  /**
   * returns a string representation for the Kernel
   * 
   * @return 		a string representaiton of the kernel
   */
  public String toString() {
    String	result;
    
    if (getUseLowerOrder())
      result = new StringBuilder("Normalized Poly Kernel with lower order: K(x,y) = (<x,y>+1)^").append(getExponent()).append("/")
.append("((<x,x>+1)^").append(getExponent()).append("*").append("(<y,y>+1)^").append(getExponent()).append(")^(1/2)").toString();
    else
      result = new StringBuilder("Normalized Poly Kernel: K(x,y) = <x,y>^").append(getExponent()).append("/").append("(<x,x>^")
.append(getExponent()).append("*").append("<y,y>^").append(getExponent()).append(")^(1/2)").toString();
    
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 14512 $");
  }
}

