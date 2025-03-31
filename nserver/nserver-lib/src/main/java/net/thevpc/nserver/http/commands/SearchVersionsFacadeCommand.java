package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nserver.util.NServerUtils;
import net.thevpc.nuts.NId;
import net.thevpc.nuts.NSearchCmd;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.util.List;

public class SearchVersionsFacadeCommand extends AbstractFacadeCommand {

    public SearchVersionsFacadeCommand() {
        super("find-versions");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        String id = context.getQueryParam("id").orNull();
        boolean transitive = NLiteral.of(context.getQueryParam("id").orNull()).asBoolean().orElse(true);
        List<NId> fetch = null;
        try {
            fetch = NSearchCmd.of()
                    .setTransitive(transitive)
                    .addId(id).getResultIds().toList();
        } catch (Exception exc) {
            //
        }
        if (fetch != null) {
            context.setTextResponse(NServerUtils.iteratorNutsIdToString(fetch.iterator())).sendResponse();
        } else {
            throw new NWebHttpException("Nuts not Found", new NMsgCode("NUTS_NOT_FOUND"), NHttpCode.NOT_FOUND);
        }
    }
}
