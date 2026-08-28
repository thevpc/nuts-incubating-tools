package net.thevpc.nuts.indexer;

import net.thevpc.nuts.artifact.NDependency;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.artifact.NIdBuilder;
import net.thevpc.nuts.core.NConstants;
import net.thevpc.nuts.core.NStoreKey;
import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.artifact.NEnvConditionBuilder;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.core.NRepository;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NStringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class NIndexerUtils {

    public static Path getCacheDir(String entity) {
        String k = "NutsIndexerUtils.CACHE." + entity;
        String m = NWorkspace.of().getProperty(k).flatMap(x -> NLiteral.of(x).asString()).orNull();
        if (m == null) {
            m = NPath.of(NStoreKey.ofConf(NId.getForClass(NIndexerUtils.class).get())).resolve(entity).toString();
            NWorkspace.of().setProperty(k, m);
        }
        return new File(m).toPath();
    }

    public static Map<String, String> nutsRepositoryToMap(NRepository repository, int level) {
        if (repository == null) {
            return new HashMap<>();
        }
        Map<String, String> entity = new HashMap<>();
        entity.put("name", repository.name());
        entity.put("type", repository.repositoryType());
        entity.put("location", repository.config().location().toString());
        entity.put("enabled", String.valueOf(repository.config().isEnabled()));
        entity.put("speed", String.valueOf(repository.config().speed()));
        NWorkspace ws = repository.workspace();
        if (level == 0) {
            entity.put("mirrors", Arrays.toString(
                    repository.config().mirrors().stream()
                            .map(nutsRepository -> mapToJson(nutsRepositoryToMap(nutsRepository, level + 1)))
                            .toArray()));
            entity.put("parents", mapToJson(nutsRepositoryToMap(repository.parentRepository(), level + 1)));
        }
        return entity;
    }

    public static String mapToJson(Map<String, String> map) {
        return NElementWriter.ofJson().formatPlain(map);
    }

    public static Map<String, String> nutsRepositoryToMap(NRepository repository) {
        return nutsRepositoryToMap(repository, 0);
    }

    public static Map<String, String> nutsIdToMap(NId id) {
        Map<String, String> entity = new HashMap<>();
        id = id.builder().face(StringUtils.isEmpty(id.face()) ? "default" : id.face()).build();
        _condPut(entity, "name", id.artifactId());
        _condPut(entity, "namespace", id.repository());
        _condPut(entity, "group", id.groupId());
        _condPut(entity, "classifier", id.classifier());
        _condPut(entity, "version", id.version().value());
        _condPut(entity, "face", id.face());
        _condPut(entity, NConstants.IdProperties.OS, String.join(",", id.condition().os()));
        _condPut(entity, NConstants.IdProperties.OS_DIST, String.join(",", id.condition().osDist()));
        _condPut(entity, NConstants.IdProperties.ARCH, String.join(",", id.condition().arch()));
        _condPut(entity, NConstants.IdProperties.PLATFORM, String.join(",", id.condition().platform()));
        _condPut(entity, NConstants.IdProperties.PROFILE, String.join(",", id.condition().profiles()));
        _condPut(entity, NConstants.IdProperties.DESKTOP, String.join(",", id.condition().desktopEnvironment()));
//        _condPut(entity, NutsConstants.IdProperties.ALTERNATIVE, id.getAlternative());
        _condPut(entity, "stringId", id.toString());
        return entity;
    }

    public static Map<String, String> nutsDependencyToMap(NDependency dependency) {
        Map<String, String> entity = new HashMap<>();
        _condPut(entity, "name", dependency.artifactId());
        _condPut(entity, "namespace", dependency.repository());
        _condPut(entity, "group", dependency.groupId());
        _condPut(entity, "classifier", dependency.groupId());
        _condPut(entity, "version", dependency.version().value());
        NId id2 = dependency.toId().builder()
                .face(StringUtils.isEmpty(dependency.toId().face()) ? "default" : dependency.toId().face())
                .build();
        _condPut(entity, NConstants.IdProperties.FACE, id2.face());

        _condPut(entity, NConstants.IdProperties.OS, String.join(",", id2.condition().os()));
        _condPut(entity, NConstants.IdProperties.OS_DIST, String.join(",", id2.condition().osDist()));
        _condPut(entity, NConstants.IdProperties.ARCH, String.join(",", id2.condition().arch()));
        _condPut(entity, NConstants.IdProperties.PLATFORM, String.join(",", id2.condition().platform()));
        _condPut(entity, NConstants.IdProperties.PROFILE, String.join(",", id2.condition().profiles()));
        _condPut(entity, NConstants.IdProperties.DESKTOP, String.join(",", id2.condition().desktopEnvironment()));

//        _condPut(entity, NutsConstants.IdProperties.ALTERNATIVE, dependency.getId().getAlternative());
        _condPut(entity, "stringId", id2.toString());
        return entity;
    }

    private static void _condPut(Map<String, String> m, String k, String v) {
        if (!NStringUtils.strip(v).isEmpty()) {
            m.put(k, v);
        }
    }

    public static BooleanQuery nutsIdToQuery(
            String name,
            String namespace,
            String group,
            String version,
            String os,
            String osDist,
            String arch,
            String platform,
            String desktopEnvironment,
            String classifier
//            ,String alternative
    ) {
        return new BooleanQuery.Builder()
                .add(new PhraseQuery.Builder().add(new Term("name", name)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term("namespace", namespace)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term("group", group)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term("classifier", classifier)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term("version", version)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term(NConstants.IdProperties.OS, os)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term(NConstants.IdProperties.OS_DIST, osDist)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term(NConstants.IdProperties.ARCH, arch)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term(NConstants.IdProperties.PLATFORM, platform)).build(), BooleanClause.Occur.MUST)
                .add(new PhraseQuery.Builder().add(new Term(NConstants.IdProperties.DESKTOP, desktopEnvironment)).build(), BooleanClause.Occur.MUST)
//                .add(new PhraseQuery.Builder().add(new Term(NutsConstants.IdProperties.ALTERNATIVE, alternative)).build(), BooleanClause.Occur.MUST)
                .add(new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD))
                .build();
    }

    public static NId mapToNutsId(Map<String, String> map) {
        return NIdBuilder.of()
                .artifactId(NStringUtils.strip(map.get("name")))
                .repository(NStringUtils.strip(map.get("namespace")))
                .groupId(NStringUtils.strip(map.get("group")))
                .classifier(NStringUtils.strip(map.get("classifier")))
                .version(NStringUtils.strip(map.get("version")))
                .condition(
                        NEnvConditionBuilder.of()
                                //TODO what if the result is ',' separated array?
                                .arch(Collections.singletonList(NStringUtils.strip(map.get(NConstants.IdProperties.ARCH))))
                                .os(Collections.singletonList(NStringUtils.strip(map.get(NConstants.IdProperties.OS))))
                                .osDist(Collections.singletonList(NStringUtils.strip(map.get(NConstants.IdProperties.OS_DIST))))
                                .platform(Collections.singletonList(NStringUtils.strip(map.get(NConstants.IdProperties.PLATFORM))))
                                .desktopEnvironment(Collections.singletonList(NStringUtils.strip(map.get(NConstants.IdProperties.DESKTOP))))
                )
//                .setAlternative(trim(map.get(NutsConstants.IdProperties.ALTERNATIVE)))
                .build();
    }

    public static Query mapToQuery(Map<String, String> map, String... exclus) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Set<String> set = Arrays.stream(exclus).collect(Collectors.toSet());
        if (set.size() > 0) {
            set.add("stringId");
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!set.contains(entry.getKey())) {
                builder.add(new PhraseQuery.Builder()
                                .add(new Term(entry.getKey(),
                                        NStringUtils.strip(entry.getValue()))).build(),
                        BooleanClause.Occur.MUST);
            }
        }
        builder.add(new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD));
        return builder.build();
    }

}
