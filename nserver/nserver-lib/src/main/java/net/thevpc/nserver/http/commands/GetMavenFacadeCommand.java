package net.thevpc.nserver.http.commands;

import net.thevpc.nhttp.server.api.NWebHttpException;
import net.thevpc.nserver.util.NServerUtils;
import net.thevpc.nserver.util.XmlHelper;
import net.thevpc.nuts.*;
import net.thevpc.nserver.AbstractFacadeCommand;
import net.thevpc.nserver.FacadeCommandContext;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.util.NStream;
import net.thevpc.nuts.web.NHttpCode;
import net.thevpc.nuts.web.NHttpMethod;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class GetMavenFacadeCommand extends AbstractFacadeCommand {
    public GetMavenFacadeCommand() {
        super("get-mvn");
    }

    public static boolean acceptUri(String uri) {
        return uri.endsWith(".pom") || uri.endsWith(".jar") || uri.endsWith("/maven-metadata.xml");
    }

    @Override
    public void executeImpl(FacadeCommandContext context) throws IOException {
        context.requireAuth();
        URI uri = context.getRequestURI();
//        System.out.println("get-mvn " + uri.toString());
        List<String> split = NServerUtils.split(uri.toString(), "/");
        String n = split.get(split.size() - 1);
        if (n.endsWith(".pom")) {
            if (split.size() >= 4) {
                NId id = NIdBuilder.of().setArtifactId(split.get(split.size() - 3))
                        .setGroupId(String.join(".", split.subList(0, split.size() - 3)))
                        .setVersion(split.get(split.size() - 2)).build();
                NDefinition fetch = NFetchCmd.of(id)
                        .getResultDefinition();
                NDescriptor d = fetch.getDescriptor();
                if (context.getMethod() == NHttpMethod.HEAD) {
                    context.setTextResponse("");
                    return;
                }
                try {
                    XmlHelper xml = new XmlHelper();
                    xml.push("project")
                            .append("modelVersion", "4.0.0")
                            .append("groupId", d.getId().getGroupId())
                            .append("artifactId", d.getId().getArtifactId())
                            .append("version", d.getId().getVersion().toString())
                            .append("name", d.getName())
                            .append("description", d.getDescription())
                            .append("packaging", d.getPackaging())
                    ;
                    if (d.getParents().size() > 0) {
                        xml.push("parent")
                                .append("groupId", d.getParents().get(0).getGroupId())
                                .append("artifactId", d.getParents().get(0).getArtifactId())
                                .append("version", d.getParents().get(0).getVersion().toString())
                                .pop();
                    }

                    xml.push("properties");
                    for (NDescriptorProperty e : d.getProperties()) {
                        xml.append(e.getName(), e.getValue().asString().get());
                    }
                    xml.pop();

                    xml.push("dependencies");
                    for (NDependency dependency : d.getDependencies()) {
                        xml.push("dependency")
                                .append("groupId", dependency.getGroupId())
                                .append("artifactId", dependency.getArtifactId())
                                .append("version", dependency.getVersion().toString())
                                .append("scope", NServerUtils.toMvnScope(dependency.getScope()))
                                .pop();

                    }
                    xml.pop();
                    if (d.getParents().size() > 0) {
                        xml.push("parent")
                                .append("groupId", d.getParents().get(0).getGroupId())
                                .append("artifactId", d.getParents().get(0).getArtifactId())
                                .append("version", d.getParents().get(0).getVersion().toString())
                                .pop();
                    }
                    if (d.getStandardDependencies().size() > 0) {
                        //dependencyManagement
                        xml.push("dependencyManagement");
                        xml.push("dependencies");
                        for (NDependency dependency : d.getStandardDependencies()) {
                            xml.push("dependency")
                                    .append("groupId", dependency.getGroupId())
                                    .append("artifactId", dependency.getArtifactId())
                                    .append("version", dependency.getVersion().toString())
                                    .append("scope", NServerUtils.toMvnScope(dependency.getScope()))
                                    .pop();

                        }
                        xml.pop();
                        xml.pop();
                    }
                    context.setXmlResponse(new String(xml.toXmlBytes())).sendResponse();
                } catch (Exception ex) {
                    context.setErrorResponse(ex).sendResponse();
                }
            } else {
                context.setErrorResponse(new NWebHttpException("File Note Found", new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND));
            }
        } else if (n.endsWith(".jar")) {
            if (split.size() >= 4) {
                NId id = NIdBuilder.of().setArtifactId(split.get(split.size() - 3))
                        .setGroupId(String.join(".", split.subList(0, split.size() - 3)))
                        .setVersion(split.get(split.size() - 2)).build();
                NDefinition fetch = NFetchCmd.of(id)
                        .getResultDefinition();
                if (fetch.getContent().isPresent()) {
                    if (context.getMethod() == NHttpMethod.HEAD) {
                        context.setXmlResponse("").sendResponse();
                        return;
                    }
                    context.setFileResponse(fetch.getContent().orNull()).sendResponse();
                } else {
                    context.setErrorResponse(new NWebHttpException("File Note Found", new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND));
                }
            } else {
                context.setErrorResponse(new NWebHttpException("File Note Found", new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND));
            }
        } else if (n.equals("maven-metadata.xml")) {
            if (split.size() >= 3) {
                NId id = NIdBuilder.of().setArtifactId(split.get(split.size() - 2))
                        .setGroupId(String.join(".", split.subList(0, split.size() - 2))).build();
                NStream<NId> resultIds = NSearchCmd.of().addId(id).setDistinct(true).setSorted(true).getResultIds();
                if (context.getMethod() == NHttpMethod.HEAD) {
                    context.setXmlResponse("").sendResponse();
                    return;
                }
                try {
                    XmlHelper xml = new XmlHelper();
                    xml.push("metadata")
                            .append("groupId", id.getGroupId())
                            .append("artifactId", id.getArtifactId())
                            .push("versioning")
                    ;
                    List<NId> versions = resultIds.toList();
                    if (versions.size() > 0) {
                        xml.append("release", versions.get(0).getVersion().toString());
                        xml.push("versions");
                        for (NId resultId : versions) {
                            xml.append("version", resultId.getVersion().toString());
                        }
                        xml.pop();
                    }
                    xml.pop();
                    context.setXmlResponse(new String(xml.toXmlBytes()));
                } catch (Exception ex) {
                    context.setErrorResponse(ex);
                }
            } else {
                context.setErrorResponse(new NWebHttpException("File Note Found", new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND));
            }
        } else {
            context.setErrorResponse(new NWebHttpException("File Note Found", new NMsgCode("NOT_FOUND"), NHttpCode.NOT_FOUND));
        }

    }
}
