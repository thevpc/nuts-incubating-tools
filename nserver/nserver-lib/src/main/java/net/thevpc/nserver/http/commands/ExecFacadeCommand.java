package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.FormDataItem;
import net.thevpc.nuts.NExecCmd;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.*;
import net.thevpc.nuts.NSession;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ExecFacadeCommand extends AbstractFacadeCommand {
    public ExecFacadeCommand() {
        super("exec");
    }

    public static class CommandRequest {
        public String command;
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        context.requireAuth();
        CommandRequest r = new CommandRequest();
        r.command = context.getFormaData("command").get().getSource().readString();
        FormDataItem in = context.getFormaData("in").orNull();
        List<String> stringList = NCmdLine.parseDefault(r.command).get().toStringList();
        NSession session = NSession.of().copy();
        NMemoryPrintStream out = NPrintStream.ofMem(NTerminalMode.FILTERED);
        NMemoryPrintStream err = out; // NPrintStream.ofMem()
        if (in != null) {
            session.setTerminal(NTerminal.of(
                    in.getSource().getInputStream(),
                    out,
                    err
            ));
            stringList.addAll(0, Arrays.asList("nsh", "-s"));
            int result = session.callWith(() -> NExecCmd.of()
                    .addCommand(stringList)
                    .getResultCode());
            context.setTextResponse(result + "\n" + session.out().toString())
                    .sendResponse();
        } else {
            session.setTerminal(NTerminal.of(
                    new ByteArrayInputStream(new byte[0]),
                    out,
                    err
            ));
            int result = session.callWith(() -> NExecCmd.of()
                    .addCommand(stringList)
                    .getResultCode());
            context.setTextResponse(result + "\n" + session.out().toString())
                    .sendResponse();
        }
    }
}
