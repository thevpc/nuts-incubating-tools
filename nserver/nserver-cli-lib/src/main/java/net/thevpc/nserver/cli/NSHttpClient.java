package net.thevpc.nserver.cli;

import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NStringUtils;
import net.thevpc.nuts.web.NWebCli;
import net.thevpc.nuts.web.NWebRequest;

public class NSHttpClient {
    private String url;
    private String login;
    private String password;

    public String getUrl() {
        return url;
    }

    public NSHttpClient setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public NSHttpClient setLogin(String login) {
        this.login = login;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public NSHttpClient setPassword(String password) {
        this.password = password;
        return this;
    }

    public String version() {
        return NWebCli.of().POST(resolveUrl("version"))
                .doWith(this::prepareSecurity)
                .run().getContentAsString();
    }

    private void prepareSecurity(NWebRequest r) {
        r.addParameter(NBlankable.isBlank(login) ? null : "ul", login)
                .addParameter((NBlankable.isBlank(login) || NBlankable.isBlank(password)) ? null : "up", password);
    }

    private String resolveUrl(String extra) {
        String url = this.url;
        if (NBlankable.isBlank(url)) {
            url = "localhost:8899";
        }
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        if (NBlankable.isBlank(extra)) {
            url = NStringUtils.pjoin(
                    "/",
                    url,
                    extra);
        }
        return url;
    }
}
