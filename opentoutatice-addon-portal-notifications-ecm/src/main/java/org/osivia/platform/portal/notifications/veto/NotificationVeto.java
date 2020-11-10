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

import java.util.Arrays;

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

    /** Authorized systems events. */
    public final static String[] authorizedSystemEvents = {TaskEventNames.WORKFLOW_CANCELED, TaskEventNames.WORKFLOW_ABANDONED,
            TaskEventNames.WORKFLOW_TASK_ASSIGNED, TaskEventNames.WORKFLOW_TASK_COMPLETED, TaskEventNames.WORKFLOW_TASK_REJECTED};

    @Override
    public boolean accept(Event event) throws Exception {
        if (event.getContext() instanceof DocumentEventContext) {
            String eventName = event.getName();
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();

            NuxeoPrincipal originatingPrincipal = (NuxeoPrincipal) docCtx.getPrincipal();
            return !blockSystemEvents(eventName, originatingPrincipal);
        }
        return false;
    }

    /**
     * Block system events.
     */
    protected boolean blockSystemEvents(String eventName, NuxeoPrincipal principal) {
    	
    	String defaultAdministratorId = Framework.getProperty("nuxeo.ldap.defaultAdministratorId");
		if(defaultAdministratorId == null) {
			defaultAdministratorId = "Administrator";
		}
		
    	boolean block = false;
        if (StringUtils.equalsIgnoreCase(LoginComponent.SYSTEM_USERNAME, principal.getName()) || StringUtils.equalsIgnoreCase(defaultAdministratorId, principal.getName())) {
           block = !Arrays.asList(authorizedSystemEvents).contains(eventName);
        }
        return block;
    }


}
