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
 *   lbillon
 *    
 */
package org.osivia.platform.portal.notifications.automation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.service.DocumentNotificationInfosProvider;

/**
 * Remove all subscriptions to notifications on the document
 * 
 * @author lbillon
 * 
 */
@Operation(id = AllNotificationsUnsubscribe.ID, category = Constants.CAT_NOTIFICATION, label = "unsubscribe to the document", description = "Remove all subscriptions to notifications on the document")
public class AllNotificationsUnsubscribe {

	public static final String ID = "Notification.AllNotificationsUnsubscribe";

	@Context
	protected CoreSession session;

	@OperationMethod
	public void run(DocumentModel document) throws ClientException, ClassNotFoundException {

		DocumentNotificationInfosProvider service = Framework.getService(DocumentNotificationInfosProvider.class);

		service.unsubscribe(session, document);
	}

}
