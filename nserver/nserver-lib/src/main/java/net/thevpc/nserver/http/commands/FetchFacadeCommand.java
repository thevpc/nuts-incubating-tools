package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nuts.NDefinition;
import net.thevpc.nuts.NFetchCmd;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FetchFacadeCommand extends AbstractFacadeCommand {
    public FetchFacadeCommand() {
        super("fetch");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        String id = context.getQueryParam("id").orNull();
        boolean transitive = NLiteral.of(context.getQueryParam("transitive").orNull()).asBoolean().orElse(true);
        NDefinition fetch = null;
        try {
            fetch = NFetchCmd.of(id)
                    .setTransitive(transitive)
                    .getResultDefinition();
        } catch (Exception exc) {
            //
        }
        if (fetch != null && fetch.getContent().map(NPath::exists).orElse(false)) {
            context.setFileResponse(fetch.getContent().orNull()).sendResponse();
        } else {
            throw new NWebHttpException("Nuts not Found",new NMsgCode("NOT_FOUND",id), NHttpCode.NOT_FOUND);
        }
    }
}
