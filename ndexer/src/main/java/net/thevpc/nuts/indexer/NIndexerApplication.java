package net.thevpc.nuts.indexer;


import net.thevpc.nuts.NApp;
import net.thevpc.nuts.NAppDefinition;
import net.thevpc.nuts.NAppRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@NAppDefinition
@EnableScheduling
@SpringBootApplication
public class NIndexerApplication {

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NAppRunner
    public void run() {

    }


}
