package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nuts.*;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GetBootFacadeCommand extends AbstractFacadeCommand {
    public GetBootFacadeCommand() {
        super("boot");
    }
//            @Override
//            public void execute(FacadeCommandContext context) throws IOException {
//                executeImpl(context);
//            }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        context.requireAuth();
        String version = null;
        for (Map.Entry<String, List<String>> e : context.getQueryParams().entrySet()) {
            if (e.getKey().equals("version")) {
                version = e.getValue().get(0);
            } else {
                version = e.getKey();
            }
        }
        if (version == null) {
            NDefinition def = NSearchCmd.of().addId(NConstants.Ids.NUTS_API).setLatest(true).setContent(true).getResultDefinitions()
                    .findFirst().orNull();
            if (def != null && def.getContent().isPresent()) {
                context.addResponseHeader("content-disposition", "attachment; filename=\"nuts-" + def.getId().getVersion().toString() + ".jar\"");
                context.setFileResponse(def.getContent().orNull()).sendResponse();
            } else {
                throw new NWebHttpException("Nuts not Found",new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND);
            }
        } else {
            NDefinition def = NFetchCmd.of(NId.get(NConstants.Ids.NUTS_API).get().builder().setVersion(version).build())
                    .setContent(true).getResultDefinition();
            if (def != null && def.getContent().isPresent()) {
                context.addResponseHeader("content-disposition", "attachment; filename=\"nuts-" + def.getId().getVersion().toString() + ".jar\"");
                context.setFileResponse(def.getContent().orNull()).sendResponse();
            } else {
                throw new NWebHttpException("Nuts not Found",new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND);
            }
        }
    }
}
