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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.service.DocumentNotificationInfosProvider;


/**
 * Get all the documents followed by the current user
 * 
 * @author lbillon
 * 
 */
@Operation(id = GetUserSubscriptions.ID, category = Constants.CAT_NOTIFICATION, label = "Get documents subscriptions", description = "Get all the documents followed by the current user")
public class GetUserSubscriptions {

	public static final String ID = "Notification.GetUserSubscriptions";

	@Context
	protected CoreSession session;

	@OperationMethod
    public DocumentModelList run() throws ClientException {

		DocumentNotificationInfosProvider service = Framework.getService(DocumentNotificationInfosProvider.class);
		return service.getUserSubscriptions(session);
		
	}

}
