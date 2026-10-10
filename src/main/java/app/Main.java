package app;

import io.mangoo.core.Application;
import io.mangoo.enums.Mode;

public class Main {
    public static void main(String[] args) {
        Application.start(Mode.DEV);
    }
}