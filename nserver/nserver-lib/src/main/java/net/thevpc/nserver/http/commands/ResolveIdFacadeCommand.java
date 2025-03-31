package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nuts.NFetchCmd;
import net.thevpc.nuts.NId;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ResolveIdFacadeCommand extends AbstractFacadeCommand {
    public ResolveIdFacadeCommand() {
        super("resolve-id");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        String id = context.getQueryParam("id").orNull();
        boolean transitive = NLiteral.of(context.getQueryParam("transitive").orNull()).asBoolean().orElse(true);
        NId fetch = null;
        try {
            fetch = NFetchCmd.of(id)
                    .setTransitive(transitive)
                    .getResultId();
        } catch (Exception exc) {
            //
        }
        if (fetch != null) {
            context.setTextResponse(fetch.toString()).sendResponse();
        } else {
            context.setErrorResponse(new NWebHttpException("Not Found", new NMsgCode("NOT_FOUND",id), NHttpCode.NOT_FOUND));
        }
    }
}
