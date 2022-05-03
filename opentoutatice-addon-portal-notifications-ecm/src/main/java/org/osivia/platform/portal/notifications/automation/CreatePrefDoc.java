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
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.service.UserPreferencesService;

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

    @Param(name = "freq", required = false)
    protected String freq;    

    @OperationMethod
    public DocumentModel run() throws Exception {

    	UserPreferencesService service = Framework.getService(UserPreferencesService.class);
    	DocumentModel userNotif = service.getOrCreatePrefDoc(session, spaceId, username);
    	
    	if(freq != null) {
    		userNotif.setPropertyValue("ttcpn:frequency", freq);
    		session.saveDocument(userNotif);
    	}
    	
    	return userNotif;
    	
    }
}
