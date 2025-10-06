package net.thevpc.nuts.toolbox.ncode;

import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.cmdline.NCmdLineRunner;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.core.NSession;
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

class NCodeMainCmdProcessor implements NCmdLineRunner {
    private List<String> paths = new ArrayList<>();
    private List<StringComparator> typeComparators = new ArrayList<>();
    private List<StringComparator> fileComparators = new ArrayList<>();
    private boolean caseInsensitive = false;
    private NSession session;

    public NCodeMainCmdProcessor(NSession session) {
        this.session = session;
    }

    @Override
    public void init(NCmdLine cmdLine) {
        cmdLine.setExpandSimpleOptions(true);
    }

    @Override
    public boolean next(NArg arg, NCmdLine cmdLine) {
        if(arg.isOption()){
            switch (arg.getStringKey().get()) {
                case "-i": {
                    arg = cmdLine.nextFlag().get();
                    caseInsensitive = arg.getBooleanValue().get();
                    return true;
                }
                case "-t": {
                    typeComparators.add(comp(cmdLine.nextEntry().get().getStringValue().get()));
                    return true;
                }
                case "-f": {
                    fileComparators.add(comp(cmdLine.nextEntry().get().getStringValue().get()));
                    return true;
                }
            }
            return false;
        }else{
            paths.add(cmdLine.next().get().image());
            return true;
        }
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



    @Override
    public void run(NCmdLine cmdLine) {
        if (paths.isEmpty()) {
            paths.add(".");
        }
        if(typeComparators.isEmpty() && fileComparators.isEmpty()){
            cmdLine.throwMissingArgument("filter");
        }
        List<Object> results=new ArrayList<>();
        if(!typeComparators.isEmpty()) {
            for (String path : paths) {
                navigate(SourceFactory.create(new File(path)), new JavaSourceFilter(typeComparators, fileComparators), new JavaSourceFormatter(), session, results);
            }
        }else{
            for (String path : paths) {
                navigate(SourceFactory.create(new File(path)), new PathSourceFilter(fileComparators), new PathSourceFormatter(), session, results);
            }
        }
        session.out().println(results);
    }

}
