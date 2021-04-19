package PostProcessingPipeLine;

import Data.VAO;
import Util.StateManager;
import Programs.ToScreenShaderProgram;

public class ToScreenPostEffect {

    private static final float[] VERTEX_DATA = {-1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f};

    private static VAO vao;

    private ImageRenderer imageRenderer;
    private ToScreenShaderProgram toScreenShaderProgram;

    public ToScreenPostEffect() {
        toScreenShaderProgram = new ToScreenShaderProgram(StateManager.getContext());
        imageRenderer = new ImageRenderer();

        vao = new VAO(VERTEX_DATA);
    }

    public void render(int texture) {
        toScreenShaderProgram.useProgram();
        toScreenShaderProgram.setUniforms(texture);
        imageRenderer.renderQuad(vao);
    }
}
