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
 *    ChartEvent.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.EventObject;
import java.util.Vector;

/**
 * Event encapsulating info for plotting a data point on the StripChart
 * 
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 10216 $
 */
public class ChartEvent extends EventObject {

  /** for serialization */
  private static final long serialVersionUID = 7812460715499569390L;

  private Vector<String> m_legendText;
  private float m_max;
  private float m_min;

  private boolean m_reset;

  /**
   * Y values of the data points
   */
  private float[] m_dataPoint;

  /**
   * Creates a new <code>ChartEvent</code> instance.
   * 
   * @param source the source of the event
   * @param legendText a vector of strings to display in the legend
   * @param min minimum y value
   * @param max maximum y value
   * @param dataPoint an array of y values to plot
   * @param reset true if display is to be reset
   */
  public ChartEvent(Object source, Vector<String> legendText, float min,
    float max, float[] dataPoint, boolean reset) {
    super(source);
    m_legendText = legendText;
    m_max = max;
    m_min = min;
    m_dataPoint = dataPoint;
    m_reset = reset;
  }

  /**
   * Creates a new <code>ChartEvent</code> instance.
   * 
   * @param source the source of the event
   */
  public ChartEvent(Object source) {
    super(source);
  }

  /**
   * Get the legend text vector
   * 
   * @return a <code>Vector</code> value
   */
  public Vector<String> getLegendText() {
    return m_legendText;
  }

  /**
   * Set the legend text vector
   * 
   * @param lt a <code>Vector</code> value
   */
  public void setLegendText(Vector<String> lt) {
    m_legendText = lt;
  }

  /**
   * Get the min y value
   * 
   * @return a <code>double</code> value
   */
  public float getMin() {
    return m_min;
  }

  /**
   * Set the min y value
   * 
   * @param m a <code>double</code> value
   */
  public void setMin(float m) {
    m_min = m;
  }

  /**
   * Get the max y value
   * 
   * @return a <code>double</code> value
   */
  public float getMax() {
    return m_max;
  }

  /**
   * Set the max y value
   * 
   * @param m a <code>float</code> value
   */
  public void setMax(float m) {
    m_max = m;
  }

  /**
   * Get the data point
   * 
   * @return a <code>double[]</code> value
   */
  public float[] getDataPoint() {
    return m_dataPoint;
  }

  /**
   * Set the data point
   * 
   * @param m_dataPoint2 a <code>double[]</code> value
   */
  public void setDataPoint(float[] m_dataPoint2) {
    m_dataPoint = m_dataPoint2;
  }

  /**
   * Set the reset flag
   * 
   * @param reset a <code>boolean</code> value
   */
  public void setReset(boolean reset) {
    m_reset = reset;
  }

  /**
   * get the value of the reset flag
   * 
   * @return a <code>boolean</code> value
   */
  public boolean getReset() {
    return m_reset;
  }
}
