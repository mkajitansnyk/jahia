/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.taglibs.facet;

import org.apache.commons.collections.KeyValue;
import org.apache.commons.collections.keyvalue.DefaultKeyValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * Functions Tester.
 *
 * @author Christophe Laprun
 * @since <pre>Oct 17, 2013</pre>
 */
@RunWith(JUnit4.class)
public class FunctionsTest {

    /**
     * Method: getDeleteFacetUrl(Object facetFilterObj, KeyValue facetValue, String queryString)
     */
    @Test
    public void testGetDeleteFacetUrl() throws Exception {
        String query = "j:tags###4c1b0348-89d0-461e-b31d-d725b8e6ea18###3056820\\:FACET\\:tags:4c1b0348\\-89d0\\-461e\\-b31d\\-d725b8e6ea18|||j:tags###cdc62535-bcac-44d7-b4da-be8c865d7a58###3056820\\:FACET\\:tags:cdc62535\\-bcac\\-44d7\\-b4da\\-be8c865d7a58";
        KeyValue facetValue1 = new DefaultKeyValue("4c1b0348-89d0-461e-b31d-d725b8e6ea18", "3056820\\:FACET\\:tags:4c1b0348\\-89d0\\-461e\\-b31d\\-d725b8e6ea18");
        KeyValue facetValue2 = new DefaultKeyValue("cdc62535-bcac-44d7-b4da-be8c865d7a58", "3056820\\:FACET\\:tags:cdc62535\\-bcac\\-44d7\\-b4da\\-be8c865d7a58");


        assertEquals("j:tags###cdc62535-bcac-44d7-b4da-be8c865d7a58###3056820\\:FACET\\:tags:cdc62535\\-bcac\\-44d7\\-b4da\\-be8c865d7a58", Functions.getDeleteFacetUrl(facetValue1, query));
        assertEquals("j:tags###4c1b0348-89d0-461e-b31d-d725b8e6ea18###3056820\\:FACET\\:tags:4c1b0348\\-89d0\\-461e\\-b31d\\-d725b8e6ea18", Functions.getDeleteFacetUrl(facetValue2, query));
    }

} 
