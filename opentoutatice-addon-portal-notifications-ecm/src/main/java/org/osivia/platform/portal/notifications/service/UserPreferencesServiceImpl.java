package org.osivia.platform.portal.notifications.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPSession;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.batch.NotificationBean;
import org.osivia.platform.portal.notifications.batch.NotificationFrequency;
import org.osivia.platform.portal.notifications.batch.NotificationsHelper;
import org.osivia.platform.portal.notifications.batch.NotifiedDocument;
import org.osivia.platform.portal.notifications.service.DocumentNotificationInfosProviderImpl.SubscriptionStatus;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;

public class UserPreferencesServiceImpl implements UserPreferencesService {
	
    protected static final Log log = LogFactory.getLog("fr.toutatice.notifications");
    
	private LDAPSession ldapSession;

	private String notifRepository;


	public UserPreferencesServiceImpl() {
		
    	notifRepository = Framework.getProperty("ottc.notifications.repository", "notificationRepo");

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
		if(ldapSession != null) {
			try {
				entry = ldapSession.getEntryFromSource(login, false);
			}
			catch(ClientException e) {
				ldapSession = null;
			}
		}
		
		if(ldapSession == null) {
			
			DirectoryService service = Framework.getService(DirectoryService.class);
			LDAPDirectory directory = (LDAPDirectory) service.getDirectory("userLdapDirectory");
			
			ldapSession = (LDAPSession) directory.getSession();
			
			entry = ldapSession.getEntryFromSource(login,false);

		}
		
		return entry;
	}
	


