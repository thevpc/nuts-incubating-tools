package net.thevpc.nuts.toolbox.ndb;

import net.thevpc.nuts.NApp;
import net.thevpc.nuts.NAppRunOptions;
import net.thevpc.nuts.NApplication;
import net.thevpc.nuts.NSession;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.toolbox.ndb.nosql.mongodb.NMongoSupport;
import net.thevpc.nuts.toolbox.ndb.sql.derby.NDerbyMain;
import net.thevpc.nuts.toolbox.ndb.sql.nmysql.NMysqlMain;
import net.thevpc.nuts.toolbox.ndb.sql.postgres.NPostgresSupport;

public class NdbMain implements NApplication {

    public static void main(String[] args) {
        new NdbMain().run(NAppRunOptions.ofExit(args));
    }

    @Override
    public void run() {
        run(NApp.of().getCmdLine());
    }

    public void run(NCmdLine cmdLine) {
        NSession session = NSession.get().get();
        while (cmdLine.hasNext()) {
            if (cmdLine.next("mysql", "mariadb").isPresent()) {
                new NMysqlMain(session).run(cmdLine);
                return;
            } else if (cmdLine.next("derby").isPresent()) {
                new NDerbyMain(session).run(cmdLine);
                return;
            } else if (cmdLine.next("mongo", "mongodb").isPresent()) {
                new NMongoSupport(session).run(cmdLine);
                return;
            } else if (cmdLine.next("postgres", "postgresql").isPresent()) {
                new NPostgresSupport(session).run(cmdLine);
                return;
            } else {
                session.configureLast(cmdLine);
            }
        }
        NApp.of().printHelp();
    }
}
