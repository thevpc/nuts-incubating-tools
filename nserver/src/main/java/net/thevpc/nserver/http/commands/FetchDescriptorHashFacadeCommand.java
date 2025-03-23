package net.thevpc.nserver.http.commands;

import net.thevpc.nuts.NFetchCmd;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FetchDescriptorHashFacadeCommand extends AbstractFacadeCommand {
    public FetchDescriptorHashFacadeCommand() {
        super("fetch-descriptor-hash");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        Map<String, List<String>> parameters = context.getParameters();
        List<String> idList = parameters.get("id");
        String id = (idList==null || idList.isEmpty())?null: idList.get(0);
        boolean transitive = parameters.containsKey("transitive");
        String hash = null;
        try {
            hash = NFetchCmd.of(id)
                    .setTransitive(transitive)
                    .getResultDescriptorHash();
        } catch (Exception exc) {
            //
        }
        if (hash != null) {
            context.sendResponseText(200, hash);
        } else {
            context.sendError(404, "Nuts not Found");
        }
    }
}
