package org.osivia.platform.portal.notifications.batch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.osivia.platform.portal.notifications.service.UserPreferencesService;

/**
 * 
 * @author Loïc Billon
 *
 */
public class NotificationsHelper {

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
			// Fréquence horaire => H+1
//    		c.add(Calendar.HOUR_OF_DAY, 1);
			
			c.add(Calendar.MINUTE, 3);
		}
		else if(frequency == NotificationFrequency.DAILY) {
			// Fréquence journalière => J+1,17h
    		c.set(Calendar.HOUR_OF_DAY, 17);
    		c.add(Calendar.DAY_OF_YEAR, 1);
    		
		}
		else if(frequency == NotificationFrequency.WEEKLY) {
			// Fréquence hedbo => prochain samedi, 10h
    		c.set(Calendar.HOUR_OF_DAY, 10);
    		c.set(Calendar.DAY_OF_WEEK, 7);
    		
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
