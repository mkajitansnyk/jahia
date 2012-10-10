/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.ajax.gwt.client.widget.ckeditor;

import org.jahia.ajax.gwt.client.util.security.PermissionsUtils;
import org.jahia.ajax.gwt.client.widget.form.CKEditorField;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.widget.Component;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * User: ktlili
 * Date: Nov 25, 2009
 * Time: 2:11:47 PM
 */
public class CKEditor extends Component {
    private String instanceId;
    private CKEditorConfig config;
    private boolean isLoaded;
    private boolean isDetached = false;
	private CKEditorField field;
    private boolean focus=false;

    public CKEditor(CKEditorConfig config, CKEditorField field) {
    	super();
        if (config == null) {
            config = new CKEditorConfig();
            String toolbar = "Light";
            if (PermissionsUtils.isPermitted("wysiwyg-editor-toolbar/full") || PermissionsUtils.isPermitted("studioModeAccess")) {
                toolbar = "Full";
            } else if (PermissionsUtils.isPermitted("wysiwyg-editor-toolbar/basic")) {
                toolbar = "Basic";
            }
            config.setDefaultToolbar(toolbar);
        }
        this.config = config;
        this.field = field;
    }

    @Override
    protected void onRender(Element target, int index) {
        Element ele = DOM.createTextArea();
        DOM.setElementAttribute(ele, "width", config.getWidth());
        DOM.setElementAttribute(ele, "height", config.getHeight());
        setElement(ele, target, index);
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
		        instanceId = getElement().getId();
		        DOM.setElementAttribute(getElement(), "name", instanceId);
                destroyEditor();
                initEditor();
                isLoaded = true;
                field.afterCKEditorInstanceReady();
                if (focus) {
                    focusCK();
                }
			}
		});

        super.onRender(target, index);
    }

    @Override
    protected void onDetach() {
        destroyEditor();
        isDetached = true;
        super.onDetach();
    }

    @Override
    protected void onAttach() {
        // added to a gwt panel, not rendered
        if (isDetached) {
            initEditor();
            isDetached = false;
        }
        super.onAttach();
    }

    /**
     * Set html content
     *
     * @param html
     */
    public void setData(final String html) {
    	if (isLoaded) {
    		_setCKData(html);
    	}
    }

    /**
     * Get html content
     *
     * @return
     */
    public String getData() {
        if (isLoaded) {
            return getCKData();
        }
        Log.error("cKeditor is not yet loaded. getData() returns null value");
        return null;
    }
    
    /**
     * Clear
     */
    public void clear() {
        setData(null);
    }


    /**
     * return is dirty
     *
     * @return
     */
    public boolean isDirty() {
        try {
            return checkDirty();
        } catch (Exception e) {
            Log.error("Error calling checkDirty() on CKEditor instance.Cause: " + e.getMessage(), e);
            return false;
        }
    }


    /**
     * Native method that set the HTML of the CKEditor
     *
     * @param html
     */
    private native void _setCKData(String html)/*-{
        $wnd.CKEDITOR.instances[this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::instanceId].setData(html);
      }-*/;

    /**
     * Native method to get the HTML of the CKEditor
     *
     * @return
     */
    private native String getCKData()/*-{
        return $wnd.CKEDITOR.instances[this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::instanceId].getData();
      }-*/;


    @Override
    public void focus() {
        focus=true;
    }

    public void onFocus() {
        field.onFocus();
    }
    public void onBlur() {
        field.onBlur();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void checkWCAGCompliance(String editorId) {
        CKEditorField fld = CKEditorField.getInstance(editorId);
        if (fld != null) {
            fld.checkWCAGCompliance();
        }
    }

    /**
     * init editior
     *
     * @return
     */

    private native boolean initEditor()/*-{
        var config = this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::config;
        var cfg = {};
        if ((typeof $wnd.CKEDITOR.customConfig) != 'undefined') {
            $wnd.CKEDITOR.tools.extend(cfg,  $wnd.CKEDITOR.customConfig, true);
        } 
        eval("var overrideOptions=" + config.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditorConfig::toString()());
        if ((typeof cfg.toolbar == 'undefined') && (typeof overrideOptions.toolbar == 'undefined') && (typeof overrideOptions.defaultToolbar != 'undefined')) {
            overrideOptions.toolbar = overrideOptions.defaultToolbar;
        } 
        $wnd.CKEDITOR.tools.extend(cfg, overrideOptions, true);
        var oCKeditor = new $wnd.CKEDITOR.replace(this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::instanceId, cfg);
        oCKeditor.checkWCAGCompliance = this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::checkWCAGCompliance(Ljava/lang/String;);
        thisck = this;
        oCKeditor.on('blur', function(event) {
            thisck.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::onBlur()()
        });
        oCKeditor.on('focus', function(event) {
            thisck.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::onFocus()()
        });

        return true;
      }-*/;

    private native void destroyEditor()/*-{
        var instance = $wnd.CKEDITOR.instances[this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::instanceId];
        if(instance) {
            instance.destroy();
            $wnd.CKEDITOR.remove(instance);
        }
    }-*/;

    /**
     * Check dirty
     *
     * @return
     */
    private native boolean checkDirty()/*-{
        return $wnd.CKEDITOR.instances[this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::instanceId].checkDirty();
      }-*/;

    private native void focusCK()/*-{
        var instance = $wnd.CKEDITOR.instances[this.@org.jahia.ajax.gwt.client.widget.ckeditor.CKEditor::instanceId];
        if (instance)  {
            instance.config.startupFocus = true;

        }
    }-*/;

}
