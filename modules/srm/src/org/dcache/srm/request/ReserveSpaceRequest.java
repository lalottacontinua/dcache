/*
COPYRIGHT STATUS:
  Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
  software are sponsored by the U.S. Department of Energy under Contract No.
  DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
  non-exclusive, royalty-free license to publish or reproduce these documents
  and software for U.S. Government purposes.  All documents and software
  available from this server are protected under the U.S. and Foreign
  Copyright Laws, and FNAL reserves all rights.
 
 
 Distribution of the software available from this server is free of
 charge subject to the user following the terms of the Fermitools
 Software Legal Information.
 
 Redistribution and/or modification of the software shall be accompanied
 by the Fermitools Software Legal Information  (including the copyright
 notice).
 
 The user is asked to feed back problems, benefits, and/or suggestions
 about the software to the Fermilab Software Providers.
 
 
 Neither the name of Fermilab, the  URA, nor the names of the contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.
 
 
 
  DISCLAIMER OF LIABILITY (BSD):
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
  OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
  FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.
 
 
  Liabilities of the Government:
 
  This software is provided by URA, independent from its Prime Contract
  with the U.S. Department of Energy. URA is acting independently from
  the Government and in its own private capacity and is not acting on
  behalf of the U.S. Government, nor as its contractor nor its agent.
  Correspondingly, it is understood and agreed that the U.S. Government
  has no connection to this software and in no manner whatsoever shall
  be liable for nor assume any responsibility or obligation for any claim,
  cost, or damages arising out of or resulting from the use of the software
  available from this server.
 
 
  Export Control:
 
  All documents and software available from this server are subject to U.S.
  export control laws.  Anyone downloading information from this server is
  obligated to secure any necessary Government licenses before exporting
  documents or software obtained from this server.
 */

/*
 * FileRequest.java
 *
 * Created on July 5, 2002, 12:04 PM
 */

package org.dcache.srm.request;

import org.dcache.srm.SRMUser;
import org.dcache.srm.scheduler.FatalJobFailure;
import org.dcache.srm.scheduler.NonFatalJobFailure;
import org.dcache.srm.util.Configuration;
import org.dcache.srm.scheduler.Job;
import org.dcache.srm.scheduler.State;
import org.dcache.srm.scheduler.IllegalStateTransition;
import org.dcache.srm.v2_2.TAccessLatency;
import org.dcache.srm.v2_2.TRetentionPolicy;
import org.dcache.srm.v2_2.TRetentionPolicyInfo;
import org.dcache.srm.v2_2.TStatusCode;
import org.dcache.srm.v2_2.TReturnStatus;
import org.dcache.srm.v2_2.SrmReserveSpaceResponse;
import org.dcache.srm.v2_2.SrmStatusOfReserveSpaceRequestResponse;
import org.apache.axis.types.UnsignedLong;
import org.dcache.srm.SRMInvalidRequestException;
import org.apache.log4j.Logger;
/**
 * File request is an abstract "SRM file request"
 * its concrete subclasses are GetFileRequest,PutFileRequest and CopyFileRequest
 * File request is one in a set of file requests within a request
 * each Request is identified by its requestId
 * and each file request is identified by its fileRequestId within Request
 * File Request contains  a reference to its Request
 *
 *
 * @author  timur
 * @version
 */
public class ReserveSpaceRequest extends Request {
    private static final Logger logger =
            Logger.getLogger (ReserveSpaceRequest.class);
    
    private long sizeInBytes ;
    private TRetentionPolicy retentionPolicy =null;
    private TAccessLatency accessLatency = null;
    private String spaceToken;
    private long spaceReservationLifetime;
    
    
    /** Creates new ReserveSpaceRequest */
    public ReserveSpaceRequest(
            Long  requestCredentalId,
            SRMUser user,
            long lifetime,
            int maxNumberOfRetries,
            long sizeInBytes ,
            long spaceReservationLifetime,
            TRetentionPolicy retentionPolicy,
            TAccessLatency accessLatency,
            String description,
            String clienthost) throws Exception {
              super(user,
              requestCredentalId,
              maxNumberOfRetries,
              0,
              lifetime,
              description,
              clienthost);
        
        this.sizeInBytes = sizeInBytes ;
        if(retentionPolicy != null ) {
            this.retentionPolicy = retentionPolicy;
        }
        
        if( accessLatency != null) {
            this.accessLatency = accessLatency;
        }
        
        this.spaceReservationLifetime = spaceReservationLifetime;
        storeInSharedMemory();
        say("created");
        
    }
    
