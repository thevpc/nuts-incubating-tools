package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;

public class DirectoryListDigestCommand extends AbstractFacadeCommand {
    public DirectoryListDigestCommand() {
        super("directory-list-digest");
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
            throw new NWebHttpException("File not Found", new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND);
        }
        NPath nPath = NPath.of(b.path);
        if (nPath.isDirectory()) {
            context.setJsonResponse(
                    nPath.list().stream().map(x -> x.getDigestString(b.algo)).toArray(String[]::new)
            ).sendResponse();
            return;
        }
        throw new NWebHttpException("Not a directory", new NMsgCode("NOT_FOUND", b.path), NHttpCode.NOT_FOUND);
    }
}
