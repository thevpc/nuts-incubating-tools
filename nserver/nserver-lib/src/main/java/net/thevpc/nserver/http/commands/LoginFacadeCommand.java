package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NAuthenticationRequest;
import net.thevpc.nhttp.server.api.NLoginResult;
import net.thevpc.nhttp.server.error.NWebUnauthorizedSecurityException;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.util.NOptional;

import java.io.IOException;

public class LoginFacadeCommand extends AbstractFacadeCommand {
    public LoginFacadeCommand() {
        super("login");
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }


    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        LoginRequest r = context.getRequestBodyAs(LoginRequest.class);
        String apiKey = context.getApiKeyRequestHeader().orNull();
        String realm = context.getRealmRequestHeader().orNull();
        NLoginResult u = context.authenticateWithCredentials(
                new NAuthenticationRequest()
                        .setUserName(r.username)
                        .setPassword(r.password)
                        .setApiKey(apiKey)
                        .setRealm(realm)
        );
        if (u != null) {
            context.setJsonResponse(u).sendResponse();
            return;
        }
        throw new NWebUnauthorizedSecurityException(new NMsgCode("INVALID_USERNAME_OR_PASSWORD"), "Invalid username or password");
    }
}
