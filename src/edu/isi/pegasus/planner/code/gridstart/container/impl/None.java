/**
 *  Copyright 2007-2017 University Of Southern California
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

package edu.isi.pegasus.planner.code.gridstart.container.impl;

import edu.isi.pegasus.planner.classes.ADag;
import edu.isi.pegasus.planner.classes.AggregatedJob;
import edu.isi.pegasus.planner.classes.Job;
import edu.isi.pegasus.planner.classes.PegasusBag;

import edu.isi.pegasus.planner.namespace.Pegasus;

import java.io.File;
import java.io.IOException;

/**
 * An interface to determine how a job gets wrapped to be launched on various 
 * containers, as a shell-script snippet that can be embedded in PegasusLite
 *
 * @author vahi
 */
public class None extends Abstract {
    
    
    /**
     * Initiailizes the Container  shell wrapper
     * @param bag 
     * @param dag 
     */
    public void initialize( PegasusBag bag, ADag dag ){
        super.initialize(bag, dag);
    }
    
    /**
     * Returns the snippet to wrap a single job execution
     * In this implementation we don't wrap with any container, just plain
     * shell invocation is returned.
     * 
     * @param job
     * 
     * @return 
     */
    public String wrap( Job job ){
        StringBuilder sb = new StringBuilder();
        String ckpt_style = (String)job.vdsNS.get( Pegasus.CHECKPOINT_STYLE_KEY );
        System.err.println("Pegasus.CHECKPOINT_STYLE_KEY = " + ckpt_style);

        sb.append( super.inputFilesToPegasusLite(job) );
        sb.append( super.enableForIntegrity(job) );

        if ( ckpt_style.equalsIgnoreCase( Pegasus.CONDOR_STYLE ) ) {
            //condor vanilla universe checkpointing
            String ckpt_signal = (String)job.vdsNS.get( Pegasus.CHECKPOINT_CONDOR_SIGNAL_KEY );
            System.err.println("Pegasus.CHECKPOINT_CONDOR_SIGNAL_KEY= " + ckpt_signal);
            String ckpt_action = (String)job.vdsNS.get( Pegasus.CHECKPOINT_CONDOR_ACTION_KEY );
            System.err.println("Pegasus.CHECKPOINT_CONDOR_ACTION_KEY = " + ckpt_action );

            job.condorVariables.construct( "+WantCheckpointSignal", "true" );
            job.condorVariables.construct( "+WantFTOnCheckpoint", "true" );
            job.condorVariables.construct( "+CheckpointSig", ckpt_signal );

            job.condorVariables.construct( "+CheckpointExitBySignal", "true" );
            job.condorVariables.construct( "+SuccessCheckpointExitBySignal", "true" );
            job.condorVariables.construct( "+CheckpointExitSignal", ckpt_signal );
            job.condorVariables.construct( "+SuccessCheckpointExitSignal", ckpt_signal );

            sb.append( "# Adding condor signal handler" ).append( '\n' );
            if ( ckpt_action.equalsIgnoreCase( "wait_and_exit" ) ) {
                sb.append( "# On signal wait and exit" ).append( '\n' );
            } else if ( ckpt_action.equalsIgnoreCase( "stop_and_exit" ) ) {
                sb.append( "# On signal stop and exit" ).append( '\n' );
            } else {
                StringBuilder error = new StringBuilder();
                error.append( "Job " ).append( job.getID() ).
                  append( " Invalid ").append( Pegasus.CHECKPOINT_CONDOR_ACTION_KEY ).
                  append( " associated: " ).append( ckpt_action );
                throw new RuntimeException( error.toString() );
            }
            sb.append( "set +e" ).append( '\n' );//PM-701
            sb.append( "job_ec=0" ).append( "\n" );

            appendStderrFragment( sb, Abstract.PEGASUS_LITE_MESSAGE_PREFIX, "Executing the user task" );
            sb.append( job.getRemoteExecutable() ).append( job.getArguments() ).append( '\n' );
            //capture exitcode of the job
            sb.append( "job_ec=$?" ).append( "\n" );
            sb.append( "set -e" ).append( '\n' );//PM-701
        } else {
            sb.append( "set +e" ).append( '\n' );//PM-701
            sb.append( "job_ec=0" ).append( "\n" );

            appendStderrFragment( sb, Abstract.PEGASUS_LITE_MESSAGE_PREFIX, "Executing the user task" );
            sb.append( job.getRemoteExecutable() ).append( job.getArguments() ).append( '\n' );
            //capture exitcode of the job
            sb.append( "job_ec=$?" ).append( "\n" );
            sb.append( "set -e" ).append( '\n' );//PM-701
        }
        sb.append( super.outputFilesToPegasusLite(job) );
        return sb.toString();
    }
    
    /**
     * Returns the snippet to wrap a single job execution
     * 
     * @param job
     * 
     * @return 
     */
    public String wrap( AggregatedJob job ){
        StringBuilder sb = new StringBuilder();
        
        sb.append( super.inputFilesToPegasusLite(job) );
        sb.append( super.enableForIntegrity(job) );
        sb.append( "set +e" ).append( '\n' );//PM-701
        sb.append( "job_ec=0" ).append( "\n" );
        
        try{
            appendStderrFragment( sb, Abstract.PEGASUS_LITE_MESSAGE_PREFIX, "Executing the user's clustered task" );
            //for clustered jobs we embed the contents of the input
            //file in the shell wrapper itself
            sb.append( job.getRemoteExecutable() ).append( " " ).append( job.getArguments() );
            sb.append( " << EOF" ).append( '\n' );

            //PM-833 figure out the job submit directory
            String jobSubmitDirectory = new File( job.getFileFullPath( mSubmitDir, ".in" )).getParent();

            sb.append( slurpInFile( jobSubmitDirectory, job.getStdIn() ) );
            sb.append( "EOF" ).append( '\n' );

            //capture exitcode of the job
            sb.append( "job_ec=$?" ).append( "\n" );
        
            
            //rest the jobs stdin
            job.setStdIn( "" );
            job.condorVariables.removeKey( "input" );
        }
        catch( IOException ioe ){
            throw new RuntimeException( "[Pegasus-Lite] Error while wrapping job " + job.getID(), ioe );
        }

        sb.append( "set -e" ).append( '\n' );//PM-701
        sb.append( super.outputFilesToPegasusLite(job) );
        return sb.toString();
    }
    
    /**
     * Return the description 
     * @return 
     */
    public String describe(){
        return "No container wrapping";
    }
}
