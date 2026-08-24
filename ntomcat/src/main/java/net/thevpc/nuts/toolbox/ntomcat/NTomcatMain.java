package net.thevpc.nuts.toolbox.ntomcat;

import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.app.NAppRun;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.toolbox.ntomcat.remote.RemoteTomcat;
import net.thevpc.nuts.toolbox.ntomcat.local.LocalTomcat;

public class NTomcatMain  {

    public static void main(String[] args) {
        NApplication.builder(args).run();
    }

    @NAppRun
    public void run() {
//        NRepository apacheRepo = NWorkspace.of().findRepository("apache-tomcat").orNull();
        NSession session = NSession.of();
//        if (apacheRepo == null) {
//            NWorkspace.of().addRepository(
//                    new NAddRepositoryOptions()
//                            .setRepositoryModel(new ApacheTomcatRepositoryModel())
//                            .setTemporary(true)
//
//            );
//        }
        NCmdLine cmdLine = NApplication.of().cmdLine();
        Boolean local = null;
        boolean skipFirst = false;
        if (cmdLine.hasNext()) {
            NArg a = cmdLine.peek().get();
            String s = a.asString().orElse("");
            if ((s.equals   ("--remote") || s.equals("-r"))) {
                cmdLine.skip();
                local = false;
            } else if ((s.equals("--local") || s.equals("-l"))) {
                cmdLine.skip();
                local = true;
            }
        }
        if (local == null) {
            local = true;
        }
        if (local) {
            LocalTomcat m = new LocalTomcat(cmdLine);
            m.runArgs();
            session.flush();
        } else {
            RemoteTomcat m = new RemoteTomcat(session, cmdLine);
            m.runArgs();
            session.flush();
        }
    }

}
