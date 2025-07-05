package net.thevpc.nuts.toolbox.docusaurus;

import net.thevpc.nuts.*;
import net.thevpc.nuts.cmdline.NCmdLineRunner;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;

import net.thevpc.nuts.util.NMsg;

import java.nio.file.Paths;

@NApp.Info
public class NDocusaurusMain {

    boolean start;
    boolean build;
    String workdir = null;
    boolean buildPdf = false;

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NApp.Runner
    public void run() {
        NApp.of().runCmdLine(new NCmdLineRunner() {
            @Override
            public boolean next(NArg arg, NCmdLine cmdLine) {
                if (arg.isOption()) {
                    switch (arg.key()) {
                        case "-d":
                        case "--dir": {
                            if (workdir == null) {
                                return cmdLine.matcher().matchEntry((v) -> workdir = v.stringValue()).anyMatch();
                            }
                            return false;
                        }
                    }
                    return false;
                } else {
                    switch (arg.asString().get()) {
                        case "start": {
                            return cmdLine.matcher().matchFlag((v) -> start = v.booleanValue()).anyMatch();
                        }
                        case "build": {
                            return cmdLine.matcher().matchFlag((v) -> build = v.booleanValue()).anyMatch();
                        }
                        case "pdf": {
                            return cmdLine.matcher().matchFlag((v) -> buildPdf = v.booleanValue()).anyMatch();
                        }
                    }
                    return false;
                }
            }

            @Override
            public void validate(NCmdLine cmdLine) {
                if (!start && !build && !buildPdf) {
                    cmdLine.throwMissingArgument(
                            NMsg.ofC("missing command. try %s", NMsg.ofCode("sh", "ndocusaurus pdf | start | build"))
                    );
                }
            }

            @Override
            public void run(NCmdLine cmdLine) {
                if (workdir == null) {
                    workdir = ".";
                }
                DocusaurusProject docusaurusProject = new DocusaurusProject(workdir,
                        Paths.get(workdir).resolve(".dir-template").resolve("src").toString()
                );
                new DocusaurusCtrl(docusaurusProject)
                        .setBuildWebSite(build)
                        .setStartWebSite(start)
                        .setBuildPdf(buildPdf)
                        .setAutoInstallNutsPackages(NWorkspace.of().getBootOptions().getConfirm().orElse(NConfirmationMode.ASK) == NConfirmationMode.YES)
                        .run();
            }
        });
    }


}
