package net.thevpc.nuts.indexer;


import net.thevpc.nuts.NAppRunOptions;
import net.thevpc.nuts.NApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NIndexerApplication implements NApplication {

    public static void main(String[] args) {
        new NIndexerApplication().run(NAppRunOptions.ofExit(args));
    }

    @Override
    public void run() {

    }


}
