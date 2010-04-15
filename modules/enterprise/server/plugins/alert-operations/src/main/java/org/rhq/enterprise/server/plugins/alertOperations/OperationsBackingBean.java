/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.plugins.alertOperations;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for the operations alert sender
 * @author Joseph Marques
 */
public class OperationsBackingBean extends CustomAlertSenderBackingBean {

    private Map<String, String> selectionModeOptions = new LinkedHashMap<String, String>();
    private Map<String, String> ancestorTypeOptions = new LinkedHashMap<String, String>();
    private Map<String, String> descendantTypeOptions = new LinkedHashMap<String, String>();
    private Map<String, String> operationNameOptions = new LinkedHashMap<String, String>();

    private String selectionMode = "";
    private String resourceId;
    private String ancestorTypeId;
    private String descendantName;
    private String descendantTypeId;
    private String operationDefinitionId;

    private String effectiveResourceTypeName;
    private String effectiveResourceTypeId;

    private ConfigurationDefinition argumentsConfigurationDefinition;
    private Configuration argumentsConfiguration;

    private Subject overlord;

    private Subject getOverlord() {
        if (overlord == null) {
            overlord = LookupUtil.getSubjectManager().getOverlord();
        }
        return overlord;
    }

    @Override
    public void loadView() {
        selectionMode = get("selection-mode", "none");

        // always load first list
        selectionModeOptions.put("Self", "self");
        selectionModeOptions.put("Specific Resource", "specific");
        selectionModeOptions.put("Relative Resource", "relative");

        String argumentsConfigurationId = get("operation-arguments-configuration-id", null);
        Configuration previousArguments = null;
        if (argumentsConfigurationId != null && !argumentsConfigurationId.equals("none")) {
            // look it up and then delete it, because the user may switch options in the conditional form, invalidating this
            previousArguments = LookupUtil.getConfigurationManager().getConfiguration(getOverlord(),
                Integer.parseInt(argumentsConfigurationId));
        }

        // load secondList if currentType is selected
        if (selectionMode.equals("none")) {
            return;
        }

        if (selectionMode.equals("specific")) {
            resourceId = get("selection-specific-resource-id", "");
        } else if (selectionMode.equals("relative")) {
            ancestorTypeId = get("selection-relative-ancestor-type-id", "none");
            descendantName = get("selection-relative-descendant-name", "Name (optional)");
            descendantTypeId = get("selection-relative-descendant-type-id", "none");

            ResourceType contextType = computeResourceTypeFromContext(); // should not be null
            List<ResourceType> ancestors = LookupUtil.getResourceTypeManager().getResourceTypeAncestorsWithOperations(
                getOverlord(), contextType.getId());
            load(ancestorTypeOptions, ancestors);

            if (ancestorTypeId.equals("none") == false) {
                List<ResourceType> descendants = LookupUtil.getResourceTypeManager()
                    .getResourceTypeDescendantsWithOperations(getOverlord(), Integer.parseInt(ancestorTypeId));
                load(descendantTypeOptions, descendants);
            } else {
                List<ResourceType> descendants = LookupUtil.getResourceTypeManager()
                    .getResourceTypeDescendantsWithOperations(getOverlord(), contextType.getId());
                load(descendantTypeOptions, descendants);
            }
        }

        // compute effectiveResourceTypeId from given info
        ResourceType type = null;
        if (selectionMode.equals("self")) {
            type = computeResourceTypeFromContext();

        } else if (selectionMode.equals("specific")) {
            if (resourceId.equals("") == false) {
                Resource resource = LookupUtil.getResourceManager().getResource(getOverlord(),
                    Integer.parseInt(resourceId));
                type = resource.getResourceType();
            }

        } else if (selectionMode.equals("relative")) {
            try {
                if (descendantTypeId.equals("none") == false) {
                    type = LookupUtil.getResourceTypeManager().getResourceTypeById(getOverlord(),
                        Integer.parseInt(descendantTypeId));
                }

                if (ancestorTypeId.equals("none") == false) {
                    type = LookupUtil.getResourceTypeManager().getResourceTypeById(getOverlord(),
                        Integer.parseInt(ancestorTypeId));
                }
            } catch (ResourceTypeNotFoundException rtnfe) {
                // debugging message
            }
        }

        if (type == null) {
            return;
        }

        effectiveResourceTypeId = String.valueOf(type.getId());
        effectiveResourceTypeName = type.getName();

        operationDefinitionId = get("operation-definition-id", "none");
        List<OperationDefinition> definitions = LookupUtil.getOperationManager().findSupportedResourceTypeOperations(
            getOverlord(), Integer.valueOf(effectiveResourceTypeId), false);
        for (OperationDefinition nextDefinition : definitions) {
            operationNameOptions.put(nextDefinition.getDisplayName(), String.valueOf(nextDefinition.getId()));
        }

        if (operationDefinitionId.equals("none")) {
            return;
        }

        try {
            OperationDefinition operation = LookupUtil.getOperationManager().getOperationDefinition(getOverlord(),
                Integer.parseInt(operationDefinitionId));
            argumentsConfigurationDefinition = operation.getParametersConfigurationDefinition();

            if (argumentsConfigurationDefinition == null) {
                return;
            }

            if (previousArguments == null) {
                // create it for the first time or if this was previous removed due to switching form options
                Configuration emptyConfiguration = LookupUtil.getConfigurationManager()
                    .getConfigurationFromDefaultTemplate(argumentsConfigurationDefinition);
                argumentsConfiguration = emptyConfiguration.deepCopy(false);
            } else {
                argumentsConfiguration = previousArguments;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load(Map<String, String> resourceTypeOptions, List<ResourceType> types) {
        for (ResourceType nextType : types) {
            resourceTypeOptions.put(nextType.getName(), String.valueOf(nextType.getId()));
        }
    }

    private ResourceType computeResourceTypeFromContext() {
        AlertDefinition definition = LookupUtil.getAlertDefinitionManager().getAlertDefinitionById(getOverlord(),
            Integer.parseInt(contextId));

        ResourceType type = null;
        if (definition.getResource() != null) {
            type = definition.getResource().getResourceType();
        } else if (definition.getResourceGroup() != null) {
            type = definition.getResourceGroup().getResourceType();
        } else {
            type = definition.getResourceType();
        }
        return type;
    }

    private String get(String propertyName, String defaultValue) {
        return alertParameters.getSimpleValue(propertyName, defaultValue);
    }

    @Override
    public void internalCleanup() {
        cleanupPreviousArguments();
    }

    @Override
    public void saveView() {
        set(selectionMode, "selection-mode");
        set(resourceId, "selection-specific-resource-id");
        set(ancestorTypeId, "selection-relative-ancestor-type-id");
        set("Name (optional)".equals(descendantName) ? null : descendantName, "selection-relative-descendant-name");
        set(descendantTypeId, "selection-relative-descendant-type-id");
        set(operationDefinitionId, "operation-definition-id");

        // cleanup previous arguments configuration
        cleanupPreviousArguments();

        // persist new one
        if (operationDefinitionId != null && !operationDefinitionId.equals("none") && argumentsConfiguration != null) {
            argumentsConfiguration = persistConfiguration(argumentsConfiguration);
            set(String.valueOf(argumentsConfiguration.getId()), "operation-arguments-configuration-id");
        }

        alertParameters = persistConfiguration(alertParameters);
    }

    private void cleanupPreviousArguments() {
        String previousArgumentsConfigurationId = get("operation-arguments-configuration-id", null);
        set(null, "operation-arguments-configuration-id");
        if (previousArgumentsConfigurationId != null && !previousArgumentsConfigurationId.equals("none")) {
            LookupUtil.getConfigurationManager().deleteConfigurations(
                Arrays.asList(Integer.parseInt(previousArgumentsConfigurationId)));
        }
    }

    private boolean set(String value, String propertyName) {
        if (value == null) {
            Property previous = alertParameters.remove(propertyName);
            if (previous == null) {
                return false; // removing a non-existence property, no change
            }
            return ((PropertySimple) previous).getStringValue() != null;
        }
        PropertySimple property = alertParameters.getSimple(propertyName);
        if (property == null) {
            property = new PropertySimple(propertyName, value);
            alertParameters.put(property);
            return true; // adding a property that previously didn't exist
        } else {
            String oldStringValue = property.getStringValue();
            property.setStringValue(value);
            return !value.equals(oldStringValue);
        }
    }

    public Map<String, String> getSelectionModeOptions() {
        return selectionModeOptions;
    }

    public Map<String, String> getAncestorTypeOptions() {
        return ancestorTypeOptions;
    }

    public Map<String, String> getDescendantTypeOptions() {
        return descendantTypeOptions;
    }

    public Map<String, String> getOperationNameOptions() {
        return operationNameOptions;
    }

    public String getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(String selectionMode) {
        this.selectionMode = selectionMode;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getAncestorTypeId() {
        return ancestorTypeId;
    }

    public void setAncestorTypeId(String ancestorTypeId) {
        this.ancestorTypeId = ancestorTypeId;
    }

    public String getDescendantName() {
        return descendantName;
    }

    public void setDescendantName(String descendantName) {
        this.descendantName = descendantName;
    }

    public String getDescendantTypeId() {
        return descendantTypeId;
    }

    public void setDescendantTypeId(String descendantTypeId) {
        this.descendantTypeId = descendantTypeId;
    }

    public String getOperationDefinitionId() {
        return operationDefinitionId;
    }

    public void setOperationDefinitionId(String operationDefinitionId) {
        this.operationDefinitionId = operationDefinitionId;
    }

    public String getEffectiveResourceTypeId() {
        return effectiveResourceTypeId;
    }

    public String getEffectiveResourceTypeName() {
        return effectiveResourceTypeName;
    }

    public ConfigurationDefinition getArgumentsConfigurationDefinition() {
        return argumentsConfigurationDefinition;
    }

    public void setArgumentsConfigurationDefinition(ConfigurationDefinition argumentsConfigurationDefinition) {
        this.argumentsConfigurationDefinition = argumentsConfigurationDefinition;
    }

    public Configuration getArgumentsConfiguration() {
        return argumentsConfiguration;
    }

    public void setArgumentsConfiguration(Configuration argumentsConfiguration) {
        this.argumentsConfiguration = argumentsConfiguration;
    }

    public String getNoParametersMessage() {
        return "This operation does not take any parameters.";
    }

    public String getNotInitializedMessage() {
        return "This operation parameters definition has not been initialized.";
    }
}
