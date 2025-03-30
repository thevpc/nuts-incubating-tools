/**
 * ====================================================================
 * Nuts : Network Updatable Things Service
 * (universal package manager)
 * <br>
 * is a new Open Source Package Manager to help install packages
 * and libraries for runtime execution. Nuts is the ultimate companion for
 * maven (and other build managers) as it helps installing all package
 * dependencies at runtime. Nuts is not tied to java and is a good choice
 * to share shell scripts and other 'things' . Its based on an extensible
 * architecture to help supporting a large range of sub managers / repositories.
 * <br>
 * <p>
 * Copyright [2020] [thevpc]
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3 (the "License");
 * you may  not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <br>
 * ====================================================================
 */
package net.thevpc.nserver;

import net.thevpc.nserver.http.commands.FormDataItem;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nserver.http.NHttpServletFacadeContext;
import net.thevpc.nuts.NWorkspace;
import net.thevpc.nuts.util.NOptional;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by vpc on 1/24/17.
 */
public class FacadeCommandContext implements NHttpServletFacadeContext {

    private NHttpServletFacadeContext base;
    private NWorkspace workspace;
    private String serverId;
    private String command;
    private String path;

    public FacadeCommandContext(NHttpServletFacadeContext base, String serverId, String command, String path, NWorkspace workspace) {
        this.base = base;
        this.workspace = workspace;
        this.serverId = serverId;
        this.command = command;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getCommand() {
        return command;
    }

    public NWorkspace getWorkspace() {
        return workspace;
    }

    public String getServerId() {
        return serverId;
    }

    @Override
    public URI getRequestURI() {
        return base.getRequestURI();
    }

    @Override
    public OutputStream getResponseBody() {
        return base.getResponseBody();
    }

    @Override
    public void sendResponseHeaders(int code, long length) {
        base.sendResponseHeaders(code, length);
    }

    @Override
    public void sendError(int code, String msg) {
        base.sendError(code, msg);
    }

    @Override
    public void sendResponseText(int code, String text) {
        base.sendResponseText(code, text);
    }

    @Override
    public void sendResponseFile(int code, File file) {
        base.sendResponseFile(code, file);
    }

    public void sendResponseBytes(int code, byte[] bytes) {
        base.sendResponseBytes(code, bytes);
    }

    @Override
    public void sendResponseFile(int code, Path file) {
        base.sendResponseFile(code, file);
    }

    @Override
    public void sendResponseFile(int code, NPath file) {
        base.sendResponseFile(code, file);
    }

    @Override
    public Set<String> getRequestHeaderKeys(String header) {
        return base.getRequestHeaderKeys(header);
    }

    @Override
    public String getRequestHeader(String header) {
        return base.getRequestHeader(header);
    }

    @Override
    public List<String> getRequestHeaders(String header) {
        return base.getRequestHeaders(header);
    }

    @Override
    public InputStream getRequestBody() {
        return base.getRequestBody();
    }


    @Override
    public Map<String, List<String>> getRequestParameters() {
        return base.getRequestParameters();
    }

    @Override
    public void addResponseHeader(String name, String value) {
        base.addResponseHeader(name, value);
    }

    @Override
    public String getRequestMethod() {
        return base.getRequestMethod();
    }

    @Override
    public boolean isGetMethod() {
        return base.isGetMethod();
    }

    @Override
    public boolean isPostMethod() {
        return base.isPostMethod();
    }

    @Override
    public boolean isHeadMethod() {
        return base.isHeadMethod();
    }

    @Override
    public byte[] getRequestBodyAsBytes() throws NIOException {
        return base.getRequestBodyAsBytes();
    }

    @Override
    public String getRequestBodyAsString() throws NIOException {
        return base.getRequestBodyAsString();
    }

    @Override
    public <T> T getRequestBodyAs(Class<T> type, NContentType contentType) throws NIOException {
        return base.getRequestBodyAs(type, contentType);
    }

    public boolean isMultipartRequest() {
        return base.isMultipartRequest();
    }

    public NOptional<String> getMultipartRequestBoundary() {
        return base.getMultipartRequestBoundary();
    }

    @Override
    public List<String> getRequestParameters(String name) {
        return base.getRequestParameters(name);
    }

    @Override
    public NOptional<String> getRequestParameter(String name) {
        return base.getRequestParameter(name);
    }

    @Override
    public Map<String, FormDataItem> getFormaDataMap() {
        return base.getFormaDataMap();
    }

    @Override
    public NOptional<FormDataItem> getFormaData(String name) {
        return base.getFormaData(name);
    }
}
