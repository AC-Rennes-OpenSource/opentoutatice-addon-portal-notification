package org.osivia.platform.portal.notifications.batch;

/**
 * 
 * @author Loïc Billon
 *
 */
public enum NotificationFrequency {
	
	DAILY,
	
	WEEKLY,
	
	HOURLY,
	
	NOTHING;
	
	public String getQueue() {
		return "NOTIFICATION_"+this.name();
	}
}
