/**
 *  Copyright 2007-2015 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.isi.pegasus.planner.estimate;

import edu.isi.pegasus.planner.classes.ADag;
import edu.isi.pegasus.planner.classes.Job;
import edu.isi.pegasus.planner.classes.PegasusBag;
import java.util.Map;

/**
 * An interface to allow us to estimate job characteristics
 *
 * @author Karan Vahi
 */
public interface Estimator {
    
    /**
     * Initialization method
     * 
     * @param dag  the workflow 
     * @param bag  bag of Pegasus initialization objects. 
     */
    public void initialize(ADag dag, PegasusBag bag);
    
    /**
     * Returns all estimates for a job
     * 
     * @param job
     * 
     * @return 
     */
    public Map<String,String> getAllEstimates(Job job );
    
    /**
     * Return the estimated Runtime of a job
     * 
     * @param job  the job for which estimation is required
     * 
     * @return 
     */
    public String getRuntime( Job job );
    
    /**
     * Return the estimated memory requirements of a job
     * 
     * @param job  the job for which estimation is required
     * 
     * @return the memory usage
     */
    public String getMemory( Job job );
    
}
