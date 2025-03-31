package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nuts.NDescriptor;
import net.thevpc.nuts.NFetchCmd;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.web.NHttpCode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FetchDescriptorFacadeCommand extends AbstractFacadeCommand {

    public FetchDescriptorFacadeCommand() {
        super("fetch-descriptor");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        Map<String, List<String>> parameters = context.getQueryParams();
        String id = context.getQueryParam("id").orNull();
        boolean transitive = NLiteral.of(context.getQueryParam("transitive").orNull()).asBoolean().orElse(true);
        NDescriptor fetch = null;
        try {
            fetch = NFetchCmd.of(id)
                    .setTransitive(transitive)
                    .getResultDescriptor();
        } catch (Exception exc) {
            //
        }
        if (fetch != null) {
            context.setTextResponse(fetch.toString()).sendResponse();
        } else {
            throw new NWebHttpException("Nuts not Found",new NMsgCode("NOT_FOUND",id), NHttpCode.NOT_FOUND);
        }
    }
}
