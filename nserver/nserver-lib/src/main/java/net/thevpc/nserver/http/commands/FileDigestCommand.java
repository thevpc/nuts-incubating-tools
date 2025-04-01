package net.thevpc.nserver.http.commands;

import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMapBuilder;

import java.io.IOException;

public class FileDigestCommand extends AbstractFacadeCommand {
    public FileDigestCommand() {
        super("file-digest");
    }

    public static class GetFileRequest {
        public String path;
        public String algo;
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        context.requireAuth();
        GetFileRequest b = context.getRequestBodyAs(GetFileRequest.class, NContentType.JSON);
        if (b == null || NBlankable.isBlank(b.path)) {
            context.setJsonResponse(
                    NMapBuilder.ofLinked()
                            .put("hash", "")
                            .build()

            ).sendResponse();
            return;
        }
        NPath nPath = NPath.of(b.path);
        switch (nPath.type()) {
            case NOT_FOUND:
                context.setJsonResponse(
                        NMapBuilder.ofLinked()
                                .put("hash", "")
                                .build()

                ).sendResponse();
                break;
            default: {
                context.setJsonResponse(
                        NMapBuilder.ofLinked()
                                .put("hash", nPath.getDigestString(b.algo))
                                .build()
                ).sendResponse();
                break;
            }
        }
    }
}
