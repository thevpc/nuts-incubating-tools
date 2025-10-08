package net.thevpc.nuts.toolbox.ntomcat.local;

import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPrintStream;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.text.NTextBuilder;
import net.thevpc.nuts.text.NTextStyle;
import net.thevpc.nuts.toolbox.ntomcat.local.config.LocalTomcatDomainConfig;
import net.thevpc.nuts.text.NMsg;

public class LocalTomcatDomainConfigService extends LocalTomcatServiceBase {

    private String name;
    private LocalTomcatDomainConfig config;
    private LocalTomcatConfigService tomcat;

    public LocalTomcatDomainConfigService(String name, LocalTomcatDomainConfig config, LocalTomcatConfigService tomcat) {
        this.config = config;
        this.tomcat = tomcat;
        this.name = name;
    }

    public LocalTomcatDomainConfig getConfig() {
        return config;
    }

    public LocalTomcatConfigService getTomcat() {
        return tomcat;
    }

    public String getName() {
        return name;
    }

    public NPath getDomainDeployPath() {
        NPath b = tomcat.getCatalinaBase();
        if (b == null) {
            b = tomcat.getCatalinaHome();
        }
        NPath p = config.getDeployPath()==null ?null: NPath.of(config.getDeployPath());
        if (p == null) {
            p = tomcat.getDefaulDeployFolder(name);
        }
        return b.resolve(b);
    }

    public LocalTomcatDomainConfigService remove() {
        tomcat.getConfig().getDomains().remove(name);
        for (LocalTomcatAppConfigService aa : tomcat.getApps()) {
            if (name.equals(aa.getConfig().getDomain())) {
                aa.remove();
            }
        }
        NOut.println(NMsg.ofC("%s domain removed.", getBracketsPrefix(name)));
        return this;
    }
    public NText getBracketsPrefix(String str) {
        return NTextBuilder.of()
                .append("[")
                .append(str, NTextStyle.primary5())
                .append("]");
    }

    public LocalTomcatDomainConfigService print(NPrintStream out) {
        NElementWriter.ofJson().write(getConfig(),out);
        return this;
    }

}