    /** this constructor is used for restoring the previously
     * saved FileRequest from persitance storage
     */
    
    
    public ReserveSpaceRequest(
            Long id,
            Long nextJobId,
            long creationTime,
            long lifetime,
            int stateId,
            String errorMessage,
            SRMUser user,
            String scheduelerId,
            long schedulerTimeStamp,
            int numberOfRetries,
            int maxNumberOfRetries,
            long lastStateTransitionTime,
            JobHistory[] jobHistoryArray,
            Long  requestCredentalId,
            long sizeInBytes,
            long spaceReservationLifetime,
            String spaceToken,
            String retentionPolicy,
            String accessLatency,
            String description,
            String clienthost,
            String statusCodeString) {
                super(id,
                nextJobId,
                creationTime,  
                lifetime,
                stateId, 
                errorMessage, 
                user,
                scheduelerId,
                schedulerTimeStamp,
                numberOfRetries, 
                maxNumberOfRetries, 
                lastStateTransitionTime,
                jobHistoryArray,//VVV
                requestCredentalId,
                0,
                false,
                description,
                clienthost,
                statusCodeString);
        this.sizeInBytes = sizeInBytes;
        this.spaceToken = spaceToken;
        
        this.retentionPolicy = retentionPolicy == null?null: TRetentionPolicy.fromString(retentionPolicy);
        this.accessLatency = accessLatency == null ?null :TAccessLatency.fromString(accessLatency);
        this.spaceReservationLifetime = spaceReservationLifetime;
        
        say("restored");
    }
    
    
    public void say(String s) {
        getStorage().log("ReserveSpaceRequest id # "+getId()+" :"+s);
    }
    
    public void esay(String s) {
        getStorage().elog("ReserveSpaceRequest id #"+getId()+" :"+s);
    }
    
    public RequestCredential getCredential() {
        return RequestCredential.getRequestCredential(credentialId);
    }
    
    /**
     * Getter for property credentialId.
     * @return Value of property credentialId.
     */
    public Long getCredentialId() {
        return credentialId;
    }
    
    public void esay(Throwable t) {
        getStorage().elog("ReserveSpaceRequest id #"+getId()+" Throwable:"+t);
        getStorage().elog(t);
    }
    
    
    public String toString() {
        return toString(false);
    }
    
    public String toString(boolean longformat) {
        StringBuffer sb = new StringBuffer();
        sb.append(" ReserveSpaceRequest ");
        sb.append(" id =").append(getId());
        sb.append(" state=").append(getState());
        if(longformat) {
            sb.append('\n').append("History of State Transitions: \n");
            sb.append(getHistory());
        }
        return sb.toString();
    }
    
    
    protected void stateChanged(State oldState) {
    }
    
    
    public void run() throws NonFatalJobFailure, FatalJobFailure {
        try{
            SrmReserveSpaceCallbacks callbacks = new SrmReserveSpaceCallbacks(this.getId());
            getStorage().srmReserveSpace(
            getUser(),
            sizeInBytes,
            spaceReservationLifetime,
            retentionPolicy == null ? null:retentionPolicy.getValue(),
            accessLatency == null ? null:accessLatency.getValue(),
            getDescription(),
            callbacks
            );
            setState(State.ASYNCWAIT,
                    "waiting Space Reservation completion");
        } catch(Exception e) {
            if(e instanceof NonFatalJobFailure ) {
                throw (NonFatalJobFailure) e;
            }
            if(e instanceof FatalJobFailure ) {
                throw (FatalJobFailure) e;
            }
            
            esay("can not reserve space: ");
            esay(e);
            try {
                setState(State.FAILED,e.toString());
            } catch(IllegalStateTransition ist) {
                esay("Illegal State Transition : " +ist.getMessage());
            }
        }
        
    }
    
    public SrmStatusOfReserveSpaceRequestResponse getSrmStatusOfReserveSpaceRequestResponse() {
        SrmStatusOfReserveSpaceRequestResponse response = 
                new SrmStatusOfReserveSpaceRequestResponse();
        response.setReturnStatus(getTReturnStatus());
        response.setRetentionPolicyInfo(new TRetentionPolicyInfo(retentionPolicy, accessLatency));
        response.setSpaceToken(getSpaceToken());
        response.setSizeOfTotalReservedSpace(new UnsignedLong(sizeInBytes) );
        response.setSizeOfGuaranteedReservedSpace(new UnsignedLong(sizeInBytes));
        response.setLifetimeOfReservedSpace(new Integer((int)(spaceReservationLifetime/1000L)));
        return response;
        
    }
    
    public SrmReserveSpaceResponse getSrmReserveSpaceResponse() {
        SrmReserveSpaceResponse response = new SrmReserveSpaceResponse();
        response.setReturnStatus(getTReturnStatus());
        response.setRetentionPolicyInfo(new TRetentionPolicyInfo(retentionPolicy, accessLatency));
        response.setRequestToken(String.valueOf(getId()));
        response.setSpaceToken(getSpaceToken());
        response.setSizeOfTotalReservedSpace(new UnsignedLong(sizeInBytes) );
        response.setSizeOfGuaranteedReservedSpace(new UnsignedLong(sizeInBytes));
        response.setLifetimeOfReservedSpace(new Integer((int)(spaceReservationLifetime/1000L)));
        return response;
    }
    
