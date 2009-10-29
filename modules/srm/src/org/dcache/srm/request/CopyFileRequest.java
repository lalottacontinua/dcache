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

//import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
//import java.net.URLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import diskCacheV111.srm.RequestFileStatus;
import org.dcache.srm.FileMetaData;
import org.dcache.srm.PrepareToPutCallbacks;
import org.dcache.srm.SRMException;
//import org.dcache.srm.ReleaseSpaceCallbacks;
import org.dcache.srm.SrmReserveSpaceCallbacks;
import org.dcache.srm.SrmReleaseSpaceCallbacks;
import org.dcache.srm.SrmUseSpaceCallbacks;
import org.dcache.srm.SrmCancelUseOfSpaceCallbacks;
import org.globus.util.GlobusURL;
import org.dcache.srm.scheduler.State;
import org.dcache.srm.scheduler.Scheduler;
import org.dcache.srm.scheduler.IllegalStateTransition;
//import org.dcache.srm.SRMException;
import org.dcache.srm.scheduler.Job;
//import org.dcache.srm.scheduler.JobCreator;
import org.dcache.srm.scheduler.JobStorage;
//import org.dcache.srm.scheduler.IllegalStateTransition;
import org.dcache.srm.scheduler.FatalJobFailure;
import org.dcache.srm.scheduler.NonFatalJobFailure;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.dcache.srm.util.ShellCommandExecuter;
import org.dcache.srm.v2_2.*;
import org.apache.axis.types.URI;
import org.dcache.srm.SRMUser;
import org.dcache.srm.SRMInvalidRequestException;
import org.apache.log4j.Logger;


/**
 *
 * @author  timur
 * @version
 */
public final class CopyFileRequest extends FileRequest {
    
    private static final Logger logger = Logger.getLogger(CopyFileRequest.class);
	private String from_url;
	private String to_url;
	private GlobusURL from_turl;
	private GlobusURL to_turl;
	private String local_from_path;
	private String local_to_path;
	private long size = 0;
	private String fromFileId;
	private String toFileId;
	private String toParentFileId;
	private transient FileMetaData toParentFmd;
	private String remoteRequestId;
	private String remoteFileId;
	private String transferId;
	private Exception transferError;
	//these are used if the transfer is performed in the pull mode for 
	// storage of the space reservation related info
	private String spaceReservationId;
	private boolean weReservedSpace;
	private boolean spaceMarkedAsBeingUsed=false;
	
	/** Creates new FileRequest */
    
	public CopyFileRequest(Long requestId,
			       Long  requestCredentalId,
			       String from_url,
			       String to_url,
			       String spaceToken,
			       long lifetime,
			       int max_number_of_retries) throws Exception {
		super(requestId, 
		      requestCredentalId,
                    lifetime, max_number_of_retries);
		say("CopyFileRequest");
		this.from_url = from_url;
		this.to_url = to_url;
		this.spaceReservationId = spaceToken;
		say("constructor from_url=" +from_url+" to_url="+to_url);
	}
	
	/**
	 * restore constructore, used for restoring the existing
	 * file request from the database
	 */
	
	public CopyFileRequest(
		Long id,
		Long nextJobId,
		JobStorage jobStorage,
		long creationTime,
		long lifetime,
		int stateId,
		String errorMessage,
		String scheduelerId,
		long schedulerTimeStamp,
		int numberOfRetries,
		int maxNumberOfRetries,
		long lastStateTransitionTime,
		JobHistory[] jobHistoryArray,
		Long requestId,
		Long requestCredentalId,
		String statusCodeString,
		String FROMURL,
		String TOURL,
		String FROMTURL,
		String TOTURL,
		String FROMLOCALPATH,
		String TOLOCALPATH,
		long size,
		String fromFileId,
		String toFileId,
		String REMOTEREQUESTID,
		String REMOTEFILEID,
		String spaceReservationId,
		String transferId
		)  throws java.sql.SQLException {
		super(id,
		      nextJobId,
		      creationTime,
		      lifetime,
		      stateId,
		      errorMessage,
		      scheduelerId,
		      schedulerTimeStamp, 
		      numberOfRetries,
		      maxNumberOfRetries,
		      lastStateTransitionTime, 
		      jobHistoryArray,
		      requestId,
		      requestCredentalId,
		      statusCodeString);
		this.from_url = FROMURL;
		this.to_url = TOURL;
		try {
			if(FROMTURL != null && (!FROMTURL.equalsIgnoreCase("null"))) {
				this.from_turl = new GlobusURL(FROMTURL);
			}
			if(TOTURL != null && (!TOTURL.equalsIgnoreCase("null"))) {
				this.to_turl = new GlobusURL(TOTURL);
			}
		}
		catch(MalformedURLException murle) {
			throw new IllegalArgumentException(murle.toString());
		}
		this.local_from_path = FROMLOCALPATH;
		this.local_to_path = TOLOCALPATH;
		this.size = size;
		this.fromFileId =fromFileId;
		this.toFileId = toFileId;
		if(REMOTEREQUESTID != null && (!REMOTEREQUESTID.equalsIgnoreCase("null"))) {
			this.remoteRequestId = REMOTEREQUESTID;
		}
		if(REMOTEFILEID != null && (!REMOTEFILEID.equalsIgnoreCase("null"))) {
			this.remoteFileId = REMOTEFILEID;
		}
		this.spaceReservationId = spaceReservationId;
	}
    
	public void say(String s) {
		if(getStorage() != null) {
			getStorage().log("CopyFileRequest #"+getId()+": "+s);
		}
	}
    
	public void esay(String s) {
		if(getStorage() != null) {
			getStorage().elog("CopyFileRequest #"+getId()+": "+s);
		}
	}
	
	public void esay(Throwable t) {
		if(getStorage() != null) {
			getStorage().elog(t);
		}
	}
	
	public void done() {
		say("done()");
	}
	
	public void error() {
		done();
	}
    
	public RequestFileStatus getRequestFileStatus() {
		RequestFileStatus rfs = new RequestFileStatus();
		rfs.fileId = getId().intValue();
		rfs.SURL = getFrom_url();
		rfs.size = 0;
		rfs.TURL = getTo_url();
		State state = getState();
		if(state == State.DONE) {
			rfs.state = "Done";
		}
		else if(state == State.READY) {
			rfs.state = "Ready";
		}
		else if(state == State.TRANSFERRING) {
			rfs.state = "Running";
		}
		else if(state == State.FAILED
			|| state == State.CANCELED ) {
			rfs.state = "Failed";
		}
		else {
			rfs.state = "Pending";
		}
		return rfs;
	}
	
	public String getToURL() {
        rlock();
        try {
            return getTo_url();
        } finally {
            runlock();
        }
	}
    
	public String getFromURL() {
        rlock();
        try {
            return getFrom_url();
        } finally {
            runlock();
        }
	}
    
