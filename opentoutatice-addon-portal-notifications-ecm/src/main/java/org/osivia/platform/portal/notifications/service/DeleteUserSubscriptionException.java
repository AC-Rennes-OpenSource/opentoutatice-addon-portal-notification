/**
 * 
 */
package org.osivia.platform.portal.notifications.service;

import org.nuxeo.ecm.core.api.ClientException;


/**
 * @author david
 *
 */
public class DeleteUserSubscriptionException extends ClientException {

    private static final long serialVersionUID = -841424428769289164L;

    /**
     * @param message
     */
    public DeleteUserSubscriptionException(String message) {
        super(message);
    }

}
