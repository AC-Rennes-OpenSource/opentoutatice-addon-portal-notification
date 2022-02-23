/*
 * (C) Copyright 2015 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
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
package org.osivia.platform.portal.notifications.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;

/**
 * Service for managing notifications
 * 
 * @author lbillon
 * 
 */
public interface DocumentNotificationInfosProvider extends DocumentInformationsProvider {

	/**
	 * Subscribe to all notifications on the document
	 * 
	 * @param coreSession
	 * @param currentDocument
	 */
	void subscribe(CoreSession coreSession, DocumentModel currentDocument);

	/**
	 * Remove all subscriptions to notifications on the document
	 * 
	 * @param coreSession
	 * @param currentDocument
	 * @throws ClientException
	 * @throws ClassNotFoundException
	 */
	void unsubscribe(CoreSession coreSession, DocumentModel currentDocument) throws ClientException, ClassNotFoundException;

	/**
	 * Return a list of documents followed by the current user
	 * 
	 * @param coreSession
	 * @return list of followed documents
	 */
    DocumentModelList getUserSubscriptions(CoreSession coreSession);

}
