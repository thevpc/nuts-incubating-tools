package net.thevpc.nmvn.lib.config;

import net.thevpc.nmvn.lib.model.BumpInstruction;
import net.thevpc.nmvn.lib.model.BumpPolicy;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NMvnConfigLoader {

    public static final String DEFAULT_CONFIG_FILE = "nmvn.tson";
    public static final String ALT_CONFIG_FILE = ".nmvn/config.tson";

    public static Path resolveConfigFile(String cliConfigPath, Path workingDir) {
        if (cliConfigPath != null && !cliConfigPath.trim().isEmpty()) {
            Path p = Paths.get(cliConfigPath.trim());
            return p.isAbsolute() ? p : workingDir.resolve(p);
        }
        Path p1 = workingDir.resolve(DEFAULT_CONFIG_FILE);
        if (Files.isRegularFile(p1)) {
            return p1;
        }
        Path p2 = workingDir.resolve(ALT_CONFIG_FILE);
        if (Files.isRegularFile(p2)) {
            return p2;
        }
        return p1; // default to nmvn.tson even if doesn't exist yet
    }

    public static NMvnConfig load(Path configFile) {
        Nuts.require();
        NMvnConfig config = new NMvnConfig();
        if (configFile == null || !Files.isRegularFile(configFile)) {
            return config;
        }

        NElement element = NElementReader.ofTson().read(NPath.of(configFile));
        if (element == null) {
            return config;
        }

        NObjectElement obj = element.asObject().orNull();
        if (obj == null) {
            return config;
        }

        // roots
        obj.get("roots").flatMap(NElement::asArray).ifPresent(arr -> {
            List<String> rList = new ArrayList<>();
            for (NElement item : arr) {
                item.asStringValue().ifPresent(rList::add);
            }
            if (!rList.isEmpty()) {
                config.setRoots(rList);
            }
        });

        // global excludes
        obj.get("excludes").flatMap(NElement::asArray).ifPresent(arr -> {
            List<String> eList = new ArrayList<>();
            for (NElement item : arr) {
                item.asStringValue().ifPresent(eList::add);
            }
            if (!eList.isEmpty()) {
                config.setExcludes(eList);
            }
        });

        // rootExcludes
        obj.get("rootExcludes").flatMap(NElement::asObject).ifPresent(reObj -> {
            Map<String, List<String>> reMap = new LinkedHashMap<>();
            for (NElement entry : reObj.children()) {
                if (entry instanceof NPairElement) {
                    NPairElement pair = (NPairElement) entry;
                    String rootKey = pair.key().asStringValue().orElse("");
                    List<String> perRootList = new ArrayList<>();
                    pair.value().asArray().ifPresent(arr -> {
                        for (NElement item : arr) {
                            item.asStringValue().ifPresent(perRootList::add);
                        }
                    });
                    reMap.put(rootKey, perRootList);
                }
            }
            config.setRootExcludes(reMap);
        });

        // bumpPolicy
        obj.get("bumpPolicy").flatMap(NElement::asObject).ifPresent(bpObj -> {
            BumpPolicy policy = config.getBumpPolicy();
            bpObj.getStringValue("defaultIncrement").ifPresent(inc -> policy.setDefaultIncrement(BumpPolicy.IncrementType.parse(inc)));
            bpObj.getStringValue("snapshotSuffix").ifPresent(policy::setSnapshotSuffix);
            bpObj.getStringValue("cascadePolicy").ifPresent(cas -> policy.setCascadePolicy(BumpPolicy.CascadePolicy.parse(cas)));
        });

        // instructions
        obj.get("instructions").flatMap(NElement::asArray).ifPresent(arr -> {
            List<BumpInstruction> instList = new ArrayList<>();
            for (NElement item : arr) {
                item.asObject().ifPresent(instObj -> {
                    String g = instObj.getStringValue("groupId").orNull();
                    String a = instObj.getStringValue("artifactId").orNull();
                    String v = instObj.getStringValue("toVersion").orNull();
                    if (g != null && a != null && v != null) {
                        instList.add(new BumpInstruction(g, a, v));
                    }
                });
            }
            config.setInstructions(instList);
        });

        // historyFile
        obj.getStringValue("historyFile").ifPresent(config::setHistoryFile);

        return config;
    }

    public static void save(NMvnConfig config, Path targetFile) {
        Nuts.require();
        NObjectElementBuilder builder = NElement.ofObjectBuilder();

        // roots
        NArrayElementBuilder rootsArr = NElement.ofArrayBuilder();
        for (String r : config.getRoots()) {
            rootsArr.add(NElement.ofString(r));
        }
        builder.set("roots", rootsArr.build());

        // excludes
        NArrayElementBuilder excArr = NElement.ofArrayBuilder();
        for (String e : config.getExcludes()) {
            excArr.add(NElement.ofString(e));
        }
        builder.set("excludes", excArr.build());

        // rootExcludes
        if (!config.getRootExcludes().isEmpty()) {
            NObjectElementBuilder reObj = NElement.ofObjectBuilder();
            for (Map.Entry<String, List<String>> entry : config.getRootExcludes().entrySet()) {
                NArrayElementBuilder perRootArr = NElement.ofArrayBuilder();
                for (String p : entry.getValue()) {
                    perRootArr.add(NElement.ofString(p));
                }
                reObj.set(entry.getKey(), perRootArr.build());
            }
            builder.set("rootExcludes", reObj.build());
        }

        // bumpPolicy
        NObjectElementBuilder bpObj = NElement.ofObjectBuilder();
        bpObj.set("defaultIncrement", NElement.ofString(config.getBumpPolicy().getDefaultIncrement().name().toLowerCase()));
        bpObj.set("snapshotSuffix", NElement.ofString(config.getBumpPolicy().getSnapshotSuffix()));
        bpObj.set("cascadePolicy", NElement.ofString(config.getBumpPolicy().getCascadePolicy().name().toLowerCase().replace("_", "-")));
        builder.set("bumpPolicy", bpObj.build());

        // instructions
        if (!config.getInstructions().isEmpty()) {
            NArrayElementBuilder instArr = NElement.ofArrayBuilder();
            for (BumpInstruction inst : config.getInstructions()) {
                NObjectElementBuilder instObj = NElement.ofObjectBuilder();
                instObj.set("groupId", NElement.ofString(inst.getGroupId()));
                instObj.set("artifactId", NElement.ofString(inst.getArtifactId()));
                instObj.set("toVersion", NElement.ofString(inst.getToVersion()));
                instArr.add(instObj.build());
            }
            builder.set("instructions", instArr.build());
        }

        builder.set("historyFile", NElement.ofString(config.getHistoryFile()));

        if (targetFile.getParent() != null) {
            try {
                Files.createDirectories(targetFile.getParent());
            } catch (Exception ignored) {
            }
        }
        NElementWriter.ofTson()
                .formatter(NElementFormatter.ofPretty())
                .write(builder.build(), NPath.of(targetFile));
    }
}
