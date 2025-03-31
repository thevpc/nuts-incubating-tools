package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nuts.NFetchCmd;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FetchHashFacadeCommand extends AbstractFacadeCommand {

    public FetchHashFacadeCommand() {
        super("fetch-hash");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        String id = context.getQueryParam("id").orNull();
        boolean transitive = NLiteral.of(context.getQueryParam("transitive").orNull()).asBoolean().orElse(true);
        String hash = null;
        try {
            hash = NFetchCmd.of(id)
                    .setTransitive(transitive)
                    .getResultContentHash();
        } catch (Exception exc) {
            //
        }
        if (hash != null) {
            context.setTextResponse(hash).sendResponse();
        } else {
            throw new NWebHttpException("Nuts not Found",new NMsgCode("NOT_FOUND",id), NHttpCode.NOT_FOUND);
        }
    }
}
