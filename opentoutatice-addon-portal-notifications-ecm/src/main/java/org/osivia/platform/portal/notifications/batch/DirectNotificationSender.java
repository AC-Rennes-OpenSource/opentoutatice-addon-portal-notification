package org.osivia.platform.portal.notifications.batch;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationEventListener;

/**
 * Classe d'envoi de mail direct (sans listener et en erreur en cas de problème de distribution)
 * Inclue un logo si configuré et fichier existant.
 * 
 * @see NotificationEventListener
 * @author Loïc Billon
 *
 */
public interface DirectNotificationSender {

	public final static String PROP_LOGO_PATH = "org.opentoutatice.notifications.logopath";

	public final static String PROP_TEMPLATE = "org.opentoutatice.notifications.template";

	void sendNotification(Event event, DocumentEventContext ctx) throws ClientException;

}
