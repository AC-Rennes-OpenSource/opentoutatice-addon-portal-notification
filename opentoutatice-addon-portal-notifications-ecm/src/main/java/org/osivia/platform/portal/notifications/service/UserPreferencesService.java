package org.osivia.platform.portal.notifications.service;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.osivia.platform.portal.notifications.batch.NotificationBean;
import org.osivia.platform.portal.notifications.service.DocumentNotificationInfosProviderImpl.SubscriptionStatus;

/**
 * 
 * @author Loïc Billon
 *
 */
public interface UserPreferencesService {

	
	public static final String TTCPN_NEXTDATE = "ttcpn:prochaineNotif";
	public static final String TTCPN_LASTDATE = "ttcpn:derniereNotif";
	public static final String TTCPN_FREQ = "ttcpn:freq";
	public static final String TTCPN_SPACEID = "ttcpn:espace";
	public static final String TTCPN_USERID = "ttcpn:utilisateur";
	public static final String TTCPN_PATHS = "ttcpn:paths";;
	
	/**
	 * Subscribe to notifications on a document
	 * @param session
	 * @param currentDocument
	 */
	void subscribe(CoreSession session, DocumentModel currentDocument);

	/**
	 * Get current subscription on a document
	 * @param session
	 * @param currentDocument
	 * @return
	 */
	SubscriptionStatus getStatus(CoreSession session, DocumentModel currentDocument);

	/**
	 * Unsubscribe to notifications on a document
	 * @param coreSession
	 * @param currentDocument
	 */
	void unsubscribe(CoreSession coreSession, DocumentModel currentDocument);

	/**
	 * Save space notification preferences
	 * @param session
	 * @param spaceId
	 * @param username
	 * @param freq
	 * @return
	 */
	DocumentModel savePreferences(CoreSession session, String spaceId, String username, String freq);

	/**
	 * Save planificiation
	 * @param preference
	 */
	void savePlanification(DocumentModel preference);

	/**
	 * Produce a notificaiton document
	 * @param notif
	 */
	void createNotification(NotificationBean notif);

}
