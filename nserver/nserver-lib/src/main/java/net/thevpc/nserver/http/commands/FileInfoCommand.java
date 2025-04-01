package net.thevpc.nserver.http.commands;

import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathType;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMapBuilder;

import java.io.IOException;
import java.util.Map;

public class FileInfoCommand extends AbstractFacadeCommand {
    public FileInfoCommand() {
        super("file-info");
    }

    public static class GetFileRequest {
        public String path;
    }

    public static Map<String, Object> fileInfo(NPath nPath) {
        if (nPath == null) {
            return NMapBuilder.<String, Object>ofLinked()
                    .put("path", null)
                    .put("type", NPathType.NOT_FOUND)
                    .build();
        }
        NPathType type = nPath.type();
        switch (type) {
            case NOT_FOUND: {
                return NMapBuilder.<String, Object>ofLinked()
                        .put("path", nPath.toString())
                        .put("type", NPathType.NOT_FOUND)
                        .build();
            }
            case DIRECTORY: {
                return NMapBuilder.<String, Object>ofLinked()
                        .put("path", nPath.toString())
                        .put("lastAccessInstant", nPath.lastAccessInstant())
                        .put("lastModifiedInstant", nPath.lastModifiedInstant())
                        .put("name", nPath.getName())
                        .put("type", type)
                        .put("directoryChildrenCount", nPath.list().size())
                        .build();
            }
        }
        return NMapBuilder.<String, Object>ofLinked()
                .put("path", nPath.toString())
                .put("contentLength", nPath.contentLength())
                .put("contentType", nPath.contentType())
                .put("contentEncoding", nPath.contentEncoding())
                .put("lastAccessInstant", nPath.lastAccessInstant())
                .put("lastModifiedInstant", nPath.lastModifiedInstant())
                .put("name", nPath.getName())
                .put("type", type)
                .build();
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        context.requireAuth();
        GetFileRequest b = context.getRequestBodyAs(GetFileRequest.class, NContentType.JSON);
        if (b == null || NBlankable.isBlank(b.path)) {

            context.setJsonResponse(fileInfo(null)).sendResponse();
            return;
        }
        NPath nPath = NPath.of(b.path);
        context.setJsonResponse(fileInfo(nPath)).sendResponse();
    }
}
