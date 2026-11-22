package io.github.angelclient.client;

import io.github.angelclient.client.security.Guard;
import net.minecraft.client.main.Main;

public class PreMain {

    public static void main(String[] args) {
        Guard.enforce();
        Main.main(args);
    }

}
