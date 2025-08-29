package net.thevpc.nuts.toolbox.docusaurus;

import net.thevpc.nsite.context.NSiteContext;
import net.thevpc.nuts.io.NPath;

class DaucusaurusNSiteContext extends NSiteContext {
    public DaucusaurusNSiteContext() {
        super();
        this.getExecutorManager().setDefaultExecutor("text/ndoc-nsh-project", new DocusaurusNshEvaluator(this));
        setProjectFileName("project.nsh");
    }

    public void executeProjectFile(NPath path, String mimeTypesString) {
        getExecutorManager().executeRegularFile(path, "text/ndoc-nsh-project");
    }
}
