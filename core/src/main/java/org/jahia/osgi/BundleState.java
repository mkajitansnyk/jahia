/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.osgi;

import java.util.HashMap;

import org.osgi.framework.Bundle;

/**
 * OSGi bundle states.
 */
public enum BundleState {

    UNINSTALLED(Bundle.UNINSTALLED),
    INSTALLED(Bundle.INSTALLED),
    RESOLVED(Bundle.RESOLVED),
    STARTING(Bundle.STARTING),
    STOPPING(Bundle.STOPPING),
    ACTIVE(Bundle.ACTIVE);

    int value;

    private static final HashMap<Integer, BundleState> STATE_BY_VALUE = new HashMap<Integer, BundleState>();
    static {
        for (BundleState state : BundleState.values()) {
            STATE_BY_VALUE.put(state.toInt(), state);
        }
    }

    private BundleState(int value) {
        this.value = value;
    }

    /**
     * @return org.osgi.framework.Bundle state value corresponding to this bundle state
     */
    public int toInt() {
        return value;
    }

    /**
     * Get bundle state corresponding to an org.osgi.framework.Bundle state value.
     *
     * @param value org.osgi.framework.Bundle state constant value
     * @return Corresponding bundle state
     */
    public static BundleState fromInt(int value) {
        BundleState state = STATE_BY_VALUE.get(value);
        if (state == null) {
            throw new IllegalArgumentException(String.format("Unknown bundle state value: %s", value));
        }
        return state;
    }
}