	public String getFromPath() throws java.net.MalformedURLException {
		String path = new GlobusURL(getFrom_url()).getPath();
		int indx=path.indexOf(SFN_STRING);
		if( indx != -1) {
			path=path.substring(indx+SFN_STRING.length());
		}
		if(!path.startsWith("/")) {
			path = "/"+path;
		}
		say("getFromPath() returns "+path);
		return path;
	}
    
	public String getToPath() throws java.net.MalformedURLException {
		String path = new GlobusURL(getTo_url()).getPath();
		int indx=path.indexOf(SFN_STRING);
		if( indx != -1) {
			path=path.substring(indx+SFN_STRING.length());
		}
		if(!path.startsWith("/")) {
			path = "/"+path;
		}
		say("getToPath() returns "+path);
		return path;
	}
    
	/** Getter for property from_turl.
	 * @return Value of property from_turl.
	 */
	public org.globus.util.GlobusURL getFrom_turl() {
        rlock();
        try {
            return from_turl;
        } finally {
            runlock();
        }
	}
    
	/** Setter for property from_turl.
	 * @param from_turl New value of property from_turl.
	 */
	public void setFrom_turl(org.globus.util.GlobusURL from_turl) {
        wlock();
        try {
            this.from_turl = from_turl;
        } finally {
            wunlock();
        }
	}
    
	/** Getter for property to_turl.
	 * @return Value of property to_turl.
	 */
	public org.globus.util.GlobusURL getTo_turl() {
        rlock();
        try {
            return to_turl;
        } finally {
            runlock();
        }
	}
	
	/** Setter for property to_turl.
	 * @param to_turl New value of property to_turl.
	 */
	public void setTo_turl(org.globus.util.GlobusURL to_turl) {
        wlock();
        try {
            this.to_turl = to_turl;
        } finally {
            wunlock();
        }
	}
	
	/** Getter for property size.
	 * @return Value of property size.
	 */
	public long getSize() {
        rlock();
        try {
            return size;
        } finally {
            runlock();
        }
	}
	
	/** Setter for property size.
	 * @param size New value of property size.
	 */
	public void setSize(long size) {
        rlock();
        try {
            this.size = size;
        } finally {
            runlock();
        }
	}