	protected SubscriptionStatus getStatusInRepo(CoreSession notifSession, DocumentModel currentDocument) {
		
		
        SubscriptionStatus status = SubscriptionStatus.no_subscriptions;
		
    	// Test if doc for current space exists
    	DocumentModelList userNotifs = notifSession.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
    	
    	searchpath:
    	for(DocumentModel userNotif : userNotifs) {
    		String[] paths = (String[]) userNotif.getPropertyValue(UserPreferencesService.TTCPN_PATHS);
    		
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
    	
    	return status;
		
	}

	
	protected DocumentModel getPreferences(CoreSession notifSession, String workspaceId, String username) {
		
    	DocumentModelList userNotifs = notifSession.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND "+UserPreferencesService.TTCPN_USERID+" = '"+
    			username+"' AND "+UserPreferencesService.TTCPN_SPACEID+" = '"+workspaceId+"' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
    	
    	if(userNotifs.size() == 0) {
    		String currentUsername = notifSession.getPrincipal().getName();
    		
    		UnrestritctedPreferencesCreator runner = new UnrestritctedPreferencesCreator(notifSession, username, currentUsername, workspaceId);
    		runner.runUnrestricted();
    		
    		return runner.getPref();
    		
    	}
    	else if(userNotifs.size() == 1) {
    		return userNotifs.get(0);
    	}
    	else throw new ClientException();
	}


	protected DocumentModel getNotificationFolder(CoreSession notifSession) {
    	// === Test if folder notifs exists
    	DocumentModelList userNotifs = notifSession.query("SELECT * FROM Document WHERE ecm:primaryType = 'NotificationsUtilisateur' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
    	
    	return userNotifs.get(0);
		
	}
	
	// --------------------------------------

	
	@Override
	public DocumentModel savePreferences(CoreSession defaultSession, String spaceId, String username, String freq) {

		
		CoreSession notifSession = SessionFactory.getSession(notifRepository);
		
		DocumentModel preferences = getPreferences(notifSession, spaceId, username);
		
		if(StringUtils.isNotBlank(freq)) {
			
			NotificationFrequency frequency = NotificationFrequency.valueOf(freq);
			NotificationsHelper.setNextPlanification(preferences, frequency);
	   		
			preferences.setPropertyValue(UserPreferencesService.TTCPN_FREQ, freq);

			preferences = notifSession.saveDocument(preferences);
		}

		return preferences;

	}
	
	@Override
	public void savePlanification(DocumentModel preference) {

		CoreSession notifSession = null;
		
		notifSession = CoreInstance.openCoreSession(notifRepository);

		NotificationFrequency frequency = NotificationFrequency.valueOf(
				preference.getPropertyValue(UserPreferencesService.TTCPN_FREQ).toString());
		
		NotificationsHelper.setNextPlanification(preference, frequency);
   		
		notifSession.saveDocument(preference);

	}
	
	public void subscribe(CoreSession session, DocumentModel currentDocument) {
		
		CoreSession notifSession = SessionFactory.getSession(notifRepository);

		if(getStatusInRepo(notifSession, currentDocument) == SubscriptionStatus.can_subscribe) {
			

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
			
			DocumentModel prefDoc = getPreferences(notifSession, workspaceId, personUid);
			String[] paths = (String[]) prefDoc.getPropertyValue(UserPreferencesService.TTCPN_PATHS);
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


			prefDoc.setPropertyValue(UserPreferencesService.TTCPN_PATHS, newPaths);
			
			notifSession.saveDocument(prefDoc);
			
			
		}

		
	}	
	


	@Override
	public void unsubscribe(CoreSession session, DocumentModel currentDocument) {
		
		CoreSession notifSession = SessionFactory.getSession(notifRepository);

			
			if(getStatusInRepo(notifSession, currentDocument) == SubscriptionStatus.can_unsubscribe) {
				
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

        	
			
			DocumentModel prefDoc = getPreferences(notifSession, workspaceId, personUid);
			String[] paths = (String[]) prefDoc.getPropertyValue(UserPreferencesService.TTCPN_PATHS);
			
			List<String> pathsList = new ArrayList<String>(Arrays.asList(paths));
			pathsList.remove(currentDocument.getPathAsString());
			
			if(pathsList.size() > 0) {
				String[] newPaths = new String[pathsList.size()];
				pathsList.toArray(newPaths);
				
				prefDoc.setPropertyValue(UserPreferencesService.TTCPN_PATHS, newPaths);
				
				notifSession.saveDocument(prefDoc);
				
			}
			else {
				prefDoc.setPropertyValue(UserPreferencesService.TTCPN_PATHS, null);
				notifSession.saveDocument(prefDoc);
			}
			
			
		}
		
	}
	
	
	public SubscriptionStatus getStatus(CoreSession session, DocumentModel currentDocument) {
		
		CoreSession notifSession = SessionFactory.getSession(notifRepository);
			
		SubscriptionStatus status = getStatusInRepo(notifSession, currentDocument);

		return status;

		
	}

	@Override
	public void createNotification(NotificationBean notif) {
	
		CoreSession notifSession = null;

		notifSession = CoreInstance.openCoreSession(notifRepository);
		
		DocumentModel parent = getNotificationFolder(notifSession);
		long longid = new Date().getTime();
		
		DocumentModel notifDoc = notifSession.createDocumentModel(parent.getPathAsString(), Long.toString(longid), "Notification");
		
		notifDoc.setPropertyValue("ntf:freq", notif.getFreq().name());
		Calendar from = notif.getFrom();
		notifDoc.setPropertyValue("ntf:derniereNotif", from.getTime());
		notifDoc.setPropertyValue("ntf:espace", notif.getSpaceWebid());
		
	    List<Map<String, Object>> complexValuesList = new ArrayList<Map<String, Object>>();
	    for(NotifiedDocument ndoc : notif.getDocs()) {
    	    Map<String, Object> complexValue = new HashMap<String, Object>();
    	    complexValue.put("action", ndoc.getAction().name());
    	    
    	    Calendar lastContribution = ndoc.getLastContribution();
    	    complexValue.put("derniereContribution", lastContribution.getTime());
    	    complexValue.put("dernierContributeur", ndoc.getLastContributor());
    	    complexValue.put("webid", ndoc.getWebid());
    	    complexValuesList.add(complexValue);
		}
		notifDoc.setPropertyValue("ntf:docs", (Serializable) complexValuesList);

		notifSession.createDocument(notifDoc);

	}

}
