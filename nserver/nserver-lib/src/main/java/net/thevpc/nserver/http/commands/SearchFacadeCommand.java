package net.thevpc.nserver.http.commands;

import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.NSearchCmd;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nserver.bundled._IOUtils;
import net.thevpc.nserver.util.ItemStreamInfo;
import net.thevpc.nserver.util.MultipartStreamHelper;
import net.thevpc.nserver.util.NServerUtils;
import net.thevpc.nuts.NId;
import net.thevpc.nserver.http.NHttpServletFacade;

import java.io.IOException;
import java.util.Iterator;

public class SearchFacadeCommand extends AbstractFacadeCommand {

    private final NHttpServletFacade nHttpServletFacade;

    public SearchFacadeCommand(NHttpServletFacade nHttpServletFacade) {
        super("search");
        this.nHttpServletFacade = nHttpServletFacade;
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        //Content-type
        String boundary = context.getRequestHeader("Content-type");
        if (NBlankable.isBlank(boundary)) {
            context.sendError(400, "Invalid NShellCommandNode Arguments : " + getName() + " . Invalid format.");
            return;
        }
        MultipartStreamHelper stream = new MultipartStreamHelper(context.getRequestBody(), boundary);
        boolean transitive = true;
        String root = null;
        String pattern = null;
        String js = null;
        for (ItemStreamInfo info : stream) {
            String name = info.resolveVarInHeader("Content-Disposition", "name");
            switch (name) {
                case "root":
                    root = _IOUtils.loadString(info.getContent(), true).trim();
                    break;
                case "transitive":
                    transitive = Boolean.parseBoolean(_IOUtils.loadString(info.getContent(), true).trim());
                    break;
                case "pattern":
                    pattern = _IOUtils.loadString(info.getContent(), true).trim();
                    break;
                case "js":
                    js = _IOUtils.loadString(info.getContent(), true).trim();
                    break;
            }
        }
        Iterator<NId> it = NSearchCmd.of()
                .setTransitive(transitive)
                .addScripts(js).addId(pattern).getResultIds().iterator();
//                    Writer ps = new OutputStreamWriter(context.getResponseBody());
        context.sendResponseText(200, NServerUtils.iteratorNutsIdToString(it));
    }
}
