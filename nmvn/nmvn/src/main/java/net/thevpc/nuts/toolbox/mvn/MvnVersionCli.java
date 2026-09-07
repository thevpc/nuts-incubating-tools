package net.thevpc.nuts.toolbox.mvn;

import net.thevpc.nmvn.lib.config.NMvnConfig;
import net.thevpc.nmvn.lib.config.NMvnConfigLoader;
import net.thevpc.nmvn.lib.graph.DependencyEdge;
import net.thevpc.nmvn.lib.graph.MavenDependencyGraph;
import net.thevpc.nmvn.lib.model.*;
import net.thevpc.nmvn.lib.service.BumpResult;
import net.thevpc.nmvn.lib.service.ReleaseResult;
import net.thevpc.nmvn.lib.service.ScanResult;
import net.thevpc.nmvn.lib.service.VersionService;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.elem.NArrayElementBuilder;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.text.NTextStyle;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MvnVersionCli {

    private final NSession session;
    private final VersionService versionService = new VersionService();

    public MvnVersionCli(NSession session) {
        this.session = session;
    }

    public int run(String[] args, boolean jsonOutput) {
        NCmdLine cmd = NCmdLine.of(args);
        String subCommand = null;
        String configPath = null;
        boolean apply = false;
        boolean dryRun = false;
        boolean strict = false;
        Boolean cascadeVersions = null;
        List<String> roots = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        List<BumpInstruction> cliInstructions = new ArrayList<>();
        Map<MavenCoord, String> explicitReleases = new LinkedHashMap<>();

        NArg a;
        while (cmd.hasNext()) {
            if (subCommand == null) {
                if (session.configureFirst(cmd)) {
                    // handled by nuts
                } else if ((a = cmd.next("scan").orNull()) != null) {
                    subCommand = "scan";
                } else if ((a = cmd.next("bump").orNull()) != null) {
                    subCommand = "bump";
                } else if ((a = cmd.next("release", "fix-snapshots").orNull()) != null) {
                    subCommand = "release";
                } else if ((a = cmd.nextFlag("-j", "--json").orNull()) != null) {
                    jsonOutput = a.getBooleanValue().get();
                } else {
                    cmd.throwUnexpectedArgument();
                }
            } else {
                if (session.configureFirst(cmd)) {
                    // handled by nuts
                } else if ((a = cmd.nextEntry("--config").orNull()) != null) {
                    configPath = a.getStringValue().get();
                } else if ((a = cmd.nextFlag("--apply").orNull()) != null) {
                    apply = a.getBooleanValue().get();
                } else if ((a = cmd.nextFlag("--dry-run").orNull()) != null) {
                    dryRun = a.getBooleanValue().get();
                } else if ((a = cmd.nextFlag("--strict").orNull()) != null) {
                    strict = a.getBooleanValue().get();
                } else if ((a = cmd.nextFlag("--cascade-versions").orNull()) != null) {
                    if (a.getBooleanValue().get()) {
                        cascadeVersions = true;
                    }
                } else if ((a = cmd.nextFlag("--cascade-references-only").orNull()) != null) {
                    if (a.getBooleanValue().get()) {
                        cascadeVersions = false;
                    }
                } else if ((a = cmd.nextEntry("--root").orNull()) != null) {
                    roots.add(a.getStringValue().get());
                } else if ((a = cmd.nextEntry("--exclude").orNull()) != null) {
                    excludes.add(a.getStringValue().get());
                } else if ((a = cmd.nextEntry("-a", "--artifact").orNull()) != null) {
                    String val = a.getStringValue().get();
                    handleArtifactArg(subCommand, val, cliInstructions, explicitReleases);
                } else if ((a = cmd.nextNonOption().orNull()) != null) {
                    // Positional artifact instruction
                    String val = a.asString().get();
                    handleArtifactArg(subCommand, val, cliInstructions, explicitReleases);
                } else {
                    cmd.throwUnexpectedArgument();
                }
            }
        }

        if (subCommand == null) {
            NOut.println(NMsg.ofP("Usage: nmvn version <scan|bump|release> [options]"));
            return 1;
        }

        Path workingDir = Paths.get("").toAbsolutePath();
        Path cfgFile = NMvnConfigLoader.resolveConfigFile(configPath, workingDir);
        NMvnConfig config = NMvnConfigLoader.load(cfgFile);

        // Apply CLI overrides
        if (!roots.isEmpty()) {
            config.setRoots(roots);
        }
        if (!excludes.isEmpty()) {
            config.getExcludes().addAll(excludes);
        }

        boolean effectiveApply = apply && !dryRun;

        try {
            switch (subCommand) {
                case "scan":
                    return doScan(config, workingDir, jsonOutput);
                case "bump":
                    return doBump(config, workingDir, cliInstructions, cascadeVersions, effectiveApply, jsonOutput);
                case "release":
                    return doRelease(config, workingDir, explicitReleases, strict, effectiveApply, jsonOutput);
                default:
                    NOut.println(NMsg.ofC("Unknown sub-command: %s", subCommand));
                    return 1;
            }
        } catch (Exception e) {
            NOut.println(NMsg.ofStyled("Error: " + e.getMessage(), NTextStyle.danger()));
            return 1;
        }
    }

    private void handleArtifactArg(String subCommand, String val, List<BumpInstruction> cliInstructions, Map<MavenCoord, String> explicitReleases) {
        if ("bump".equals(subCommand)) {
            cliInstructions.add(BumpInstruction.parse(val));
        } else if ("release".equals(subCommand)) {
            if (val.contains("=")) {
                int eq = val.indexOf('=');
                MavenCoord ga = MavenCoord.parse(val.substring(0, eq).trim());
                explicitReleases.put(ga, val.substring(eq + 1).trim());
            } else {
                MavenCoord ga = MavenCoord.parse(val);
                explicitReleases.put(ga.toGa(), ga.getVersion());
            }
        }
    }

    private int doScan(NMvnConfig config, Path workingDir, boolean jsonOutput) throws IOException {
        ScanResult scan = versionService.scan(config, workingDir);
        Map<MavenCoord, PomArtifact> artifacts = scan.getArtifacts();
        MavenDependencyGraph graph = scan.getGraph();

        if (jsonOutput) {
            NArrayElementBuilder arr = NElement.ofArrayBuilder();
            for (PomArtifact a : artifacts.values()) {
                NObjectElementBuilder obj = NElement.ofObjectBuilder();
                obj.set("groupId", NElement.ofString(a.getGroupId()));
                obj.set("artifactId", NElement.ofString(a.getArtifactId()));
                obj.set("version", NElement.ofString(a.getResolvedVersion()));
                obj.set("path", NElement.ofString(a.getPath().toString()));

                NArrayElementBuilder depsArr = NElement.ofArrayBuilder();
                for (PomDependency dep : a.getAllReferences()) {
                    NObjectElementBuilder dObj = NElement.ofObjectBuilder();
                    dObj.set("groupId", NElement.ofString(dep.getGroupId()));
                    dObj.set("artifactId", NElement.ofString(dep.getArtifactId()));
                    dObj.set("version", NElement.ofString(dep.getResolvedVersion() != null ? dep.getResolvedVersion() : dep.getRawVersion()));
                    dObj.set("edgeType", NElement.ofString(dep.getEdgeType().name()));
                    depsArr.add(dObj.build());
                }
                obj.set("references", depsArr.build());
                arr.add(obj.build());
            }
            NOut.println(NElementWriter.ofJson().formatPlain(arr.build()));
            return 0;
        }

        NOut.println(NMsg.ofStyled(String.format("Discovered %d Maven artifact(s):", artifacts.size()), NTextStyle.primary4()));
        for (PomArtifact a : artifacts.values()) {
            NOut.println(NMsg.ofC("  [bold %s:%s] -> %s", a.getGroupId(), a.getArtifactId(), a.getResolvedVersion()));
            NOut.println(NMsg.ofC("    Path: %s", a.getPath()));

            List<DependencyEdge> outgoing = graph.getOutgoingEdges(a.toGa());
            if (!outgoing.isEmpty()) {
                NOut.println("    References:");
                for (DependencyEdge edge : outgoing) {
                    boolean internal = artifacts.containsKey(edge.getTarget());
                    String targetLabel = edge.getTarget().toGaString() + " [" + edge.getEdgeType() + "]" + (internal ? " (internal)" : " (external)");
                    NOut.println(NMsg.ofC("      -> %s", targetLabel));
                }
            }

            Set<MavenCoord> dependents = graph.getDirectDependents(a.toGa());
            if (!dependents.isEmpty()) {
                NOut.println("    Dependents (referenced by):");
                for (MavenCoord d : dependents) {
                    NOut.println(NMsg.ofC("      <- %s", d.toGaString()));
                }
            }
        }
        return 0;
    }

    private int doBump(NMvnConfig config, Path workingDir, List<BumpInstruction> explicitBumps,
                       Boolean cascadeVersions, boolean apply, boolean jsonOutput) throws IOException {
        BumpResult result = versionService.bump(config, workingDir, explicitBumps, cascadeVersions, apply);
        renderChanges(result.getChanges(), apply, jsonOutput);
        return 0;
    }

    private int doRelease(NMvnConfig config, Path workingDir, Map<MavenCoord, String> explicitReleases,
                          boolean strict, boolean apply, boolean jsonOutput) throws IOException {
        ReleaseResult result = versionService.release(config, workingDir, explicitReleases, strict, apply);
        renderChanges(result.getChanges(), apply, jsonOutput);
        return 0;
    }

    private void renderChanges(List<PomChange> changes, boolean apply, boolean jsonOutput) {
        if (changes.isEmpty()) {
            NOut.println(NMsg.ofStyled("No changes required. All POMs are up-to-date.", NTextStyle.info()));
            return;
        }

        if (jsonOutput) {
            NArrayElementBuilder arr = NElement.ofArrayBuilder();
            for (PomChange c : changes) {
                NObjectElementBuilder obj = NElement.ofObjectBuilder();
                obj.set("pomFile", NElement.ofString(c.getPomFile().toString()));
                NArrayElementBuilder diffArr = NElement.ofArrayBuilder();
                for (String line : c.getDiffLines()) {
                    diffArr.add(NElement.ofString(line));
                }
                obj.set("diff", diffArr.build());
                arr.add(obj.build());
            }
            NOut.println(NElementWriter.ofJson().formatPlain(arr.build()));
            return;
        }

        if (apply) {
            NOut.println(NMsg.ofStyled(String.format("Applied changes to %d POM file(s):", changes.size()), NTextStyle.success()));
        } else {
            NOut.println(NMsg.ofStyled(String.format("DRY RUN: %d POM file(s) would be modified (use --apply to write):", changes.size()), NTextStyle.warn()));
        }

        for (PomChange change : changes) {
            NOut.println();
            NOut.println(NMsg.ofStyled("File: " + change.getPomFile(), NTextStyle.underlined()));
            for (String line : change.getDiffLines()) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    NOut.println(NMsg.ofStyled(line, NTextStyle.success()));
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    NOut.println(NMsg.ofStyled(line, NTextStyle.danger()));
                } else if (line.startsWith("@@")) {
                    NOut.println(NMsg.ofStyled(line, NTextStyle.info()));
                } else {
                    NOut.println(NMsg.ofP(line));
                }
            }
        }
    }
}
