package org.osivia.platform.portal.notifications.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPSession;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.service.DocumentNotificationInfosProviderImpl.SubscriptionStatus;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;

public class UserPreferencesServiceImpl implements UserPreferencesService {
	
    protected static final Log log = LogFactory.getLog(UserPreferencesServiceImpl.class);
    
	private final String preferencesPath;

	private LDAPSession session;

	private String repository;


	public UserPreferencesServiceImpl() {
		
		preferencesPath = Framework.getProperty("ottc.notifs.preferencesPath", "/preferences//conteneur-preferences");
    	repository = Framework.getProperty("ottc.notifications.repository", "notificationRepo");

	}
	
	/**
	 * This method get a fresh user from ldap, recycling a dedicated connexion to ldap.
	 * If ldap client is unbinded, try to create a new one.
	 * 
	 * @param login str
	 * @return document ldap entry
	 */
	private DocumentModel getLdapEntry(String login) {
		DocumentModel entry = null;
		if(session != null) {
			try {
				entry = session.getEntryFromSource(login, false);
			}
			catch(ClientException e) {
				session = null;
			}
		}
		
		if(session == null) {
			
			DirectoryService service = Framework.getService(DirectoryService.class);
			LDAPDirectory directory = (LDAPDirectory) service.getDirectory("userLdapDirectory");
			
			session = (LDAPSession) directory.getSession();
			
			entry = session.getEntryFromSource(login,false);

		}
		
		return entry;
	}
	
	public void subscribe(CoreSession session, DocumentModel currentDocument) {
		
		if(getStatus(session, currentDocument) == SubscriptionStatus.can_subscribe) {
			

			// get workspaceId of the current document
			String workspaceId = null;
			DocumentModel workspace = ToutaticeDocumentHelper.getWorkspace(session, currentDocument, true);
			if(workspace != null) {
				workspaceId = workspace.getProperty("webc:url").getValue(String.class);
				
			}
			else {
				throw new ClientException("User can not subscribe to this document");
			}
			
			DocumentModel ldapEntry = getLdapEntry(session.getPrincipal().getName());
			String personUid = ldapEntry.getProperty("pseudonymizedId").getValue(String.class);
			
			if(StringUtils.isBlank(personUid)) {
				throw new ClientException("User can not subscribe to this document");

			}

        	CoreSession notificationRepoSession = CoreInstance.openCoreSession(repository);
			
			DocumentModel prefDoc = getOrCreatePrefDoc(notificationRepoSession, workspaceId, personUid);
			String[] paths = (String[]) prefDoc.getPropertyValue("ttcpn:paths");
			String[] newPaths = null;
			if(paths != null) {
				List<String> pathsList = new ArrayList<String>(Arrays.asList(paths));
				pathsList.add(currentDocument.getPathAsString());
				newPaths = new String[pathsList.size()];
				pathsList.toArray(newPaths);
			}
			else {
				newPaths = new String[1];
				newPaths[0] = currentDocument.getPathAsString();
			}


			prefDoc.setPropertyValue("ttcpn:paths", newPaths);
			
			notificationRepoSession.saveDocument(prefDoc);
			
			CoreInstance.closeCoreSession(notificationRepoSession);
			
		}
	}
	
	public SubscriptionStatus getStatus(CoreSession session, DocumentModel currentDocument) {
		
		boolean sessionOpened = false;
		if(session.getRepositoryName().equals("default")) {
			session = CoreInstance.openCoreSession(repository);
			sessionOpened = true;
			
		}
		
		
        SubscriptionStatus status = SubscriptionStatus.no_subscriptions;
		
    	// Test if doc for current space exists
    	DocumentModelList userNotifs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND ecm:currentLifeCycleState != 'deleted'");
    	
    	searchpath:
    	for(DocumentModel userNotif : userNotifs) {
    		String[] paths = (String[]) userNotif.getPropertyValue("ttcpn:paths");
    		
    		for(String path : paths) {
    			if(currentDocument.getPathAsString().equals(path)) {
    				status = SubscriptionStatus.can_unsubscribe;
    				break searchpath;
    			}
    			else if (currentDocument.getPathAsString().startsWith(path)) {
    				status = SubscriptionStatus.has_inherited_subscriptions;
    			}
    		}
    		
    	}
    	
    	if(status == SubscriptionStatus.no_subscriptions) {
    		status = SubscriptionStatus.can_subscribe;
    	}
    	
    	if(sessionOpened) {
    		CoreInstance.closeCoreSession(session);
    	}
    	
    	return status;
		
	}

	public DocumentModel getOrCreatePrefDoc(CoreSession session, String workspaceId, String username) {

    	DocumentModelList userNotifs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND ttcpn:user_id = '"+
    			username+"' AND ttcpn:space_id = '"+workspaceId+"' AND ecm:currentLifeCycleState != 'deleted'");
    	
