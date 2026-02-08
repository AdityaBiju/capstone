package scb.general.ejb;

import scb.util.*;
import scb.exception.SCBApplicationException;
import scb.exception.SQLExecuteException;



import java.util.*;
import javax.ejb.*;
import javax.naming.*;

import scb.util.*;
import scb.wrapper.*;
import scb.exception.*;
//import scb.security.process.*;
import scb.general.factory.SCBParamDAOFactory;
import scb.general.wrapper.*;
//import scb.security.process.QuerySessionHome;

/**
 * This is a Session Bean Class
 */

public class SCBParamSessionBean implements SessionBean {
	private SessionContext mySessionCtx = null;

	public SCBParamEntityHome scbparamEntityHome=null;
	public TSCBParamEntityHome tScbParamEntityHome=null;
	public SCBParamLockEntityHome scbparamLockEntityHome=null;


	public InitialContext initialContext=null;
	SystemProperties sysProperties=null;

	//public scb.security.process.QuerySessionHome querySessionHome=null;
	//public scb.security.process.QuerySession querySession=null;
	SECQuerySessionProxy querySessionProxy = null;
	LogWriter logger = null;

	public boolean authorise(AbstractWrapper argWrapper) throws  SCBApplicationException {
			try {
	
				int authorisationLevel = argWrapper.getAuthorisationLevel();
				if (authorisationLevel == EBBSConstants.MAKER_ONLY) {
					throw new SCBApplicationException("BUS", "2500", "Invalid Operation.");
				}
	
				SCBParamWrapper tempWrapper = null;
				SCBParamEntity scbparamEntity = null;
				TSCBParamEntity tScbParamEntity = null;
	
				try {
					releaseLock(argWrapper);
					SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
					tScbParamEntity = tScbParamEntityHome.findByPrimaryKeyForUpdate(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
					tempWrapper = (SCBParamWrapper) tScbParamEntity.getWrapper();
	
					//querySession.isAuthorizer(argWrapper.getCheckerDetails().getUserID().trim(), tempWrapper.getMakerDetails().getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					querySessionProxy.executeMethodOnRemoteObject("isAuthorizer",new Class[] {String.class, String.class, String.class},argWrapper.getCheckerDetails().getUserID().trim(), tempWrapper.getMakerDetails().getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_CREATE) {
						tempWrapper.statusFlag = EBBSConstants.ACTIVE;
						tempWrapper.setCheckerDetails(scbparamWrapper.getCheckerDetails());
						scbparamEntity = scbparamEntityHome.create(tempWrapper);
						tScbParamEntity.remove();
	
					} else if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_DELETE) {
						scbparamEntity = scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
						//Changes for EBBSHRM-19183 By 1606639
						/*scbparamEntity.setStatusFlag(EBBSConstants.MARKED_DELETE);
						scbparamEntity.setCheckerDetails(argWrapper.getCheckerDetails());*/
						
						
						SCBParamWrapper wrapperFromMain = (SCBParamWrapper) scbparamEntity.getWrapper();
						wrapperFromMain.statusFlag = EBBSConstants.MARKED_DELETE;
						wrapperFromMain.checkerDetail = argWrapper.getCheckerDetails();
						scbparamEntity.setWrapper(wrapperFromMain);
						
						tScbParamEntity.remove();
						//ScbParamHistoryEntity ScbParamHistoryEntity = ScbParamHistoryEntityHome.create(scbparamWrapper);
	
					} else if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_MODIFY || tempWrapper.statusFlag == EBBSConstants.AUTHORISE_REVERSAL ||  tempWrapper.statusFlag == EBBSConstants.REJECT_REVERSAL) {
						scbparamEntity = scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
						tempWrapper.statusFlag = EBBSConstants.ACTIVE;
						//System.out.println("Inside authorise....method  flag is adding ");
						tempWrapper.checkerDetail = argWrapper.getCheckerDetails();
						scbparamEntity.setWrapper(tempWrapper);
						tScbParamEntity.remove();
					}
					
				try {
					SystemParameterCache.getInstance().clearSystemParameterCache(Utility.getCompanyID());
				} catch (Exception e) {
					e.printStackTrace();
				}

				} catch(ObjectNotFoundException ob) {
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException("DBS","2001","Record not found");
	
				} catch(FinderException fe) {
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException(fe);
	
				} catch(RemoveException re) {
					mySessionCtx.setRollbackOnly();
					if(re.getMessage()==null)
						throw new SCBApplicationException("DBS","2009","Rejection Process Failed");
					throw new SCBApplicationException(re);
	
				} catch(CreateException ce) {
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException(ce);
				}
				return true;
			}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public java.io.Serializable create(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {

			int authorisationLevel = argWrapper.getAuthorisationLevel();
			SCBParamWrapper scbparamWrapper =(SCBParamWrapper) argWrapper;

			try {
				SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
				throw new SCBApplicationException("DBS","2003","Record already exists");

			} catch(ObjectNotFoundException ob) {
				try {
					if(authorisationLevel == EBBSConstants.MAKER_ONLY) {
						scbparamWrapper.statusFlag = EBBSConstants.ACTIVE;
						SCBParamEntity scbParamEntity = scbparamEntityHome.create(scbparamWrapper);
						scbparamWrapper.statusFlag = BusinessConstants.CREATED;
						//ScbParamHistoryEntity ScbParamHistoryEntity = ScbParamHistoryEntityHome.create(scbparamWrapper);

					} else {
						scbparamWrapper.statusFlag = EBBSConstants.AUTHORISE_CREATE;
						TSCBParamEntity tScbParamEntity = tScbParamEntityHome.create(scbparamWrapper);
					}
				} catch(DuplicateKeyException de) {
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException("DBS","2003","Primary Key already exists");

				} catch(CreateException ce) {
					mySessionCtx.setRollbackOnly();
					if(ce.getMessage()==null)
						throw new SCBApplicationException("DBS","2004","Create process failed");
					else
						throw new SCBApplicationException(ce);
				}

			} catch(FinderException fe) {
				if(fe.getMessage()==null)
					throw new SCBApplicationException("DBS","2004","Create process failed");
				else
					throw new SCBApplicationException(fe);
			}
			return new Boolean(true);
		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public boolean delete(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {

			int authorisationLevel = argWrapper.getAuthorisationLevel();
			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
			SCBParamEntity scbparamEntity=null;

			if (authorisationLevel == EBBSConstants.MAKER_ONLY) {
				try {
					scbparamEntity = scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
					scbparamEntity.remove();
					scbparamWrapper.statusFlag = BusinessConstants.DELETED;
					//ScbParamHistoryEntity ScbParamHistoryEntity = ScbParamHistoryEntityHome.create(scbparamWrapper);
					return true;

				} catch (FinderException fe) {
					if (fe.getMessage() == null)
						throw new SCBApplicationException("DBS", "2005", "Record Already Deleted");
					else
						throw new SCBApplicationException(fe);
				} catch (RemoveException re) {
					if (re.getMessage() == null)
						throw new SCBApplicationException("BUS", "2006", "Delete Process Failed");
					else
						throw new SCBApplicationException(re);
				} catch (Exception re) {
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException("SYS", "1000", "Contact System Administrator");
				}

			} else {

				try {
					releaseLock(argWrapper);
					TSCBParamEntity tScbParamEntity = tScbParamEntityHome.findByPrimaryKeyForUpdate(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
					//Changes for EBBSHRM-19183 By 1606639
					//short flag = tScbParamEntity.getStatusFlag();
					
					
					SCBParamWrapper wrapperFromTemp = (SCBParamWrapper) tScbParamEntity.getWrapper();
					short flag = wrapperFromTemp.statusFlag;
					
					if(flag==EBBSConstants.AUTHORISE_MODIFY || flag==EBBSConstants.AUTHORISE_CREATE || flag==EBBSConstants.REJECT_CREATE || flag==EBBSConstants.REJECT_MODIFY) {
						throw new SCBApplicationException("BUS","3007","Cannot delete record,Record under process");

					} else if(flag==EBBSConstants.REJECT_DELETE) {
						scbparamEntity=scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
						/*tScbParamEntity.setStatusFlag(EBBSConstants.AUTHORISE_DELETE);
						scbparamEntity.setStatusFlag(EBBSConstants.AUTHORISE_DELETE);
						tScbParamEntity.setRejectReason(null);
						tScbParamEntity.setMakerDetails(argWrapper.getMakerDetails());
						tScbParamEntity.setCheckerDetails(null);*/
						
						
						SCBParamWrapper wrapperFromMain = (SCBParamWrapper) scbparamEntity.getWrapper();
						wrapperFromMain.statusFlag = EBBSConstants.AUTHORISE_DELETE;
						wrapperFromTemp.statusFlag = EBBSConstants.AUTHORISE_DELETE;
						wrapperFromTemp.reason = null;
						wrapperFromTemp.makerDetail = argWrapper.getMakerDetails();
						wrapperFromTemp.checkerDetail = null;
						scbparamEntity.setWrapper(wrapperFromMain);
						tScbParamEntity.setWrapper(wrapperFromTemp);

					} else if(flag==EBBSConstants.AUTHORISE_DELETE) {
						//Changes for EBBSHRM-19183 By 1606639
						//tScbParamEntity.setMakerDetails(argWrapper.getMakerDetails());
						
						
						wrapperFromTemp.makerDetail = argWrapper.getMakerDetails();
						tScbParamEntity.setWrapper(wrapperFromTemp);

					} else {
						throw new SCBApplicationException("BUS","3007","Cannot delete record,It is under process");
					}
				} catch(ObjectNotFoundException ob) {
					try {
						scbparamEntity = scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
						//Changes for EBBSHRM-19183 By 1606639
						//scbparamEntity.setStatusFlag(EBBSConstants.AUTHORISE_DELETE);
						SCBParamWrapper mainWrapper = (SCBParamWrapper) scbparamEntity.getWrapper();
						
						
						mainWrapper.statusFlag = EBBSConstants.AUTHORISE_DELETE;
						scbparamEntity.setWrapper(mainWrapper);
						
						mainWrapper.makerDetail = argWrapper.getMakerDetails();
						mainWrapper.checkerDetail = null;
						tScbParamEntityHome.create(mainWrapper);

					} catch(ObjectNotFoundException obe) {
						mySessionCtx.setRollbackOnly();
						throw new SCBApplicationException("DBS","2001","Record not found");

					} catch(FinderException fe) {
						mySessionCtx.setRollbackOnly();
						if(fe.getMessage()==null)
							throw new SCBApplicationException();
						throw new SCBApplicationException(fe);

					} catch(CreateException ce) {
						mySessionCtx.setRollbackOnly();
						if(ce.getMessage()==null)
							throw new SCBApplicationException();
						throw new SCBApplicationException(ce);
					}

				} catch(FinderException fe) {
					mySessionCtx.setRollbackOnly();
					if(fe.getMessage()==null)
						throw new SCBApplicationException("DBS", "2006", "Delete Process Failed");
					throw new SCBApplicationException(fe);

				} catch(Exception e) {
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException("SYS","1000","Cotact System Administrator");
				}
				return true;
			}

		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public void ejbActivate()  {}


	public void ejbCreate() throws CreateException {
		try {

			try {
				sysProperties = SystemProperties.getInstance();
				//Changes for EBBSHRM-19183 By 1606639
				/*scbparamEntityHome  = (SCBParamEntityHome) sysProperties.localLookup(EBBSConstants.GENERAL_APPLICATION  + "/SCBParamEntity");
				tScbParamEntityHome = (TSCBParamEntityHome) sysProperties.localLookup(EBBSConstants.GENERAL_APPLICATION + "/TSCBParamEntity");
				scbparamLockEntityHome = (SCBParamLockEntityHome)sysProperties.localLookup(EBBSConstants.GENERAL_APPLICATION + "/SCBParamLockEntity");*/
				
				scbparamEntityHome  = (SCBParamEntityHome)SCBParamDAOFactory.getDAOInstance("SCBParamEntity");
				tScbParamEntityHome = (TSCBParamEntityHome)SCBParamDAOFactory.getDAOInstance("TSCBParamEntity");
				scbparamLockEntityHome = (SCBParamLockEntityHome)SCBParamDAOFactory.getDAOInstance("SCBParamLockEntity");

				//querySessionHome = (QuerySessionHome) sysProperties.localLookup(EBBSConstants.SECURITY_APPLICATION+"/"+"QuerySession");
				//querySession = querySessionHome.create();
				querySessionProxy = new SECQuerySessionProxy();
			} catch(NamingException e) {
				scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
				throw new CreateException(EBBSConstants.NAMING_EXCEPTION);
			}
			//This is a System generated output
		} catch (Exception e) {
			scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			throw new EJBException("In ejbCreate"+e.getMessage());
		}
	}


	public void ejbPassivate()  {}

	public void ejbRemove()  {}


	public AbstractWrapper fetchForAuthorisation(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {
			int authorisationLevel = argWrapper.getAuthorisationLevel();

			if (authorisationLevel == EBBSConstants.MAKER_ONLY) {
				throw new SCBApplicationException("BUS", "2500", "Invalid Operation.");
			}

			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
			SCBParamWrapper wrapper = null;


			try {

				if(scbparamWrapper == null)
					throw new FinderException(EBBSConstants.ILLEGAL_ARGUMENT_EXCEPTION);

				TSCBParamEntity tScbParamEntity = tScbParamEntityHome.findByPrimaryKey(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
				wrapper = (SCBParamWrapper) tScbParamEntity.getWrapper();

				if (wrapper.statusFlag == EBBSConstants.AUTHORISE_MODIFY) {
					SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
					wrapper.setModifiedFields(scbparamEntity.getWrapper());

				}

				if(wrapper.statusFlag==EBBSConstants.AUTHORISE_MODIFY || wrapper.statusFlag==EBBSConstants.AUTHORISE_CREATE || wrapper.statusFlag==EBBSConstants.AUTHORISE_DELETE || wrapper.statusFlag == EBBSConstants.AUTHORISE_REVERSAL || wrapper.statusFlag == EBBSConstants.REJECT_REVERSAL) {

					try {
						//if (querySession.isAuthorizer(scbparamWrapper.checkerDetail.getUserID(),wrapper.makerDetail.getUserID(),SystemProperties.getInstance().getCurrentApplicationID())) {
						Boolean isAuthorizer = (boolean)querySessionProxy.executeMethodOnRemoteObject("isAuthorizer",new Class[] {String.class, String.class, String.class},scbparamWrapper.checkerDetail.getUserID(),wrapper.makerDetail.getUserID(),SystemProperties.getInstance().getCurrentApplicationID());
						if(isAuthorizer) {
							if(isLocked(wrapper.scbparamkey,wrapper.scbparamvalue, scbparamWrapper.checkerDetail))
								throw new SCBApplicationException("BUS","3002","Record under process");
						}

					} catch(SCBApplicationException ae) {
						if (ae.getErrorType().equals("BUS") && ae.getErrorCode().equals("3005")) {
							if(wrapper.statusFlag==EBBSConstants.AUTHORISE_MODIFY)
								wrapper.statusFlag=EBBSConstants.AUTHORISE_MODIFY_NOT_ALLOWED;
							else if(wrapper.statusFlag==EBBSConstants.AUTHORISE_CREATE)
								wrapper.statusFlag=EBBSConstants.AUTHORISE_CREATE_NOT_ALLOWED;
							else if(wrapper.statusFlag==EBBSConstants.AUTHORISE_DELETE)
								wrapper.statusFlag=EBBSConstants.AUTHORISE_DELETE_NOT_ALLOWED;

						} else throw ae;
					}

				} else
					throw new SCBApplicationException("BUS","3004","No such record for authorisation");

			} catch(ObjectNotFoundException ob) {
				try {
					SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
					throw new SCBApplicationException("BUS","3004");

				} catch(FinderException e) {
					if(e instanceof ObjectNotFoundException) {
						throw new SCBApplicationException("DBS","2001");

					} else if(e.getMessage()==null) {
						throw new SCBApplicationException("DBS","2002");

					} else
						throw new SCBApplicationException(e);
				}

			} catch(FinderException fe) {
				if(fe.getMessage()==null)
					throw new SCBApplicationException("BUS","3002","Fetch process failed");
				else
					throw new SCBApplicationException(fe);

			} catch(Exception ex) {
				scb.util.LogWriter.getInstance().printErrorMessage(ex);// //- F16 reengineering
				if(ex instanceof SCBApplicationException)
					throw (SCBApplicationException) ex;
				throw new SCBApplicationException("SYS","1000","Contact System Administrator");
			}
			return wrapper;
		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public AbstractWrapper fetchForDeletion(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {

			int authorisationLevel = argWrapper.getAuthorisationLevel() ;
			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
			SCBParamWrapper wrapper = null;

			SCBParamEntity  scbparamEntity  = null;
			TSCBParamEntity tScbParamEntity = null;

			try	{
				scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
				wrapper = (SCBParamWrapper) scbparamEntity.getWrapper();

				if (authorisationLevel == EBBSConstants.MAKER_ONLY) {
					return wrapper;
				}

				if(wrapper.statusFlag==EBBSConstants.REJECT_DELETE) {
					tScbParamEntity = tScbParamEntityHome.findByPrimaryKey(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
					wrapper = (SCBParamWrapper) tScbParamEntity.getWrapper();
				}

				if(wrapper.statusFlag==EBBSConstants.ACTIVE || wrapper.statusFlag==EBBSConstants.AUTHORISE_DELETE || wrapper.statusFlag==EBBSConstants.REJECT_DELETE) {
					//querySession.isModifyAllowed(scbparamWrapper.makerDetail.getUserID().trim(),wrapper.makerDetail.getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					querySessionProxy.executeMethodOnRemoteObject("isModifyAllowed", new Class[] {String.class, String.class, String.class},scbparamWrapper.makerDetail.getUserID().trim(),wrapper.makerDetail.getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					if(isLocked(wrapper.scbparamkey,wrapper.scbparamvalue, argWrapper.getMakerDetails()))
						throw new SCBApplicationException("BUS","3002","Record under use,Retry later");
					return wrapper;

				} else if( wrapper.statusFlag==EBBSConstants.AUTHORISE_MODIFY || wrapper.statusFlag==EBBSConstants.REJECT_MODIFY) {
					throw new SCBApplicationException("BUS","3002","Record under use,Retry later");

				} else if(wrapper.statusFlag==EBBSConstants.MARKED_DELETE)
					throw new SCBApplicationException("BUS","3003","Record already deleted");

			} catch(ObjectNotFoundException ob) {
				throw new SCBApplicationException("DBS","2001","Record not found");

			} catch(FinderException fe) {
				if(fe.getMessage()==null)
					throw new SCBApplicationException("BUS","3002","Fetch process failed");
				else
					throw new SCBApplicationException(fe);

			} catch(Exception ex) {
				scb.util.LogWriter.getInstance().printErrorMessage(ex);// //- F16 reengineering
				if(ex instanceof SCBApplicationException)
					throw (SCBApplicationException)ex;
				else
					throw new SCBApplicationException("SYS","1000","Contact System administrator");
			}
			return wrapper;

		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}

	public AbstractWrapper fetchForModification(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {

			logger = new LogWriter("SCBPARAM","FFM","");
			int authorisationLevel = argWrapper.getAuthorisationLevel() ;
			SCBParamWrapper scbParamWrapper = (SCBParamWrapper) argWrapper;
			SCBParamWrapper wrapper = null;
			SCBParamEntity scbParamEntity   = null;
			TSCBParamEntity tScbParamEntity = null;

			try {
				if(scbParamWrapper==null)
					throw new FinderException(EBBSConstants.ILLEGAL_ARGUMENT_EXCEPTION);

				scbParamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbParamWrapper.scbparamkey));
				wrapper = (SCBParamWrapper) scbParamEntity.getWrapper();
				logger.println("Input for Lock From Main Table:"+wrapper.scbparamkey+"|"+wrapper.scbparamvalue);
				//===
				if(authorisationLevel == EBBSConstants.MAKER_ONLY) {
					return wrapper;
				}
				//===

				if (wrapper.statusFlag==EBBSConstants.MARKED_DELETE || wrapper.statusFlag==EBBSConstants.AUTHORISE_DELETE || wrapper.statusFlag==EBBSConstants.REJECT_DELETE)
					throw new SCBApplicationException("BUS", "3001", "Record already marked for deletion, cannot modify it");

				if(wrapper.statusFlag==EBBSConstants.ACTIVE) {
					//querySession.isModifyAllowed(scbParamWrapper.makerDetail.getUserID().trim(), wrapper.makerDetail.getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					querySessionProxy.executeMethodOnRemoteObject("isModifyAllowed", new Class[] {String.class, String.class, String.class},scbParamWrapper.makerDetail.getUserID().trim(), wrapper.makerDetail.getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					if(isLocked(wrapper.scbparamkey,wrapper.scbparamvalue, argWrapper.getMakerDetails()))
						throw new SCBApplicationException("BUS","3002","Record under use,Retry later");
					return wrapper;

				} else
					throw new ObjectNotFoundException();

			} catch(ObjectNotFoundException ex) {
				try {

					//==
					if (authorisationLevel == EBBSConstants.MAKER_ONLY) {
						throw new SCBApplicationException("DBS", "2001", "Record not found");
					}
					//==

					short[] statusFlag = {EBBSConstants.AUTHORISE_MODIFY, EBBSConstants.AUTHORISE_CREATE, EBBSConstants.REJECT_MODIFY, EBBSConstants.REJECT_CREATE };
					tScbParamEntity = tScbParamEntityHome.findByStatusFlag(scbParamWrapper.scbparamkey, statusFlag);
					wrapper = (SCBParamWrapper) tScbParamEntity.getWrapper();
					logger.println("Input for Lock From Temp Table:"+wrapper.scbparamkey+"|"+wrapper.scbparamvalue);
					//querySession.isModifyAllowed(scbParamWrapper.makerDetail.getUserID(), wrapper.makerDetail.getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					querySessionProxy.executeMethodOnRemoteObject("isModifyAllowed", new Class[] {String.class, String.class, String.class},scbParamWrapper.makerDetail.getUserID(), wrapper.makerDetail.getUserID().trim(),SystemProperties.getInstance().getCurrentApplicationID());
					if(isLocked(wrapper.scbparamkey,wrapper.scbparamvalue, argWrapper.getMakerDetails()))
						throw new SCBApplicationException("BUS","3002","record under process, Retry later");

				} catch(ObjectNotFoundException fe) {
					throw new SCBApplicationException("DBS","2001","Record not found");

				} catch(FinderException fe) {
					if(fe.getMessage()==null)
						throw new SCBApplicationException("BUS","3002","Fetch process failed");
					else
						throw new SCBApplicationException(fe);
				}

			} catch(FinderException fe) {
				if(fe.getMessage()==null)
					throw new SCBApplicationException("BUS","3002","Fetch process failed");
				else
					throw new SCBApplicationException(fe);

			} catch(Exception ex) {
				scb.util.LogWriter.getInstance().printErrorMessage(ex);// //- F16 reengineering
				if(ex instanceof SCBApplicationException)
					throw (SCBApplicationException) ex;
				throw new SCBApplicationException("SYS","1000","Contact System Administrator");

			}
			return wrapper;

		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public AbstractWrapper fetchForView(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {

			short statusFlag[] = new short[1];
			statusFlag[0] = EBBSConstants.ACTIVE;
			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
			try
			{
				if(scbparamWrapper==null)
					throw new FinderException(EBBSConstants.ILLEGAL_ARGUMENT_EXCEPTION);

				SCBParamEntity scbparamEntity = scbparamEntityHome.findByStatusFlag(scbparamWrapper.scbparamkey, statusFlag);
				scbparamWrapper = (SCBParamWrapper) scbparamEntity.getWrapper();

			} catch(ObjectNotFoundException ob) {
				throw new SCBApplicationException("DBS","2001","Record not found");

			} catch(FinderException fe) {
				if(fe.getMessage()==null)
					throw new SCBApplicationException("BUS","3002","fetch process failed");
				else
					throw new SCBApplicationException(fe);

			} catch(Exception ex) {
				scb.util.LogWriter.getInstance().printErrorMessage(ex);// //- F16 reengineering
				throw new SCBApplicationException("SYS","1000","Contact System administrator");

			}
			return scbparamWrapper;

		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public SessionContext getSessionContext() {
		return mySessionCtx;
	}


	public boolean isLocked(String scbparamkey, String scbparamval, UserDetail userDetail) throws  SCBApplicationException {

		boolean flag = false;
		try {
			SCBParamLockEntity lockEntity = scbparamLockEntityHome.findByPrimaryKey(new SCBParamLockEntityKey(scbparamkey,scbparamval));
			
			//String userId1 = lockEntity.getMakerDetails().getUserID().trim();
			SCBParamWrapper wrapperFromLock = (SCBParamWrapper) lockEntity.getWrapper();
			String userId1 = wrapperFromLock.makerDetail.getUserID().trim();
			String userId2 = userDetail.getUserID().trim();

			if(userId1.equals(userId2))
				flag = false;
			else
				flag = true;

		} catch(ObjectNotFoundException oe) {
			try {
				scbparamLockEntityHome.create(new SCBParamLockEntityKey(scbparamkey,scbparamval), userDetail);
				flag = false;

			} catch(DuplicateKeyException dup) {
				flag = true;

			} catch(Exception e) {
				scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
				throw new SCBApplicationException("DBS","2017");
			}

		} catch(Exception e) {
			scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			throw new SCBApplicationException("DBS","2017");
		}
		return flag;

	}


	public boolean modify(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {

			scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 1 ");
			int authorisationLevel = argWrapper.getAuthorisationLevel();
			boolean isMainChanged = false;
			TSCBParamEntity tScbParamEntity = null;
			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
			scb.util.LogWriter.getInstance().println("ScbParam -inside 1A ..."+ scbparamWrapper.scbparamvalue);

			if (authorisationLevel == EBBSConstants.MAKER_ONLY) {
				try {

					SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));

					SCBParamWrapper oldWrapper = (SCBParamWrapper) scbparamEntity.getWrapper();
					if (!scbparamWrapper.equals(oldWrapper)) {
						scbparamWrapper.statusFlag = EBBSConstants.ACTIVE;
						scbparamEntity.setWrapper(scbparamWrapper);
						scbparamWrapper.statusFlag = BusinessConstants.MODIFIED;
						//ScbParamHistoryEntity ScbParamHistoryEntity = ScbParamHistoryEntityHome.create(scbparamWrapper);
					}

				} catch (FinderException fe) {
					mySessionCtx.setRollbackOnly();
					if (fe.getMessage() == null)
						throw new SCBApplicationException("DBS", "2008", "Update Process Failed");
					else
						throw new SCBApplicationException(fe);
				} catch (Exception ex) {
					throw new SCBApplicationException("SYS", "1000", "Contact System Administrator");
				}
				return true;

			} else {
				try {
					releaseLock(argWrapper);
					tScbParamEntity = tScbParamEntityHome.findByPrimaryKeyForUpdate(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
					SCBParamWrapper tempWrapper = (SCBParamWrapper) tScbParamEntity.getWrapper();

					boolean isTempChanged = !scbparamWrapper.equals(tempWrapper);
					scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside checkign the boolean .. "+isTempChanged);
					if( tempWrapper.statusFlag==EBBSConstants.AUTHORISE_CREATE ) {
						scbparamWrapper.statusFlag = EBBSConstants.AUTHORISE_CREATE;
						if(isTempChanged) {
							tScbParamEntity.setWrapper(scbparamWrapper);
						}
						return true;

					} else if(tempWrapper.statusFlag==EBBSConstants.REJECT_CREATE) {
						scbparamWrapper.statusFlag = EBBSConstants.AUTHORISE_CREATE;
						scbparamWrapper.reason = null;
						if(isTempChanged){
							tScbParamEntity.setWrapper(scbparamWrapper);

						} else {
							//Changes for EBBSHRM-19183 By 1606639
							/*tScbParamEntity.setStatusFlag(EBBSConstants.AUTHORISE_CREATE);
							tScbParamEntity.setRejectReason(null);*/
							
							tempWrapper.statusFlag = EBBSConstants.AUTHORISE_CREATE;
							tempWrapper.reason = null;
							tScbParamEntity.setWrapper(tempWrapper);
						}
						return true;
					}
					SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
					SCBParamWrapper mainWrapper = (SCBParamWrapper) scbparamEntity.getWrapper();
					isMainChanged = !scbparamWrapper.equals(mainWrapper);
					scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside printing the maincheanged ... "+isMainChanged);
					scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside printing the  ... "+tempWrapper.statusFlag);
					if(tempWrapper.statusFlag==EBBSConstants.REJECT_MODIFY) {
						scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 2 ");
						scbparamWrapper.statusFlag = EBBSConstants.AUTHORISE_MODIFY;
						scbparamWrapper.reason = null;
						if(isTempChanged) {
							scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 3 ");
							if(isMainChanged) {
								scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 4 ");
								//Changes for EBBSHRM-19183 By 1606639
								//scbparamEntity.setStatusFlag(EBBSConstants.AUTHORISE_MODIFY);
								
								mainWrapper.statusFlag = EBBSConstants.AUTHORISE_MODIFY;
								scbparamEntity.setWrapper(mainWrapper);
								
								tScbParamEntity.setWrapper(scbparamWrapper);

							} else {
								tScbParamEntity.remove();
								//scbparamEntity.setStatusFlag(EBBSConstants.ACTIVE);
								
								
								mainWrapper.statusFlag = EBBSConstants.ACTIVE;
								scbparamEntity.setWrapper(mainWrapper);
								return false;
							}

						} else {
							//scbparamEntity.setStatusFlag(EBBSConstants.AUTHORISE_MODIFY);
							
							
							mainWrapper.statusFlag = EBBSConstants.AUTHORISE_MODIFY;
							scbparamEntity.setWrapper(mainWrapper);
							
							/*tScbParamEntity.setStatusFlag(scbparamWrapper.statusFlag);
							tScbParamEntity.setRejectReason(null);*/
							
							
							tempWrapper.statusFlag = EBBSConstants.AUTHORISE_MODIFY;
							tempWrapper.reason = null;
							tScbParamEntity.setWrapper(tempWrapper);
						}

					} else if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_MODIFY) {
						scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 5 ");
						scbparamWrapper.statusFlag = EBBSConstants.AUTHORISE_MODIFY;
						if(isTempChanged) {
							scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 6 ");
							if(isMainChanged) {
								scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 7 "+scbparamWrapper.scbparamvalue);
								tScbParamEntity.setWrapper(scbparamWrapper);

							} else {
								scb.util.LogWriter.getInstance().printDebugMode("ScbParam -inside 8 ");
								tScbParamEntity.remove();
								
								//scbparamEntity.setStatusFlag(EBBSConstants.ACTIVE);
								
								
								mainWrapper.statusFlag = EBBSConstants.ACTIVE;
								scbparamEntity.setWrapper(mainWrapper);
								//			  		return false;
								return true;
							}
						}
					}

				} catch(ObjectNotFoundException ob) {
					try {
						SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKeyForUpdate(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
						SCBParamWrapper mainWrapper = (SCBParamWrapper) scbparamEntity.getWrapper();
						isMainChanged = !scbparamWrapper.equals(mainWrapper);
						if(isMainChanged) {
							//Changes for EBBSHRM-19183 By 1606639
							//scbparamEntity.setStatusFlag(EBBSConstants.AUTHORISE_MODIFY);
							
							
							mainWrapper.statusFlag = EBBSConstants.AUTHORISE_MODIFY;
							scbparamEntity.setWrapper(mainWrapper);
							
							scbparamWrapper.statusFlag = EBBSConstants.AUTHORISE_MODIFY;
							tScbParamEntityHome.create(scbparamWrapper);
							return true;
						}

					} catch(CreateException ce) {
						scb.util.LogWriter.getInstance().printErrorMessage(ce);// //- F16 reengineering
						mySessionCtx.setRollbackOnly();
						if(ce.getMessage()==null)
							throw new SCBApplicationException("DBS","2011","Error while saving changes. Updation Failed.");
						throw new SCBApplicationException(ce);

					} catch(ObjectNotFoundException oe) {
						scb.util.LogWriter.getInstance().printErrorMessage(oe);// //- F16 reengineering
						mySessionCtx.setRollbackOnly();
						throw new SCBApplicationException("DBS","2012","Record Not Found, Updation failed");

					} catch(FinderException fe) {
						scb.util.LogWriter.getInstance().printErrorMessage(fe);// //- F16 reengineering
						mySessionCtx.setRollbackOnly();
						if(fe.getMessage()==null)
							throw new SCBApplicationException("DBS", "2008", "Updation Process Failed.");
						throw new SCBApplicationException(fe);
					}

				} catch(Exception ex) {
					scb.util.LogWriter.getInstance().printErrorMessage(ex);// //- F16 reengineering
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException("SYS","1000","Contact System Administrator");
				}
				return true;
			}

		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public boolean reject(AbstractWrapper argWrapper)throws  SCBApplicationException {
		try {

			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
			boolean flag = false;

			try {

				releaseLock(argWrapper);
				if(scbparamWrapper.checkerDetail!=null && scbparamWrapper.makerDetail!=null) {
					throw new SCBApplicationException("BUS","3006","Rejection should come from either maker or checker");
				}

				if(scbparamWrapper.checkerDetail!=null) {
					try {
						TSCBParamEntity tScbParamEntity = tScbParamEntityHome.findByPrimaryKey(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
						SCBParamWrapper tempWrapper = (SCBParamWrapper) tScbParamEntity.getWrapper();

						if(tempWrapper.makerDetail.getUserID().equals(scbparamWrapper.checkerDetail.getUserID()))
							throw new SCBApplicationException("BUS","3005","Maker cannot authorise the record");
						if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_CREATE)
							scbparamWrapper.statusFlag=EBBSConstants.REJECT_CREATE;
						else if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_DELETE)
							scbparamWrapper.statusFlag=EBBSConstants.REJECT_DELETE;
						else if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_MODIFY)
							scbparamWrapper.statusFlag=EBBSConstants.REJECT_MODIFY;
						else if (tempWrapper.statusFlag == EBBSConstants.AUTHORISE_REVERSAL)
							scbparamWrapper.statusFlag = EBBSConstants.REJECT_REVERSAL;
						else if (tempWrapper.statusFlag == EBBSConstants.REJECT_REVERSAL)
							scbparamWrapper.statusFlag = EBBSConstants.REJECT_REVERSAL;

						//tempWrapper.applicationID = scbparamWrapper.applicationID ;
						tempWrapper.statusFlag = scbparamWrapper.statusFlag ;
						tempWrapper.checkerDetail = scbparamWrapper.checkerDetail ;
						tempWrapper.reason = scbparamWrapper.reason ;
						tScbParamEntity.setWrapper(tempWrapper) ;
						//Changes for EBBSHRM-19183 By 1606639
						/*tScbParamEntity.setCheckerDetails(scbparamWrapper.checkerDetail);
						tScbParamEntity.setStatusFlag(scbparamWrapper.statusFlag);
						tScbParamEntity.setRejectReason(scbparamWrapper.reason);*/

						if(tempWrapper.statusFlag!=EBBSConstants.REJECT_CREATE) {
							SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
							
							//scbparamEntity.setStatusFlag(scbparamWrapper.statusFlag);
							
							SCBParamWrapper wrapperFromMain = (SCBParamWrapper) scbparamEntity.getWrapper();
							wrapperFromMain.statusFlag = scbparamWrapper.statusFlag;
							scbparamEntity.setWrapper(wrapperFromMain);
						}
						flag=true;

					} catch(ObjectNotFoundException ob) {
						throw new SCBApplicationException("DBS","2001","Record not found");

					} catch(FinderException fe) {
						scb.util.LogWriter.getInstance().printErrorMessage(fe);// //- F16 reengineering
						if(fe.getMessage()==null)
							throw new SCBApplicationException("DBS","2009","Rejection process failed");
						else
							throw new SCBApplicationException(fe.getMessage());
					}

				} else if(scbparamWrapper.makerDetail!=null) {
					try {
						TSCBParamEntity tScbParamEntity = tScbParamEntityHome.findByPrimaryKey(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
						SCBParamWrapper tempWrapper = (SCBParamWrapper)tScbParamEntity.getWrapper();

						if(tempWrapper.statusFlag==EBBSConstants.AUTHORISE_MODIFY || tempWrapper.statusFlag==EBBSConstants.AUTHORISE_DELETE|| tempWrapper.statusFlag==EBBSConstants.REJECT_MODIFY || tempWrapper.statusFlag==EBBSConstants.REJECT_DELETE) {
							SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
							//Changes for EBBSHRM-19183 By 1606639
							//scbparamEntity.setStatusFlag(EBBSConstants.ACTIVE);
							
							
							SCBParamWrapper wrapperFromMain = (SCBParamWrapper) scbparamEntity.getWrapper();
							wrapperFromMain.statusFlag = EBBSConstants.ACTIVE;
							scbparamEntity.setWrapper(wrapperFromMain);
							
						}else if (tempWrapper.statusFlag == EBBSConstants.AUTHORISE_REVERSAL
								|| tempWrapper.statusFlag == EBBSConstants.REJECT_REVERSAL) {
							SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
							//scbparamEntity.setStatusFlag(EBBSConstants.MARKED_DELETE);
							
							
							SCBParamWrapper wrapperFromMain = (SCBParamWrapper) scbparamEntity.getWrapper();
							wrapperFromMain.statusFlag = EBBSConstants.MARKED_DELETE;
							scbparamEntity.setWrapper(wrapperFromMain);
						}
						tScbParamEntity.remove();
						flag = true;

					} catch(ObjectNotFoundException ob) {
						mySessionCtx.setRollbackOnly();
						throw new SCBApplicationException("DBS","2001","Record not found");

					} catch(FinderException fe) {
						scb.util.LogWriter.getInstance().printErrorMessage(fe);// //- F16 reengineering
						mySessionCtx.setRollbackOnly();
						if(fe.getMessage()==null)
							throw new SCBApplicationException("DBS","2009","Fetch process failed");
						else
							throw new SCBApplicationException(fe.getMessage());

					} catch(RemoveException re) {
						scb.util.LogWriter.getInstance().printErrorMessage(re);// //- F16 reengineering
						mySessionCtx.setRollbackOnly();
						if(re.getMessage()==null)
							throw new SCBApplicationException("DBS","2009","Fetch process failed");
						else
							throw new SCBApplicationException(re.getMessage());
					}
				}

			} catch(SCBApplicationException ae) {
				mySessionCtx.setRollbackOnly();
				throw new SCBApplicationException(ae.getErrorCode(),ae.getErrorType(),ae.getMessage());

			} catch(Exception ex) {
				scb.util.LogWriter.getInstance().printErrorMessage(ex);// //- F16 reengineering
				mySessionCtx.setRollbackOnly();
				throw new SCBApplicationException("SYS","1000","Contact System Administrator");

			}
			return flag;
		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public boolean releaseLock(AbstractWrapper argWrapper) throws  SCBApplicationException {
		try {

			boolean flag = false;
			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;

			try {
				SCBParamLockEntity lockEntity = scbparamLockEntityHome.findByPrimaryKeyForUpdate(new SCBParamLockEntityKey(scbparamWrapper.scbparamkey,scbparamWrapper.scbparamvalue));
				lockEntity.remove();
				flag = true;

			} catch(FinderException fe) {
				scb.util.LogWriter.getInstance().printErrorMessage(fe);// //- F16 reengineering

			} catch(RemoveException re) {
				scb.util.LogWriter.getInstance().printErrorMessage(re);// //- F16 reengineering
				if(re.getMessage()==null)
					throw new SCBApplicationException("DBS","2013","Error while releasing lock");
				else
					throw new SCBApplicationException(re.getMessage());
			}
			return flag;
		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}


	public void setSessionContext(javax.ejb.SessionContext ctx)  {
		mySessionCtx = ctx;
	}


	public java.io.Serializable validate(AbstractWrapper argWrapper, boolean checkPrimaryKey) throws  SCBApplicationException {
		try {

			SCBParamWrapper scbparamWrapper = (SCBParamWrapper) argWrapper;
			ErrorObject errorObject[] = null;
			Vector v = new Vector();

			try {

				if(checkPrimaryKey) {
					try {
						SCBParamEntity scbparamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbparamWrapper.scbparamkey));
						throw new SCBApplicationException("DBS","2003","Record already exists");

					} catch(ObjectNotFoundException ob) {
						try {
							TSCBParamEntity tScbParamEntity = tScbParamEntityHome.findByPrimaryKey(new TSCBParamEntityKey(scbparamWrapper.scbparamkey));
							throw new SCBApplicationException("DBS","2003","Record already exists");

						} catch(FinderException fe) {
							scb.util.LogWriter.getInstance().printErrorMessage(fe);// //- F16 reengineering

						} catch(SCBApplicationException ae) {
							scb.util.LogWriter.getInstance().printErrorMessage(ae);// //- F16 reengineering
							throw new SCBApplicationException("DBS","2003","Record already Exists");

						}

					} catch(FinderException fe) {
						scb.util.LogWriter.getInstance().printErrorMessage(fe);// //- F16 reengineering
					}

				}



			} catch(SCBApplicationException ae) {
				scb.util.LogWriter.getInstance().printErrorMessage(ae);// //- F16 reengineering
				throw new SCBApplicationException("DBS","2003","Record already Exists");

			} catch (Exception e) {
				scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
				throw new SCBApplicationException("SYS", "1000", "Contact System Administrator.");
			}

			if(v.size()>0) {
				errorObject = new ErrorObject[v.size()];
				v.copyInto(errorObject);
			}
			Object[] obj = {scbparamWrapper, errorObject};
			return obj;
		}catch(Exception e){
			if(e instanceof scb.exception.SCBApplicationException) throw (scb.exception.SCBApplicationException) e; scb.util.LogWriter.getInstance().printErrorMessage(e);// //- F16 reengineering
			try{
				scb.util.LogWriter.getInstance().printDebugMode("Inside of SetRollBackOnly:System generated message");
				if(!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			}catch(Exception _e){}
			throw new scb.exception.SCBApplicationException("SYS","1500","ApplicationError");
		}
	}

	public AbstractWrapper fetchDeletedRecord(AbstractWrapper wrapper)
			throws SCBApplicationException {
		try {
			logger = LogWriter.getInstance("SystemParametersSessionBean","fetchdDletedRecord");
			SCBParamWrapper scbParamWrapper = null;
			try {
				scbParamWrapper = (SCBParamWrapper) wrapper;
				if (scbParamWrapper.scbparamkey == null)
					throw new FinderException(EBBSConstants.ILLEGAL_ARGUMENT_EXCEPTION);
				SCBParamEntity scbparamEntity =scbparamEntityHome.findByStatusFlag(new SCBParamEntityKey(scbParamWrapper.scbparamkey), new short[] {EBBSConstants.MARKED_DELETE,EBBSConstants.REJECT_REVERSAL });
				scbParamWrapper = (SCBParamWrapper) scbparamEntity.getWrapper();
				if (scbParamWrapper.statusFlag == EBBSConstants.REJECT_REVERSAL) {
					TSCBParamEntity tscbparamentity = tScbParamEntityHome.findByPrimaryKey(new TSCBParamEntityKey(scbParamWrapper.scbparamkey));
					scbParamWrapper = (SCBParamWrapper) tscbparamentity.getWrapper();
				}
			} catch (ObjectNotFoundException oe) {
				logger.println("Record Not found ");
				throw new SCBApplicationException("DBS", "2001","Record Not found");
			} catch (FinderException e) {
				logger.printErrorMessage(e.getMessage());
				logger.println("Fetch Process Failed");
				if (e.getMessage() == null)
					throw new SCBApplicationException(e);
			} catch (Exception ex) {
				logger.printErrorMessage(ex.getMessage());
				throw new SCBApplicationException("SYS", "1000","Contact System Administrator");
			}
			return scbParamWrapper;
		} catch (Exception e) {
			scb.util.LogWriter.getInstance().printErrorMessage(e); 
			if (e instanceof scb.exception.ApplicationException)
				throw (scb.exception.SCBApplicationException) e;
			logger.printErrorMessage(e.getMessage());
			try {
				if (!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			} catch (Exception _e) {
			}
			throw new scb.exception.SCBApplicationException("SYS", "1500","ApplicationError");
		}
	}//fetchDeletedRecord
	
	public boolean restore(AbstractWrapper argWrapper) throws SCBApplicationException {
		try {
			logger = LogWriter.getInstance("SCBParameSessionBean", "restore");
			SCBParamWrapper scbParamWrapper = (SCBParamWrapper) argWrapper;
			try {
				// releaseLock(scbParamWrapper);
				TSCBParamEntity tSCBParamEntity = tScbParamEntityHome.findByPrimaryKeyForUpdate(new TSCBParamEntityKey(scbParamWrapper.scbparamkey));
				String user = scbParamWrapper.getMakerDetails().getUserID().trim();
				//Changes for EBBSHRM-19183 By 1606639
				//String maker = tSCBParamEntity.getMakerDetails().getUserID().trim();
				
				
				SCBParamWrapper wrapperFromTemp = (SCBParamWrapper) tSCBParamEntity.getWrapper();
				String maker = wrapperFromTemp.makerDetail.getUserID().trim();
				
				logger.println("User is : " + user + " and the Maker is :"+ maker);
				//querySession.isModifyAllowed(user, maker, SystemProperties.getInstance().getCurrentApplicationID());
				querySessionProxy.executeMethodOnRemoteObject("isModifyAllowed", new Class[] {String.class, String.class, String.class},user, maker, SystemProperties.getInstance().getCurrentApplicationID());
				short statusflag=0;
				
				if(null!=tSCBParamEntity) {
					//statusflag = tSCBParamEntity.getStatusFlag();
				
					statusflag = wrapperFromTemp.statusFlag;
				}
				if (statusflag == EBBSConstants.AUTHORISE_MODIFY || statusflag == EBBSConstants.AUTHORISE_CREATE || statusflag == EBBSConstants.REJECT_CREATE || statusflag == EBBSConstants.REJECT_MODIFY) {
					throw new SCBApplicationException("BUS", "3007","Cannot delete record, It is under process");
				} else if (statusflag == EBBSConstants.REJECT_REVERSAL) {
					scbParamWrapper.statusFlag = EBBSConstants.REJECT_REVERSAL;
					tSCBParamEntity.setWrapper(scbParamWrapper);
				} else if (statusflag == EBBSConstants.AUTHORISE_REVERSAL) {
					scbParamWrapper.statusFlag = EBBSConstants.AUTHORISE_REVERSAL;
					tSCBParamEntity.setWrapper(scbParamWrapper);
				} else {
					throw new SCBApplicationException("BUS", "3007","Cannot delete record, It is under process");
				}
			} catch (ObjectNotFoundException ob) {
				try {
					SCBParamEntity scbParamEntity = scbparamEntityHome.findByPrimaryKey(new SCBParamEntityKey(scbParamWrapper.scbparamkey));
					String user = scbParamWrapper.makerDetail.getUserID().trim();
					//Changes for EBBSHRM-19183 By 1606639
					//String maker = scbParamEntity.getMakerDetails().getUserID().trim();
					
					
					SCBParamWrapper wrapperFromMain = (SCBParamWrapper) scbParamEntity.getWrapper();
					String maker = wrapperFromMain.makerDetail.getUserID().trim();
					
					
					logger.println("SystemParameters : User is : " + user+ " and the Maker is :" + maker);
					//querySession.isModifyAllowed(user, maker, SystemProperties.getInstance().getCurrentApplicationID());
					querySessionProxy.executeMethodOnRemoteObject("isModifyAllowed",new Class[] {String.class, String.class, String.class},user, maker, SystemProperties.getInstance().getCurrentApplicationID());
					//scbParamEntity.setStatusFlag(EBBSConstants.AUTHORISE_REVERSAL);
					wrapperFromMain.statusFlag = EBBSConstants.AUTHORISE_REVERSAL;
					scbParamEntity.setWrapper(wrapperFromMain);
					SCBParamWrapper wrapper1 = (SCBParamWrapper) scbParamEntity.getWrapper();
					scbParamWrapper.checkerDetail = null;
					scbParamWrapper.statusFlag = EBBSConstants.AUTHORISE_REVERSAL;
					tScbParamEntityHome.create(scbParamWrapper);
					logger.println("Record created in temp");
				} catch (ObjectNotFoundException ob2) {
					logger.printErrorMessage("SystemParameters - Object Not Found in Temp and Main Details :"+ ob2);
					mySessionCtx.setRollbackOnly();
					throw new SCBApplicationException("DBS", "2001","Record Not Found");
				} catch (FinderException fe) {
					logger.printErrorMessage(" SystemParameters - Finder Exception in Main Delete "+ fe);
					mySessionCtx.setRollbackOnly();					
					if (fe.getMessage() != null)
						throw new SCBApplicationException();					
					throw new SCBApplicationException(fe);
				} catch (CreateException ce) {
					logger.printErrorMessage("Create Exception in Delete Main due to Temp");
					mySessionCtx.setRollbackOnly();
					if (ce.getMessage() == null)
						throw new SCBApplicationException();
					throw new SCBApplicationException(ce);
				}
			} catch (FinderException fe) {
				logger.printErrorMessage(" SystemParameters - FinderException in Main Details : "+ fe);
				if (fe.getMessage() != null)
					throw new SCBApplicationException("DBS", "2006","Restore Process Failed");
				throw new SCBApplicationException(fe);
			} catch (Exception e) {
				logger.printErrorMessage("Exception in Delete Details : " + e);
				mySessionCtx.setRollbackOnly();
				logger.printErrorMessage("Exception  " + e);
				if (e instanceof SCBApplicationException)
					throw (SCBApplicationException) e;
				throw new SCBApplicationException("SYS", "1000","Contact System Administrator");
			}
			return true;
		} catch (Exception e) {
			//System.out.println("");
			if (e instanceof scb.exception.ApplicationException)
				throw (scb.exception.SCBApplicationException) e;
			logger.printErrorMessage("Exception  : " + e);
			try {
				if (!mySessionCtx.getRollbackOnly())
					mySessionCtx.setRollbackOnly();
			} catch (Exception _e) {
			}
			throw new scb.exception.SCBApplicationException("SYS", "1500","ApplicationError");
		}
	}//restore

}
