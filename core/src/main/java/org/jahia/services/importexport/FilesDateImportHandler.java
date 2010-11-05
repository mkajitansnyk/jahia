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

package org.jahia.services.importexport;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created by IntelliJ IDEA.
 * User: toto
 * Date: May 26, 2008
 * Time: 10:59:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class FilesDateImportHandler extends DefaultHandler {
    
    private static final transient Logger logger = LoggerFactory.getLogger(FilesDateImportHandler.class);
    
    private Map<String, Date> fileToDate = new HashMap<String, Date>();
    private DateFormat df = new SimpleDateFormat(ImportExportService.DATE_FORMAT);

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String value = attributes.getValue("jahia:lastModification");
        if (value != null) {
            try {
                fileToDate.put(attributes.getValue("jahia:path"), df.parse(value));
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public Map<String, Date> getFileToDate() {
        return fileToDate;
    }
}
