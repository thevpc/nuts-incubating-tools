/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.thevpc.nuts.toolbox.ncode.sources;

import net.thevpc.nuts.toolbox.ncode.Source;

import java.io.File;

/**
 *
 * @author thevpc
 */
public class SourceFactory {

    public static Source wrap(Source src) {
        if (!src.isStream()) {
            return src;
        }
        String name = src.getName().toLowerCase();
        if (name.endsWith(".zip") || name.endsWith(".jar")) {
            return new ZipSource(src);
        }
        if (name.endsWith(".class")) {
            return new JavaTypeSource(src);
        }
        return src;
    }

    public static Source create(File file) {
        if (file.isDirectory()) {
            return new FolderSource(file);
        }
        return wrap(new FileSource(file));
    }
}
