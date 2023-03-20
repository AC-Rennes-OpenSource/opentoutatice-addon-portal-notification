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
import org.nuxeo.ecm.core.api.*;
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

	private String notifRepository;


	public UserPreferencesServiceImpl() {

    	notifRepository = Framework.getProperty("opentoutatice.notifications.repository");

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

			NuxeoPrincipal principal = (NuxeoPrincipal) notifSession.getPrincipal();
			String personUid = principal.getModel().getPropertyValue("pseudonymizedId").toString();

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

			NuxeoPrincipal principal = (NuxeoPrincipal) notifSession.getPrincipal();
			String personUid = principal.getModel().getPropertyValue("pseudonymizedId").toString();
			
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
	public DocumentModel createNotification(NotificationBean notif) {
	
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

		return notifSession.createDocument(notifDoc);

	}

	@Override
	public List<String> getUserSubscriptionPaths(CoreSession coreSession) {

		CoreSession notifSession = CoreInstance.openCoreSession(notifRepository);

		NuxeoPrincipal principal = (NuxeoPrincipal) notifSession.getPrincipal();
		Serializable personUid = principal.getModel().getPropertyValue("pseudonymizedId");

		List<String> pathsArray = new ArrayList<>();
		if(personUid != null) {
			DocumentModelList userNotifs = notifSession.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND "+UserPreferencesService.TTCPN_USERID+" = '"+
					personUid+"' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");

			for(DocumentModel userNotif : userNotifs) {
				String[] paths = (String[]) userNotif.getPropertyValue(UserPreferencesService.TTCPN_PATHS);
				pathsArray.addAll(Arrays.asList(paths));

			}
		}
		return pathsArray;
	}

	@Override
	public void spaceNotificationsUnsubscribe(CoreSession session, DocumentModel workspace) {

		CoreSession notifSession = CoreInstance.openCoreSession(notifRepository);

		NuxeoPrincipal principal = (NuxeoPrincipal) notifSession.getPrincipal();
		Serializable pseudonymizedId = principal.getModel().getPropertyValue("pseudonymizedId");
		String workspaceId = workspace.getProperty("webc:url").getValue(String.class);

		DocumentModel preferences = getPreferences(notifSession, workspaceId, pseudonymizedId.toString());
		notifSession.removeDocument(new PathRef(preferences.getPathAsString()));

	}

}
