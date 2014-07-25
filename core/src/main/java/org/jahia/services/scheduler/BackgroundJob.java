/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.services.scheduler;

import org.apache.commons.id.IdentifierGenerator;
import org.apache.commons.id.IdentifierGeneratorFactory;
import org.apache.jackrabbit.core.security.JahiaLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jahia.bin.filters.jcr.JcrSessionFilter;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.utils.LanguageCodeConverters;
import org.quartz.*;

import java.util.Date;

/**
 * Base class for Jahia background tasks.
 *
 * @author toto
 */
public abstract class BackgroundJob implements StatefulJob {
    private final static Logger logger = LoggerFactory.getLogger(BackgroundJob.class);

    private static IdentifierGenerator idGen = IdentifierGeneratorFactory.newInstance().uuidVersionFourGenerator();

    //job details constants
    public static final String JOB_CREATED = "created";
    public static final String JOB_BEGIN = "begin";
    public static final String JOB_END = "end";
    public static final String JOB_STATUS = "status";
    public static final String JOB_DURATION = "duration";
    public static final String JOB_CURRENT_LOCALE = "currentLocale";
    public static final String JOB_SITEKEY = "sitekey";
    public static final String JOB_USERKEY = "userkey";
    public static final String JOB_MESSAGE = "message";

    public static final String STATUS_ADDED = "added";
    public static final String STATUS_SCHEDULED = "scheduled";
    public static final String STATUS_EXECUTING = "executing";
    public static final String STATUS_SUCCESSFUL = "successful";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELED = "canceled";

    public static JobDetail createJahiaJob(String desc, Class<? extends BackgroundJob> jobClass) {
        // jobdetail is non-volatile,durable,non-recoverable
        JobDetail jobDetail = new JobDetail(getGroupName(jobClass) + "-" + idGen.nextIdentifier(),
                getGroupName(jobClass),
                jobClass,
                false,
                true,
                false);
        jobDetail.setDescription(desc);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(JOB_CREATED, new Date()); // creation date
        jobDataMap.put(JOB_STATUS, STATUS_ADDED); // status "added"
        final JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
        JahiaUser currentUser = sessionFactory.getCurrentUser();
        if (currentUser != null) {
            jobDataMap.put(JOB_USERKEY, sessionFactory.getCurrentUser().getName());
        }
        jobDataMap.put(JOB_CURRENT_LOCALE, sessionFactory.getCurrentLocale() != null ? sessionFactory
                .getCurrentLocale().toString() : null);

        jobDetail.setJobDataMap(jobDataMap);
        return jobDetail;
    }

    public static String getGroupName(Class<? extends BackgroundJob> c) {
        String name = c.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDetail jobDetail = ctx.getJobDetail();
        JobDataMap data = jobDetail.getJobDataMap();
        final JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
        try {
        	String userKey = data.getString(JOB_USERKEY);
        	if ((userKey!= null) && (!userKey.equals(JahiaLoginModule.SYSTEM))) {
	            JahiaUser user = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUserByKey((String) data.get(JOB_USERKEY)).getJahiaUser();
	            if (user != null) {
	            	sessionFactory.setCurrentUser(user);
	            	logger.debug("Executing job as user {}", userKey);
	            } else {
	            	logger.warn("Unable to lookup job user for key {}", userKey);
	            }
        	} else {
                logger.debug("Executing job as system user");
            }
        	String langKey = data.getString(JOB_CURRENT_LOCALE);
        	if (langKey != null) {
        		sessionFactory.setCurrentLocale(LanguageCodeConverters.languageCodeToLocale(langKey));
            	logger.debug("Executing job with locale {}", langKey);
        	}
        	
        	// do execute job
            executeJahiaJob(ctx);
            
        } catch (Exception e) {
            logger.error("Error executing job " + jobDetail.getKey(), e);
            data.put(JOB_STATUS, STATUS_FAILED);
            if (e instanceof JobExecutionException) {
                throw (JobExecutionException) e;
            } else {
                throw new JobExecutionException(e);
            }
        } finally {
            try {
            	this.postExecution(ctx);
            } finally {
                JcrSessionFilter.endRequest();
            }
        }
    }

    public abstract void executeJahiaJob(JobExecutionContext jobExecutionContext)
            throws Exception;

    /**
     * Sub class can perform specific post execution task
     *
     * @param jobExecutionContext
     */
    protected void postExecution(JobExecutionContext jobExecutionContext) {
        // by default do nothing
    }
}
