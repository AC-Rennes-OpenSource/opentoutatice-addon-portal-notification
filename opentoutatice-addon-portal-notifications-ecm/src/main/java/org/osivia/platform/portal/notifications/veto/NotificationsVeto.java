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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.runtime.api.login.LoginComponent;


/**
 * @author David Chevrier.
 *
 */
public class NotificationsVeto implements NotificationListenerVeto {

    /** Authorized systems events. */
    public final static List<String> authorizedSystemEvents = new ArrayList<String>(5) {

        {
            add("workflowProcessCanceled");
            add("workflowAbandoned");

            add("workflowTaskAssigned");
            add("workflowTaskCompleted");
            add("workflowTaskRejected");

        }
    };

    /**
     * Block system events.
     */
    @Override
    public boolean accept(Event event) throws Exception {
        String eventName = event.getName();

        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        NuxeoPrincipal currentUser = (NuxeoPrincipal) docCtx.getPrincipal();

        boolean isSystemInitiator = StringUtils.equalsIgnoreCase(LoginComponent.SYSTEM_USERNAME, currentUser.getName());
        
        if(isSystemInitiator){
            return authorizedSystemEvents.contains(eventName);
        }

        return !isSystemInitiator;

    }

}
