/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package net.thevpc.nuts.toolbox.nbackup;

import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.app.NAppRunner;
import net.thevpc.nuts.cmdline.*;
import net.thevpc.nuts.command.NExecCmd;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.elem.NElementParser;
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

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NAppRunner
    public void run() {
        NSession session = NSession.of();
        NOut.println(NMsg.ofC("%s Backup Tool.", NMsg.ofStyledKeyword("Nuts")));
        NApp.of().runCmdLine(new NCmdLineRunner() {
            @Override
            public boolean next(NArg arg, NCmdLine cmdLine) {
                if (arg.isOption()) {
                    return false;
                } else {
                    if (arg.isOption()) {
                        return false;
                    } else {
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
                        return cmdLine.matcher()
                                .with("--server").matchEntry((v) -> options.config.setRemoteServer(v.stringValue()))
                                .with("--user").matchEntry((v) -> options.config.setRemoteUser(v.stringValue()))
                                .with("--local").matchEntry((v) -> options.config.setRemoteUser(v.stringValue()))
                                .with("--add-path").matchEntry((v) -> addPath(v.stringValue()))
                                .with("--remove-path").matchEntry((v) -> options.config.getPaths().removeIf(x -> Objects.equals(String.valueOf(x).trim(), v.stringValue().trim())))
                                .with("--clear-paths").matchTrueFlag((v) -> options.config.getPaths().clear())
                                .with("--save").matchTrueFlag((v) -> options.cmd = Cmd.SAVE)
                                .with("--save").matchTrueFlag((v) -> options.cmd = Cmd.SAVE)
                                .with("--show").matchTrueFlag((v) -> options.cmd = Cmd.SHOW)
                                .withNonOption().matchAny(v -> addPath(v.image()))
                                .anyMatch();
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
