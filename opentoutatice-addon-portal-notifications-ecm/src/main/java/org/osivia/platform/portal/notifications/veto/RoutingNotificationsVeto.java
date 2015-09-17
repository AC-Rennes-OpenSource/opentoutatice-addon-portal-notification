/*
 * (C) Copyright 2015 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
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
 *   dchevrier
 *    
 */
package org.osivia.platform.portal.notifications.veto;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;

import fr.toutatice.ecm.platform.core.helper.ToutaticeQueryHelper;


/**
 * @author David Chevrier.
 *
 */
public class RoutingNotificationsVeto implements NotificationListenerVeto {
    
    public static final String GET_WF_ON_DOCUMENT_QUERY = "select * from DocumentRoute where docri:participatingDocuments = '%s' "
            + "and ecm:currentLifeCycleState IN ('ready','running') order by dc:created";
    
    // FIXME: restore with just ToutaticeWorkflowHelper.getWorkflowsOnDocument 
    // when dependence on opentoutatice changed (version > 3.1.2)
    @Override
    public boolean accept(Event event) throws Exception {
        boolean accept = true;

        if (DocumentEventTypes.DOCUMENT_UPDATED.equals(event.getName())) {

            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            DocumentModel sourceDocument = docCtx.getSourceDocument();

            List<DocumentRoute> workflowsOnDocument = getWorkflowsOnDocument(sourceDocument);
            accept = CollectionUtils.isEmpty(workflowsOnDocument);

        }

        return accept;
    }
    
    /**
     * @param currentDoc
     * @return workflows on document.
     */
    public static List<DocumentRoute> getWorkflowsOnDocument(DocumentModel currentDoc){
        List<DocumentRoute> routes = new ArrayList<DocumentRoute>(1);
        
        CoreSession session = currentDoc.getCoreSession();
        String query = String.format(GET_WF_ON_DOCUMENT_QUERY, currentDoc.getId());
        
        UnrestrictedQueryRunner queryRunner = new UnrestrictedQueryRunner(session, query);
        DocumentModelList wfs = queryRunner.runQuery();
        
        if(CollectionUtils.isNotEmpty(wfs)){
            for(DocumentModel wf : wfs){
              routes.add(wf.getAdapter(DocumentRoute.class));
            }
        }

        return routes;
    }
    
    /**
     * To query in unrestricted mode.
     * 
     * @author David Chevrier.
     *
     */
    public static class UnrestrictedQueryRunner extends UnrestrictedSessionRunner {

        String query;

        DocumentModelList docs;

        protected UnrestrictedQueryRunner(CoreSession session, String query) {
            super(session);
            this.query = query;
        }

        @Override
        public void run() throws ClientException {
            docs = session.query(query);
            for (DocumentModel documentModel : docs) {
                documentModel.detach(true);
            }
        }

        public DocumentModelList runQuery() throws ClientException {
            runUnrestricted();
            return docs;
        }
    }


}
