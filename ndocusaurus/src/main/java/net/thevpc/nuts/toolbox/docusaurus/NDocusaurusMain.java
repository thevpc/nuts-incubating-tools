package net.thevpc.nuts.toolbox.docusaurus;

import net.thevpc.nuts.app.NAppComplete;
import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.app.NAppRun;
import net.thevpc.nuts.cmdline.NCmdLine;

import net.thevpc.nuts.core.NConfirmationMode;
import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.text.NMsg;

import java.nio.file.Paths;

@NApp
public class NDocusaurusMain {

    boolean start;
    boolean build;
    String workdir = null;
    boolean buildPdf = false;

    public static void main(String[] args) {
        NApplication.builder(args).run();
    }

    private NCmdLine parseCmdLine() {
        NCmdLine cmdLine = NApplication.of().cmdLine();
        cmdLine.matcher()
                .when("-d", "--dir").and(c -> workdir == null).asEntry(a -> workdir = a.stringValue())
                .when("start").asFlag(a -> start = a.booleanValue())
                .when("build").asFlag(a -> build = a.booleanValue())
                .when("pdf").asFlag(a -> buildPdf = a.booleanValue())
                .withDefaults()
                .requireAll();
        return cmdLine;
    }

    @NAppComplete
    public void complete() {
        parseCmdLine().printCompleteResult();
    }

    @NAppRun
    public void run() {
        NCmdLine cmdLine = parseCmdLine();
        if (!start && !build && !buildPdf) {
            cmdLine.throwMissingArgument(
                    NMsg.ofC("missing command. try %s", NMsg.ofCode("sh", "ndocusaurus pdf | start | build"))
            );
        }
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
                .setAutoInstallNutsPackages(NWorkspace.of().bootOptions().confirm().orElse(NConfirmationMode.ASK) == NConfirmationMode.YES)
                .run();
    }


}
