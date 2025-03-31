package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.NDefinition;
import net.thevpc.nuts.NFetchCmd;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GetFileFacadeCommand extends AbstractFacadeCommand {
    public GetFileFacadeCommand() {
        super("get-file");
    }

    public static class GetFileRequest {
        public String path;
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        GetFileRequest b = context.getRequestBodyAs(GetFileRequest.class, NContentType.JSON);
        if (b == null || NBlankable.isBlank(b.path)) {
            throw new NWebHttpException("File not Found", new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND);
        }
        NPath nPath = NPath.of(b.path);
        switch (nPath.type()) {
            case FILE: {
                context.setFileResponse(nPath).sendResponse();
                return;
            }
            case NOT_FOUND: {
                throw new NWebHttpException("File not Found", new NMsgCode("NOT_FOUND", b.path), NHttpCode.NOT_FOUND);
            }
            default: {
                throw new NWebHttpException("File a file", new NMsgCode("NOT_FOUND", b.path), NHttpCode.NOT_FOUND);
            }
        }
    }
}
