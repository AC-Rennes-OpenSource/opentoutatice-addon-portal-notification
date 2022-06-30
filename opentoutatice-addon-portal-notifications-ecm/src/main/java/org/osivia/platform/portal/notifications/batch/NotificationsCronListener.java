package org.osivia.platform.portal.notifications.batch;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 * @author Loïc Billon
 *
 */
public class NotificationsCronListener implements EventListener {

	@Override
	public void handleEvent(Event event) throws ClientException {

		String repo = Framework.getProperty("opentoutatice.notifications.repository");
		
		NotificationsCronJob job = new NotificationsCronJob(repo);
		job.runUnrestricted();
		
	}


}
