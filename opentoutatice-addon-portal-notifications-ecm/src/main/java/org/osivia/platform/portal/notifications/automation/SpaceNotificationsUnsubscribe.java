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
 * Delete a user notification preferences
 * 
 * @author Loïc Billon
 *
 */
@Operation(id = SpaceNotificationsUnsubscribe.ID, category = Constants.CAT_NOTIFICATION, label = "Delete notification preferences"
		+ "Drop notification preferences for a user on a workspace")
public class SpaceNotificationsUnsubscribe {
	

    protected static final Log log = LogFactory.getLog(SpaceNotificationsUnsubscribe.class);

    public static final String ID = "Notification.SpaceNotificationsUnsubscribe";

    @Context
    protected CoreSession session;


    @OperationMethod
    public void run(DocumentModel space) throws Exception {

    	UserPreferencesService service = Framework.getService(UserPreferencesService.class);

    	service.spaceNotificationsUnsubscribe(session, space);

    }
}
