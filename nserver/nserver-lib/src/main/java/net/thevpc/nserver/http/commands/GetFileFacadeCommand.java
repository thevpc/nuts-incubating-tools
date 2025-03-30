package net.thevpc.nserver.http.commands;

import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.NDefinition;
import net.thevpc.nuts.NFetchCmd;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;

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
            context.sendError(404, "File Not Found");
            return;
        }
        NPath nPath = NPath.of(b.path);
        switch (nPath.type()) {
            case FILE: {
                context.sendResponseFile(200, nPath);
                return;
            }
            case NOT_FOUND: {
                context.sendError(404, "Not found");
                return;
            }
            default: {
                context.sendError(400, "Not a file");
                return;
            }
        }
    }
}
