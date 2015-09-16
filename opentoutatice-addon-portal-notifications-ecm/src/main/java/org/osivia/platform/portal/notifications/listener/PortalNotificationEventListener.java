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
package org.osivia.platform.portal.notifications.listener;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationEventListener;
import org.nuxeo.ecm.platform.notification.api.Notification;


/**
 * @author David Chevrier.
 *
 */
public class PortalNotificationEventListener extends NotificationEventListener {

    /**
     * To do not notify him self.
     */
    @Override
    protected void sendNotificationSignalForUser(Notification notification, String subscriptor, 
            Event event, DocumentEventContext ctx) throws ClientException {
        
        String currentUser = ctx.getPrincipal().getName();
        if(!StringUtils.equals(currentUser, subscriptor)){
            super.sendNotificationSignalForUser(notification, subscriptor, event, ctx);
        }

    }

}
