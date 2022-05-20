package org.osivia.platform.portal.notifications.batch;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.batch.NotifiedDocument.NotifiedAction;
import org.osivia.platform.portal.notifications.service.UserPreferencesService;

/**
 * Batch unit of a notification
 * 
 * @author Loïc Billon
 *
 */
public class NotificationWork extends AbstractWork {

	private static final Log log = LogFactory.getLog("fr.toutatice.notifications");
	
	private static final String DOCS_MODIFIED = "SELECT * FROM Document WHERE dc:modified > TIMESTAMP '%s' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0 AND dc:lastContributor != '%s'";

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3011962625744956574L;

	private DocumentModel preference;


	public NotificationWork(DocumentModel preference) {
		super(preference.getId());
		this.preference = preference;
		
	}
	
	@Override
	public String getTitle() {
		return preference.getId();
	}

	@Override
	public void work() throws Exception {
		
		UserPreferencesService ups = Framework.getService(UserPreferencesService.class);
		
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		
		String login = preference.getPropertyValue("dc:creator").toString();
				
        Framework.loginAsUser(login);
        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal(login);
        CoreSession userSession = CoreInstance.openCoreSession(null, principal);

        try {
            // Requête pour voir les changements
    		NxQueryBuilder queryBuilder = new NxQueryBuilder(userSession);
    		
    		
    		Serializable propertyValue = preference.getPropertyValue(UserPreferencesService.TTCPN_LASTDATE);
    		if(propertyValue instanceof Calendar) {
    			Calendar c = (Calendar) propertyValue;
    			
    			String formattedDate = sdf.format(c.getTime());
    			
    			queryBuilder.nxql(String.format(DOCS_MODIFIED, formattedDate, login));
    			ElasticSearchService es = Framework.getService(ElasticSearchService.class);
    			DocumentModelList documents = es.query(queryBuilder);
    			
    			if(documents.isEmpty()) {
    				if(log.isDebugEnabled()) {
    					
    					String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();
    					
    					log.debug("Rien à envoyer pour "+login+" sur "+espace);
    				}
    			}
    			else {
    			
    				if(log.isDebugEnabled()) {
    					
    					String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();
    					
    					log.debug("Envoi notification à "+login+" sur "+espace+", "+documents.size()+" nouveautés.");
    					
    					NotificationBean notif = new NotificationBean();
    					notif.setSpaceWebid(espace);
    					notif.setFrom(c);
    					notif.setFreq(NotificationFrequency.valueOf(
    							preference.getPropertyValue(UserPreferencesService.TTCPN_FREQ).toString()));
    					
    					List<NotifiedDocument> ndocs = new ArrayList<NotifiedDocument>();
    					for(DocumentModel doc : documents) {
    						NotifiedDocument ndoc = new NotifiedDocument();
    						
    						ndoc.setWebid(doc.getPropertyValue("ttc:webid").toString());
    						ndoc.setLastContribution((Calendar) doc.getPropertyValue("dc:modified"));
    						ndoc.setLastContributor(doc.getPropertyValue("dc:lastContributor").toString());
    						
    						if(doc.getPropertyValue("dc:modified").equals(doc.getPropertyValue("dc:created"))) {
    							ndoc.setAction(NotifiedAction.CREATE);
    						}
    						ndocs.add(ndoc);
    					}
    					notif.setDocs(ndocs);
    					
    					ups.createNotification(notif);
    				}
    			}
    		}
        }
        finally {
        	CoreInstance.closeCoreSession(userSession);
        }
		
		ups.savePlanification(preference);
	}
	
	@Override
	public String getCategory() {
		NotificationFrequency frequency = NotificationFrequency.valueOf(
				preference.getPropertyValue(UserPreferencesService.TTCPN_FREQ).toString());
		
		return frequency.getQueue();
		
	}

}
