/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.services.workflow.jbpm.custom;

import org.jahia.services.content.*;
import org.jahia.services.workflow.HistoryWorkflowTask;
import org.jahia.services.workflow.WorkflowDefinition;
import org.jahia.services.workflow.WorkflowService;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.jahia.api.Constants;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.workflow.WorkflowVariable;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.activity.ExternalActivityBehaviour;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sets a property on a node
 */
public class SetPropertyActivity implements ExternalActivityBehaviour {
    private static final long serialVersionUID = 1L;
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(Publish.class);

    private String propertyName;
    private String value;

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void execute(ActivityExecution execution) throws Exception {
        final List<String> uuids = (List<String>) execution.getVariable("nodeIds");
        final String workspace = (String) execution.getVariable("workspace");
        final String userKey = (String) execution.getVariable("user");

        JCRTemplate.getInstance().doExecuteWithSystemSession(null,workspace,new JCRCallback<Object>() {
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                for (String uuid : uuids) {
                    session.getNodeByUUID(uuid).setProperty(propertyName, value);
                }
                session.save();
                return null;
            }
        });

        execution.takeDefaultTransition();
    }

    public void signal(ActivityExecution execution, String signalName, Map<String, ?> parameters) throws Exception {
    }

}
