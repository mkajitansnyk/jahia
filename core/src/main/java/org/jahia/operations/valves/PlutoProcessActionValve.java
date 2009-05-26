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
package org.jahia.operations.valves;

import java.util.Enumeration;

import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.pluto.PortletContainer;
import org.apache.pluto.PortletContainerException;
import org.apache.pluto.PortletWindow;
import org.apache.pluto.descriptors.portlet.PortletDD;
import org.apache.pluto.driver.AttributeKeys;
import org.apache.pluto.driver.core.PortalRequestContext;
import org.apache.pluto.driver.core.PortletWindowImpl;
import org.apache.pluto.driver.services.portal.PortletWindowConfig;
import org.apache.pluto.driver.url.PortalURL;
import org.jahia.data.applications.EntryPointInstance;
import org.jahia.exceptions.JahiaException;
import org.jahia.params.ParamBean;
import org.jahia.params.ProcessingContext;
import org.jahia.pipelines.PipelineException;
import org.jahia.pipelines.valves.Valve;
import org.jahia.pipelines.valves.ValveContext;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.applications.pluto.JahiaUserRequestWrapper;
import org.jahia.services.applications.pluto.JahiaPortletUtil;
import org.jahia.services.cache.CacheKeyGeneratorService;
import org.jahia.services.cache.CacheService;

public class PlutoProcessActionValve implements Valve {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(PlutoProcessActionValve.class);
    private CacheService cacheInstance = null;
    private CacheKeyGeneratorService cacheKeyGeneratorService = null;

    public PlutoProcessActionValve() {
    }

    /**
     * initialize
     */
    public void initialize() {
    }

