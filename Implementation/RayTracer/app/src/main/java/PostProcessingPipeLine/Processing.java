package PostProcessingPipeLine;

import Util.StateManager;

public class Processing {

    private static ToScreenPostEffect toScreenPostEffect;

    public static void init() {
        toScreenPostEffect = new ToScreenPostEffect();
    }

    public static void postToScreen(int sceneColourTexture) {
        toScreenPostEffect.render(sceneColourTexture);
    }
}
