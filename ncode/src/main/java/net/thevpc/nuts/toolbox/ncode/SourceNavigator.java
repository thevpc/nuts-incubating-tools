/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.thevpc.nuts.toolbox.ncode;

import java.util.List;

/**
 * @author thevpc
 */
public class SourceNavigator {

    public static void navigate(Source s, SourceFilter filter, SourceProcessor processor, List<Object> results) {
        try {
            navigate0(s, filter, processor, results);
        } catch (ExitException ex) {
            //System.err.println(ex);
        }
    }

    public static void navigate0(Source s, SourceFilter filter, SourceProcessor processor, List<Object> results) {
        if (filter == null || filter.accept(s)) {
//            System.out.println("ACCEPT "+s);
            Object a = processor.process(s);
            if (a != null) {
                results.add(a);
            }
        } else {
//            System.out.println("REJECT "+s);
        }
        if (filter != null && !filter.lookInto(s)) {
            throw new ExitException();
        }
        for (Source children : s.getChildren()) {
            navigate0(children, filter, processor, results);
        }

    }

    private static class ExitException extends RuntimeException {

    }
}