    	if(userNotifs.size() == 0) {
    		String currentUsername = session.getPrincipal().getName();
    		
    		SetUnrestrictedPrefs runner = new SetUnrestrictedPrefs(session, preferencesPath, username, currentUsername, workspaceId);
    		runner.runUnrestricted();
    		
    		return runner.getUserNotif();
    		
    	}
    	else if(userNotifs.size() == 1) {
    		return userNotifs.get(0);
    	}
    	else throw new ClientException();
    	
    	
	}

	@Override
	public void unsubscribe(CoreSession session, DocumentModel currentDocument) {
		
		if(getStatus(session, currentDocument) == SubscriptionStatus.can_unsubscribe) {
			

			// get workspaceId of the current document
			String workspaceId = null;
			DocumentModel workspace = ToutaticeDocumentHelper.getWorkspace(session, currentDocument, true);
			if(workspace != null) {
				workspaceId = workspace.getProperty("webc:url").getValue(String.class);
				
			}
			else {
				throw new ClientException("User can not subscribe to this document");
			}
			
			DocumentModel ldapEntry = getLdapEntry(session.getPrincipal().getName());
			String personUid = ldapEntry.getProperty("pseudonymizedId").getValue(String.class);
			
			if(StringUtils.isBlank(personUid)) {
				throw new ClientException("User can not subscribe to this document");

			}

        	CoreSession notificationRepoSession = CoreInstance.openCoreSession(repository);
			
			DocumentModel prefDoc = getOrCreatePrefDoc(notificationRepoSession, workspaceId, personUid);
			String[] paths = (String[]) prefDoc.getPropertyValue("ttcpn:paths");
			
			List<String> pathsList = new ArrayList<String>(Arrays.asList(paths));
			pathsList.remove(currentDocument.getPathAsString());
			
			if(pathsList.size() > 0) {
				String[] newPaths = new String[pathsList.size()];
				pathsList.toArray(newPaths);
				
				prefDoc.setPropertyValue("ttcpn:paths", newPaths);
				
				notificationRepoSession.saveDocument(prefDoc);
				
			}
			else {
				prefDoc.setPropertyValue("ttcpn:paths", null);
				notificationRepoSession.saveDocument(prefDoc);
			}
			
			CoreInstance.closeCoreSession(notificationRepoSession);
			
		}
	}
	
	private static class SetUnrestrictedPrefs extends UnrestrictedSessionRunner {

		private final String preferencesPath;
		private final String username;
		private final String currentUsername;
		private final String workspaceId;
		
		private DocumentModel userNotif;
		

		protected SetUnrestrictedPrefs(CoreSession session, String preferencesPath, String username, String currentUsername,
				String workspaceId) {
			super(session);
			this.preferencesPath = preferencesPath;
			
			this.username = username;
			this.currentUsername = currentUsername;
			this.workspaceId = workspaceId;
			
		}

		@Override
		public void run() throws ClientException {
			
	    	
	    	// === Test if folder pref exists
	    	DocumentModelList userPrefs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesUtilisateur' AND ecm:name = '"+
	    			username+"' AND ecm:currentLifeCycleState != 'deleted'");
	    	
	    	DocumentModel userPref = null;
	    	if(userPrefs.size() == 0) {
	    		userPref = session.createDocumentModel(preferencesPath, username, "PreferencesUtilisateur");
	    		userPref.setPropertyValue("dc:title", username);

	    		userPref = session.createDocument(userPref);
	    		
	    		log.warn("Création des preferences utilisateur pour "+username);

	    		
	    		ACP acp = session.getACP(userPref.getRef());
	    		ACL acl = new ACLImpl();
	    		acl.add(new ACE(currentUsername, "ReadWrite", true));
				acp.addACL(acl);
				session.setACP(userPref.getRef(), acp, true);
				
	    	}
	    	else if(userPrefs.size() == 1) {
	    		
	    		log.warn("Preferences utilisateur trouvées pour "+username);
	    		
	    		userPref = userPrefs.get(0);
	    	}
	    	
	    	
	    	
	    	// Test if doc for current space exists
	    	DocumentModelList userNotifs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND ttcpn:user_id = '"+
	    			username+"' AND ttcpn:space_id = '"+workspaceId+"' AND ecm:currentLifeCycleState != 'deleted'");
	    	
	    	if(userNotifs.size() == 0) {
	    		userNotif = session.createDocumentModel(userPref.getPathAsString(), workspaceId, "PreferencesNotification");
	    		userNotif.setPropertyValue("ttcpn:user_id", username);
	    		userNotif.setPropertyValue("ttcpn:space_id", workspaceId);
	    		userNotif.setPropertyValue("dc:title", workspaceId);
	    		userNotif = session.createDocument(userNotif);
	    		
	    		log.warn("Création des preferences de notification d'espace "+workspaceId+" pour "+username);

	    	}
	    	else if(userNotifs.size() == 1) {
	    		userNotif = userNotifs.get(0);
	    		
	    		log.warn("Oreferences de notification d'espace trouvées pour "+workspaceId+" / "+username);

	    	}
	    	
		}

		public DocumentModel getUserNotif() {
			return userNotif;
		}
		
	}
}
