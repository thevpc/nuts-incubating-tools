/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.thevpc.nuts.toolbox.ncode;

import net.thevpc.nuts.*;

/**
 * @author thevpc
 */
public class NCodeMain implements NApplication {
    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @Override
    public void run() {
        NSession session = NSession.of();
        NApp.of().runCmdLine(new NCodeMainCmdProcessor(session));
    }

}
