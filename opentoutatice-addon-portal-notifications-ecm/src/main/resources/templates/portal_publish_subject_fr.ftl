<#if eventId == "documentPublished" || eventId == "documentPublicationApproved" 
	|| eventId == "documentSetOnLine" || eventId == "workflowOnlineTaskApproved" >
Le document '${docTitle}' a été publié
<#elseif eventId == "documentWaitingPublication"
	|| eventId == "workflowOnlineTaskAssigned">
Demande de publication du document '${docTitle}'
<#elseif eventId == "documentPublicationRejected"
	|| eventId == "workflowOnlineTaskRejected">
Rejet de la demande de publication du document '${docTitle}'
</#if>