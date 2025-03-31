package net.thevpc.nserver.http.commands;

import net.thevpc.nuts.NIdBuilder;
import net.thevpc.nuts.NWorkspace;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;

import java.io.IOException;

public class VersionFacadeCommand extends AbstractFacadeCommand {
    public VersionFacadeCommand() {
        super("version");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        context.setTextResponse(
                NIdBuilder.of()
                        .setRepository(context.getServerId())
                        .setGroupId("net.thevpc.nuts")
                        .setArtifactId("nuts-server")
                        .setVersion(NWorkspace.of().getRuntimeId().getVersion().toString())
                        .build().toString()
        ).sendResponse();
    }
}
