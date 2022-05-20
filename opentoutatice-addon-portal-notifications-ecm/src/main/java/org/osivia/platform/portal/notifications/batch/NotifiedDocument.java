package org.osivia.platform.portal.notifications.batch;

import java.util.Calendar;

/**
 * 
 * @author Loïc Billon
 *
 */
public class NotifiedDocument {

	public enum NotifiedAction {CREATE, MODIFY};
	
	private String webid;
	
	private String type;
	
	private String title;
	
	private String lastContributor;
	
	private Calendar lastContribution;
	
	private NotifiedAction action = NotifiedAction.MODIFY;

	public String getWebid() {
		return webid;
	}

	public void setWebid(String webid) {
		this.webid = webid;
	}

	public String getLastContributor() {
		return lastContributor;
	}

	public void setLastContributor(String lastContributor) {
		this.lastContributor = lastContributor;
	}

	public Calendar getLastContribution() {
		return lastContribution;
	}

	public void setLastContribution(Calendar lastContribution) {
		this.lastContribution = lastContribution;
	}

	public NotifiedAction getAction() {
		return action;
	}

	public void setAction(NotifiedAction action) {
		this.action = action;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	
	
}
