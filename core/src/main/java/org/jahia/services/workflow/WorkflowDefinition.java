/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
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
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.services.workflow;

/**
 * Workflow process definition.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 4 févr. 2010
 */
public class WorkflowDefinition extends WorkflowBase {
    private final String key;
    private String formResourceName;

    public WorkflowDefinition(String name, String key, String provider) {
        super(name, provider);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkflowDefinition that = (WorkflowDefinition) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getProvider() != null ? !getProvider().equals(that.getProvider()) : that.getProvider() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (getProvider() != null ? getProvider().hashCode() : 0);
        return result;
    }

    public String getFormResourceName() {
        return formResourceName;
    }

    public void setFormResourceName(String formResourceName) {
        this.formResourceName = formResourceName;
    }

    /**
     * Returns the name of this item.
     *
     * @return the name of this item
     */
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * Returns the name of the workflow item provider.
     *
     * @return the name of the workflow item provider
     */
    @Override
    public String getProvider() {
        return super.getProvider();
    }
}
