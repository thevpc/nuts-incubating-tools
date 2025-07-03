/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package net.thevpc.nuts.toolbox.nbackup;

import net.thevpc.nuts.*;
import net.thevpc.nuts.cmdline.*;
import net.thevpc.nuts.elem.NElementParser;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsg;

import java.util.Objects;

/**
 * @author vpc
 */
public class NBackup implements NApplication {

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @Override
    public void run() {
        NSession session = NSession.of();
        NOut.println(NMsg.ofC("%s Backup Tool.", NMsg.ofStyledKeyword("Nuts")));
        NApp.of().runCmdLine(new NCmdLineRunner() {
            @Override
            public boolean next(NArg arg, NCmdLine cmdLine) {
                if(arg.isOption()){
                    return false;
                }else{
                    if(arg.isOption()){
                        return false;
                    }else{
                        NArg a = cmdLine.next().get();
                        switch (a.toString()) {
                            case "pull": {
                                runPull(cmdLine);
                                return true;
                            }
                        }
                        return false;
                    }
                }
            }
        });
    }

    public void runPull(NCmdLine cmdLine) {
        cmdLine.setConfigurable(NSession.of())
                .run(new NCmdLineRunner() {
            private Options options = new Options();

            @Override
            public void init(NCmdLine cmdLine) {
                NPath configFile = getConfigFile();
                Config config = null;
                if (configFile.isRegularFile()) {
                    try {
                        config = NElementParser.ofJson().parse(
                                configFile, Config.class
                        );
                    } catch (Exception ex) {
                        //
                    }
                }
                if (config != null) {
                    options.config = config;
                }
            }

            private NPath getConfigFile() {
                return NApp.of().getConfFolder().resolve("backup.json");
            }

            @Override
            public boolean next(NArg arg, NCmdLine cmdLine) {
                if(arg.isOption()){
                    return cmdLine.withFirst(
                            (aa, c)->c.with("--server").nextEntry((v)->options.config.setRemoteServer(v.stringValue()))
                            , (aa, c)->c.with("--user").nextEntry((v)->options.config.setRemoteUser(v.stringValue()))
                            , (aa, c)->c.with("--local").nextEntry((v)->options.config.setRemoteUser(v.stringValue()))
                            , (aa, c)->c.with("--add-path").nextEntry((v)->addPath(v.stringValue()))
                            , (aa, c)->c.with("--remove-path").nextEntry((v)->options.config.getPaths().removeIf(x -> Objects.equals(String.valueOf(x).trim(), v.stringValue().trim())))
                            , (aa, c)->c.with("--clear-paths").nextTrueFlag((v)->options.config.getPaths().clear())
                            , (aa, c)->c.with("--save").nextTrueFlag((v)->options.cmd = Cmd.SAVE)
                            , (aa, c)->c.with("--show").nextTrueFlag((v)->options.cmd = Cmd.SHOW)
                    );
                }else{
                    NArg a = cmdLine.next().get();
                    addPath(a.toString());
                    return true;
                }
            }

            private void addPath(String a) {
                int i = a.indexOf('=');
                if (i > 0) {
                    options.config.getPaths().add(new DecoratedPath(a.substring(i + 1), a.substring(0, i)));
                } else {
                    options.config.getPaths().add(new DecoratedPath(a, null));
                }
            }

            @Override
            public void run(NCmdLine cmdLine) {
                Config config = options.config;
                if (config == null) {
                    config = new Config();
                }
                NSession session = NSession.of();
                NOut.println(NMsg.ofC("Config File %s", getConfigFile()));

                switch (options.cmd) {
                    case SAVE: {
                        NElementWriter.ofJson().write(config,getConfigFile());
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
                NPath.of(localPath).getParent().mkdirs();
                NOut.println(NMsg.ofC("[%s] Backup %s from %s.",
                        NMsg.ofStyledWarn(config.getRemoteServer()),
                        NMsg.ofStyledKeyword(name),
                        NMsg.ofStyledPath(remotePath)
                ));
                NOut.println(NCmdLine.of(cmd));
                NExecCmd.of().addCommand(cmd).failFast().run();
            }
        });
    }
}
