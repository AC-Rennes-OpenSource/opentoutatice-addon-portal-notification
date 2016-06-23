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
 *
 * Contributors:
 *   mberhaut1
 *    
 */
package org.osivia.platform.portal.notifications.activator;

import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationEventListener;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osivia.platform.portal.notifications.listener.OttcNotificationEventListener;

import fr.toutatice.ecm.platform.core.helper.ToutaticeEmailHelper;


public class OsiviaBundleActivator implements BundleActivator {
	
	@Override
	public void start(BundleContext context) throws Exception {
		installEmailHelper();
	}
	
	 @Override
	    public void stop(BundleContext context) throws Exception {
	        // Nothing
	    }
	 
	 /**
	  * PortalNotificationEventListener replaces default NotificationEventListener.
	  * We set customize EmailHelper for it.
	  * 
	  * @throws Exception
	  */
	private void installEmailHelper() throws Exception {
		ToutaticeEmailHelper toutaticeEmailHelper = new ToutaticeEmailHelper();
		
		EventService eventService = Framework.getService(EventService.class);
        List<PostCommitEventListener> listeners = eventService.getPostCommitEventListeners();
        
        Iterator<PostCommitEventListener> iterator = listeners.iterator();
        boolean isPortalNotifier = false;
        
        while(iterator.hasNext() && !isPortalNotifier){
            PostCommitEventListener postCommitEventListener = iterator.next();
            
            if (postCommitEventListener.getClass().equals(OttcNotificationEventListener.class)) {
                ((OttcNotificationEventListener) postCommitEventListener).setEmailHelper(toutaticeEmailHelper);
                isPortalNotifier = true;
            }
        }
		
	}

}
