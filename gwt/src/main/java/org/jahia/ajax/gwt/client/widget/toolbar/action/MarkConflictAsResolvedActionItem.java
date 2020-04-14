/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.ajax.gwt.client.widget.toolbar.action;

import com.extjs.gxt.ui.client.widget.Info;

import org.jahia.ajax.gwt.client.core.BaseAsyncCallback;
import org.jahia.ajax.gwt.client.core.JahiaGWTParameters;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.messages.Messages;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;
import org.jahia.ajax.gwt.client.widget.LinkerSelectionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Mark conflicted file as resolved in SCM
 */
@SuppressWarnings("serial")
public class MarkConflictAsResolvedActionItem extends BaseActionItem {

    @Override
    public void onComponentSelection() {
        LinkerSelectionContext lh = linker.getSelectionContext();
        final GWTJahiaNode singleSelection = lh.getSingleSelection();
        if (singleSelection == null) {
            return;
        }
        JahiaContentManagementService.App.getInstance().markConflictAsResolved(JahiaGWTParameters.getSiteKey(), singleSelection, new BaseAsyncCallback<Object>() {
            public void onSuccess(Object result) {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("node", singleSelection);
                linker.refresh(data);
                Info.display(Messages.get("label.information", "Information"), Messages.get("label.sourceControl.resolve.success", "Conflict marked as resolved with success"));
            }

            public void onApplicationFailure(Throwable caught) {
                        Info.display(
                                Messages.get("label.error", "Error"),
                                Messages.get("label.sourceControl.resolve.failure",
                                        "Failed to mark conflict as resolved") + "\n" + caught.getLocalizedMessage());
            }
        });
    }

    @Override
    public void handleNewLinkerSelection() {
        LinkerSelectionContext lh = linker.getSelectionContext();
        GWTJahiaNode singleSelection = lh.getSingleSelection();
        setEnabled(singleSelection != null && singleSelection.isNodeType("jmix:sourceControl") && "unmerged".equals(singleSelection.get("scmStatus")));
    }

}