    /**
     * invoke
     *
     * @param context      Object
     * @param valveContext ValveContext
     * @throws PipelineException
     */
    public void invoke(Object context, ValveContext valveContext)
            throws PipelineException {

        ProcessingContext processingContext = (ProcessingContext) context;
        PortletContainer container = (PortletContainer)
                ((ParamBean) processingContext).getContext().getAttribute(AttributeKeys.PORTLET_CONTAINER);

        try {
            final ParamBean jParams = ((ParamBean) processingContext);
            JahiaUserRequestWrapper request = new JahiaUserRequestWrapper(jParams.getUser(), jParams.getRequest());
            HttpServletResponse response = jParams.getResponse();
            PortalRequestContext portalRequestContext =
                    new PortalRequestContext(((ParamBean) processingContext).getContext(), request, response);

            PortalURL portalURL = portalRequestContext.getRequestedPortalURL();
            String actionWindowId = portalURL.getActionWindow();
            String resourceWindowId = portalURL.getResourceWindow();

            PortletWindowConfig actionWindowConfig = null;
            PortletWindowConfig resourceWindowConfig = null;

            if (resourceWindowId != null) {
                resourceWindowConfig = PortletWindowConfig.fromId(resourceWindowId);
            } else if (actionWindowId != null) {
                actionWindowConfig = PortletWindowConfig.fromId(actionWindowId);
            }

            // Action window config will only exist if there is an action request.
            if (actionWindowConfig != null) {
                flushPortletCache(processingContext, jParams, actionWindowConfig);
                PortletWindowImpl portletWindow = new PortletWindowImpl(
                        actionWindowConfig, portalURL);
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing action request for window: "
                            + portletWindow.getId().getStringId());
                }

                EntryPointInstance entryPointInstance = ServicesRegistry.getInstance().getApplicationsManagerService().getEntryPointInstance(actionWindowConfig.getMetaInfo());
                if (entryPointInstance != null) {
                    request.setEntryPointInstance(entryPointInstance);
                } else {
                    logger.warn("Couldn't find related entryPointInstance, roles might not work properly !");
                }
                copyAttribute("org.jahia.data.JahiaData", jParams, request, portletWindow);
                copyAttribute("currentRequest", jParams, request, portletWindow);
                copyAttribute("currentSite", jParams, request, portletWindow);
                copyAttribute("currentPage", jParams, request, portletWindow);
                copyAttribute("currentUser", jParams, request, portletWindow);
                copyAttribute("currentJahia", jParams, request, portletWindow);
                JahiaPortletUtil.copySharedMapFromJahiaToPortlet(jParams, request, portletWindow,true);


                try {
                    container.doAction(portletWindow, request, jParams.getResponse());
                    JahiaPortletUtil.copySharedMapFromPortletToJahia(jParams,request,portletWindow);
                } catch (PortletContainerException ex) {
                    throw new ServletException(ex);
                } catch (PortletException ex) {
                    throw new ServletException(ex);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Action request processed.\n\n");
                }
                return;
            }
            //Resource request
            else if (resourceWindowConfig != null) {
                try {
                    if (request.getParameterNames().hasMoreElements())
                        setPublicRenderParameter(container, request, portalURL, portalURL.getResourceWindow());
                } catch (PortletContainerException e) {
                    logger.warn(e);
                }
                PortletWindowImpl portletWindow = new PortletWindowImpl(
                        resourceWindowConfig, portalURL);
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing resource Serving request for window: "
                            + portletWindow.getId().getStringId());
                }
                try {
                    container.doServeResource(portletWindow, request, jParams.getRealResponse());
                } catch (PortletContainerException ex) {
                    throw new ServletException(ex);
                } catch (PortletException ex) {
                    throw new ServletException(ex);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Action request processed.\n\n");
                }
                return;
            }
        } catch (Exception t) {
            logger.error("Error while processing action", t);
        } finally {
        }

        // continue valve processing...
        valveContext.invokeNext(context);
    }

    /**
     * Flush the portlet Cache
     *
     * @param processingContext
     * @param jParams
     * @param actionWindowConfig
     * @throws JahiaException
     */
    private void flushPortletCache(ProcessingContext processingContext, ParamBean jParams, PortletWindowConfig actionWindowConfig) throws JahiaException {
        String cacheKey = null;
        // Check if cache is available for this portlet
        cacheKey = "portlet_instance_" + actionWindowConfig.getMetaInfo();
        final EntryPointInstance entryPointInstance = ServicesRegistry.getInstance().getApplicationsManagerService().getEntryPointInstance(actionWindowConfig.getMetaInfo());
        if (entryPointInstance != null && entryPointInstance.getCacheScope() != null && entryPointInstance.getCacheScope().equals(MimeResponse.PRIVATE_SCOPE)) {
            cacheKey += "_" + jParams.getUser().getUserKey();
        }
        // Try to flush the entry in cache
        cacheInstance.getContainerHTMLCacheInstance().remove(cacheKeyGeneratorService.computeContainerEntryKey(
                null, cacheKey, processingContext.getUser(),
                processingContext.getLocale().toString(),
                processingContext.getOperationMode(),
                processingContext.getScheme()));
    }

    /**
     * Acces method for the CacheInstance
     *
     * @param cacheInstance
     */
    public void setCacheInstance(CacheService cacheInstance) {
        this.cacheInstance = cacheInstance;
    }

    /**
     * Acces method for the CacheKeyGeneratorService
     *
     * @param cacheKeyGeneratorService
     */
    public void setCacheKeyGeneratorService(CacheKeyGeneratorService cacheKeyGeneratorService) {
        this.cacheKeyGeneratorService = cacheKeyGeneratorService;
    }

    /**
     * Set public render parameter or the portal URL
     *
     * @param container
     * @param request
     * @param portalURL
     * @param portletID
     * @throws ServletException
     * @throws PortletContainerException
     */
    private void setPublicRenderParameter(PortletContainer container, HttpServletRequest request, PortalURL portalURL, String portletID) throws ServletException, PortletContainerException {
        String applicationId = PortletWindowConfig.parseContextPath(portletID);
        String portletName = PortletWindowConfig.parsePortletName(portletID);
        PortletDD portletDD = container.getOptionalContainerServices().getPortletRegistryService()
                .getPortletDescriptor(applicationId, portletName);
        Enumeration<?> parameterNames = request.getParameterNames();
        if (parameterNames != null) {
            while (parameterNames.hasMoreElements()) {
                String parameterName = (String) parameterNames.nextElement();
                if (portletDD.getPublicRenderParameter() != null) {
                    if (portletDD.getPublicRenderParameter().contains(parameterName)) {
                        String value = request.getParameter(parameterName);
                        portalURL.addPublicParameterActionResourceParameter(parameterName, value);
                    }
                }
            }
        }
    }

    /**
     * Copy jahia request attibute in portlet request attribute
     *
     * @param attributeName
     * @param processingContext
     * @param portalRequest
     * @param window
     */
    private void copyAttribute(String attributeName, ProcessingContext processingContext, HttpServletRequest portalRequest, PortletWindow window) {
        copyAttribute(attributeName, processingContext, portalRequest, window, null, false);
    }

    /**
     * Copy jahia session or request attribute into the portalRequest.
     *
     * @param attributeName
     * @param processingContext
     * @param portalRequest
     * @param window
     * @param fromSession       true means that the attribute is in  Jahia Session else it's taked from the request
     */
    private void copyAttribute(String attributeName, ProcessingContext processingContext, HttpServletRequest portalRequest, PortletWindow window, Object defaultValue, boolean fromSession) {
        Object objectToCopy;
        if (fromSession) {
            // get from session
            objectToCopy = processingContext.getSessionState().getAttribute(attributeName);
            if (objectToCopy == null) {
                objectToCopy = defaultValue;
                processingContext.getSessionState().setAttribute(attributeName, objectToCopy);
            }
        } else {
            // get from request
            objectToCopy = processingContext.getAttribute(attributeName);
            if (objectToCopy == null) {
                objectToCopy = defaultValue;
                processingContext.setAttribute(attributeName, objectToCopy);
            }
        }

        // add in the request attribute
        portalRequest.setAttribute("Pluto_" + window.getId().getStringId() + "_" + attributeName, objectToCopy);

    }
}


