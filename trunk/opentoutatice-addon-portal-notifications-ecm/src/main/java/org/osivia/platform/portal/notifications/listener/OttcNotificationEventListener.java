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

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationEventListener;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.runtime.api.Framework;


/**
 * @author David Chevrier.
 *
 */
public class OttcNotificationEventListener extends NotificationEventListener {

    /**
     * Send mail to subscriptor only if rules given by
     * - blockSubscriptorIfNotAllowed
     * - blockUserWhenSubscriptor
     * methods are checked.
     */
    @Override
    protected void sendNotificationSignalForUser(Notification notification, String subscriptor, Event event, DocumentEventContext ctx) throws ClientException {
        /* We don't apply rules for (automatic) Email notification. */
        // FIXME: think about autosubscribed notifications?
        if (DocumentEventTypes.EMAIL_DOCUMENT_SEND.equals(event.getName())) {
            super.sendNotificationSignalForUser(notification, subscriptor, event, ctx);
        } else if (checkRulesForSubscriptor(notification, subscriptor, event, ctx)) {
            super.sendNotificationSignalForUser(notification, subscriptor, event, ctx);
        }
    }

    /**
     * Check rules given by
     * - blockSubscriptorIfNotAllowed
     * - blockUserWhenSubscriptor
     * methods.
     */
    protected boolean checkRulesForSubscriptor(Notification notification, String subscriptor, Event event, DocumentEventContext ctx) {
        /*
         * We get subscriptor session to check permission
         * (NotificationEventListener use system session cause it's asynchronous).
         */
        LoginContext loginContext = null;
        CoreSession subscriptorSession = null;
        try {
            loginContext = Framework.loginAsUser(subscriptor);
            subscriptorSession = CoreInstance.openCoreSession(null);
            return !blockUserWhenSubscriptor(ctx, subscriptor);
        } catch (LoginException lie) {
            throw new ClientException(lie);
        } finally {
            if (subscriptorSession != null) {
                CoreInstance.closeCoreSession(subscriptorSession);
            }
            if (loginContext != null) {
                try {
                    loginContext.logout();
                } catch (LoginException loe) {
                    throw new ClientException(loe);
                }
            }
        }
    }

    /**
     * Block notifications on document when user (who has done action on document)
     * has subscribed on document.
     * 
     * NotificationEventListener#gatherConcernedUsersForDocument method can not be override,
     * so we work at his level.
     */
    protected boolean blockUserWhenSubscriptor(DocumentEventContext ctx, String subscriptor) {
        return StringUtils.equals(ctx.getPrincipal().getName(), subscriptor);
    }

}
