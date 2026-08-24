/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package net.thevpc.nuts.toolbox.nbackup;

import net.thevpc.nuts.app.NAppComplete;
import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.app.NAppRun;
import net.thevpc.nuts.cmdline.*;
import net.thevpc.nuts.command.NExec;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.text.NMsg;

import java.util.Objects;

/**
 * @author vpc
 */
public class NBackup {

    private AnyOptions anyOptions;

    public static void main(String[] args) {
        NApplication.builder(args).run();
    }

    public NCmdLine parseCmdLine() {
        NCmdLine cmdLine = NApplication.of().cmdLine();
        NArg a = cmdLine.peek().orNull();
        if (a == null) {
            cmdLine.throwMissingArgument("pull");
            return cmdLine;
        }
        switch (a.image()) {
            case "pull": {
                PullOptions options = new PullOptions();
                anyOptions = options;
                NPath configFile = getConfigFile();
                Config config = null;
                if (configFile.isRegularFile()) {
                    try {
                        config = NElementReader.ofJson().read(
                                configFile, Config.class
                        );
                    } catch (Exception ex) {
                        //
                    }
                }
                if (config != null) {
                    options.config = config;
                }
                cmdLine.matcher()
                        .when("--server").asEntry((v) -> options.config.setRemoteServer(v.stringValue()))
                        .when("--user").asEntry((v) -> options.config.setRemoteUser(v.stringValue()))
                        .when("--local").asEntry((v) -> options.config.setRemoteUser(v.stringValue()))
                        .when("--add-path").asEntry((v) -> addPath(v.stringValue()))
                        .when("--remove-path").asEntry((v) -> options.config.getPaths().removeIf(x -> Objects.equals(String.valueOf(x).trim(), v.stringValue().trim())))
                        .when("--clear-paths").asTrueFlag((v) -> options.config.getPaths().clear())
                        .when("--save").asTrueFlag((v) -> options.cmd = Cmd.SAVE)
                        .when("--save").asTrueFlag((v) -> options.cmd = Cmd.SAVE)
                        .when("--show").asTrueFlag((v) -> options.cmd = Cmd.SHOW)
                        .whenNonOption().asArg(v -> addPath(v.image()))
                        .requireAll();
                return cmdLine;
            }
            default: {
                cmdLine.throwMissingArgument("pull");
                return cmdLine;
            }
        }
    }

    @NAppComplete
    public void complete() {
        parseCmdLine().printCompleteResult();
    }

    @NAppRun
    public void run() {
        NOut.println(NMsg.ofC("%s Backup Tool.", NMsg.ofStyledKeyword("Nuts")));
        NCmdLine cmdLine = parseCmdLine();
        if (anyOptions instanceof PullOptions) {
            runPull(cmdLine);
        }
    }

    public static class AnyOptions {

    }

    private void addPath(String a) {
        int i = a.indexOf('=');
        PullOptions options = (PullOptions) anyOptions;
        if (i > 0) {
            options.config.getPaths().add(new DecoratedPath(a.substring(i + 1), a.substring(0, i)));
        } else {
            options.config.getPaths().add(new DecoratedPath(a, null));
        }
    }

    private NPath getConfigFile() {
        return NApplication.of().confFolder().resolve("backup.json");
    }

    public void runPull(NCmdLine cmdLine) {
        PullOptions options = (PullOptions) anyOptions;
        Config config = options.config;
        if (config == null) {
            config = new Config();
        }
        NSession session = NSession.of();
        NOut.println(NMsg.ofC("Config File %s", getConfigFile()));

        switch (options.cmd) {
            case SAVE: {
                NElementWriter.ofJson().write(config, getConfigFile());
                break;
            }
            case SHOW: {
                NElementWriter.ofJson().writeln(config);
                break;
            }
            case RUN: {
                if (config.getPaths().isEmpty()) {
                    cmdLine.throwMissingArgument("path");
                }
                if (NBlankable.isBlank(config.getRemoteUser())) {
                    cmdLine.throwMissingArgument("--user");
                }
                if (NBlankable.isBlank(config.getRemoteServer())) {
                    cmdLine.throwMissingArgument("--server");
                }
                if (NBlankable.isBlank(config.getLocalPath())) {
                    cmdLine.throwMissingArgument("--local");
                }
                NOut.println(NMsg.ofC("Using local path %s", NMsg.ofStyledPath(config.getLocalPath())));
                for (DecoratedPath path : config.getPaths()) {
                    get(path, config, session);
                }
                break;
            }
        }

    }

    private void get(DecoratedPath dpath, Config config, NSession session) {
        String localPath = config.getLocalPath();
        String remotePath = dpath.getPath();
        String name = dpath.getName();
        if (!remotePath.startsWith("/")) {
            remotePath = "/home/" + config.getRemoteUser() + "/" + remotePath;
        }
        if (!remotePath.startsWith("/")) {
            localPath += "/";
        }
        localPath += remotePath;
        String[] cmd = {
                "rsync",
                "-azP" + (session.isDry() ? "nv" : ""),
                "--delete",
                config.getRemoteUser() + "@" + config.getRemoteServer() + ":" + remotePath,
                localPath};
        NPath.of(localPath).parent().mkdirs();
        NOut.println(NMsg.ofC("[%s] Backup %s from %s.",
                NMsg.ofStyledWarn(config.getRemoteServer()),
                NMsg.ofStyledKeyword(name),
                NMsg.ofStyledPath(remotePath)
        ));
        NOut.println(NCmdLine.of(cmd));
        NExec.of().command(cmd).failFast(true).run();
    }
}
