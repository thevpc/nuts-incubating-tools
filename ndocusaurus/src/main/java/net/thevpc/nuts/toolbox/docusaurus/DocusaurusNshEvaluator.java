package net.thevpc.nuts.toolbox.docusaurus;

import net.thevpc.nsite.context.NSiteContext;
import net.thevpc.nsite.executor.NSiteExprEvaluator;
import net.thevpc.nsite.executor.nsh.ProcessCmd;
import net.thevpc.nsite.util.StringUtils;
import net.thevpc.nuts.NOut;
import net.thevpc.nuts.NSession;
import net.thevpc.nuts.io.NTerminal;
import net.thevpc.nsh.eval.NshContext;
import net.thevpc.nsh.parser.nodes.NshVar;
import net.thevpc.nsh.parser.nodes.NshVarListener;
import net.thevpc.nsh.parser.nodes.NshVariables;
import net.thevpc.nsh.Nsh;
import net.thevpc.nsh.NshConfig;

public class DocusaurusNshEvaluator implements NSiteExprEvaluator {
    private Nsh shell;
    private NSiteContext fileTemplater;

    public DocusaurusNshEvaluator(NSiteContext fileTemplater) {
        this.fileTemplater = fileTemplater;
        shell = new Nsh(new NshConfig().setIncludeDefaultBuiltins(true).setIncludeExternalExecutor(true));
        shell.getRootContext().setSession(shell.getRootContext().getSession().copy());
        shell.getRootContext().vars().addVarListener(
                new NshVarListener() {
                    @Override
                    public void varAdded(NshVar nshVar, NshVariables vars, NshContext context) {
                        setVar(nshVar.getName(), nshVar.getValue());
                    }

                    @Override
                    public void varValueUpdated(NshVar nshVar, String oldValue, NshVariables vars, NshContext context) {
                        setVar(nshVar.getName(), nshVar.getValue());
                    }

                    @Override
                    public void varRemoved(NshVar nshVar, NshVariables vars, NshContext context) {
                        setVar(nshVar.getName(), null);
                    }
                }
        );
        shell.getRootContext()
                .builtins()
                .set(new ProcessCmd(fileTemplater));
    }

    public void setVar(String varName, String newValue) {
        fileTemplater.getLog().debug("eval", varName + "=" + StringUtils.toLiteralString(newValue));
        fileTemplater.setVar(varName, newValue);
    }

    @Override
    public Object eval(String content, NSiteContext context) {
        return NSession.of().copy()
                .setTerminal(NTerminal.ofMem())
                .callWith(
                        () -> {
                            NshContext ctx = shell.createInlineContext(
                                    shell.getRootContext(),
                                    context.getSourcePath().orElseGet(() -> "nsh"), new String[0]
                            );
                            shell.executeScript(content, ctx);
                            return NOut.out().toString();
                        }
                );

    }

    @Override
    public String toString() {
        return "nsh";
    }

}
