package org.osivia.platform.portal.notifications.batch;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.service.UserPreferencesService;

/**
 * Cron for planification and notifications production
 * 
 * @author Loïc Billon
 *
 */
public class NotificationsCronJob  extends UnrestrictedSessionRunner {


	private static final Log log = LogFactory.getLog("fr.toutatice.notifications");

	private static final String PREFS_SS_PROCHAINE_DATE = "SELECT * FROM PreferencesNotification WHERE "+UserPreferencesService.TTCPN_NEXTDATE+" IS NULL AND "+UserPreferencesService.TTCPN_FREQ+" != 'NOTHING' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0";
	
	private static final String PREFS_A_TRAITER = "SELECT * FROM PreferencesNotification WHERE "+UserPreferencesService.TTCPN_NEXTDATE+" < TIMESTAMP '%s' AND "+UserPreferencesService.TTCPN_FREQ+" != 'NOTHING' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0";

	
	private ElasticSearchService es;

	private SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

	private WorkManager workManager;
	
	protected NotificationsCronJob(String repositoryName) {
		super(repositoryName);

		es = Framework.getService(ElasticSearchService.class);
		
		workManager = Framework.getLocalService(WorkManager.class);
	}

	@Override
	public void run() throws ClientException {
		
		// Déterminer la date et l'heure du prochain envoi
		planification();
		
		// Traiter les notifications à envoyer
		alimentation();
	}

	private void planification() {
		
		log.info("===== Planification =====");
		
		// Query sur tous les documents préférences qui n'ont pas de date de prochain envoi
		NxQueryBuilder queryBuilder = new NxQueryBuilder(session);
		queryBuilder.nxql(PREFS_SS_PROCHAINE_DATE);
		DocumentModelList preferences = es.query(queryBuilder);
		
		log.info("Planification de "+preferences.size()+" préférences");
		
		// Examen de la fréquence souhaitée
		for(DocumentModel preference : preferences) {

			Serializable freq = preference.getPropertyValue(UserPreferencesService.TTCPN_FREQ);
			if(freq != null) {
				NotificationFrequency frequency = NotificationFrequency.valueOf(freq.toString());

				NotificationsHelper.setNextPlanification(preference, frequency);

				session.saveDocument(preference);
			}

		}
		
		
	}
	

	private void alimentation() {
		
		log.info("===== Alimentation des files =====");
		
		// Query sur tous les documents préférences qui ont une date de prochain envoi dépassé
		NxQueryBuilder queryBuilder = new NxQueryBuilder(session);
		
		
		String formattedDate = sdf.format(new Date());
		queryBuilder.nxql(String.format(PREFS_A_TRAITER, formattedDate));
		DocumentModelList preferences = es.query(queryBuilder);
		
		log.info(preferences.size()+" à trairer");

		
		// Dispatch sur la file
		for(DocumentModel preference : preferences) {
			NotificationWork work = new NotificationWork(preference);
			State workState = workManager.getWorkState(work.getId());
			if((workState == null || workState != State.SCHEDULED)) {
				workManager.schedule(work);
				
	    		if(log.isDebugEnabled()) {
	    			
	    			String login = preference.getPropertyValue("dc:creator").toString();
	    			String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();
	    			
	    			log.debug("Traitement de "+login+" sur "+espace+" en file "+work.getCategory());
	    		}
			}
			else if (workState == State.SCHEDULED) {

	    		if(log.isDebugEnabled()) {
	    			String login = preference.getPropertyValue("dc:creator").toString();
	    			String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();
	    			
	    			log.debug("Le traitement de "+login+" sur "+espace+" est déjà en file "+work.getCategory());
	    		}
    		
			}
		}
		
	}
}
