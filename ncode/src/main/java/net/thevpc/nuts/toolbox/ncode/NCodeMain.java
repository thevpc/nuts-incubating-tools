/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.thevpc.nuts.toolbox.ncode;

import net.thevpc.nuts.app.NApplication;
import net.thevpc.nuts.app.NAppRun;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.toolbox.ncode.bundles.strings.StringComparator;
import net.thevpc.nuts.toolbox.ncode.bundles.strings.StringComparators;
import net.thevpc.nuts.toolbox.ncode.filters.JavaSourceFilter;
import net.thevpc.nuts.toolbox.ncode.filters.PathSourceFilter;
import net.thevpc.nuts.toolbox.ncode.processors.JavaSourceFormatter;
import net.thevpc.nuts.toolbox.ncode.processors.PathSourceFormatter;
import net.thevpc.nuts.toolbox.ncode.sources.SourceFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.thevpc.nuts.toolbox.ncode.SourceNavigator.navigate;

/**
 * @author thevpc
 */
public class NCodeMain {
    private final List<String> paths = new ArrayList<>();
    private final List<StringComparator> typeComparators = new ArrayList<>();
    private final List<StringComparator> fileComparators = new ArrayList<>();
    private boolean caseInsensitive = false;

    public static void main(String[] args) {
        NApplication.builder(args).run();
    }

    private NCmdLine parseCmdLine() {
        NCmdLine cmdLine = NApplication.of().cmdLine();
        cmdLine.matcher()
                .when("-i").asFlag(a -> caseInsensitive = a.booleanValue())
                .when("-t").asEntry(a -> typeComparators.add(comp(a.stringValue())))
                .when("-f").asEntry(a -> fileComparators.add(comp(a.stringValue())))
                .whenNonOption().asArg(a -> paths.add(a.image()))
                .withDefaults()
                .requireAll();
        return cmdLine;
    }

    @NAppRun
    public void complete() {
        parseCmdLine().printCompleteResult();
    }

    private StringComparator comp(String x) {
        boolean negated = false;
        if (x.startsWith("!")) {
            negated = true;
            x = x.substring(1);
        }
        if (!x.startsWith("^")) {
            x = "*" + x;
        }
        if (!x.startsWith("$")) {
            x = x + "*";
        }
        StringComparator c = caseInsensitive ? StringComparators.ilike(x) : StringComparators.like(x);
        if (negated) {
            c = StringComparators.not(c);
        }
        return c;
    }

    @NAppRun
    public void run() {
        NCmdLine cmdLine = parseCmdLine();
        if (paths.isEmpty()) {
            paths.add(".");
        }
        if (typeComparators.isEmpty() && fileComparators.isEmpty()) {
            cmdLine.throwMissingArgument("filter");
        }
        List<Object> results = new ArrayList<>();
        if (!typeComparators.isEmpty()) {
            for (String path : paths) {
                navigate(SourceFactory.create(new File(path)), new JavaSourceFilter(typeComparators, fileComparators), new JavaSourceFormatter(), results);
            }
        } else {
            for (String path : paths) {
                navigate(SourceFactory.create(new File(path)), new PathSourceFilter(fileComparators), new PathSourceFormatter(), results);
            }
        }
        NOut.println(results);
    }

}
