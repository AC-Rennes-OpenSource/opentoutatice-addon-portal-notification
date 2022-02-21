package org.osivia.platform.portal.notifications.automation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

@Operation(id = CreatePrefDoc.ID, category = Constants.CAT_NOTIFICATION, label = "Create a notification preference"
		+ "Create all documents about a user and a workspace for notifications")
		
public class CreatePrefDoc {
	

    protected static final Log log = LogFactory.getLog(CreatePrefDoc.class);

    public static final String ID = "Notification.CreatePrefDoc";

    @Context
    protected CoreSession session;

    @Param(name = "username", required = true)
    protected String username;
    
    @Param(name = "spaceId", required = true)
    protected String spaceId;    

    @Param(name = "freq", required = true)
    protected String freq;    
	
    @Param(name = "preferencesPath", required = true)
    protected String preferencesPath;	
    
    @OperationMethod
    public DocumentModel run() throws Exception {

    	
    	// === Test if folder pref exists
    	DocumentModelList userPrefs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesUtilisateur' AND ecm:name = '"+
    			username+"' AND ecm:currentLifeCycleState != 'deleted'");
    	
    	DocumentModel userPref = null;
    	if(userPrefs.size() == 0) {
    		userPref = session.createDocumentModel(preferencesPath, username, "PreferencesUtilisateur");
    		userPref.setPropertyValue("dc:title", username);

    		session.createDocument(userPref);
    		
    		log.warn("Création des preferences utilisateur pour "+username);
    		
    		// TODO acl
//    		ACP acp = userPref.getACP();
//    		
//			session.setACP(userPref.getRef(), acp , true);
    	}
    	else if(userPrefs.size() == 1) {
    		
    		log.warn("Preferences utilisateur trouvées pour "+username);
    		
    		userPref = userPrefs.get(0);
    	}
    	
    	
    	
    	// Test if doc for current space exists
    	DocumentModelList userNotifs = session.query("SELECT * FROM Document WHERE ecm:primaryType = 'PreferencesNotification' AND ttcpn:user_id = '"+
    			username+"' AND ttcpn:space_id = '"+spaceId+"' AND ecm:currentLifeCycleState != 'deleted'");
    	
    	DocumentModel userNotif = null;
    	if(userNotifs.size() == 0) {
    		userNotif = session.createDocumentModel(userPref.getPathAsString(), spaceId, "PreferencesNotification");
    		userNotif.setPropertyValue("ttcpn:user_id", username);
    		userNotif.setPropertyValue("ttcpn:space_id", spaceId);
    		userNotif.setPropertyValue("ttcpn:frequency", freq);
    		userNotif.setPropertyValue("dc:title", spaceId);
    		session.createDocument(userNotif);
    		
    		log.warn("Création des preferences de notification d'espace "+spaceId+" pour "+username);

    	}
    	else if(userNotifs.size() == 1) {
    		userNotif = userNotifs.get(0);
    		
    		log.warn("Oreferences de notification d'espace trouvées pour "+spaceId+" / "+username);

    	}
    	
		return userNotif;
    	
    }
}
