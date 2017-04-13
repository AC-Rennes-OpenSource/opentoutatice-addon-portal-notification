/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * Contributors:
 * mberhaut1
 */
package org.osivia.platform.portal.notifications.automation;

import junit.framework.Assert;

import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.DocRef;

public class HandleNotificationTest {

	public static void main(String[] args) throws Exception {
		HttpAutomationClient client = new HttpAutomationClient("http://localhost:8080/nuxeo/site/automation");

		try {
			Session session = client.getSession("Administrator", "Administrator");
			Assert.assertNotNull(session);

			OperationRequest request = session.newRequest(HandleNotification.ID);
			request.setInput(new DocRef("/4718229641511231015/sap/dap-musique"));
			request.set("userid", "user:mberhaut1"); //"group:Bureau_430"); //"user:mberhaut1");
			request.set("notifications", new String[] {"DUNPublication","DUNUnPublication"});
			request.set("sendEmail", false);
			request.set("action", HandleNotification.NOTIFICATION_ACTION_ADD);
			
			Object result = request.execute();
			Assert.assertTrue(null != result);
			
			System.out.println("Fin de l'opération");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (null != client) {
				client.shutdown();
			}
		}
	}
}