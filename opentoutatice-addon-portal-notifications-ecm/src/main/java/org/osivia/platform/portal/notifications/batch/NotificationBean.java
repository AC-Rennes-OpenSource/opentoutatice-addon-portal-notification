package org.osivia.platform.portal.notifications.batch;

import java.util.Calendar;
import java.util.List;

/**
 * DTO object for notification
 * 
 * @author Loïc Billon
 *
 */
public class NotificationBean {
	
	private String spaceWebid;
	
	private String spaceTitle;

	private NotificationFrequency freq;
	
	private Calendar from;
	
	public List<NotifiedDocument> docs;

	
	public String getSpaceTitle() {
		return spaceTitle;
	}

	public void setSpaceTitle(String spaceTitle) {
		this.spaceTitle = spaceTitle;
	}

	public String getSpaceWebid() {
		return spaceWebid;
	}

	public void setSpaceWebid(String spaceWebid) {
		this.spaceWebid = spaceWebid;
	}

	public NotificationFrequency getFreq() {
		return freq;
	}

	public void setFreq(NotificationFrequency freq) {
		this.freq = freq;
	}

	public Calendar getFrom() {
		return from;
	}

	public void setFrom(Calendar from) {
		this.from = from;
	}

	public List<NotifiedDocument> getDocs() {
		return docs;
	}

	public void setDocs(List<NotifiedDocument> docs) {
		this.docs = docs;
	}
	
	
	
}
