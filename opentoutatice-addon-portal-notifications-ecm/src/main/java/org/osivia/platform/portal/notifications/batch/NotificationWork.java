package org.osivia.platform.portal.notifications.batch;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPSession;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
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
	
	private static final String DOCS_MODIFIED = "SELECT * FROM Document WHERE dc:modified > TIMESTAMP '%s' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0 AND dc:lastContributor != '%s' AND ecm:mixinType != 'Folderish' AND ";
	
	private static final String FETCH_WORKSPACE = "SELECT * FROM Document WHERE webc:url = '%s' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0";

	private static final String FETCH_DOC = "SELECT * FROM Document WHERE ttc:webid = '%s' AND ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0";

	/**
	 * 
	 */
	private static final long serialVersionUID = 3011962625744956574L;

	private DocumentModel preference;
	
	private Map<String, String> frTypes;


	public NotificationWork(DocumentModel preference) {
		super(preference.getId());
		this.preference = preference;
		
		frTypes = new HashMap<>();
		frTypes.put("Folder", "le dossier");
		frTypes.put("File", "le fichier");
		frTypes.put("Picture", "l'image");
		frTypes.put("Audio", "le document audio");
		frTypes.put("Note", "la note");
		frTypes.put("Thread", "le sujet de discussion");
	
		
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

//        try {
		// Requête pour voir les changements
		NxQueryBuilder queryBuilder = new NxQueryBuilder(userSession);

		Serializable propertyValue = preference.getPropertyValue(UserPreferencesService.TTCPN_LASTDATE);
		if (propertyValue instanceof Calendar) {
			Calendar c = (Calendar) propertyValue;

			String formattedDate = sdf.format(c.getTime());

			StringBuilder sbNxql = new StringBuilder(String.format(DOCS_MODIFIED, formattedDate, login));
			sbNxql.append("(");
			String[] paths = (String[]) preference.getPropertyValue(UserPreferencesService.TTCPN_PATHS);
			for(int i = 0; i < paths.length; i++) {
				
				if(i > 0) {
					
					sbNxql.append(" OR ");

				}
				
				sbNxql.append("ecm:path STARTSWITH '");
				sbNxql.append(paths[i]);
				sbNxql.append("'");
				
			}

			sbNxql.append(")");

			DocumentModelList documents = null;
					
			if(paths.length > 0) {
				queryBuilder.nxql(sbNxql.toString());
				ElasticSearchService es = Framework.getService(ElasticSearchService.class);
				
				if (log.isDebugEnabled()) {
					log.debug(sbNxql.toString());
				}
				
				documents = es.query(queryBuilder);

			}
			
			if (documents == null) {
				if (log.isDebugEnabled()) {

					String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();

					log.debug("Aucun document n'est suivi pour " + login + " sur " + espace);
				}
			}
			else if (documents.isEmpty()) {
				if (log.isDebugEnabled()) {

					String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();

					log.debug("Rien à envoyer pour " + login + " sur " + espace);
				}
			} else {
				String espace = preference.getPropertyValue(UserPreferencesService.TTCPN_SPACEID).toString();

				log.debug("Envoi notification à " + login + " sur " + espace + ", " + documents.size()
						+ " nouveautés.");

				NotificationBean notif = new NotificationBean();
				notif.setSpaceWebid(espace);
				notif.setFrom(c);
				notif.setFreq(NotificationFrequency
						.valueOf(preference.getPropertyValue(UserPreferencesService.TTCPN_FREQ).toString()));

				List<NotifiedDocument> ndocs = new ArrayList<NotifiedDocument>();
				for (DocumentModel doc : documents) {
					NotifiedDocument ndoc = new NotifiedDocument();

					ndoc.setWebid(doc.getPropertyValue("ttc:webid").toString());
					ndoc.setLastContribution((Calendar) doc.getPropertyValue("dc:modified"));
					ndoc.setLastContributor(doc.getPropertyValue("dc:lastContributor").toString());

					if (doc.getPropertyValue("dc:modified").equals(doc.getPropertyValue("dc:created"))) {
						ndoc.setAction(NotifiedAction.CREATE);
					}
					ndocs.add(ndoc);
				}

				notif.setDocs(ndocs);

				DocumentModel notificationDocument = ups.createNotification(notif);
					
				Map<String, Serializable> prepareEmail = prepareEmail(notif, userSession);
  		        
				String repo = Framework.getProperty("opentoutatice.notifications.repository");
		        CoreSession userNotifSession = CoreInstance.openCoreSession(repo, principal);
				DocumentEventContext ctx = new DocumentEventContext(userNotifSession, principal, notificationDocument);
									
				ctx.setProperties(prepareEmail);
				Event event = ctx.newEvent("periodicEmailSend");

				try {

					DirectNotificationSender sender = Framework.getService(DirectNotificationSender.class);
					sender.sendNotification(event, ctx);

				} catch (Exception e) {
					log.error("Can not get EventProducer : email won't be sent", e);
					return;
				}
			}
		}

		ups.savePlanification(preference);
	}
	
	
	
	private Map<String, Serializable> prepareEmail(NotificationBean notif, CoreSession userSession) {
		
		NxQueryBuilder queryBuilder = new NxQueryBuilder(userSession);
		queryBuilder.nxql(String.format(FETCH_WORKSPACE, notif.getSpaceWebid()));
		ElasticSearchService es = Framework.getService(ElasticSearchService.class);
		DocumentModelList documents = es.query(queryBuilder);
		DocumentModel workspace = documents.get(0);
		
		
		// TODO Auto-generated method stub
		Map<String, Serializable> options = new HashMap<String, Serializable>();

		String mailSubject = "Activité sur votre espace "+workspace.getTitle();
		
        // options for confirmation email
		
		NotificationManager service2 = Framework.getService(NotificationManager.class);
		
		String template = Framework.getProperty(DirectNotificationSender.PROP_TEMPLATE);
		
		Notification notification = service2.getNotificationByName(template);
		
		if(notification == null) {
			throw new NuxeoException("Template non trouvé");
		}
		
		options.put(NotificationConstants.NOTIFICATION_KEY, notification);
        options.put(NotificationConstants.DESTINATION_KEY, new String(userSession.getPrincipal().getName()));
        options.put(NotificationConstants.RECIPIENTS_KEY, new String[] { userSession.getPrincipal().getName() });
        options.put("mailSubject", mailSubject);
        
        String lastDateStr;
        
        if(notif.getFreq().equals(NotificationFrequency.DAILY)) {
        	lastDateStr = "depuis hier.";
        }
        else if(notif.getFreq().equals(NotificationFrequency.WEEKLY)) {
        	lastDateStr = "depuis la semaine dernière.";
        }
        else {
        	lastDateStr = "récemment.";
        }
        
        options.put("lastDateStr", lastDateStr);
        
        DirectoryService service = Framework.getService(DirectoryService.class);
		LDAPDirectory directory = (LDAPDirectory) service.getDirectory("userLdapDirectory");
		
		LDAPSession ldapSession = (LDAPSession) directory.getSession();
		
		
        
        StringBuilder sb = new StringBuilder();

        for(NotifiedDocument notified : notif.getDocs()) {
        	
        	queryBuilder.nxql(String.format(FETCH_DOC, notified.getWebid()));
    		
    		DocumentModelList documents2 = es.query(queryBuilder);
    		DocumentModel currentDoc = documents2.get(0);
        	
        	sb.append("<p>");
        	
        	// ==== auteur
        	DocumentModel entry = ldapSession.getEntry(notified.getLastContributor(),false);
        	Serializable firstName = entry.getPropertyValue("firstName");
			Serializable lastName = entry.getPropertyValue("lastName");
			
			// Compute display Name
			String displayName = null;
			if(firstName != null && lastName != null) {
				displayName = firstName.toString() + " " + lastName.toString();
			}
			else if(lastName != null) {
				displayName = lastName.toString();
			}
			
			sb.append(displayName);
			
        	// ==== action
			
			if(notified.getAction().equals(NotifiedAction.CREATE)) {
				sb.append(" a créé ");	
			}
			else sb.append(" a modifié ");
			
			// ==== tyoe
			String frType = frTypes.get(currentDoc.getType());
			if(frType != null) {
				sb.append(frType);
				sb.append(" ");
			}
			
			// ==== Titre
			sb.append(currentDoc.getTitle());
			sb.append(" ");
			
			// ==== Date de modificaiton
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM - HH:mm");
			if(notif.getFreq().equals(NotificationFrequency.WEEKLY)) {
				sdf = new SimpleDateFormat("dd/MM - HH:mm");
			}
			else {
				sdf = new SimpleDateFormat("HH:mm");
			}
			sb.append("à ");
			sb.append(sdf.format(notified.getLastContribution().getTime()));
			sb.append(" ");
			
			// ===== Permalien
			sb.append("<a href=\"");
			sb.append(Framework.getProperty("portal.permalink"));
			sb.append("/");
			sb.append(Framework.getProperty("nuxeo.permalink.service.param1"));
			sb.append("/share/");
			sb.append(notified.getWebid());
			sb.append("\"/>Consulter");
			sb.append("</a>");
			
        	sb.append("</p>");
        }
        
        options.put("notifiedDocs", sb.toString());

        
        options.put("category", DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY);
        
        return options;
	}

		
	
	
	@Override
	public String getCategory() {
		NotificationFrequency frequency = NotificationFrequency.valueOf(
				preference.getPropertyValue(UserPreferencesService.TTCPN_FREQ).toString());
		
		return frequency.getQueue();
		
	}

}
