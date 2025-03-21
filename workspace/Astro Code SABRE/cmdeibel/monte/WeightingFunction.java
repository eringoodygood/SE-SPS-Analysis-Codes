/***************************************************************
 * Nuclear Simulation Java Class Libraries
 * Copyright (C) 2003 Yale University
 * 
 * Original Developer
 *     Dale Visser (dale@visser.name)
 * 
 * OSI Certified Open Source Software
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the University of Illinois/NCSA 
 * Open Source License.
 * 
 * This program is distributed in the hope that it will be 
 * useful, but without any warranty; without even the implied 
 * warranty of merchantability or fitness for a particular 
 * purpose. See the University of Illinois/NCSA Open Source 
 * License for more details.
 * 
 * You should have received a copy of the University of 
 * Illinois/NCSA Open Source License along with this program; if 
 * not, see http://www.opensource.org/
 **************************************************************/
/*
 * WeightingFunction.java
 *
 * Created on March 9, 2001, 2:50 PM
 */

package cmdeibel.monte;

/**
 * Used by <code>DataSet</code> to define a weighting function
 * in obtaining summary statistics.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale Visser</a>
 */
public interface WeightingFunction {
    
    public double weight(double value);

}
