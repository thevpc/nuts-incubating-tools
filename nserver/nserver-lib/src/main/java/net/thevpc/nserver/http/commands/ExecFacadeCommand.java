package net.thevpc.nserver.http.commands;

import net.thevpc.nuts.NExecCmd;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.*;
import net.thevpc.nuts.NSession;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ExecFacadeCommand extends AbstractFacadeCommand {
    public ExecFacadeCommand() {
        super("exec");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {

//                String boundary = context.getRequestHeaderFirstValue("Content-type");
//                if (StringUtils.isEmpty(boundary)) {
//                    context.sendError(400, "Invalid NShellCommandNode Arguments : " + getName() + " . Invalid format.");
//                    return;
//                }
//                MultipartStreamHelper stream = new MultipartStreamHelper(context.getRequestBody(), boundary);
//                NutsDescriptor descriptor = null;
//                String receivedContentHash = null;
//                InputStream content = null;
//                File contentFile = null;
//                for (ItemStreamInfo info : stream) {
//                    String name = info.resolveVarInHeader("Content-Disposition", "name");
//                    switch (name) {
//                        case "descriptor":
//                            descriptor = CoreNutsUtils.parseNutsDescriptor(info.getContent(), true);
//                            break;
//                        case "content-hash":
//                            receivedContentHash = CoreSecurityUtils.evalSHA1(info.getContent(), true);
//                            break;
//                        case "content":
//                            contentFile = CoreIOUtils.createTempFile(descriptor, false);
//                            CoreIOUtils.copy(info.getContent(), contentFile, true, true);
//                            break;
//                    }
//                }
//                if (contentFile == null) {
//                    context.sendError(400, "Invalid NShellCommandNode Arguments : " + getName() + " : Missing File");
//                }
        NCmdLine cmd = NCmdLine.parseDefault(context.getRequestBodyAsString()).get();
        NSession session = NSession.of().copy();
        NMemoryPrintStream out = NPrintStream.ofMem(NTerminalMode.FILTERED);
        NMemoryPrintStream err = out; // NPrintStream.ofMem()
        session.setTerminal(NTerminal.of(
                new ByteArrayInputStream(new byte[0]),
                out,
                err
        ));
        int result = session.callWith(() -> NExecCmd.of()
                .addCommand(cmd.toStringList())
                .getResultCode());
        context.setTextResponse(result + "\n" + session.out().toString())
                .sendResponse();
    }
}
