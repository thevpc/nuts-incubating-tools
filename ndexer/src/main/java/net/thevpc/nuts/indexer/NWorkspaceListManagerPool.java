package net.thevpc.nuts.indexer;

import net.thevpc.nuts.core.NWorkspaceList;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class NWorkspaceListManagerPool {

    private final Map<String, NWorkspaceList> pool = new LinkedHashMap<>();

    public synchronized NWorkspaceList openListManager(String name) {
        NWorkspaceList o = pool.get(name);
        if (o == null) {
            o = NWorkspaceList.of().setName(name);
            pool.put(name, o);
        }
        return o;
    }
}
