/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2010 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.io.IOUtils;
import org.jahia.bin.listeners.JahiaContextLoaderListener;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;

import javax.servlet.RequestDispatcher;

/**
 * Implementation of the {@link Template} that uses {@link RequestDispatcher} to forward to a JSP resource.
 * User: toto
 * Date: Sep 28, 2009
 * Time: 7:20:38 PM
 */
public class FileSystemTemplate implements Comparable<FileSystemTemplate>, Template {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(FileSystemTemplate.class);
    private String path;
    private String fileExtension;
    private String key;
    private JahiaTemplatesPackage ownerPackage;
    private String displayName;    
    private Properties properties;

    private static Map<String,Properties> propCache = new HashMap<String, Properties>();
    
    public static void clearPropertiesCache() {
        propCache.clear();
    }

    public FileSystemTemplate(String path, String key, JahiaTemplatesPackage ownerPackage, String displayName) {
        this.path = path;
        this.key = key;
        this.ownerPackage = ownerPackage;
        this.displayName = displayName;
        int lastDotPos = path.lastIndexOf(".");
        if (lastDotPos > 0) {
            this.fileExtension = path.substring(lastDotPos+1);
        }

        String propName = StringUtils.substringBeforeLast(path, "." + fileExtension) + ".properties";

        if (!propCache.containsKey(propName)) {
            properties = new Properties();            
            propCache.put(propName, properties);
            InputStream is = JahiaContextLoaderListener.getServletContext().getResourceAsStream(propName);
            if (is != null) {
                try {
                    properties.load(is);
                } catch (IOException e) {
                    logger.warn("Unable to read associated properties file under " + propName, e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } else {
            properties = propCache.get(propName);
        }
    }

    public String getPath() {
        return path;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getKey() {
        return key;
    }

    public JahiaTemplatesPackage getModule() {
        return ownerPackage;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Return printable information about the script : type, localization, file, .. in order to help
     * template developer to find the original source of the script
     *
     * @return
     */
    public String getInfo() {
        return "Path dispatch : " + path;
    }

    /**
     * Return properties of the template
     *
     * @return
     */
    public Properties getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileSystemTemplate template = (FileSystemTemplate) o;

        if (displayName != null ? !displayName.equals(template.displayName) : template.displayName != null) {
            return false;
        }
        if (key != null ? !key.equals(template.key) : template.key != null) {
            return false;
        }
        if (ownerPackage != null ? !ownerPackage.equals(template.ownerPackage) : template.ownerPackage != null) {
            return false;
        }
        if (path != null ? !path.equals(template.path) : template.path != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (ownerPackage != null ? ownerPackage.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        return result;
    }

    public int compareTo(FileSystemTemplate template) {
        if (ownerPackage == null) {
            if (template.ownerPackage != null ) {
                return 1;
            } else {
                return key.compareTo(template.key);
            }
        } else {
            if (template.ownerPackage == null ) {
                return -1;
            } else if (!ownerPackage.equals(template.ownerPackage)) {
                return ownerPackage.getName().compareTo(template.ownerPackage.getName());
            } else {
                return key.compareTo(template.key);
            }
        }
    }
}
