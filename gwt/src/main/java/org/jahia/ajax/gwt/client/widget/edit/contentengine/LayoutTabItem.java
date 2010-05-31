package org.jahia.ajax.gwt.client.widget.edit.contentengine;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import org.jahia.ajax.gwt.client.core.BaseAsyncCallback;
import org.jahia.ajax.gwt.client.data.GWTJahiaValueDisplayBean;
import org.jahia.ajax.gwt.client.data.GWTRenderResult;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaItemDefinition;
import org.jahia.ajax.gwt.client.messages.Messages;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: toto
 * Date: Jan 6, 2010
 * Time: 7:52:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class LayoutTabItem extends PropertiesTabItem {
    private LayoutContainer wrapperPanel;
    private LayoutContainer htmlPreview;

    public LayoutTabItem(NodeHolder engine) {
        super(Messages.get("org.jahia.jcr.edit.layout.tab", "Layout"), engine, GWTJahiaItemDefinition.LAYOUT);
        //setIcon(ContentModelIconProvider.CONTENT_ICONS.engineTabLayout());
    }

    @Override
    public void attachPropertiesEditor() {
        if (engine.getNode() != null) {
            final ComboBox<GWTJahiaValueDisplayBean> templateField = (ComboBox<GWTJahiaValueDisplayBean>) propertiesEditor.getFieldsMap().get("j:template");
            final ComboBox<GWTJahiaValueDisplayBean> skinField = (ComboBox<GWTJahiaValueDisplayBean>) propertiesEditor.getFieldsMap().get("j:skin");
            final ComboBox<GWTJahiaValueDisplayBean> subNodesTemplateField = (ComboBox<GWTJahiaValueDisplayBean>) propertiesEditor.getFieldsMap().get("j:subNodesTemplate");
            final SelectionChangedListener<GWTJahiaValueDisplayBean> listener = new SelectionChangedListener<GWTJahiaValueDisplayBean>() {
                public void selectionChanged(SelectionChangedEvent<GWTJahiaValueDisplayBean> se) {
                    Map<String, String> contextParams = new HashMap<String, String>();
                    if (skinField != null && skinField.getValue() != null) {
                        contextParams.put("forcedSkin", skinField.getValue().getValue());
                    }
                    if (subNodesTemplateField != null && subNodesTemplateField.getValue() != null) {
                        contextParams.put("forcedSubNodesTemplate", subNodesTemplateField.getValue().getValue());
                    }
                    updatePreview((templateField != null && templateField.getValue() != null) ? templateField.getValue().getValue() : null, contextParams);
                }
            };
            if (templateField != null) {
                templateField.addSelectionChangedListener(listener);
            }
            if (skinField != null) {
                skinField.addSelectionChangedListener(listener);
            }
            if (subNodesTemplateField != null) {
                subNodesTemplateField.addSelectionChangedListener(listener);
            }


            if (wrapperPanel == null) {
                wrapperPanel = new LayoutContainer(new RowLayout());
                wrapperPanel.setScrollMode(Style.Scroll.AUTO);
                add(wrapperPanel);
            }
            wrapperPanel.add(propertiesEditor);

            htmlPreview = new LayoutContainer(new FitLayout());
            htmlPreview.addStyleName("x-panel");
            htmlPreview.setScrollMode(Style.Scroll.AUTO);
            wrapperPanel.add(htmlPreview);
        }
    }

    /**
     * Update preview
     *
     * @param template
     * @param contextParams
     */
    private void updatePreview(String template, Map<String, String> contextParams) {
        if (engine.getNode() != null) {
            JahiaContentManagementService.App.getInstance().getRenderedContent(engine.getNode().getPath(), null, null, template, "preview", contextParams, false, null, new BaseAsyncCallback<GWTRenderResult>() {
                public void onSuccess(GWTRenderResult result) {
                    HTML html = new HTML(result.getResult());
                    setHTML(html);
                    layout();
                }
            });
        } else {
            setHTML(null);
        }
    }

    /**
     * set preview HTML
     *
     * @param html
     */
    public void setHTML(HTML html) {
        htmlPreview.removeAll();
        if (html != null) {
            htmlPreview.add(html);
        }
        htmlPreview.layout();
    }

}
