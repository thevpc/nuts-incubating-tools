/**
 * ====================================================================
 * Nuts : Network Updatable Things Service
 * (universal package manager)
 * <br>
 * is a new Open Source Package Manager to help install packages
 * and libraries for runtime execution. Nuts is the ultimate companion for
 * maven (and other build managers) as it helps installing all package
 * dependencies at runtime. Nuts is not tied to java and is a good choice
 * to share shell scripts and other 'things' . Its based on an extensible
 * architecture to help supporting a large range of sub managers / repositories.
 *
 * <br>
 * <p>
 * Copyright [2020] [thevpc]
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3 (the "License");
 * you may  not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <br>
 * ====================================================================
 */
package net.thevpc.nserver;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.NWorkspaceSecurityManager;

/**
 * Created by vpc on 1/24/17.
 */
public abstract class AbstractFacadeCommand implements FacadeCommand {

    private String name;

    public AbstractFacadeCommand(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute(FacadeCommandContext context) throws IOException, LoginException {
        Map<String, List<String>> parameters = context.getParameters();
        List<String> ulList = parameters.get("ul");
        String userLogin = (ulList == null || ulList.isEmpty()) ? null : ulList.get(0);
        List<String> upList = parameters.get("up");
        String userPasswordS = (upList == null || upList.isEmpty()) ? null : upList.get(0);

        char[] userPassword = userPasswordS == null ? null : userPasswordS.toCharArray();
        NWorkspaceSecurityManager secu = NWorkspaceSecurityManager.of();
        userLogin = userLogin == null ? null : new String(secu.getCredentials(userLogin.toCharArray()));
        userPassword = userPassword == null ? null : secu.getCredentials(userPassword);
        if (!NBlankable.isBlank(userLogin)) {
            boolean loggedId = false;
            try {
                NWorkspaceSecurityManager.of().login(userLogin, userPassword);
                loggedId = true;
                executeImpl(context);
            } finally {
                if (loggedId) {
                    NWorkspaceSecurityManager.of().logout();
                }
            }
        } else {
            executeImpl(context);
        }
    }

    public abstract void executeImpl(FacadeCommandContext context) throws IOException, LoginException;
}
