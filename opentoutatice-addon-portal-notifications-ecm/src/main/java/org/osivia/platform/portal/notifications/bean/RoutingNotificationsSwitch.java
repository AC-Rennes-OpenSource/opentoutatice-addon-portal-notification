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
 * dchevrier
 */
package org.osivia.platform.portal.notifications.bean;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskQueryConstant;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.service.DocumentTaskProvider;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;
import org.nuxeo.runtime.api.Framework;
import org.osivia.platform.portal.notifications.constants.ExtendedSeamPrecedence;

import fr.toutatice.ecm.platform.core.constants.ToutaticeGlobalConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeWorkflowHelper;


/**
 * Allows to redirect Seam events to Nuxeo events
 * (used in notifications for instance).
 * 
 * @author David Chevrier.
 *
 */
@Name("routingNotifications")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = ExtendedSeamPrecedence.ADD_ON)
public class RoutingNotificationsSwitch implements Serializable {

    @In(create = true)
    protected transient CoreSession documentManager;

    @In
    protected transient EventService eventService;

    private static final long serialVersionUID = -5223906396024845056L;

    /** Indicator of current first Node Route. */
    private boolean isFirstNode;
    /** Actors of Route. */
    private List<String> actors = new ArrayList<String>();

    /**
     * Redirect workflow canceled Seam event to Nuxeo event
     * (if it is not first node of route).
     */
    @Observer(value = {TaskEventNames.WORKFLOW_CANCELED})
    public void redirectToWfCanceledEvent() {

        if (!isFirstNode) {
            Principal principal = documentManager.getPrincipal();
            NavigationContext navCtx = (NavigationContext) SeamComponentCallHelper.getSeamComponentByName("navigationContext");
            DocumentModel currentDocument = navCtx.getCurrentDocument();
            Map<String, Serializable> properties = new HashMap<String, Serializable>();
            properties.put(NotificationConstants.RECIPIENTS_KEY, actors.toArray(new String[0]));

            DocumentEventContext docCtx = new DocumentEventContext(documentManager, principal, currentDocument);
            eventService.fireEvent(TaskEventNames.WORKFLOW_CANCELED, docCtx);
        }

    }

    /**
     * true if we are on first Node of Route
     * (i.e. just after Start Node).
     */
    @Observer(value = {ToutaticeGlobalConst.BEFORE_WF_CANCELED_EVENT})
    public void isFirstNodeStep() {
//        isFirstNode = false;
//
//        NavigationContext navCtx = (NavigationContext) SeamComponentCallHelper.getSeamComponentByName("navigationContext");
//        DocumentModel currentDocument = navCtx.getCurrentDocument();
//        
//        List<Task> taskInstances = DocumentTaskProvider.getTasks(
//                TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_PP,
//                documentManager, true, null, currentDocument.getId(), currentDocument.getId());
//        
//        if (CollectionUtils.isNotEmpty(taskInstances)) {
//            
//            Task currentTask = taskInstances.get(0);
//            
//        
//        List<DocumentRoute> workflowsOnDocument = ToutaticeWorkflowHelper.getWorkflowsOnDocument(currentDocument);
//        if (CollectionUtils.isNotEmpty(workflowsOnDocument)) {
//            DocumentRoute documentRoute = workflowsOnDocument.get(0);
//            DocumentModel documentModelRoute = documentRoute.getDocument();
//            
//            GraphRoute route = documentModelRoute.getAdapter(GraphRoute.class);
//            
//            GraphNode startNode = route.getStartNode();
//                Transition transition = startNode.getOutputTransitions().get(0);
//                String targetId = transition.getTarget();
//                GraphNode targetNode = route.getNode(targetId);
//                if(targetNode.hasTask() || targetNode.hasMultipleTasks()){
//                    targetNode.getTaskAssignees();
//                }
//        }
//        }
//        
//        
//
//        TaskService taskService = (TaskService) Framework.getService(TaskService.class);
//        //List<Task> taskInstancesr = taskService.getTaskInstances(currentDocument, null, documentManager);
//
//        if (CollectionUtils.isNotEmpty(taskInstances)) {
//
//            
//            //actors = currentTask.getActors();
//
//            List<DocumentRoute> workflowsOnDocument = ToutaticeWorkflowHelper.getWorkflowsOnDocument(currentDocument);
//            if (CollectionUtils.isNotEmpty(workflowsOnDocument)) {
//                DocumentRoute documentRoute = workflowsOnDocument.get(0);
//                DocumentModel documentModelRoute = documentRoute.getDocument();
//                
//                String firstNodeName = getFirstNodeName(documentManager, documentModelRoute);
//                
//                //DocumentModel currentTaskModel = currentTask.getDocument();
////                String[] currentTaskNamePath = StringUtils.split(currentTaskModel.getPathAsString(), "/");
////                String currentTaskName = currentTaskNamePath[currentTaskNamePath.length - 1];
////                
////                isFirstNode = StringUtils.contains(currentTaskName, firstNodeName);
//                
//            }
//        }

    }
    
    /**
     * @param documentManager
     * @param documentModelRoute
     * @return the start node of route.
     */
    protected DocumentModel getStartNode(CoreSession documentManager, DocumentModel documentModelRoute){
        DocumentModel startNode = null;
        
        Filter startNodeFilter = new Filter() {

            private static final long serialVersionUID = -1362865077000277152L;

            @Override
            public boolean accept(DocumentModel docModel) {
                return (Boolean) docModel.getPropertyValue("rnode:start");
            }
            
        };
        
        DocumentModelList children = documentManager.getChildren(documentModelRoute.getRef(), "RouteNode", startNodeFilter, null);
        if(CollectionUtils.isNotEmpty(children)){
            startNode = children.get(0);
        }
        
        return startNode;
    }
    
    /**
     * @param documentManager
     * @param documentModelRoute
     * @return the first Node name of Route
     */
    protected String getFirstNodeName(CoreSession documentManager, DocumentModel documentModelRoute){
        String name = null;
        
        DocumentModel startNode = getStartNode(documentManager, documentModelRoute);
        if(startNode != null){
            name = (String) startNode.getPropertyValue("rnode:transitions/0/targetId");
        }
        
        return name;
    }


}
