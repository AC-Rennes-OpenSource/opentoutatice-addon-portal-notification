package org.osivia.platform.portal.notifications.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.batch.NotificationFrequency;

/**
 * 
 * @author Loïc Billon
 *
 */
public class UnrestritctedPreferencesCreator extends UnrestrictedSessionRunner {

    protected static final Log log = LogFactory.getLog("fr.toutatice.notifications");
    
	private final String username;
	private final String currentUsername;
	private final String workspaceId;
	
	private DocumentModel pref;

	
	private final String domainPath = Framework.getProperty("opentoutatice.notifications.path", "/preferences/");

	protected UnrestritctedPreferencesCreator(CoreSession session, String username, String currentUsername,
			String workspaceId) {
		super(session);
		
		this.username = username;
		this.currentUsername = currentUsername;
		this.workspaceId = workspaceId;
		
	}

	@Override
	public void run() throws ClientException {
		
    	// === Test if folder notifs exists
    	DocumentModelList userNotifs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'NotificationsUtilisateur' AND ecm:name = '"+
    			username+"' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
    	
    	DocumentModel userNotif = null;
    	if(userNotifs.size() == 0) {
    		
    		DocumentModel rootFolderNotif = getRootFolderNotif();
    		
    		userNotif = session.createDocumentModel(rootFolderNotif.getPathAsString(), username, "NotificationsUtilisateur");
    		userNotif.setPropertyValue("dc:title", username);

    		userNotif = session.createDocument(userNotif);
    		
    		log.warn("Création du dossier de notifications utilisateur pour "+username);

    		
    		ACP acp = session.getACP(userNotif.getRef());
    		ACL acl = new ACLImpl();
    		acl.add(new ACE(currentUsername, "ReadWrite", true));
			acp.addACL(acl);
			session.setACP(userNotif.getRef(), acp, true);
			
    	}
    	
    	DocumentModel userPref = getUserFolderPref();
    	
    	// Test if doc for current space exists
    	DocumentModelList prefs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND "+UserPreferencesService.TTCPN_USERID+"= '"+
    			username+"' AND "+UserPreferencesService.TTCPN_SPACEID+" = '"+workspaceId+"' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
    	
    	
    	if(prefs.size() == 0) {
    		pref = session.createDocumentModel(userPref.getPathAsString(), workspaceId, "PreferencesNotification");
    		pref.setPropertyValue(UserPreferencesService.TTCPN_USERID, username);
    		pref.setPropertyValue(UserPreferencesService.TTCPN_SPACEID, workspaceId);
			pref.setPropertyValue(UserPreferencesService.TTCPN_FREQ, NotificationFrequency.DAILY.toString());
    		pref.setPropertyValue("dc:title", workspaceId);
    		pref = session.createDocument(pref);
    		
    		log.warn("Création des preferences de notification d'espace "+workspaceId+" pour "+username);

    	}
    	else if(prefs.size() == 1) {
    		pref = prefs.get(0);
    		
    		log.warn("Preferences de notification d'espace trouvées pour "+workspaceId+" / "+username);

    	}
    	
	}
	private DocumentModel getRootFolderNotif() {
		DocumentModel rootFolderNotif = null;
		// Test root folder
		DocumentModelList rootFoldersNotif = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'ConteneurNotifications'  AND ecm:path STARTSWITH '"+domainPath+"' "
				+ "AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
		if(rootFoldersNotif.size() == 0) {
			rootFolderNotif = session.createDocumentModel(domainPath, "conteneur-notifications", "ConteneurNotifications");
			rootFolderNotif.setPropertyValue("dc:title", "Conteneur de notifications");

			rootFolderNotif = session.createDocument(rootFolderNotif);
		}
		else {
			rootFolderNotif = rootFoldersNotif.get(0);
		}
		return rootFolderNotif;
	}
	
	private DocumentModel getRootFolderPref() {
		DocumentModel rootFolderPref = null;
		// Test root folder
		DocumentModelList rootFolders = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'ConteneurPreferences'  AND ecm:path STARTSWITH '"+domainPath+"' "
				+ "AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
		if(rootFolders.size() == 0) {
			rootFolderPref = session.createDocumentModel(domainPath, "conteneur-preferences", "ConteneurPreferences");
			rootFolderPref.setPropertyValue("dc:title", "Conteneur de préférences");

			rootFolderPref = session.createDocument(rootFolderPref);
		}
		else {
			rootFolderPref = rootFolders.get(0);
		}
		return rootFolderPref;
	}
	
	private DocumentModel getUserFolderPref() {
		

    	// === Test if folder pref exists
    	DocumentModelList userPrefs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesUtilisateur' AND ecm:name = '"+
    			username+"' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0");
    	
    	DocumentModel userPref = null;
    	
    	if(userPrefs.size() == 0) {
    		
    		DocumentModel rootFolderPref = getRootFolderPref();
    		
    		
    		userPref = session.createDocumentModel(rootFolderPref.getPathAsString(), username, "PreferencesUtilisateur");
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
    	
    	return userPref;
    	
	}

	public DocumentModel getPref() {
		return pref;
	}
	
	
	
}

