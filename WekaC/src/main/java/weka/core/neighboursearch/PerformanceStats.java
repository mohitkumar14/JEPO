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
 * PerformanceStats.java
 * Copyright (C) 2007-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core.neighboursearch;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.AdditionalMeasureProducer;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * The class that measures the performance of a nearest
 * neighbour search (NNS) algorithm.
 * 
 * @author Ashraf M. Kibriya (amk14[at-the-rate]cs[dot]waikato[dot]ac[dot]nz)
 * @version $Revision: 10141 $
 */
public class PerformanceStats
  implements AdditionalMeasureProducer, Serializable, RevisionHandler {
  
  /** for serialization. */
  private static final long serialVersionUID = -7215110351388368092L;
  
  /** The total number of queries looked at. */
  protected int m_NumQueries;
  
  //Point-stats variables
  /** The min and max data points looked for a query by 
   * the NNS algorithm. */
  public float m_MinP, m_MaxP;

  /** The sum of data points looked 
   * at for all the queries. 
   */
  public float m_SumP; 
  /** The squared sum of data points looked 
   * at for all the queries. 
   */
  public float m_SumSqP;
  /** The number of data points looked at
   * for the current/last query.
   */
  public float m_PointCount;
  
  //Coord-stats variables
  /** The min and max coordinates(attributes) looked
   * at per query. 
   */
  public float m_MinC, m_MaxC;
  /** The sum of coordinates/attributes looked at
   * for all the queries.
   */
  public float m_SumC;
  /** The squared sum of coordinates/attributes looked at
   * for all the queries.
   */
  public float m_SumSqC;
  /**
   * The number of coordinates looked at for
   * the current/last query.
   */
  public float m_CoordCount;
  
  /**
   * default constructor.
   */
  public PerformanceStats() { 
    reset(); 
  }
  
  /**
   * Resets all internal fields/counters.
   */ 
  public void reset() {
    m_NumQueries = 0;
    //point stats
    m_SumP = m_SumSqP = m_PointCount = 0;
    m_MinP = Integer.MAX_VALUE;
    m_MaxP = Integer.MIN_VALUE;
    //coord stats
    m_SumC = m_SumSqC = m_CoordCount = 0;
    m_MinC = Integer.MAX_VALUE;
    m_MaxC = Integer.MIN_VALUE;
  }
  
  /**
   * Signals start of the nearest neighbour search.
   * Initializes the stats object.
   */
  public void searchStart() {
    m_PointCount = 0;
    m_CoordCount = 0;
  }
  
  /**
   * Signals end of the nearest neighbour search.
   * Calculates the statistics for the search.
   */
  public void searchFinish() {
    m_NumQueries++;  m_SumP += m_PointCount;  m_SumSqP += m_PointCount*m_PointCount;
    if (m_PointCount < m_MinP) m_MinP = m_PointCount;
    if (m_PointCount > m_MaxP) m_MaxP = m_PointCount;
    //coord stats
    float coordsPerPt = m_CoordCount / m_PointCount;;
    m_SumC += coordsPerPt; m_SumSqC += coordsPerPt*coordsPerPt; 
    if(coordsPerPt < m_MinC) m_MinC = coordsPerPt;
    if(coordsPerPt > m_MaxC) m_MaxC = coordsPerPt;
  }
  
  /**
   * Increments the point count 
   * (number of datapoints looked at).
   */
  public void incrPointCount() {
    m_PointCount++;
  }
  
  /**
   * Increments the coordinate count
   * (number of coordinates/attributes 
   * looked at).
   */
  public void incrCoordCount() {
    m_CoordCount++;
  }
  
  /**
   * adds the given number to the point count.
   * 
   * @param n The number to add to the point count.
   */
  public void updatePointCount(int n) {
    m_PointCount += n;
  }
  
  /**
   * Returns the number of queries.
   * 
   * @return The number of queries.
   */
  public int getNumQueries() {
    return m_NumQueries;
  }
  
  /**
   * Returns the total number of points visited.
   * 
   * @return The total number.
   */
  public float getTotalPointsVisited() {
    return m_SumP;
  }
  
  /**
   * Returns the mean of points visited.
   * 
   * @return The mean points visited.
   */
  public float getMeanPointsVisited() {
    return m_SumP/(float)m_NumQueries;
  }
  
  /** 
   * Returns the standard deviation of points visited.
   * 
   * @return The standard deviation.
   */
  public float getStdDevPointsVisited() {
    return (float) Math.sqrt((m_SumSqP - (m_SumP*m_SumP)/(float)m_NumQueries)/(m_NumQueries-1));
  }
  
  /**
   * Returns the minimum of points visited.
   * 
   * @return The minimum.
   */
  public float getMinPointsVisited() {
    return m_MinP;
  }
  
  /**
   * Returns the maximum of points visited.
   * 
   * @return The maximum.
   */
  public float getMaxPointsVisited() {
    return m_MaxP;
  }

  /*************----------Coord Stat functions---------**************/
  
  /**
   * Returns the total sum of coords per point.
   * 
   * @return The total per point.
   */
  public float getTotalCoordsPerPoint() {
    return m_SumC;
  }
  
  /**
   * Returns the mean of coords per point.
   * 
   * @return The mean.
   */
  public float getMeanCoordsPerPoint() {
    return m_SumC/(float)m_NumQueries;
  } 
  
  /**
   * Returns the standard deviation of coords per point.
   * 
   * @return The standard deviation.
   */
  public float getStdDevCoordsPerPoint() {
    return (float) Math.sqrt((m_SumSqC - (m_SumC*m_SumC)/(float)m_NumQueries)/(m_NumQueries-1));
  }
  
  /**
   * Returns the minimum of coords per point.
   * 
   * @return The minimum.
   */
  public float getMinCoordsPerPoint() {
    return m_MinC;
  }
  
  /**
   * Returns the maximum of coords per point.
   * 
   * @return The maximum.
   */
  public float getMaxCoordsPerPoint() {
    return m_MaxC;
  }

  /*****----MiscFunctions----****/
  
  /**
   * Returns an enumeration of the additional measure names.
   * 
   * @return An enumeration of the measure names.
   */
  public Enumeration<String> enumerateMeasures() {
    Vector<String> newVector = new Vector<String>();
    
    newVector.addElement("measureTotal_points_visited");
    newVector.addElement("measureMean_points_visited");
    newVector.addElement("measureStdDev_points_visited");
    newVector.addElement("measureMin_points_visited");
    newVector.addElement("measureMax_points_visited");
    //coord stats
    newVector.addElement("measureTotalCoordsPerPoint");
    newVector.addElement("measureMeanCoordsPerPoint");
    newVector.addElement("measureStdDevCoordsPerPoint");
    newVector.addElement("measureMinCoordsPerPoint");
    newVector.addElement("measureMaxCoordsPerPoint");
    
    return newVector.elements();
  }
  
  /**
   * Returns the value of the named measure.
   * 
   * @param additionalMeasureName The name of the measure to query for 
   * its value.
   * @return The value of the named measure.
   * @throws IllegalArgumentException If the named measure is not 
   * supported.
   */
  public float getMeasure(String additionalMeasureName) throws IllegalArgumentException {
    if (additionalMeasureName.compareToIgnoreCase("measureTotal_points_visited") == 0) {
      return (float) getTotalPointsVisited();
    } else if (additionalMeasureName.compareToIgnoreCase("measureMean_points_visited") == 0) {
      return (float) getMeanPointsVisited();
    } else if (additionalMeasureName.compareToIgnoreCase("measureStdDev_points_visited") == 0) {
      return (float) getStdDevPointsVisited();
    } else if (additionalMeasureName.compareToIgnoreCase("measureMin_points_visited") == 0) {
      return (float) getMinPointsVisited();
    } else if (additionalMeasureName.compareToIgnoreCase("measureMax_points_visited") == 0) {
      return (float) getMaxPointsVisited();
    }
    //coord stats
    else if (additionalMeasureName.compareToIgnoreCase("measureTotalCoordsPerPoint") == 0) {
      return (float) getTotalCoordsPerPoint();
    } else if (additionalMeasureName.compareToIgnoreCase("measureMeanCoordsPerPoint") == 0) {
      return (float) getMeanCoordsPerPoint();
    } else if (additionalMeasureName.compareToIgnoreCase("measureStdDevCoordsPerPoint") == 0) {
      return (float) getStdDevCoordsPerPoint();
    } else if (additionalMeasureName.compareToIgnoreCase("measureMinCoordsPerPoint") == 0) {
      return (float) getMinCoordsPerPoint();
    } else if (additionalMeasureName.compareToIgnoreCase("measureMaxCoordsPerPoint") == 0) {
      return (float) getMaxCoordsPerPoint();
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
			  + " not supported by PerformanceStats.");
    }
  }
  
  /**
   * Returns a string representation of the statistics.
   * 
   * @return The statistics as string.
   */
  public String getStats() {
    StringBuffer buf = new StringBuffer();
    
    buf.append("           min, max, total, mean, stddev\n");
    buf.append("Points:    "+getMinPointsVisited()+", "+getMaxPointsVisited()+","+getTotalPointsVisited()+
	       ","+getMeanPointsVisited()+", "+getStdDevPointsVisited()+"\n");
    
    return buf.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10141 $");
  }
}
