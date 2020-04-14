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
package org.jahia.taglibs.jcr.node;

import org.apache.taglibs.standard.tag.common.core.Util;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.taglibs.jcr.AbstractJCRTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * User: toto
 * Date: 3/1/11
 * Time: 14:25
 */
public class JCRNodeIconTag extends AbstractJCRTag {
    private static final long serialVersionUID = 8898161399087791837L;

    private transient static Logger logger = LoggerFactory.getLogger(JCRNodeTypeTag.class);

    private JCRNodeWrapper node;
    private Object type;
    private String var;
    private int scope = PageContext.PAGE_SCOPE;

    public void setNode(JCRNodeWrapper node) {
        this.node = node;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = Util.getScope(scope);
    }

    public int doStartTag() throws JspException {
        try {
            if (node != null) {
                pageContext.setAttribute(var, JCRContentUtils.getIcon(node), scope);
            } else if (type != null) {
                pageContext.setAttribute(var, JCRContentUtils.getIcon(type instanceof ExtendedNodeType ? (ExtendedNodeType) type : NodeTypeRegistry.getInstance().getNodeType(String.valueOf(type))), scope);
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }

        return EVAL_BODY_INCLUDE;
    }

    /**
     * Default processing of the end tag returning EVAL_PAGE.
     *
     * @return EVAL_PAGE
     * @throws javax.servlet.jsp.JspException if an error occurs while processing this tag
     * @see javax.servlet.jsp.tagext.Tag#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException {
        resetState();
        return super.doEndTag();
    }

    @Override
    protected void resetState() {
        node = null;
        type = null;
        var = null;
        scope = PageContext.PAGE_SCOPE;
        super.resetState();
    }

}
