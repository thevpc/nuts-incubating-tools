package net.thevpc.nserver.http.commands;

import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class PutFileFacadeCommand extends AbstractFacadeCommand {
    public PutFileFacadeCommand() {
        super("put-file");
    }

    public static class PutFileRequest {
        public String path;
        public String file;
    }


    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        PutFileRequest r = new PutFileRequest();
        r.path = context.getRequestParameter("path");
        r.file = context.getRequestParameter("file");

        Map<String, FormDataItem> formData =context.getFormaDataMap();
        if()

    }


}
