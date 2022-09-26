package org.osivia.platform.portal.notifications.batch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.service.UserPreferencesService;

/**
 * 
 * @author Loïc Billon
 *
 */
public class NotificationsHelper {
	
	public final static String PROP_HOURLY_DELAY_IN_MIN = Framework.getProperty("org.opentoutatice.notifications.hourly.delayInMin", "60");
	
	public final static String PROP_DAILY_HOUR = Framework.getProperty("org.opentoutatice.notifications.daily.hour", "17");
	
	public final static String PROP_WEEKLY_HOUR = Framework.getProperty("org.opentoutatice.notifications.weekly.hour", "10");
	public final static String PROP_WEEKLY_DAY = Framework.getProperty("org.opentoutatice.notifications.weekly.day", "7");
	

	private static final Log log = LogFactory.getLog("fr.toutatice.notifications");
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	
	public static void setNextPlanification(DocumentModel preference, NotificationFrequency frequency) {


		Calendar c = Calendar.getInstance(); 
		

		if(frequency == NotificationFrequency.NOTHING) {
    		if(log.isDebugEnabled()) {
    			
    			String login = preference.getPropertyValue("dc:creator").toString();
    			String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();
    			
    			log.debug(login+" ne demande pas de notifications sur "+espace);
    			
    		}
    		return;
		}
		if(frequency == NotificationFrequency.HOURLY) {
			
			c.add(Calendar.MINUTE, Integer.parseInt(PROP_HOURLY_DELAY_IN_MIN));
		}
		else if(frequency == NotificationFrequency.DAILY) {
			// Fréquence journalière => J+1,17h
    		c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(PROP_DAILY_HOUR));
    		c.add(Calendar.DAY_OF_YEAR, 1);
    		
		}
		else if(frequency == NotificationFrequency.WEEKLY) {
			// Fréquence hedbo => prochain samedi, 10h
    		c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(PROP_WEEKLY_HOUR));
    		c.set(Calendar.DAY_OF_WEEK, Integer.parseInt(PROP_WEEKLY_DAY));
    		
		}
		preference.setPropertyValue(UserPreferencesService.TTCPN_LASTDATE, new Date());
		preference.setPropertyValue(UserPreferencesService.TTCPN_NEXTDATE, c.getTime());
		
		if(log.isDebugEnabled()) {
			
			String login = preference.getPropertyValue("dc:creator").toString();
			String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();
			
			log.debug(login+" demande d'être notifié sur "+espace+" le "+sdf.format(c.getTime()));
		}
	}
}
