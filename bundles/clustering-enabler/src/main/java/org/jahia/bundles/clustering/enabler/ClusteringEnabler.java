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
package org.jahia.bundles.clustering.enabler;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.*;
import org.jahia.bin.Jahia;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.osgi.FrameworkService;
import org.jahia.services.modulemanager.spi.Config;
import org.jahia.services.modulemanager.spi.ConfigService;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Activator for the DX clustering feature that checks if the cluster is activated or not and either starts dx-clustering feature or
 * uninstalls it. The check is done when we get an event about the full startup of the OSGi container.
 *
 * @author Sergiy Shyrkov
 */
@Component(name = "org.jahia.bundles.clustering.enabler", service = EventHandler.class, property = {
        Constants.SERVICE_PID + "=org.jahia.bundles.clustering.enabler",
        Constants.SERVICE_DESCRIPTION + "=DX clustring feature enabler",
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        EventConstants.EVENT_TOPIC + "=" + FrameworkService.EVENT_TOPIC_LIFECYCLE,
        EventConstants.EVENT_FILTER + "=(type=" + FrameworkService.EVENT_TYPE_FILEINSTALL_STARTED + ")" }, immediate = true)
public class ClusteringEnabler implements EventHandler {

    private static final String FEATURE_NAME = "dx-clustering";

    private static final Logger logger = LoggerFactory.getLogger(ClusteringEnabler.class);

    private BundleContext bundleContext;

    private FeaturesService featuresService;

    private ConfigService configService;
    private ServiceTracker<Object,Object> tracker;

    @Reference
    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    @Reference
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
    }

    @Deactivate
    public void deactivate() {
        if (tracker != null) {
            tracker.close();
        }
    }

    @Override
    public void handleEvent(Event event) {
        long startTime = System.currentTimeMillis();
        logger.info("Clustering feature enabler is checking for the state of the feature " + FEATURE_NAME);

        if (featuresService == null) {
            throw new JahiaRuntimeException("Unable to lookup FeaturesService instance");
        }

        FeatureState clusteringState;
        String featureId = FEATURE_NAME;
        Feature feature = null;
        try {
            feature = featuresService.getFeature(featureId);
        } catch (Exception e) {
            throw new JahiaRuntimeException("Error retrieving feature " + FEATURE_NAME
                    + ". Ensure its repository is known to the OSGi platform.", e);
        }
        if (feature == null) {
            logger.debug("Unable to locate feature " + FEATURE_NAME + ". Ensure its repository is known to the OSGi platform.");
            return;
        }

        // get exact feature ID
        featureId = feature.getId();
        clusteringState = featuresService.getState(featureId);

        logger.info("The state of the feature {} is {}", featureId, clusteringState);

        boolean clusterActivated = SettingsBean.getInstance().isClusterActivated();

        try {
            if (clusterActivated) {
                final Set<String> clusterServices = new HashSet<>(Arrays.asList("config", "bundle", "feature"));
                tracker = new ServiceTracker<Object,Object>(bundleContext, "org.apache.karaf.cellar.core.Synchronizer", null) {
                    @Override
                    public Object addingService(ServiceReference<Object> reference) {
                        clusterServices.remove(reference.getProperty("resource"));
                        if (clusterServices.isEmpty()) {
                            FrameworkService.notifyClusterStarted();
                            tracker.close();
                            tracker = null;
                        }
                        return super.addingService(reference);
                    }
                };
                tracker.open();

                if (FeatureState.Started != clusteringState) {
                    logger.info("Installing configurations");
                    installConfigurations(feature);

                    logger.info("Installing feature {}", featureId);
                    featuresService.installFeature(featureId);
                    FrameworkService.sendEvent(FrameworkService.EVENT_TOPIC_LIFECYCLE,
                            Collections.singletonMap("type", FrameworkService.EVENT_TYPE_CLUSTERING_FEATURE_INSTALLED),
                            true);
                }
            } else {
                if (FeatureState.Uninstalled != clusteringState) {
                    logger.info("Uninstalling feature {}", featureId);
                    featuresService.uninstallFeature(featureId);
                }
            }
        } catch (Exception e) {
            String msg = "Unable to " + (clusterActivated ? "enable" : "disable") + " clustering feature. Cause: "
                    + e.getMessage();
            logger.error(msg, e);
        }

        logger.info("Clustering feature enabler finished in {} ms", System.currentTimeMillis() - startTime);
    }

    private void installConfigurations(Feature feature) throws Exception {
        for (Dependency dependency : feature.getDependencies()) {
            installConfigurations(featuresService.getFeature(dependency.getName(), dependency.getVersion()));
        }

        for (ConfigFileInfo configuration : feature.getConfigurationFiles()) {
            loadConfig(StringUtils.substringBetween(configuration.getFinalname(), "etc/", ".cfg"), configuration.getLocation());
        }
    }

    private void loadConfig(String name, String url) throws IOException, URISyntaxException {
        if (name != null) {
            Config config = configService.getConfig(name);
            if (config.getRawProperties().isEmpty()) {
                config.setContent(IOUtils.toString(new URI(url), StandardCharsets.UTF_8));
                configService.storeConfig(config);
            }
        }
    }
}
