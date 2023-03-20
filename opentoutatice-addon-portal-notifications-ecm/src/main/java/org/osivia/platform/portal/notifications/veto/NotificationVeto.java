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
 * 
 * Contributors:
 * mberhaut1
 * dchevrier
 * lbillon
 */
package org.osivia.platform.portal.notifications.veto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * @author David Chevrier.
 *
 */
public class NotificationVeto implements NotificationListenerVeto {

    /** Logger. */
    private final static Log log = LogFactory.getLog(NotificationVeto.class);
    
    private List<String> systemUsers;

    /** Authorized systems events. */
    public final static String[] authorizedSystemEvents = {TaskEventNames.WORKFLOW_CANCELED, TaskEventNames.WORKFLOW_ABANDONED,
            TaskEventNames.WORKFLOW_TASK_ASSIGNED, TaskEventNames.WORKFLOW_TASK_COMPLETED, TaskEventNames.WORKFLOW_TASK_REJECTED};


	private String workspacepath;

	/**
	 * Switch in the new scheduled mode with the perferences service.
	 */
	private Boolean scheduledNotifications;

	public NotificationVeto() {
		workspacepath = Framework.getProperty("ottc.collab.workspacepath");

		// Switch in the new scheduled mode with the perferences service.
		String scheduled = Framework.getProperty("ottc.notifications.scheduled");
		scheduledNotifications = BooleanUtils.toBoolean(scheduled);
	}

    @Override
    public boolean accept(Event event) throws Exception {
        if (event.getContext() instanceof DocumentEventContext) {
            String eventName = event.getName();
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();

			// Bloquer les notifications pour lesquelles le nouveau système périodique est mis en place.
			if(scheduledNotifications && docCtx.getSourceDocument().getPathAsString().startsWith(workspacepath)) {
				return false;
			}

            NuxeoPrincipal originatingPrincipal = (NuxeoPrincipal) docCtx.getPrincipal();
            return !blockSystemEvents(eventName, originatingPrincipal);
        }
        return false;
    }

    /**
     * Block system events.
     */
    protected boolean blockSystemEvents(String eventName, NuxeoPrincipal principal) {
    	
    	List<String> systemUsers = getSystemUsers();
    	
    	boolean block = false;
    	
    	// block all system user events
        if (systemUsers.contains(principal.getName())) {
        	
        	// except for workflow tasks
        	
           block = !Arrays.asList(authorizedSystemEvents).contains(eventName);
        }
        
        return block;
    }

	private List<String> getSystemUsers() {
		if(systemUsers == null) {
			
			systemUsers = new ArrayList<String>();
			
			// system
			systemUsers.add(LoginComponent.SYSTEM_USERNAME);
			
			// Administrator
	    	String defaultAdministratorId = Framework.getProperty("nuxeo.ldap.defaultAdministratorId");
			if(defaultAdministratorId == null) {
				defaultAdministratorId = "Administrator";
			}
			systemUsers.add(defaultAdministratorId);
			
			// other accounts
			String vetoForUsers = Framework.getProperty("notification.veto.users");
			if(StringUtils.isNotBlank(vetoForUsers)) {
				String[] split = vetoForUsers.split(",");
				
				for(int i = 0; i < split.length; i++) {
					String vetoUser = StringUtils.trim(split[i]);
					
					if(StringUtils.isNotBlank(vetoUser)) {
						systemUsers.add(vetoUser);
					}
				}
			}
			
		}
		
		
		return systemUsers;
	}


}
