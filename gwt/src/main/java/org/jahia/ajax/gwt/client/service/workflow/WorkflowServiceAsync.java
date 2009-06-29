/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.ajax.gwt.client.service.workflow;

import org.jahia.ajax.gwt.client.data.workflow.GWTJahiaWorkflowHistoryEntry;
import org.jahia.ajax.gwt.client.data.workflow.GWTJahiaWorkflowManagerState;
import org.jahia.ajax.gwt.client.data.GWTJahiaLabel;
import org.jahia.ajax.gwt.client.data.workflow.GWTJahiaWorkflowBatch;
import org.jahia.ajax.gwt.client.data.workflow.GWTJahiaWorkflowElement;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.ListLoadResult;

import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author rfelden
 * @version 17 juil. 2008 - 16:01:05
 */
public interface WorkflowServiceAsync {

    public void getAvailableLanguages(AsyncCallback<List<String>> async);

    public void getSubElements(GWTJahiaWorkflowElement parent, AsyncCallback<List<GWTJahiaWorkflowElement>> async);

    public void getFlattenedSubElements(GWTJahiaWorkflowElement parent, int depth, AsyncCallback<List<GWTJahiaWorkflowElement>> async);

    public void getPagedFlattenedSubElements(GWTJahiaWorkflowElement parent, int depth, int offset, int pageSize, String sortParameter, boolean isAscending, AsyncCallback<PagingLoadResult<GWTJahiaWorkflowElement>> async);

    public void getAvailableActions(AsyncCallback<List<GWTJahiaLabel>> async);

    public void storeBatch(Map<String, Map<String, Set<String>>> batch, AsyncCallback async);

    public void restoreBatch(AsyncCallback<Map<String, Map<String, Set<String>>>> async);

    public void executeStoredBatch(String name, String comment, AsyncCallback async);

    public void execute(GWTJahiaWorkflowBatch batch, AsyncCallback async);

    public void getHistory(GWTJahiaWorkflowElement item, AsyncCallback<ListLoadResult<GWTJahiaWorkflowHistoryEntry>> async);

    public void saveWorkflowManagerState(GWTJahiaWorkflowManagerState state, AsyncCallback async);

    public void getWorkflowManagerState(AsyncCallback<GWTJahiaWorkflowManagerState> async);

    public void getPreviewLink(String objectKey, boolean compareMode, String languageCode, AsyncCallback<String> async);

}
