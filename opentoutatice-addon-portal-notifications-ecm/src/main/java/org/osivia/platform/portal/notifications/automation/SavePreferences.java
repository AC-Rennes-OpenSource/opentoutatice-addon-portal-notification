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

/**
 * Create and save user notification preferences
 * 
 * @author Loïc Billon
 *
 */
@Operation(id = SavePreferences.ID, category = Constants.CAT_NOTIFICATION, label = "Save notification preferences"
		+ "Save notification preferences for a user on a workspace")
public class SavePreferences {
	

    protected static final Log log = LogFactory.getLog(SavePreferences.class);

    public static final String ID = "Notification.SavePreferences";

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
    	DocumentModel userNotif = service.savePreferences(session, spaceId, username, freq);

    	return userNotif;
    	
    }
}