    public synchronized final TReturnStatus getTReturnStatus()   {
        TReturnStatus status = new TReturnStatus();
        status.setExplanation(getErrorMessage());
        State state = getState();
        if(getStatusCode() != null) {
            status.setStatusCode(getStatusCode());
        }
        else if(state == State.FAILED) {
            status.setStatusCode(TStatusCode.SRM_FAILURE);
        }
        else if(state == State.CANCELED) {
            status.setStatusCode(TStatusCode.SRM_ABORTED);
        }
        else if(state == State.DONE) {
            status.setStatusCode(TStatusCode.SRM_SUCCESS);
        }
        else {
            status.setStatusCode(TStatusCode.SRM_REQUEST_INPROGRESS);
        }
        return status;
    }
    
    public static ReserveSpaceRequest getRequest(Long requestId)
            throws SRMInvalidRequestException {
        Job job = Job.getJob( requestId);
        if(job == null || !(job instanceof ReserveSpaceRequest)) {
            return null;
        }
        return (ReserveSpaceRequest) job;
    }

    private class SrmReserveSpaceCallbacks implements org.dcache.srm.SrmReserveSpaceCallbacks {
        Long requestJobId;
        public SrmReserveSpaceCallbacks(Long requestJobId){
            this.requestJobId = requestJobId;
        }
        
        public ReserveSpaceRequest getReserveSpacetRequest()
                throws SRMInvalidRequestException {
            Job job = Job.getJob(requestJobId);
            if(job != null) {
                return (ReserveSpaceRequest) job;
            }
            return null;
        }
        
        public void ReserveSpaceFailed(String reason) {

            ReserveSpaceRequest request;
            try {
                  request  = getReserveSpacetRequest();
            } catch (SRMInvalidRequestException ire) {
                logger.error(ire);
                return;
            }
            try {
                request.setState(State.FAILED,reason);
            } catch(IllegalStateTransition ist) {
                request.esay("Illegal State Transition : " +ist.getMessage());
            }
            
            request.esay("ReserveSpace error: "+ reason);
        }
        
        public void NoFreeSpace(String  reason) {
            ReserveSpaceRequest request;
            try {
                  request  = getReserveSpacetRequest();
            } catch (SRMInvalidRequestException ire) {
                logger.error(ire);
                return;
            }
            
            try {
                request.setStateAndStatusCode(State.FAILED,reason,TStatusCode.SRM_NO_FREE_SPACE);
            } catch(IllegalStateTransition ist) {
                request.esay("Illegal State Transition : " +ist.getMessage());
            }
            
            request.esay("ReserveSpace failed (NoFreeSpace), no free space : "+reason);
        }
 
        public void ReserveSpaceFailed(Exception e) {
            ReserveSpaceRequest request;
            try {
                  request  = getReserveSpacetRequest();
            } catch (SRMInvalidRequestException ire) {
                logger.error(ire);
                return;
            }
            
            try {
                request.setState(State.FAILED,e.toString());
            } catch(IllegalStateTransition ist) {
                request.esay("Illegal State Transition : " +ist.getMessage());
            }
            
            request.esay("ReserveSpace exception: ");
            request.esay(e);
        }
        
        public void SpaceReserved(String spaceReservationToken, long reservedSpaceSize) {
            ReserveSpaceRequest request;
            try {
                  request  = getReserveSpacetRequest();
            } catch (SRMInvalidRequestException ire) {
                logger.error(ire);
                return;
            }
            try {
                synchronized(request) {
                    
                    State state = request.getState();
                    if(!State.isFinalState(state)) {
                        
                        request.setSpaceToken(spaceReservationToken);
                        request.setSizeInBytes(reservedSpaceSize);
                        request.setState(State.DONE,"space reservation succeeded" );
                    }
                }
            } catch(IllegalStateTransition ist) {
                request.esay("Illegal State Transition : " +ist.getMessage());
            }
        }
        
    }
    
    public long getSizeInBytes() {
        return sizeInBytes;
    }
    
    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }
    
    public TRetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }
    
    public void setRetentionPolicy(TRetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }
    
    public TAccessLatency getAccessLatency() {
        return accessLatency;
    }
    
    public void setAccessLatency(TAccessLatency accessLatency) {
        this.accessLatency = accessLatency;
    }
       
    public String getSpaceToken() {
        return spaceToken;
    }
    
    public void setSpaceToken(String spaceToken) {
        this.spaceToken = spaceToken;
    }
    
    public long getSpaceReservationLifetime() {
        return spaceReservationLifetime;
    }
    
    public void setSpaceReservationLifetime(long spaceReservationLifetime) {
        this.spaceReservationLifetime = spaceReservationLifetime;
    }
    
    public String getMethod(){
        return "srmReserveSpace";
    }
}
