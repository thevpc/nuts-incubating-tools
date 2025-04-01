package net.thevpc.nserver.http;

import net.thevpc.nhttp.server.api.DefaultNWebUser;
import net.thevpc.nhttp.server.api.NAuthenticationRequest;
import net.thevpc.nhttp.server.api.NWebUser;
import net.thevpc.nhttp.server.api.NWebUserResolver;
import net.thevpc.nhttp.server.impl.UsersConfigFileParser;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NAssert;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NStringUtils;

import java.util.Objects;

public class FileBasedNWebUserResolver implements NWebUserResolver {
    private UsersConfigFileParser serverConfigFileParser;

    @Override
    public NWebUser loadUser(String userName) {
        userName = NStringUtils.trim(userName);
        if (NBlankable.isBlank(userName)) {
            return null;
        }
        if (serverConfigFileParser == null) {
            serverConfigFileParser = new UsersConfigFileParser(NPath.of("server-config.tson"));
        }
        NWebUser u = serverConfigFileParser.users().get(userName);
        if (u != null) {
            String _userId = u.getUserId();
            String _userName = u.getUserName();
            String password = ((DefaultNWebUser) u).getPassword();
            _userId=NStringUtils.trim(NStringUtils.firstNonBlank(_userId,_userName));
            _userName=NStringUtils.trim(NStringUtils.firstNonBlank(_userName,_userId));
            NAssert.requireNonBlank(_userId,"userId");
            NAssert.requireNonBlank(_userName,"userName");
            return new DefaultNWebUser(_userId, _userName, password);
        }
        return null;
    }

    @Override
    public NWebUser loadUserAndAuthenticate(NAuthenticationRequest request) {
        NWebUser w = loadUser(request.getUserName());
        if (w instanceof DefaultNWebUser) {
            String pwd = ((DefaultNWebUser) w).getPassword();
            if (Objects.equals(request.getPassword(), pwd)) {
                return w;
            }
        }
        return null;
    }
}
