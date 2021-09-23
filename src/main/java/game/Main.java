package game;

import engine.core.Game;
import engine.core.GameEngine;
import engine.core.Window;

public class Main {

    public static void main(String[] args) {
        try {
            Game game = new Room();
            Window.WindowOptions opts = new Window.WindowOptions();
            opts.cullFace = false;
            opts.showFps = true;
            opts.compatibleProfile = true;
            opts.antialiasing = true;
            opts.frustumCulling = false;
            GameEngine gameEng = new GameEngine("PGRF2 - OpenGL", true, opts, game);
            gameEng.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
