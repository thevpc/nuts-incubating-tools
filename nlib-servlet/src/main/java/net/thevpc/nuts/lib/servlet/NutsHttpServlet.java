/**
 * ====================================================================
 * Nuts : Network Updatable Things Service
 * (universal package manager)
 * <br>
 * is a new Open Source Package Manager to help install packages and libraries
 * for runtime execution. Nuts is the ultimate companion for maven (and other
 * build managers) as it helps installing all package dependencies at runtime.
 * Nuts is not tied to java and is a good choice to share shell scripts and
 * other 'things' . Its based on an extensible architecture to help supporting a
 * large range of sub managers / repositories.
 * <br>
 * <p>
 * Copyright [2020] [thevpc] Licensed under the GNU LESSER GENERAL PUBLIC
 * LICENSE Version 3 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * https://www.gnu.org/licenses/lgpl-3.0.en.html Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <br> ====================================================================
 */
package net.thevpc.nuts.lib.servlet;

import net.thevpc.nuts.*;
import net.thevpc.nserver.AdminServerConfig;
import net.thevpc.nserver.DefaultNWorkspaceServerManager;
import net.thevpc.nserver.NServer;
import net.thevpc.nserver.http.NHttpServletFacade;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NStringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by vpc on 1/7/17.
 */
public class NutsHttpServlet extends HttpServlet {

    public static final String DEFAULT_HTTP_SERVER = "nuts-http-server";
    public static final int DEFAULT_HTTP_SERVER_PORT = 8899;

    private static final Logger LOG = Logger.getLogger(NutsHttpServlet.class.getName());
    private NHttpServletFacade facade;
    private String serverId = "";
    private String workspaceLocation = null;
    private NId runtimeId = null;
    private int adminServerPort = -1;
    private Map<String, String> workspaces = new HashMap<>();
    private boolean adminServer = true;
    private NServer adminServerRef;

    public static int parseInt(String v1, int defaultValue) {
        try {
            if (NBlankable.isBlank(v1)) {
                return defaultValue;
            }
            return Integer.parseInt(NStringUtils.trim(v1));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (adminServerRef != null) {
            try {
                adminServerRef.stop();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Unable to stop admin server", ex);
            }
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Starting Nuts Http Server at url http://<your-server>" + config.getServletContext().getContextPath() + "/service");
        }
        if (adminServer) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Starting Nuts admin Server at <localhost>:" + (adminServerPort < 0 ? DEFAULT_HTTP_SERVER_PORT : adminServerPort));
            }
        }
        adminServerPort = parseInt(config.getInitParameter("nuts-admin-server-port"), -1);
        workspaceLocation = config.getInitParameter("nuts-workspace-location");
        runtimeId = NId.get(config.getInitParameter("nuts-runtime-id")).orNull();
        adminServer = Boolean.valueOf(config.getInitParameter("nuts-admin"));
        try {
            String s = config.getInitParameter("nuts-workspaces-map");
            if (s == null) {
                s = "";
            }
            workspaces = new HashMap<>();
            for (String s1 : s.split("[\n;]")) {
                s1 = s1.trim();
                if (s1.startsWith("#") || s1.isEmpty() || !s1.contains("=")) {
                    //ignore
                } else {
                    String[] kv = s1.split("=");
                    workspaces.put(
                            kv[0].trim(),
                            kv[1].trim()
                    );
                }
            }
        } catch (Exception e) {
            //
        }
        if (workspaces == null) {
            workspaces = new LinkedHashMap<>();
        }
        super.init(config);
        config.getServletContext().setAttribute(NHttpServletFacade.class.getName(), facade);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Map<String, NWorkspace> workspacesByLocation = new HashMap<>();
        Map<String, NWorkspace> workspacesByWebContextPath = new HashMap<>();
        NWorkspace ws = Nuts.openWorkspace(
                NWorkspaceOptionsBuilder.of()
                        .setRuntimeId(runtimeId)
                        .setWorkspace(workspaceLocation)
                        .setOpenMode(NOpenMode.OPEN_OR_CREATE)
                        .setArchetype("server")
                        .build()
        );
        DefaultNWorkspaceServerManager serverManager = new DefaultNWorkspaceServerManager(ws);
        if (workspaces.isEmpty()) {
            String wl = workspaceLocation == null ? "" : workspaceLocation;
            workspaces.put("", wl);
            workspacesByLocation.put(wl, ws);
        }
        for (Map.Entry<String, String> w : workspaces.entrySet()) {
            String webContext = w.getKey();
            String location = w.getValue();
            if (location == null) {
                location = "";
            }
            NWorkspace ws2 = workspacesByLocation.get(location);
            if (ws2 == null) {
                ws2 = Nuts.openWorkspace(
                        NWorkspaceOptionsBuilder.of()
                                .setRuntimeId(runtimeId)
                                .setWorkspace(location)
                                .setOpenMode(NOpenMode.OPEN_OR_CREATE)
                                .setArchetype("server")
                                .build()
                );
                workspacesByLocation.put(location, ws2);
            }
            workspacesByWebContextPath.put(webContext, ws2);
        }

        if (NBlankable.isBlank(serverId)) {
            String serverName = DEFAULT_HTTP_SERVER;
            try {
                serverName = InetAddress.getLocalHost().getHostName();
                if (serverName != null && serverName.length() > 0) {
                    serverName = "nuts-" + serverName;
                }
            } catch (Exception e) {
                //
            }
            if (serverName == null) {
                serverName = DEFAULT_HTTP_SERVER;
            }

            serverId = serverName;
        }

        this.facade = new NHttpServletFacade(serverId, workspacesByWebContextPath);
        if (adminServer) {
            try {
                AdminServerConfig serverConfig = new AdminServerConfig();
                serverConfig.setPort(adminServerPort);
                adminServerRef = serverManager.startServer(serverConfig);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "unable to start admin server", ex);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp);
    }

    protected void doService(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        facade.execute(new ServletNHttpServletFacadeContext(req, resp));
    }

}
