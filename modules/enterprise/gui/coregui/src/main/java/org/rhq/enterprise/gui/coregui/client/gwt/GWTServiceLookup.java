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
package org.rhq.enterprise.gui.coregui.client.gwt;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.util.rpc.MonitoringRequestCallback;

/**
 * This lookup service retrieves each RPC service and sets a
 * custom RpcRequestBuilder that adds the login session id to
 * be security checked on the server.
 *
 * @author Greg Hinkle
 */
public class GWTServiceLookup {

    public static AlertDefinitionGWTServiceAsync getAlertDefinitionService() {
        return secure(AlertDefinitionGWTServiceAsync.Util.getInstance());
    }

    public static AlertTemplateGWTServiceAsync getAlertTemplateService() {
        return secure(AlertTemplateGWTServiceAsync.Util.getInstance());
    }

    public static GroupAlertDefinitionGWTServiceAsync getGroupAlertDefinitionService() {
        return secure(GroupAlertDefinitionGWTServiceAsync.Util.getInstance());
    }

    public static ConfigurationGWTServiceAsync getConfigurationService() {
        return secure(ConfigurationGWTServiceAsync.Util.getInstance());
    }

    public static ResourceGWTServiceAsync getResourceService() {
        return secure(ResourceGWTServiceAsync.Util.getInstance());
    }

    public static ResourceGroupGWTServiceAsync getResourceGroupService() {
        return secure(ResourceGroupGWTServiceAsync.Util.getInstance());
    }

    public static ResourceTypeGWTServiceAsync getResourceTypeGWTService() {
        return secure(ResourceTypeGWTServiceAsync.Util.getInstance());
    }

    public static RoleGWTServiceAsync getRoleService() {
        return secure(RoleGWTServiceAsync.Util.getInstance());
    }

    public static SubjectGWTServiceAsync getSubjectService() {
        return secure(SubjectGWTServiceAsync.Util.getInstance());
    }

    public static SystemGWTServiceAsync getSystemService() {
        return secure(SystemGWTServiceAsync.Util.getInstance());
    }

    public static MeasurementDataGWTServiceAsync getMeasurementDataService() {
        return secure(MeasurementDataGWTServiceAsync.Util.getInstance());
    }

    public static AlertGWTServiceAsync getAlertService() {
        return secure(AlertGWTServiceAsync.Util.getInstance());
    }

    public static OperationGWTServiceAsync getOperationService() {
        return secure(OperationGWTServiceAsync.Util.getInstance());
    }

    public static BundleGWTServiceAsync getBundleService() {
        return secure(BundleGWTServiceAsync.Util.getInstance());
    }

    public static ResourceBossGWTServiceAsync getResourceBossService() {
        return secure(ResourceBossGWTServiceAsync.Util.getInstance());
    }

    public static AuthorizationGWTServiceAsync getAuthorizationService() {
        return secure(AuthorizationGWTServiceAsync.Util.getInstance());
    }

    public static AvailabilityGWTServiceAsync getAvailabilityService() {
        return secure(AvailabilityGWTServiceAsync.Util.getInstance());
    }

    public static TagGWTServiceAsync getTagService() {
        return secure(TagGWTServiceAsync.Util.getInstance());
    }

    public static RemoteInstallGWTServiceAsync getRemoteInstallService() {
        return secure(RemoteInstallGWTServiceAsync.Util.getInstance());
    }

    public static RepoGWTServiceAsync getRepoService() {
        return secure(RepoGWTServiceAsync.Util.getInstance());
    }

    public static ContentGWTServiceAsync getContentService() {
        return secure(ContentGWTServiceAsync.Util.getInstance());
    }

    public static SearchGWTServiceAsync getSearchService() {
        return secure(SearchGWTServiceAsync.Util.getInstance());
    }

    public static DashboardGWTServiceAsync getDashboardService() {
        return secure(DashboardGWTServiceAsync.Util.getInstance());
    }

    public static EventGWTServiceAsync getEventService() {
        return secure(EventGWTServiceAsync.Util.getInstance());
    }

    public static ClusterGWTServiceAsync getClusterService() {
        return secure(ClusterGWTServiceAsync.Util.getInstance());
    }

    @SuppressWarnings("unchecked")
    private static <T> T secure(Object sdt) {
        if (!(sdt instanceof ServiceDefTarget))
            return null;

        ((ServiceDefTarget) sdt).setRpcRequestBuilder(new SessionRpcRequestBuilder());

        return (T) sdt;
    }

    public static class SessionRpcRequestBuilder extends RpcRequestBuilder {

        private static int DEBUG_RPC_TIMEOUT = 60000;
        private static int RPC_TIMEOUT = 15000;

        @Override
        protected RequestBuilder doCreate(String serviceEntryPoint) {
            RequestBuilder rb = super.doCreate(serviceEntryPoint);

            if (CoreGUI.isDebugMode()) {
                // debug mode is slow, so give requests more time to complete otherwise you'll get
                // weird exceptions whose messages are extremely unhelpful in finding root cause
                rb.setTimeoutMillis(DEBUG_RPC_TIMEOUT);
            } else {
                rb.setTimeoutMillis(RPC_TIMEOUT);
            }

            // TODO: alter callback handlers to capture timeout failure and retry (at least once)
            //       to add resilience to GWT service calls
            rb.setCallback(new MonitoringRequestCallback(determineName(), rb.getCallback()));

            String sessionId = UserSessionManager.getSessionId();
            if (sessionId != null) {
                Log.debug("SessionRpcRequestBuilder is adding sessionId to request: " + sessionId);
                rb.setHeader(UserSessionManager.SESSION_NAME, sessionId);
            } else {
                Log.error("SessionRpcRequestBuilder built without a value for " + UserSessionManager.SESSION_NAME);
            }

            return rb;
        }

        public String determineName() {
            if (!GWT.isScript()) {
                // expensive name calculation only in dev-mode
                Exception e = new Exception();

                StackTraceElement[] stack = e.getStackTrace();
                // Skip the first two stack elements to get to the proxy calling
                for (int i = 2; i < stack.length; i++) {
                    StackTraceElement ste = stack[i];
                    // e.g. "org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTService_Proxy.findResourcesByCriteria(ResourceGWTService_Proxy.java:36)"
                    if (ste.getClassName().startsWith("org.rhq.enterprise.gui.coregui.client.gwt")) {
                        return ste.getClassName().substring(ste.getClassName().lastIndexOf(".") + 1) + "."
                            + ste.getMethodName();
                    }
                }
                return "unknown";
            } else {
                return "production";
            }
        }
    }

}