    @Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(" CopyFileRequest ");
		sb.append(" id =").append(getId());
		sb.append(" FromSurl=").append(getFrom_url());
		sb.append(" FromTurl=").append(getFrom_turl()==null?"null":getFrom_turl().getURL());
		sb.append(" toSurl=").append(getTo_url());
		sb.append(" toTurl=").append(getTo_turl()==null?"null":getTo_turl().getURL());
		return sb.toString();
	}
	
	/** Getter for property absolute_local_from_path.
	 * @return Value of property absolute_local_from_path.
	 */
	public String getLocal_from_path() {
        rlock();
        try {
            return local_from_path;
        } finally {
            runlock();
        }
	}
	
	/** Setter for property absolute_local_from_path.
	 * @param absolute_local_from_path New value of property absolute_local_from_path.
	 */
	public void setLocal_from_path(String local_from_path) {
        wlock();
        try {
            this.local_from_path = local_from_path;
        } finally {
            wunlock();
        }
	}
	
	/** Getter for property absolute_local_to_path.
	 * @return Value of property absolute_local_to_path.
	 */
	public String getLocal_to_path() {
        rlock();
        try {
            return local_to_path;
        } finally {
            runlock();
        }
	}
	
	/** Setter for property absolute_local_to_path.
	 * @param absolute_local_to_path New value of property absolute_local_to_path.
	 */
	public void setLocal_to_path( String local_to_path) {
        wlock();
        try {
            this.local_to_path = local_to_path;
        } finally {
            wunlock();
        }
	}
	
	/** Getter for property toFileId.
	 * @return Value of property toFileId.
	 *
	 */
	public String getToFileId() {
        rlock();
        try {
            return toFileId;
        } finally {
            runlock();
        }
	}
	
	/** Setter for property toFileId.
	 * @param toFileId New value of property toFileId.
	 *
	 */
	public void setToFileId(String toFileId) {
        wlock();
        try {
            this.toFileId = toFileId;
        } finally {
            wunlock();
        }
	}
	
	/** Getter for property fromFileId.
	 * @return Value of property fromFileId.
	 *
	 */
	public String getFromFileId() {
        rlock();
        try {
            return fromFileId;
        } finally {
            runlock();
        }

	}
	
	/** Setter for property fromFileId.
	 * @param fromFileId New value of property fromFileId.
	 *
	 */
	public void setFromFileId(String fromFileId) {
        wlock();
        try {
            this.fromFileId = fromFileId;
        } finally {
            wunlock();
        }

	}
	
	private void runScriptCopy() throws Exception {
		GlobusURL from =getFrom_turl();
		GlobusURL to = getTo_turl();
		if(from == null && getLocal_from_path() != null ) {
			if(to.getProtocol().equalsIgnoreCase("gsiftp") ||
			   to.getProtocol().equalsIgnoreCase("http") ||
			   to.getProtocol().equalsIgnoreCase("ftp") ||
			   to.getProtocol().equalsIgnoreCase("dcap")) {
				//need to add support for getting
				String fromturlstr = getStorage().getGetTurl(getUser(),getLocal_from_path(),new String[]
					{"gsiftp","http","ftp"});
				from = new GlobusURL(fromturlstr);
			}
		}
		if(to == null && getLocal_to_path() != null) {
			if(from.getProtocol().equalsIgnoreCase("gsiftp") ||
			   from.getProtocol().equalsIgnoreCase("http") ||
			   from.getProtocol().equalsIgnoreCase("ftp") ||
			   from.getProtocol().equalsIgnoreCase("dcap")) {
				String toturlstr = getStorage().getPutTurl(getUser(),getLocal_to_path(),new String[]
					{"gsiftp","http","ftp"});
				to = new GlobusURL(toturlstr);
			}
		}
		if(from ==null || to == null) {
			String error = "could not resolve either source or destination"+
				" from = "+from+" to = "+to;
			esay(error);
			throw new SRMException(error);
		}
		say("calling scriptCopy("+from.getURL()+","+to.getURL()+")");
		RequestCredential credential = getCredential();
		scriptCopy(from,to,credential.getDelegatedCredential());
		setStateToDone();
	}

	private void runLocalToLocalCopy() throws Exception {
		say("copying from local to local ");
        FileMetaData fmd ;
        try {
            fmd = getStorage().getFileMetaData(getUser(),getLocal_from_path());
        } catch (SRMException srme) {
            try {
                setStateAndStatusCode(State.FAILED,
                        srme.getMessage(),
                        TStatusCode.SRM_INVALID_PATH);
            } catch (IllegalStateTransition ist) {
                esay("Illegal State Transition : " +ist.getMessage());
            }
            return;

        }
        size = fmd.size;

		RequestCredential credential = getCredential();
		if(getToFileId() == null && getToParentFileId() == null) {
            setState(State.ASYNCWAIT,"calling storage.prepareToPut");
			PutCallbacks callbacks = new PutCallbacks(this.getId());
			say("calling storage.prepareToPut("+getLocal_to_path()+")");
			getStorage().prepareToPut(getUser(),getLocal_to_path(),
					     callbacks,
					     ((CopyRequest)getRequest()).isOverwrite());
			say("callbacks.waitResult()");
			return;
		}
		say("known source size is "+size);
		//reserve space even if the size is not known (0) as
		// if only in order to select the pool corectly
		// use 1 instead of 0, since this will cause faulure if there is no space
		// available at all
		// Space manager will account for used size correctly
		// once it becomes available from the pool
		// and space is not reserved
		// or if the space is reserved and we already tried to use this
		// space reservation and failed
		// (releasing previous space reservation)
		//


        // Use pnfs tag for the default space token
        // if the conditions are right
		TAccessLatency accessLatency =
			((CopyRequest)getRequest()).getTargetAccessLatency();
		TRetentionPolicy retentionPolicy =
			((CopyRequest)getRequest()).getTargetRetentionPolicy();
		if (getSpaceReservationId()==null &&
            retentionPolicy==null&&
            accessLatency==null &&
            getToParentFmd().spaceTokens!=null &&
            getToParentFmd().spaceTokens.length>0 ) {
                setSpaceReservationId(Long.toString(getToParentFmd().spaceTokens[0]));
		}

		if (getConfiguration().isReserve_space_implicitely() &&
                getSpaceReservationId() == null) {
            setState(State.ASYNCWAIT,"reserving space");
			long remaining_lifetime =
                    lifetime - ( System.currentTimeMillis() -creationTime);
			say("reserving space, size="+(size==0?1L:size));
			//
			//the following code allows the inheritance of the
			// retention policy from the directory metatada
			//
			if(retentionPolicy == null && 
               getToParentFmd()!= null &&
               getToParentFmd().retentionPolicyInfo != null ) {
				retentionPolicy = getToParentFmd().retentionPolicyInfo.getRetentionPolicy();
			}

			//
			//the following code allows the inheritance of the
			// access latency from the directory metatada
			//
			if(accessLatency == null && 
               getToParentFmd() != null &&
               getToParentFmd().retentionPolicyInfo != null ) {
				accessLatency = getToParentFmd().retentionPolicyInfo.getAccessLatency();
			}

			SrmReserveSpaceCallbacks callbacks =
                    new TheReserveSpaceCallbacks (getId());
			getStorage().srmReserveSpace(
				getUser(),
				size==0?1L:size,
				remaining_lifetime,
				retentionPolicy == null ? null : retentionPolicy.getValue(),
				accessLatency == null ? null : accessLatency.getValue(),
				null,
				callbacks);
			return;
		}

		if( getSpaceReservationId() != null &&
		    !isSpaceMarkedAsBeingUsed()) {
            setState(State.ASYNCWAIT,"marking space as being used");
			long remaining_lifetime =
                    lifetime - ( System.currentTimeMillis() -creationTime);
			SrmUseSpaceCallbacks  callbacks = new CopyUseSpaceCallbacks(getId());
			getStorage().srmMarkSpaceAsBeingUsed(getUser(),getSpaceReservationId(),getLocal_to_path(),
							size==0?1:size,
							remaining_lifetime,
							((CopyRequest)getRequest()).isOverwrite(),
							callbacks );
			return;
		}

        getStorage().localCopy(getUser(),getLocal_from_path(), getLocal_to_path());
        setStateToDone();
        return;
	}
    
	private void runRemoteToLocalCopy() throws Exception {
		say("copying from remote to local ");
		RequestCredential credential = getCredential();
		if(getToFileId() == null && getToParentFileId() == null) {
			setState(State.ASYNCWAIT,"calling storage.prepareToPut");
			PutCallbacks callbacks = new PutCallbacks(this.getId());
			say("calling storage.prepareToPut("+getLocal_to_path()+")");
			getStorage().prepareToPut(getUser(),getLocal_to_path(),
					     callbacks,
					     ((CopyRequest)getRequest()).isOverwrite());
			say("callbacks.waitResult()");
			return;
		}
		say("known source size is "+size);
		//reserve space even if the size is not known (0) as
		// if only in order to select the pool corectly
		// use 1 instead of 0, since this will cause faulure if there is no space
		// available at all
		// Space manager will account for used size correctly
		// once it becomes available from the pool
		// and space is not reserved 
		// or if the space is reserved and we already tried to use this 
		// space reservation and failed 
		// (releasing previous space reservation)
		//

        // Use pnfs tag for the default space token
        // if the conditions are right
		TAccessLatency accessLatency =
			((CopyRequest)getRequest()).getTargetAccessLatency();
		TRetentionPolicy retentionPolicy =
			((CopyRequest)getRequest()).getTargetRetentionPolicy();
		if (getSpaceReservationId()==null &&
            retentionPolicy==null&&
            accessLatency==null &&
            getToParentFmd().spaceTokens!=null &&
            getToParentFmd().spaceTokens.length>0 ) {
                setSpaceReservationId(Long.toString(getToParentFmd().spaceTokens[0]));
		}

		if (getConfiguration().isReserve_space_implicitely()&&getSpaceReservationId() == null) {
			setState(State.ASYNCWAIT,"reserving space");
			long remaining_lifetime = lifetime - ( System.currentTimeMillis() -creationTime);
			say("reserving space, size="+(size==0?1L:size));
			//
			//the following code allows the inheritance of the 
			// retention policy from the directory metatada
			//
			if(retentionPolicy == null && getToParentFmd()!= null && getToParentFmd().retentionPolicyInfo != null ) {
				retentionPolicy = getToParentFmd().retentionPolicyInfo.getRetentionPolicy();
			}
			//
			//the following code allows the inheritance of the 
			// access latency from the directory metatada
			//
			if(accessLatency == null && getToParentFmd() != null && getToParentFmd().retentionPolicyInfo != null ) {
				accessLatency = getToParentFmd().retentionPolicyInfo.getAccessLatency();
			}
			SrmReserveSpaceCallbacks callbacks = new TheReserveSpaceCallbacks (getId());
			getStorage().srmReserveSpace(
				getUser(), 
				size==0?1L:size, 
				remaining_lifetime, 
				retentionPolicy == null ? null : retentionPolicy.getValue(),
				accessLatency == null ? null : accessLatency.getValue(),
				null,
				callbacks);
			return;
		}
		if( getSpaceReservationId() != null &&
		    !isSpaceMarkedAsBeingUsed()) {
            setState(State.ASYNCWAIT,"marking space as being used");
			long remaining_lifetime = lifetime - ( System.currentTimeMillis() -creationTime);
			SrmUseSpaceCallbacks  callbacks = new CopyUseSpaceCallbacks(getId());
			getStorage().srmMarkSpaceAsBeingUsed(getUser(),getSpaceReservationId(),getLocal_to_path(),
							size==0?1:size,
							remaining_lifetime,
							((CopyRequest)getRequest()).isOverwrite(),
							callbacks );
			return;
		}
		if(getTransferId() == null) {
            setState(State.RUNNINGWITHOUTTHREAD,"started remote transfer, waiting completion");
			TheCopyCallbacks copycallbacks = new TheCopyCallbacks(getId());
			if(getSpaceReservationId() != null) {
				setTransferId(getStorage().getFromRemoteTURL(getUser(), getFrom_turl().getURL(), getLocal_to_path(), getUser(), credential.getId(), getSpaceReservationId().toString(), size, copycallbacks));
				
			} 
			else {
				setTransferId(getStorage().getFromRemoteTURL(getUser(), getFrom_turl().getURL(), getLocal_to_path(), getUser(), credential.getId(), copycallbacks));
			}
			long remaining_lifetime = 
				this.getCreationTime() + 
				this.getLifetime() -
				System.currentTimeMillis() ;
			saveJob();
			return;
		}
		// transfer id is not null and we are scheduled
		// there was some kind of error durign the transfer
		else {
			getStorage().killRemoteTransfer(getTransferId());
			setTransferId(null);
			throw new org.dcache.srm.scheduler.NonFatalJobFailure(getTransferError());
		}
	}
    
	private void setStateToDone(){
        try {
            setState(State.DONE, "setStateToDone called");
            try {
                ((CopyRequest)getRequest()).fileRequestCompleted();
            } catch (SRMInvalidRequestException ire) {
                esay(ire);
            }
        }
        catch(IllegalStateTransition ist) {
            esay("setStateToDone: Illegal State Transition : " +ist.getMessage());
        }
	}
    
	private void setStateToFailed(String error) throws Exception {
        try {
            setState(State.FAILED, error);
        }
        catch(IllegalStateTransition ist) {
            esay("setStateToFailed: Illegal State Transition : " +ist.getMessage());
        }
		((CopyRequest)getRequest()).fileRequestCompleted();
	}
    
	private void runLocalToRemoteCopy() throws Exception {
		if(getTransferId() == null) {
			say("copying using storage.putToRemoteTURL");
			RequestCredential credential = getCredential();
			TheCopyCallbacks copycallbacks = new TheCopyCallbacks(getId());
			setTransferId(getStorage().putToRemoteTURL(getUser(), getLocal_from_path(), getTo_turl().getURL(), getUser(), credential.getId(), copycallbacks));
			setState(State.RUNNINGWITHOUTTHREAD,"started remote transfer, waiting completion");
			saveJob();
			return;
		}      
		// transfer id is not null and we are scheduled
		// there was some kind of error durign the transfer
		else {
			getStorage().killRemoteTransfer(getTransferId());
			setTransferId(null);
			throw new org.dcache.srm.scheduler.NonFatalJobFailure(getTransferError());
		}
	}
    
	public void run() throws NonFatalJobFailure, FatalJobFailure{
		say("copying " );
		try {
			if(getFrom_turl() != null && getFrom_turl().getProtocol().equalsIgnoreCase("dcap")  ||
			   getTo_turl() != null && getTo_turl().getProtocol().equalsIgnoreCase("dcap") ||
			   getConfiguration().isUseUrlcopyScript()) {
				try {
					runScriptCopy();
					return;
				}
				catch(Exception e) {
					esay(e);
					esay("copying using script failed, trying java");
				}
			}
			if(getLocal_to_path() != null && getLocal_from_path() != null) {
                runLocalToLocalCopy();
				return;
			}
			if(getLocal_to_path() != null && getFrom_turl() != null) {
				runRemoteToLocalCopy();
				return;
			}
			if(getTo_turl() != null && getLocal_from_path() != null) {
				runLocalToRemoteCopy();
				return;
			}
			if(getFrom_turl() != null && getTo_turl() != null) {
				URL fromURL = new URL(getFrom_turl().getURL());
				URL toURL   = new URL(getTo_turl().getURL());
				javaUrlCopy(fromURL,toURL);
				say("copy succeeded");
				setStateToDone();
				return;
			}
			else {
				esay("Unknown combination of to/from ursl");
				setStateToFailed("Unknown combination of to/from ursl");
			}
		}
		catch(Exception e) {
			esay(e);
			esay("copy  failed");
			throw new NonFatalJobFailure(e.toString());
		}
		catch(Throwable t) {
			throw new FatalJobFailure(t.toString());
		}
	}
    
	private static long last_time=0L;
	private static final long serialVersionUID = 1749445378403850845L;

	public synchronized static long unique_current_time() {
		long time =  System.currentTimeMillis();
		last_time = last_time < time ? time : last_time+1;
		return last_time;
	}
    
	public void scriptCopy(GlobusURL from, GlobusURL to, GSSCredential credential) throws Exception {
		String proxy_file = null;
		try {
			String command = getConfiguration().getTimeout_script();
			command=command+" "+getConfiguration().getTimeout();
			command=command+" "+getConfiguration().getUrlcopy();
			//command=command+" -username "+ user.getName();
			command = command+" -debug "+getConfiguration().isDebug();
			if(credential != null) {
				try {
					byte [] data = ((ExtendedGSSCredential)(credential)).export(
						ExtendedGSSCredential.IMPEXP_OPAQUE);
					proxy_file = getConfiguration().getProxies_directory()+
						"/proxy_"+credential.hashCode()+"_at_"+unique_current_time();
					say("saving credential "+credential.getName().toString()+
					    " in proxy_file "+proxy_file);
					FileOutputStream out = new FileOutputStream(proxy_file);
					out.write(data);
					out.close();
					say("save succeeded ");
				}
				catch(IOException ioe) {
					esay("saving credentials to "+proxy_file+" failed");
					esay(ioe);
					proxy_file = null;
				}
			}
			if(proxy_file != null) {
				command = command+" -x509_user_proxy "+proxy_file;
				command = command+" -x509_user_key "+proxy_file;
				command = command+" -x509_user_cert "+proxy_file;
			}
			int tcp_buffer_size = getConfiguration().getTcp_buffer_size();
			if(tcp_buffer_size > 0) {
				command = command+" -tcp_buffer_size "+tcp_buffer_size;
			}
			int buffer_size = getConfiguration().getBuffer_size();
			if(buffer_size > 0) {
				command = command+" -buffer_size "+buffer_size;
			}
			int parallel_streams = getConfiguration().getParallel_streams();
			if(parallel_streams > 0) {
				command = command+" -parallel_streams "+parallel_streams;
			}
			command = command+
				" -src-protocol "+from.getProtocol();
			if(from.getProtocol().equals("file")) {
				command = command+" -src-host-port localhost";
			}
			else {
				command = command+
					" -src-host-port "+from.getHost()+":"+from.getPort();
			}
			command = command+
				" -src-path "+from.getPath()+
				" -dst-protocol "+to.getProtocol();
			if(to.getProtocol().equals("file")) {
				command = command+" -dst-host-port localhost";
			}
			else {
				command = command+
					" -dst-host-port "+to.getHost()+":"+to.getPort();
			}
			command = command+
				" -dst-path "+to.getPath();
			String from_username = from.getUser();
			if(from_username != null) {
				command = command +
					" -src_username "+from_username;
			}
			String from_pwd = from.getPwd();
			if(from_pwd != null) {
				command = command +
					" -src_userpasswd "+from_pwd;
			}
			String to_username = to.getUser();
			if(to_username != null) {
				command = command +
					" -dst_username "+to_username;
			}
			String to_pwd = to.getPwd();
			if(to_pwd != null) {
				command = command +
					" -dst_userpasswd "+to_pwd;
			}
			String gsiftpclient = getConfiguration().getGsiftpclinet();
			if(gsiftpclient != null) {
				command = command +
					" -use-kftp "+
					(gsiftpclient.toLowerCase().indexOf("kftp") != -1);
			}
			int rc = ShellCommandExecuter.execute(command);
			if(rc == 0) {
				say("return code = 0, success");
			}
			else {
				say("return code = "+rc+", failure");
				throw new java.io.IOException("return code = "+rc+", failure");
			}
		}
		finally {
			if(proxy_file != null) {
				try {
					say(" deleting proxy file"+proxy_file);
					java.io.File f = new java.io.File(proxy_file);
					f.delete();
				}
				catch(Exception e) {
					esay("error deleting proxy cash "+proxy_file);
					esay(e);
				}
			}
		}
	}
	
	public void javaUrlCopy(URL from, URL to) throws Exception {
		try {
			InputStream in = null;
			if(from.getProtocol().equals("file")) {
				in = new FileInputStream(from.getPath());
			}
			else {
				in = from.openConnection().getInputStream();
			}
			OutputStream out = null;
			if(to.getProtocol().equals("file")) {
				out = new FileOutputStream(to.getPath());
			}
			else {
				java.net.URLConnection to_connect = to.openConnection();
				to_connect.setDoInput(false);
				to_connect.setDoOutput(true);
				out = to_connect.getOutputStream();
			}
			try {
				int buffer_size = 0;//configuration.getBuffer_size();
				if(buffer_size <=0) buffer_size = 4096;
				byte[] bytes = new byte[buffer_size];
				long total = 0;
				int l;
				while( (l = in.read(bytes)) != -1) {
					total += l;
					out.write(bytes,0,l);
				}
				say("done, copied "+total +" bytes");
			} 
			finally {
				in.close();
				out.close();
			}
		}
		catch(Exception e) {
			say("failure : "+e.getMessage());
			throw e;
		}
	}
	
	protected void stateChanged(org.dcache.srm.scheduler.State oldState) {
		State state = getState();
		if(State.isFinalState(state)) {
			if( getTransferId() != null)           {
				getStorage().killRemoteTransfer(getTransferId());
			}
                        SRMUser user ;
                        try {
                            user = getUser();
                        } catch (SRMInvalidRequestException ire) {
                            esay(ire);
                            return;
                        }
			if(getSpaceReservationId() != null && isWeReservedSpace()) {
				say("storage.releaseSpace("+getSpaceReservationId()+"\"");
				SrmReleaseSpaceCallbacks callbacks = new TheReleaseSpaceCallbacks(this.getId());
				getStorage().srmReleaseSpace(  user,getSpaceReservationId(),
							  null,
							  callbacks);
			}
			if(getConfiguration().isReserve_space_implicitely() &&
			   getSpaceReservationId() != null &&
			   isSpaceMarkedAsBeingUsed() ) {
				SrmCancelUseOfSpaceCallbacks callbacks =
					new CopyCancelUseOfSpaceCallbacks(getId());
				getStorage().srmUnmarkSpaceAsBeingUsed(user,getSpaceReservationId(),getLocal_to_path(),callbacks);
			}
			if( getRemoteRequestId() != null ) {
				if(getLocal_from_path() != null ) {
					remoteFileRequestDone(getTo_url(),getRemoteRequestId(), getRemoteFileId());
				}
				else {
					remoteFileRequestDone(getFrom_url(),getRemoteRequestId(), getRemoteFileId());
				}
			}
		}
	}
    
	public void remoteFileRequestDone(String SURL,String remoteRequestId,String remoteFileId) {
		try {
			say("setting remote file status to Done, SURL="+SURL+" remoteRequestId="+remoteRequestId+
			    " remoteFileId="+remoteFileId);
			(( CopyRequest)(getRequest())).remoteFileRequestDone(SURL,remoteRequestId,remoteFileId);
                }
                catch(Exception e) {
			esay("set remote file status to done failed, surl = "+SURL+
			     " requestId = " +remoteRequestId+ " fileId = " +remoteFileId);
                }
	}    
	/** Getter for property remoteFileId.
	 * @return Value of property remoteFileId.
	 *
	 */
	public String getRemoteFileId() {
        rlock();
        try {
            return remoteFileId;
        } finally {
            runlock();
        }
	}
	/** Getter for property remoteRequestId.
	 * @return Value of property remoteRequestId.
	 *
	 */
	public String getRemoteRequestId() {
        rlock();
        try {
            return remoteRequestId;
        } finally {
            runlock();
        }
	}
	/**
	 * Getter for property from_url.
	 * @return Value of property from_url.
	 */
	public java.lang.String getFrom_url() {
        rlock();
        try {
            return from_url;
        } finally {
            runlock();
        }

	}
	/**
	 * Getter for property to_url.
	 * @return Value of property to_url.
	 */
	public java.lang.String getTo_url() {
        rlock();
        try {
            return to_url;
        } finally {
            runlock();
        }
	}
	/**
	 * Setter for property remoteRequestId.
	 * @param remoteRequestId New value of property remoteRequestId.
	 */
	public void setRemoteRequestId(String remoteRequestId) {
        wlock();
        try {
            this.remoteRequestId = remoteRequestId;
        } finally {
            wunlock();
        }
	}
	/**
	 * Setter for property remoteFileId.
	 * @param remoteFileId New value of property remoteFileId.
	 */
	public void setRemoteFileId(String remoteFileId) {
        wlock();
        try {
            this.remoteFileId = remoteFileId;
        } finally {
            wunlock();
        }
	}
	/**
	 * Getter for property spaceReservationId.
	 * @return Value of property spaceReservationId.
	 */
	public String getSpaceReservationId() {
        rlock();
        try {
            return spaceReservationId;
        } finally {
            runlock();
        }
	}
	/**
	 * Setter for property spaceReservationId.
	 * @param spaceReservationId New value of property spaceReservationId.
	 */
	public void setSpaceReservationId(String spaceReservationId) {
        wlock();
        try {
            this.spaceReservationId = spaceReservationId;
        } finally {
            wunlock();
        }
	}

    /**
     * @return the toParentFileId
     */
    private String getToParentFileId() {
        rlock();
        try {
            return toParentFileId;
        } finally {
            runlock();
        }
    }

    /**
     * @param toParentFileId the toParentFileId to set
     */
    private void setToParentFileId(String toParentFileId) {
        wlock();
        try {
            this.toParentFileId = toParentFileId;
        } finally {
            wunlock();
        }
    }

    /**
     * @return the toParentFmd
     */
    private FileMetaData getToParentFmd() {
        rlock();
        try {
            return toParentFmd;
        } finally {
            runlock();
        }
    }

    /**
     * @param toParentFmd the toParentFmd to set
     */
    private void setToParentFmd(FileMetaData toParentFmd) {
        wlock();
        try {
           this.toParentFmd = toParentFmd;
        } finally {
            wunlock();
        }
    }

    /**
     * @param transferId the transferId to set
     */
    private void setTransferId(String transferId) {
        wlock();
        try {
            this.transferId = transferId;
        } finally {
            wunlock();
        }
    }

    /**
     * @return the transferError
     */
    private Exception getTransferError() {
        rlock();
        try {
            return transferError;
        } finally {
            runlock();
        }
    }

    /**
     * @param transferError the transferError to set
     */
    private void setTransferError(Exception transferError) {
        wlock();
        try {
            this.transferError = transferError;
        } finally {
            wunlock();
        }
    }
	
	private static class PutCallbacks implements PrepareToPutCallbacks {
		Long fileRequestJobId;
		public boolean completed = false;
		public boolean success = false;
		public String fileId;
		public FileMetaData fmd;
		public String parentFileId;
		public FileMetaData parentFmd;
		public String error_message;
		
		public synchronized boolean waitResult(long timeout) {
			long start = System.currentTimeMillis(); 
			long current = start;
			while(true) {
				if(completed) {
					return success;
				}
				long wait = timeout - (current -start);
				if(wait > 0) {
					try {
						this.wait(wait);
					}
					catch(InterruptedException ie){
						
					}
				}
				else {
					completed = true;
					success = false;
					error_message = "PutCallbacks wait timeout expired";
					return false;
				}
				current = System.currentTimeMillis(); 
			}
		}
        
		public synchronized void complete(boolean success) {
			this.success = success;
			this.completed = true;
			this.notifyAll();
		}
        
		public PutCallbacks(Long fileRequestJobId) {
			if(fileRequestJobId == null) {
				throw new NullPointerException("fileRequestJobId should be non-null");
			}
			this.fileRequestJobId = fileRequestJobId;
		}
        
		public CopyFileRequest getCopyFileRequest() 
                throws java.sql.SQLException, SRMInvalidRequestException{
			Job job = Job.getJob(fileRequestJobId);
			if(job != null) {
				return (CopyFileRequest) job;
			}
			throw new java.sql.SQLException("CopyFileRequest for id="+fileRequestJobId+" is not found");
		}
		
		public void DuplicationError(String reason) {
			error_message = reason;
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(
                            State.FAILED,
                            error_message,
                            TStatusCode.SRM_DUPLICATION_ERROR);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("PutCallbacks Timeout");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			complete(false);
		}
       
		public void Error( String error) {
			error_message = error;
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setState(State.FAILED,error_message);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("PutCallbacks Timeout");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			complete(false);
		}
		
		public void Exception( Exception e) {
			error_message = e.toString();
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setState(State.FAILED,error_message);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("PutCallbacks Timeout");
			}
			catch(Exception e1) {
				e1.printStackTrace();
			}
			complete(false);
		}
		
		public void GetStorageInfoFailed(String reason) {
			error_message = reason;
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setState(State.FAILED,error_message);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				
				fr.esay("PutCallbacks Timeout");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			complete(false);
		}
		
		
		public void StorageInfoArrived(String fileId,
					       FileMetaData fmd,
					       String parentFileId, 
					       FileMetaData parentFmd) {
			try {
				CopyFileRequest fr =  getCopyFileRequest();
				fr.say("StorageInfoArrived: FileId:"+fileId);
				State state = fr.getState();
				if(state == State.ASYNCWAIT) {
					fr.say("PutCallbacks StorageInfoArrived for file "+fr.getToPath()+" fmd ="+fmd);
					fr.setToFileId(fileId);
					fr.setToParentFileId(parentFileId);
					fr.setToParentFmd(parentFmd);
					Scheduler scheduler = Scheduler.getScheduler(fr.getSchedulerId());
					try {
						scheduler.schedule(fr);
					}
					catch(Exception ie) {
						fr.esay(ie);
					}
				}
				complete(true);
			}
			catch(Exception e){
				e.printStackTrace();
				complete(false);
			}
			
		}
		
		public void Timeout() {
			error_message = "PutCallbacks Timeout";
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setState(State.FAILED,error_message);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("PutCallbacks Timeout");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			complete(false);
		}
        
		public void InvalidPathError(String reason) {
			error_message = reason;
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(
                            State.FAILED,
                            error_message,
                            TStatusCode.SRM_INVALID_PATH);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("PutCallbacks Timeout");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			complete(false);
		}
		
		public void AuthorizationError(String reason) {
			error_message = reason;
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(State.FAILED,
                            error_message,
                            TStatusCode.SRM_AUTHORIZATION_FAILURE);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				
				fr.esay("PutCallbacks Timeout");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			complete(false);
		}
	}
	
	private static class TheCopyCallbacks implements org.dcache.srm.CopyCallbacks {
		private Long fileRequestJobId;
		private boolean completed = false;
		private boolean success = false;
		
		public TheCopyCallbacks ( Long fileRequestJobId ) {
			this.fileRequestJobId = fileRequestJobId;
		}
		public synchronized boolean waitResult(long timeout) {
			long start = System.currentTimeMillis(); 
			long current = start;
			while(true) {
				if(completed) {
					return success;
				}
				long wait = timeout - (current -start);
				if(wait > 0) {
					try {
						this.wait(wait);
					}
					catch(InterruptedException ie) {
					}
				}
				else {
					completed = true;
					success = false;
					return false;
				}
				current = System.currentTimeMillis(); 
			}
		}
		
		public synchronized void complete(boolean success) {
			this.success = success;
			this.completed = true;
			this.notifyAll();
		}
        
		private CopyFileRequest getCopyFileRequest()  
                throws SRMInvalidRequestException{
			Job job = Job.getJob(fileRequestJobId);
			if(job != null) {
				return (CopyFileRequest) job;
			}
			return null;
		}
        
		public void copyComplete(String fileId, FileMetaData fmd) {
            try {
                CopyFileRequest  copyFileRequest = getCopyFileRequest();
                copyFileRequest.say("copy succeeded");
                copyFileRequest.setStateToDone();
                complete(true);
            } catch (SRMInvalidRequestException ire) {
                logger.error(ire);
            }
		}
         
		public void copyFailed(Exception e) {
			CopyFileRequest  copyFileRequest ;
            try {
                 copyFileRequest   = getCopyFileRequest();
            } catch (SRMInvalidRequestException ire) {
                logger.error(ire);
                return;
            }
			copyFileRequest.setTransferError(e);
			copyFileRequest.esay("copy failed:");
			copyFileRequest.esay(e);
			State state =  copyFileRequest.getState();
			Scheduler scheduler = Scheduler.getScheduler(copyFileRequest.getSchedulerId());
			if(!State.isFinalState(state) && scheduler != null) {
				try {
					scheduler.schedule(copyFileRequest);
				}
				catch(InterruptedException ie) {
					copyFileRequest.esay(ie);
				}
				catch(org.dcache.srm.scheduler.IllegalStateTransition ist) {
					copyFileRequest.esay(ist);
				}
			}
			complete(false);
		}
	}
     
	public  TCopyRequestFileStatus getTCopyRequestFileStatus() throws java.sql.SQLException {
		TCopyRequestFileStatus copyRequestFileStatus = new TCopyRequestFileStatus();
		copyRequestFileStatus.setFileSize(new org.apache.axis.types.UnsignedLong(size));
		copyRequestFileStatus.setEstimatedWaitTime(new Integer((int)(getRemainingLifetime()/1000)));
		copyRequestFileStatus.setRemainingFileLifetime(new Integer((int)(getRemainingLifetime()/1000)));
		org.apache.axis.types.URI to_surl;
		org.apache.axis.types.URI from_surl;
		try { to_surl= new URI(getTo_url());
		}
		catch (Exception e) { 
			esay(e);
			throw new java.sql.SQLException("wrong surl format");
		}
		try { 
			from_surl=new URI(getFrom_url());
		}
		catch (Exception e) { 
			esay(e);
			throw new java.sql.SQLException("wrong surl format");
		}
		copyRequestFileStatus.setSourceSURL(from_surl);
		copyRequestFileStatus.setTargetSURL(to_surl);
		TReturnStatus returnStatus = getReturnStatus();
		if(TStatusCode.SRM_SPACE_LIFETIME_EXPIRED.equals(returnStatus.getStatusCode())) {
			returnStatus.setStatusCode(TStatusCode.SRM_FAILURE);
		}
		copyRequestFileStatus.setStatus(returnStatus);
		return copyRequestFileStatus;
	}
	
	public TReturnStatus getReturnStatus() {
		TReturnStatus returnStatus = new TReturnStatus();
		State state = getState();
		returnStatus.setExplanation(state.toString());
		if(getStatusCode() != null) {
			returnStatus.setStatusCode(getStatusCode());
		} 
		else if(state == State.DONE) {
			returnStatus.setStatusCode(TStatusCode.SRM_SUCCESS);
		}
		else if(state == State.READY) {
			returnStatus.setStatusCode(TStatusCode.SRM_REQUEST_INPROGRESS);
		}
		else if(state == State.TRANSFERRING) {
			returnStatus.setStatusCode(TStatusCode.SRM_REQUEST_INPROGRESS);
		}
		else if(state == State.FAILED) {
			returnStatus.setStatusCode(TStatusCode.SRM_FAILURE);
			returnStatus.setExplanation("FAILED: "+getErrorMessage());
		}
		else if(state == State.CANCELED ) {
			returnStatus.setStatusCode(TStatusCode.SRM_ABORTED);
		}
		else if(state == State.TQUEUED ) {
			returnStatus.setStatusCode(TStatusCode.SRM_REQUEST_QUEUED);
		}
		else if(state == State.RUNNING || 
			state == State.RQUEUED || 
			state == State.ASYNCWAIT ) {
			returnStatus.setStatusCode(TStatusCode.SRM_REQUEST_INPROGRESS);
		}
		else {
			returnStatus.setStatusCode(TStatusCode.SRM_REQUEST_QUEUED);
		}
		return returnStatus;
	}
	
	
	public TSURLReturnStatus  getTSURLReturnStatus(String surl ) throws java.sql.SQLException{
		if(surl == null) {
			surl = getToURL();
		}
		URI tsurl;
		try {
			tsurl=new URI(surl);
		} 
		catch (Exception e) {
			esay(e);
			throw new java.sql.SQLException("wrong surl format");
		}
		TReturnStatus returnStatus =  getReturnStatus();
		if(TStatusCode.SRM_SPACE_LIFETIME_EXPIRED.equals(returnStatus.getStatusCode())) {
			returnStatus.setStatusCode(TStatusCode.SRM_FAILURE);
		}
		TSURLReturnStatus surlReturnStatus = new TSURLReturnStatus();
		surlReturnStatus.setSurl(tsurl);
		surlReturnStatus.setStatus(returnStatus);
		return surlReturnStatus;
	}

	public boolean isWeReservedSpace() {
        rlock();
        try {
            return weReservedSpace;
        } finally {
            runlock();
        }
	}
	
	public void setWeReservedSpace(boolean weReservedSpace) {
        wlock();
        try {
    		this.weReservedSpace = weReservedSpace;
        } finally {
            wunlock();
        }
	}
    
	public boolean isSpaceMarkedAsBeingUsed() {
        rlock();
        try {
    		return spaceMarkedAsBeingUsed;
        } finally {
            runlock();
        }
	}
    
	public void setSpaceMarkedAsBeingUsed(boolean spaceMarkedAsBeingUsed) {
        wlock();
        try {
    		this.spaceMarkedAsBeingUsed = spaceMarkedAsBeingUsed;
        } finally {
            wunlock();
        }
	}
    
	public static class TheReserveSpaceCallbacks implements SrmReserveSpaceCallbacks {
		Long fileRequestJobId;
		public CopyFileRequest getCopyFileRequest() 
                throws java.sql.SQLException, SRMInvalidRequestException
        {
			Job job = Job.getJob(fileRequestJobId);
			if(job != null) {
				return (CopyFileRequest) job;
			}
			return null;
		}

		public TheReserveSpaceCallbacks(Long fileRequestJobId) {
			this.fileRequestJobId = fileRequestJobId;
		}
        
		public void ReserveSpaceFailed(String reason) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setState(State.FAILED,reason);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyReserveSpaceCallbacks error: "+ reason);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		public void NoFreeSpace(String reason) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(
                            State.FAILED,
                            reason,
                            TStatusCode.SRM_NO_FREE_SPACE);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyReserveSpaceCallbacks error NoFreeSpace : "+ reason);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public void SpaceReserved(String spaceReservationToken, long reservedSpaceSize) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.say("Space Reserved: spaceReservationToken:"+spaceReservationToken);
				State state = fr.getState();
				if(state == State.ASYNCWAIT) {
					fr.say("CopyReserveSpaceCallbacks Space Reserved for file "+fr.getToPath());
					fr.setSpaceReservationId(spaceReservationToken);
					fr.setWeReservedSpace(true);
					Scheduler scheduler = Scheduler.getScheduler(fr.getSchedulerId());
					try {
						scheduler.schedule(fr);
					}
					catch(Exception ie) {
						fr.esay(ie);
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public void ReserveSpaceFailed(Exception e) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				String error = e.toString();
				try {
                    fr.setState(State.FAILED,error);
				}
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyReserveSpaceCallbacks exception");
				fr.esay(e);
			}
			catch(Exception e1) {
				e1.printStackTrace();
			}
		}
	}
     
	
	private  static class TheReleaseSpaceCallbacks implements  SrmReleaseSpaceCallbacks {
		Long fileRequestJobId;
		
		public TheReleaseSpaceCallbacks(Long fileRequestJobId) {
			this.fileRequestJobId = fileRequestJobId;
		}
		
		public CopyFileRequest getCopyFileRequest() 
                throws java.sql.SQLException, SRMInvalidRequestException {
			Job job = Job.getJob(fileRequestJobId);
			if(job != null) {
				return (CopyFileRequest) job;
			}
			return null;
		}
		
		public void ReleaseSpaceFailed( String error) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.setSpaceReservationId(null);
				fr.esay("TheReleaseSpaceCallbacks error: "+ error);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public void ReleaseSpaceFailed( Exception e) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.setSpaceReservationId(null);    
				fr.esay("TheReleaseSpaceCallbacks exception");
				fr.esay(e);
			}
			catch(Exception e1) {
				e1.printStackTrace();
			}
		}
        
		public void Timeout() {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.setSpaceReservationId(null);
				fr.esay("TheReleaseSpaceCallbacks Timeout");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
        
		public void SpaceReleased(String spaceReservationToken, long remainingSpaceSize) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.say("TheReleaseSpaceCallbacks: SpaceReleased");
				fr.setSpaceReservationId(null);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
        /**
         * Getter for property transferId.
         * @return Value of property transferId.
         */
        public java.lang.String getTransferId() {
		return transferId;
        }
	
	/**
	 * 
	 * 
	 * @param newLifetime  new lifetime in millis
	 *  -1 stands for infinite lifetime
	 * @return int lifetime left in millis
	 *  -1 stands for infinite lifetime
	 */
	public long extendLifetime(long newLifetime) throws SRMException {
		long remainingLifetime = getRemainingLifetime();
		if(remainingLifetime >= newLifetime) {
			return remainingLifetime;
		}
		long requestLifetime = getRequest().extendLifetimeMillis(newLifetime);
		if(requestLifetime <newLifetime) {
			newLifetime = requestLifetime;
		}
		if(remainingLifetime >= newLifetime) {
			return remainingLifetime;
		}
		String spaceToken =getSpaceReservationId();
		
		if(!getConfiguration().isReserve_space_implicitely() ||
		   spaceToken == null ||
		   !isWeReservedSpace()) {
			return extendLifetimeMillis(newLifetime);      
		} 
		newLifetime = extendLifetimeMillis(newLifetime);
		if( remainingLifetime >= newLifetime) {
			return remainingLifetime;
		}
		SRMUser user =(SRMUser) getUser();
		return getStorage().srmExtendReservationLifetime(user,spaceToken,newLifetime);
	}
	
	public static class CopyUseSpaceCallbacks implements SrmUseSpaceCallbacks {
		Long fileRequestJobId;

		public CopyFileRequest getCopyFileRequest() 
                throws java.sql.SQLException, SRMInvalidRequestException{
			Job job = Job.getJob(fileRequestJobId);
			if(job != null) {
				return (CopyFileRequest) job;
			}
			return null;
		}
		
		public CopyUseSpaceCallbacks(Long fileRequestJobId) {
			this.fileRequestJobId = fileRequestJobId;
		}
		
		public void SrmUseSpaceFailed( Exception e) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				String error = e.toString();
				try {
                    fr.setState(State.FAILED,error);
				} 
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyUseSpaceCallbacks exception");
				fr.esay(e);
			} 
			catch(Exception e1) {
				e1.printStackTrace();
			}
		}
		
		public void SrmUseSpaceFailed(String reason) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setState(State.FAILED,reason);
				} 
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyUseSpaceCallbacks error: "+ reason);
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * call this if space reservation exists, but has no free space
		 */
		public void SrmNoFreeSpace(String reason){
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(
                            State.FAILED,
                            reason,
                            TStatusCode.SRM_NO_FREE_SPACE);
				} 
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyUseSpaceCallbacks error: "+ reason);
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * call this if space reservation exists, but has been released
		 */
		public void SrmReleased(String reason) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(
                            State.FAILED,
                            reason,
                            TStatusCode.SRM_NO_FREE_SPACE);
				} 
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyUseSpaceCallbacks error: "+ reason);
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * call this if space reservation exists, but not authorized
		 */
		public void SrmNotAuthorized(String reason) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(
                            State.FAILED,
                            reason,
                            TStatusCode.SRM_AUTHORIZATION_FAILURE);
				} 
				catch(IllegalStateTransition ist) {
					fr.esay("can not fail state:"+ist);
				}
				fr.esay("CopyUseSpaceCallbacks error: "+ reason);
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * call this if space reservation exists, but has been released
		 */
		public void SrmExpired(String reason) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				try {
                    fr.setStateAndStatusCode(
                            State.FAILED,
                            reason,
                            TStatusCode.SRM_SPACE_LIFETIME_EXPIRED);
				} 
				catch(IllegalStateTransition ist) {
					fr.esay("Illegal State Transition : " +ist.getMessage());
				}
				fr.esay("CopyUseSpaceCallbacks error: "+ reason);
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public void SpaceUsed() {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.say("Space Marked as Being Used");
				State state = fr.getState();
				if(state == State.ASYNCWAIT) {
					fr.say("CopyUseSpaceCallbacks Space Marked as Being Used for file "+fr.getToURL());
					fr.setSpaceMarkedAsBeingUsed(true);
					Scheduler scheduler = Scheduler.getScheduler(fr.getSchedulerId());
					try {
						scheduler.schedule(fr);
					} 
					catch(Exception ie) {
						fr.esay(ie);
					}
				}
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class CopyCancelUseOfSpaceCallbacks implements SrmCancelUseOfSpaceCallbacks {
		Long fileRequestJobId;
		
		public CopyFileRequest getCopyFileRequest() 
                throws java.sql.SQLException, SRMInvalidRequestException {
			Job job = Job.getJob(fileRequestJobId);
			if(job != null) {
				return (CopyFileRequest) job;
			}
			return null;
		}
        
		public CopyCancelUseOfSpaceCallbacks(Long fileRequestJobId) {
			this.fileRequestJobId = fileRequestJobId;
		}
        
		public void CancelUseOfSpaceFailed( Exception e) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				String error = e.toString();
				fr.esay("CopyCancelUseOfSpaceCallbacks exception");
				fr.esay(e);
			} 
			catch(Exception e1) {
				e1.printStackTrace();
			}
		}
		
		public void CancelUseOfSpaceFailed(String reason) {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.esay("CopyCancelUseOfSpaceCallbacks error: "+ reason);
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public void UseOfSpaceSpaceCanceled() {
			try {
				CopyFileRequest fr = getCopyFileRequest();
				fr.say("Umarked Space as Being Used");
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
