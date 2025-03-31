package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.FormDataItem;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.io.NPath;

import java.io.IOException;

public class PutFileFacadeCommand extends AbstractFacadeCommand {
    public PutFileFacadeCommand() {
        super("put-file");
    }

    public static class PutFileRequest {
        public String path;
    }


    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        PutFileRequest r = new PutFileRequest();
        r.path = context.getFormaData("path").get().getSource().readString();
        FormDataItem content = context.getFormaData("content").get();
        NPath.of(r.path).mkParentDirs().copyFromInputStream(content.getSource().getInputStream());
        context.setTextResponse("");
    }


}
