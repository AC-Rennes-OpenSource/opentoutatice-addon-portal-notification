package org.osivia.platform.portal.notifications.batch;

import java.io.File;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.security.auth.login.LoginContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.NotificationImpl;
import org.nuxeo.ecm.platform.ec.notification.email.NotificationsRenderingEngine;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.RenderingService;
import org.nuxeo.ecm.platform.rendering.impl.DocumentRenderingContext;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.Configuration;
import freemarker.template.Template;


public class DirectNotificationSenderImpl implements DirectNotificationSender  {

    private static final Log log = LogFactory.getLog("fr.toutatice.notifications");
    
    // used for loading templates from strings
    private final Configuration stringCfg = new Configuration();
    
	
    private NotificationService notificationService = NotificationServiceHelper.getNotificationService();	
    
    
    public void sendNotification(Event event, DocumentEventContext ctx)
            throws ClientException {

        String eventId = event.getName();
        log.debug("Received a message for notification sender with eventId : "
                + eventId);

        Map<String, Serializable> eventInfo = ctx.getProperties();
        String userDest = (String) eventInfo.get(NotificationConstants.DESTINATION_KEY);
        NotificationImpl notif = (NotificationImpl) eventInfo.get(NotificationConstants.NOTIFICATION_KEY);

        // send email
        NuxeoPrincipal recepient = NotificationServiceHelper.getUsersService().getPrincipal(
                userDest);
        if (recepient == null) {
            log.error("Couldn't find user: " + userDest
                    + " to send her a mail.");
            return;
        }
        // XXX hack, principals have only one model
        DataModel model = recepient.getModel().getDataModels().values().iterator().next();
        String email = (String) model.getData("email");
        if (email == null || "".equals(email)) {
            log.error("No email found for user: " + userDest);
            return;
        }

        String subjectTemplate = notif.getSubjectTemplate();

        
        log.debug("email: " + email);
        log.debug("mail template: " + notif.getTemplate());
        log.debug("subject template: " + subjectTemplate);

        Map<String, Object> mail = new HashMap<String, Object>();
        mail.put("mail.to", email);

        String authorUsername = (String) eventInfo.get(NotificationConstants.AUTHOR_KEY);

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(
                    authorUsername);
            mail.put(NotificationConstants.PRINCIPAL_AUTHOR_KEY, author);
        }

        mail.put(NotificationConstants.DOCUMENT_KEY, ctx.getSourceDocument());
        String subject = notif.getSubject() == null ? NotificationConstants.NOTIFICATION_KEY
                : notif.getSubject();
        subject = notificationService.getEMailSubjectPrefix()
                + subject;
        mail.put("subject", subject);
        mail.put("template", notif.getTemplate());
        mail.put("subjectTemplate", subjectTemplate);

        // Transferring all data from event to email
        for (String key : eventInfo.keySet()) {
            mail.put(key, eventInfo.get(key) == null ? "" : eventInfo.get(key));
            log.debug("Mail prop: " + key);
        }

        mail.put(NotificationConstants.EVENT_ID_KEY, eventId);

        try {
            sendmail(mail);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder(
                        "Failed to send email with these properties:\n");
                for (String key : mail.keySet()) {
                    sb.append("\t " + key + ": " + mail.get(key) + "\n");
                }
                log.debug(sb.toString());
            }
            throw new ClientException("Failed to send notification email ", e);
        }
    }



    /**
     * Static Method: sendmail(Map mail).
     *
     * @param mail A map of the settings
     */
    protected void sendmail(Map<String, Object> mail) throws Exception {


    	Properties prop = Framework.getProperties();
    	Session session = Session.getInstance(prop);
    	
        // Construct a MimeMessage
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(session.getProperty("mail.from")));
        Object to = mail.get("mail.to");
        if (!(to instanceof String)) {
            log.error("Invalid email recipient: " + to);
            return;
        }
        msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse((String) to, false));

        RenderingService rs = Framework.getService(RenderingService.class);

        DocumentRenderingContext context = new DocumentRenderingContext();
        context.remove("doc");
        context.putAll(mail);
        context.setDocument((DocumentModel) mail.get("document"));
        context.put("Runtime", Framework.getRuntime());

        String customSubjectTemplate = (String) mail.get(NotificationConstants.SUBJECT_TEMPLATE_KEY);
        if (customSubjectTemplate == null) {
            String subjTemplate = (String) mail.get(NotificationConstants.SUBJECT_KEY);
            Template templ = new Template("name",
                    new StringReader(subjTemplate), stringCfg);

            Writer out = new StringWriter();
            templ.process(mail, out);
            out.flush();

            msg.setSubject(out.toString(), "UTF-8");
        } else {
            rs.registerEngine(new NotificationsRenderingEngine(
                    customSubjectTemplate));

            LoginContext lc = Framework.login();

            Collection<RenderingResult> results = rs.process(context);
            String subjectMail = "<HTML><P>No parsing Succeded !!!</P></HTML>";

            for (RenderingResult result : results) {
                subjectMail = (String) result.getOutcome();
            }
            subjectMail = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix()
                    + subjectMail;
            msg.setSubject(subjectMail, "UTF-8");

            lc.logout();
        }

        msg.setSentDate(new Date());

        rs.registerEngine(new NotificationsRenderingEngine(
                (String) mail.get(NotificationConstants.TEMPLATE_KEY)));

        LoginContext lc = Framework.login();

        Collection<RenderingResult> results = rs.process(context);
        String bodyMail = "<HTML><P>No parsing Succedeed !!!</P></HTML>";

        for (RenderingResult result : results) {
            bodyMail = (String) result.getOutcome();
        }

        lc.logout();

        rs.unregisterEngine("ftl");

        // Create a multipar message
        Multipart multipart = new MimeMultipart();
        BodyPart messageBodyPart = new MimeBodyPart();
        
        messageBodyPart.setContent(bodyMail, "text/html; charset=utf-8");
        multipart.addBodyPart(messageBodyPart);
        

		String logoPath = Framework.getProperty(PROP_LOGO_PATH);
		if (StringUtils.isNotBlank(logoPath)) {
			File f = new File(logoPath);
			if (!f.exists() || !f.canRead()) {
				log.warn("Chemin de logo "+logoPath+" incorrect.");
			}
			else {
				BodyPart attechedImages = new MimeBodyPart();
				DataSource source = new FileDataSource(f);
				attechedImages.setDataHandler(new DataHandler(source));
				attechedImages.setHeader("Content-ID", "<logo>");
				multipart.addBodyPart(attechedImages);
			}

		}
        
        // Send the message.
        msg.setContent(multipart);
        Transport.send(msg);
    }
}
