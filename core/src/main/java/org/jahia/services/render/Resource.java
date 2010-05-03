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
package org.jahia.services.render;

import org.apache.log4j.Logger;
import org.jahia.exceptions.JahiaException;
import org.jahia.params.ProcessingContext;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * A resource is the aggregation of a node and a specific template
 * It's something that can be handled by the render engine to be displayed.
 *
 * @author toto
 */
public class Resource {
    public static final String CONFIGURATION_PAGE = "page";
    public static final String CONFIGURATION_GWT = "gwt";
    public static final String CONFIGURATION_MODULE = "module";
    public static final String CONFIGURATION_INCLUDE = "include";
    public static final String CONFIGURATION_WRAPPER = "wrapper";
    public static final String CONFIGURATION_OPTION = "option";

    private static Logger logger = Logger.getLogger(Resource.class);
    private JCRNodeWrapper node;
    private String templateType;
    private String template;
    private String forcedTemplate;
    private String contextConfiguration;
    private Stack<Wrapper> wrappers;

    private Set<JCRNodeWrapper> dependencies;
    private List<String> missingResources;

    private List<Option> options;
    private ExtendedNodeType resourceNodeType;
    private Map<String, Object> moduleParams = new HashMap<String, Object>();

    /**
     * Creates a resource from the specified parameter
     * @param node The node to display
     * @param templateType template type
     * @param template the template name, null if default
     * @param forcedTemplate the template name, null if default
     * @param contextConfiguration
     */
    public Resource(JCRNodeWrapper node, String templateType, String template, String forcedTemplate,
                    String contextConfiguration) {
        this.node = node;
        this.templateType = templateType;
        this.template = template;
        this.forcedTemplate = forcedTemplate;
        this.contextConfiguration = contextConfiguration;
        dependencies = new LinkedHashSet<JCRNodeWrapper>();
        dependencies.add(node);

        missingResources = new ArrayList<String>();
        wrappers = new Stack<Wrapper>();
        options = new ArrayList<Option>();

    }

    public JCRNodeWrapper getNode() {
        return node;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getWorkspace() {
        try {
            return node.getSession().getWorkspace().getName();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public Locale getLocale() {
        try {
            return node.getSession().getLocale();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getContextConfiguration() {
        return contextConfiguration;
    }

    public List<String> getTemplates() {
        List<String> l = new ArrayList<String>();

        if (forcedTemplate != null && forcedTemplate.length() > 0) {
            l.add(forcedTemplate);
            return l;
        }
        try {
            if (node.isNodeType("jmix:renderable") && node.hasProperty("j:template")) {
                l.add( node.getProperty("j:template").getString() );
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        if (template != null && template.length() > 0) {
            l.add(template);
        }
        l.add("default");
        return l;
    }

    public String getResolvedTemplate() {
        return getTemplates().iterator().next();
    }

    public Set<JCRNodeWrapper> getDependencies() {
        return dependencies;
    }

    public List<String> getMissingResources() {
        return missingResources;
    }

    public Map<String, Object> getModuleParams() {
        return moduleParams;
    }

    public boolean hasWrapper() {
        return !wrappers.isEmpty();
    }

    public Wrapper popWrapper() {
        return wrappers.pop();
    }

    public Wrapper pushWrapper(String wrapper) {
        return wrappers.push(new Wrapper(wrapper,node));
    }

    public Wrapper pushBodyWrapper() {
        JCRNodeWrapper current = node;
        try {
            while (true) {
                if (current.hasProperty("j:bodywrapper")) {
                    return wrappers.push(new Wrapper(current.getProperty("j:bodywrapper").getString(), current));
                }
                current = current.getParent();
            }
        } catch (ItemNotFoundException e) {
            wrappers.push(new Wrapper("bodywrapper", node));
        } catch (RepositoryException e) {
            logger.error("Cannot find wrapper",e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "node=" + node.getPath() +
                ", templateType='" + templateType + '\'' +
                ", template='" + template + '\'' +
                ", forcedTemplate='" + forcedTemplate + '\'' +
                '}';
    }

    public void addOption(String wrapper, ExtendedNodeType nodeType) {
        options.add(new Option(wrapper,nodeType));
    }

    public List<Option> getOptions() {
        return options;
    }

    public boolean hasOptions() {
        return !options.isEmpty();
    }

    public void removeOption(ExtendedNodeType mixinNodeType) {
        options.remove(new Option("",mixinNodeType));
    }

    public ExtendedNodeType getResourceNodeType() {
        return resourceNodeType;
    }

    public void setResourceNodeType(ExtendedNodeType resourceNodeType) {
        this.resourceNodeType = resourceNodeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        if (node != null ? !node.equals(resource.node) : resource.node != null) return false;
        if (templateType != null ? !templateType.equals(resource.templateType) : resource.templateType != null)
            return false;
        if (template != null ? !template.equals(resource.template) : resource.template != null) return false;
        if (forcedTemplate != null ? !forcedTemplate.equals(resource.forcedTemplate) : resource.forcedTemplate != null)
            return false;
        if (wrappers != null ? !wrappers.equals(resource.wrappers) : resource.wrappers != null) return false;
        if (options != null ? !options.equals(resource.options) : resource.options != null) return false;
        if (resourceNodeType != null ? !resourceNodeType.equals(resource.resourceNodeType) : resource.resourceNodeType != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = node != null ? node.hashCode() : 0;
        result = 31 * result + (templateType != null ? templateType.hashCode() : 0);
        result = 31 * result + (template != null ? template.hashCode() : 0);
        result = 31 * result + (forcedTemplate != null ? forcedTemplate.hashCode() : 0);
        result = 31 * result + (wrappers != null ? wrappers.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        result = 31 * result + (resourceNodeType != null ? resourceNodeType.hashCode() : 0);
        return result;
    }

    public class Option implements Comparable<Option> {
        private final String wrapper;
        private final ExtendedNodeType nodeType;

        public Option(String wrapper, ExtendedNodeType nodeType) {
            this.wrapper = wrapper;
            this.nodeType = nodeType;
        }

        public ExtendedNodeType getNodeType() {
            return nodeType;
        }

        public String getWrapper() {
            return wrapper;
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Option o) {
            return nodeType.getName().compareTo(o.getNodeType().getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Option option = (Option) o;

            return nodeType.getName().equals(option.nodeType.getName());

        }

        @Override
        public int hashCode() {
            return nodeType.getName().hashCode();
        }
    }

    public class Wrapper {
        public String template;
        public JCRNodeWrapper node;

        Wrapper(String template, JCRNodeWrapper node) {
            this.template = template;
            this.node = node;
        }
    }
}
