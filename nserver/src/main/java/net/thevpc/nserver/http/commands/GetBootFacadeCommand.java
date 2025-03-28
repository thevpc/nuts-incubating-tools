package net.thevpc.nserver.http.commands;

import net.thevpc.nuts.*;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;

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
        String version = null;
        for (Map.Entry<String, List<String>> e : context.getParameters().entrySet()) {
            if (e.getKey().equals("version")) {
                version = e.getValue().toString();
            } else {
                version = e.getKey();
            }
        }
        if (version == null) {
            NDefinition def = NSearchCmd.of().addId(NConstants.Ids.NUTS_API).setLatest(true).setContent(true).getResultDefinitions()
                    .findFirst().orNull();
            if (def != null && def.getContent().isPresent()) {
                context.addResponseHeader("content-disposition", "attachment; filename=\"nuts-" + def.getId().getVersion().toString() + ".jar\"");
                context.sendResponseFile(200, def.getContent().orNull());
            } else {
                context.sendError(404, "File Note Found");
            }
        } else {
            NDefinition def = NFetchCmd.of(NId.get(NConstants.Ids.NUTS_API).get().builder().setVersion(version).build())
                    .setContent(true).getResultDefinition();
            if (def != null && def.getContent().isPresent()) {
                context.addResponseHeader("content-disposition", "attachment; filename=\"nuts-" + def.getId().getVersion().toString() + ".jar\"");
                context.sendResponseFile(200, def.getContent().orNull());
            } else {
                context.sendError(404, "File Note Found");
            }
        }
    }
}
