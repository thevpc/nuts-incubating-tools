package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nuts.*;

import net.thevpc.nuts.io.NCp;
import net.thevpc.nuts.io.NDigest;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nserver.util.ItemStreamInfo;
import net.thevpc.nserver.util.MultipartStreamHelper;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.io.InputStream;

public class DeployFacadeCommand extends AbstractFacadeCommand {
    public DeployFacadeCommand() {
        super("deploy");
    }
//            @Override
//            public void execute(FacadeCommandContext context) throws IOException {
//                executeImpl(context);
//            }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        context.requireAuth();
        String boundary = context.getRequestHeader("Content-type").orNull();
        if (NBlankable.isBlank(boundary)) {
            throw new NWebHttpException(
                    NMsg.ofC("invalid Command arguments : %s : invalid format", getName()).toString(),
                    new NMsgCode("INVALID_COMMAND", getName()),
                    NHttpCode.BAD_REQUEST
            );
        }
        MultipartStreamHelper stream = new MultipartStreamHelper(context.getRequestBody(), boundary);
        NDescriptor descriptor = null;
        String receivedContentHash = null;
        InputStream content = null;
        String contentFile = null;
        for (ItemStreamInfo info : stream) {
            String name = info.resolveVarInHeader("Content-Disposition", "name");
            switch (name) {
                case "descriptor":
                    try {
                        descriptor = NDescriptorParser.of()
                                .parse(info.getContent()).get();
                    } finally {
                        info.getContent().close();
                    }
                    break;
                case "content-hash":
                    try {
                        receivedContentHash = NDigest.of().setSource(info.getContent()).computeString();
                    } finally {
                        info.getContent().close();
                    }
                    break;
                case "content":
                    contentFile = NPath
                            .ofTempFile(
                                    NWorkspace.of().getDefaultIdFilename(
                                            descriptor.getId().builder().setFaceDescriptor().build()
                                    )).toString();
                    NCp.of()
                            .setSource(info.getContent())
                            .setTarget(NPath.of(contentFile))
                            .run();
                    break;
            }
        }
        if (contentFile == null) {
            throw new NWebHttpException(
                    NMsg.ofC("invalid Command arguments : %s : missing file", getName()).toString(),
                    new NMsgCode("INVALID_COMMAND", getName()),
                    NHttpCode.BAD_REQUEST
            );
        }
        NId id = NDeployCmd.of().setContent(NPath.of(contentFile))
                .setSha1(receivedContentHash)
                .setDescriptor(descriptor)
                .getResult().get(0);
//                NutsId id = workspace.deploy(content, descriptor, null);
        context.setTextResponse(id.toString()).sendResponse();
    }
}
