package org.osivia.platform.portal.notifications.batch;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

/**
 * 
 * @author Loïc Billon
 *
 */
public class NotificationsCronListener implements EventListener {

	@Override
	public void handleEvent(Event event) throws ClientException {

		NotificationsCronJob job = new NotificationsCronJob("notificationRepo");
		job.runUnrestricted();
		
	}


}
