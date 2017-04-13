/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * Contributors:
 * mberhaut1
 */
package org.osivia.platform.portal.notifications.automation;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;

@Operation(id = HandleNotification.ID, category = Constants.CAT_NOTIFICATION, label = "Handle subsciptions to notifications", description = "Subscribe/unsubscribe a principal to notification(s)."
		+ "WARNING! The parameter 'userid' can either be prefixed 'user:' or 'group:' to explicitely define the directory it belongs to. If not prefixed, a directory seacrch will be performed"
		+ "to deternmine the user type 'user' or 'group'")
public class HandleNotification {

    protected static final Log log = LogFactory.getLog(HandleNotification.class);

    public static final String ID = "Notification.HandleNotification";
    public static final String NOTIFICATION_ACTION_ADD = "ADD";
    public static final String NOTIFICATION_ACTION_REMOVE = "REMOVE";
    public static final String NOTIFICATION_PREFIXE_USER = "user:";
    public static final String NOTIFICATION_PREFIXE_GROUP = "group:";

    @Context
    protected OperationContext ctx;

    @Context
    protected UserManager userManager;

    @Context
    protected NotificationManager notificationManager;

    @Param(name = "userid", required = true, description = "Can either be prefixed 'user:' or 'group:' to explicitely define the directory it belongs to")
    protected String userid;

    @Param(name = "notifications", required = true)
    protected StringList notifications;

    @Param(name = "sendEmail", required = false, values = { "false", "true" })
    protected boolean sendEmail = false;

    @Param(name = "action", required = true, values = {NOTIFICATION_ACTION_ADD, NOTIFICATION_ACTION_REMOVE})
    protected String action = NOTIFICATION_ACTION_ADD;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel document) throws Exception {
    	String prefixedUserId = userid;
    	
    	// determine whether the user is of type user (person) or group
    	if (!userid.matches("^(" + NOTIFICATION_PREFIXE_USER + "|" + NOTIFICATION_PREFIXE_GROUP + ").+")) {
    		Boolean isUser = (null != userManager.getPrincipal(userid)); // use Nuxeo principal cache mechanism
    		Boolean isGroup = (null != userManager.getGroup(userid));
    		
    		if (isUser && !isGroup) {
    			prefixedUserId = NOTIFICATION_PREFIXE_USER + userid;
    		} else if (!isUser && isGroup) {
    			prefixedUserId = NOTIFICATION_PREFIXE_GROUP + userid;
    		} else {
    			// Error: cannot identify the user origin
    			if (!isUser) {
        			log.error("The userid parameter '" + userid + "' could not be found within either the user or group LDAP directories");
    			} else {
        			log.error("The userid parameter '" + userid + "' is ambiguous since was found both within the user and the group LDAP directories");
    			}
    			
    			return document;
    		}
    	}
    	
    	// Add / remove subscription
		if (NOTIFICATION_ACTION_ADD.equals(action)) {
			NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
			for (String notification : notifications) {
				// TR #7138 : Avoid appending multiple subscription on the same event to the same user/group (control not implemented within the "addSubscription" API itself)
				List<String> subscribers = notificationManager.getSubscribers(notification, document.getId());
				if (!subscribers.contains(prefixedUserId)) {
					notificationManager.addSubscription(prefixedUserId, notification, document, sendEmail, principal, notification);
				}
			}
			log.info("Subscibed user '" + userid + "' to the notification '" + notifications + "' on document '" + document.getPathAsString() + "' (UUID=" + document.getId() + ")");
		} else {
			for (String notification : notifications) {
				notificationManager.removeSubscription(prefixedUserId, notification, document.getId());
			}
			log.info("Unsubscibed user '" + userid + "' to the notification '" + notifications + "' on document '" + document.getPathAsString() + "' (UUID=" + document.getId() + ")");
		}
    	
        return document;
    }

}
