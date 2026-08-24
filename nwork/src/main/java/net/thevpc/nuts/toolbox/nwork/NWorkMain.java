package net.thevpc.nuts.toolbox.nwork;

import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.app.NAppRun;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NSession;

public class NWorkMain {

    private WorkspaceService service;

    public static void main(String[] args) {
        NApplication.builder(args).run();
    }

    @NAppRun
    public void run() {
        NSession session = NSession.of();
        this.service = new WorkspaceService(session);
        NCmdLine cmdLine = NApplication.of().cmdLine().commandName("nwork");
        NArg a;
        do {
            if (session.configureFirst(cmdLine)) {
                //
            } else if ((a = cmdLine.next("scan", "s").orNull()) != null) {
                service.scan(cmdLine, session);
                return;
            } else if ((a = cmdLine.next("find", "f").orNull()) != null) {
                service.find(cmdLine, session);
                return;
            } else if ((a = cmdLine.next("status", "t").orNull()) != null) {
                if (a.literalValue().isBoolean()) {
                    service.enableScan(cmdLine, session, a.getBooleanValue().get());
                } else {
                    service.status(cmdLine, session);
                }
                return;
            } else if ((a = cmdLine.next("push").orNull()) != null) {
                if (a.literalValue().isBoolean()) {
                    service.enableScan(cmdLine, session, a.getBooleanValue().get());
                } else {
                    service.push(cmdLine, session);
                }
                return;
            } else if ((a = cmdLine.next("list", "l").orNull()) != null) {
                service.list(cmdLine, session);
                return;
            } else if ((a = cmdLine.next("set").orNull()) != null) {
                service.setWorkspaceConfigParam(cmdLine, session);
                return;
            } else {
                cmdLine.commandName("nwork").throwUnexpectedArgument();
            }
        } while (cmdLine.hasNext());
        cmdLine.throwMissingArgument();
    }

}
