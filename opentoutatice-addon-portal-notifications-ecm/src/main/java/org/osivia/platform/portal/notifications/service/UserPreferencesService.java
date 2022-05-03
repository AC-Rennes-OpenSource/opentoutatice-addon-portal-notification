package org.osivia.platform.portal.notifications.service;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.osivia.platform.portal.notifications.service.DocumentNotificationInfosProviderImpl.SubscriptionStatus;

public interface UserPreferencesService {

	DocumentModel getOrCreatePrefDoc(CoreSession session, String workspaceId, String username);

	void subscribe(CoreSession session, DocumentModel currentDocument);

	SubscriptionStatus getStatus(CoreSession session, DocumentModel currentDocument);

	void unsubscribe(CoreSession coreSession, DocumentModel currentDocument);

}
