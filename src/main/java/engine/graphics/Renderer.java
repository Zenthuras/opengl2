package engine.graphics;

import engine.core.Scene;
import engine.core.SceneLight;
import engine.utils.FileUtils;
import engine.core.Window;
import engine.graphics.lights.DirectionalLight;
import engine.graphics.lights.PointLight;
import engine.items.complex.GameItem;
import engine.items.complex.SkyBox;
import engine.items.simple.Part;
import engine.items.simple.Solid;
import engine.loaders.MeshLoader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private final Transformation transformation;

    private Shader skyBoxShader;

    private Shader gBufferShader;

    private Shader dirLightShader;

    private Shader pointLightShader;

    private Shader fogShader;

    private final float specularPower;

    private final List<GameItem> filteredItems;

    private GBuffer gBuffer;

    private SceneBuffer sceneBuffer;

    private Mesh bufferPassMesh;

    private Matrix4f bufferPassModelMatrix;

    private final Vector4f tmpVec;

    public Renderer() {
        transformation = new Transformation();
        specularPower = 10f;
        filteredItems = new ArrayList<>();
        tmpVec = new Vector4f();
    }

    public void init(Window window) throws Exception {
        gBuffer = new GBuffer(window);
        sceneBuffer = new SceneBuffer(window);
        setupSkyBoxShader();
        setupGeometryShader();
        setupDirLightShader();
        setupPointLightShader();
        setupFogShader();

        bufferPassModelMatrix = new Matrix4f();
        bufferPassMesh = MeshLoader.load("models/buffer.obj", "models")[0];
    }

    public void render(Window window, Camera camera, Scene scene) {
        clear();

        glViewport(0, 0, window.getWidth(), window.getHeight());

        window.updateProjectionMatrix();

        renderGeometry(window, camera, scene);

        initLightRendering();
        renderPointLights(window, camera, scene);
        renderDirectionalLight(window, camera, scene);
        endLightRendering();

        renderFog(window, camera, scene);
        renderSkyBox(window, camera, scene);
    }

    private void renderSolids(List<Solid> solids) {
        for (Solid solid : solids) {
            renderSolid(solid);
        }
    }

    private void renderSolid(Solid solid) {

        for (Part part : solid.getParts()) {
            switch (part.getType()) {
                case POINTS -> {
                    GL11.glBegin(GL11.GL_POINT);
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = part.getStartX() + i * 2;
                        drawVertex(solid, indexA);
                        GL11.glEnd();
                    }
                }
                case LINES -> {
                    GL11.glBegin(GL11.GL_LINES);
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = part.getStartX() + i * 2;
                        int indexB = part.getStartX() + i * 2 + 1;
                        drawVertex(solid, indexA);
                        drawVertex(solid, indexB);
                    }
                    GL11.glEnd();
                }
                case TRIANGLES -> {
                    GL11.glBegin(GL11.GL_TRIANGLES);
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = part.getStartX() + i * 3;
                        int indexB = part.getStartX() + i * 3 + 1;
                        int indexC = part.getStartX() + i * 3 + 2;
                        drawVertex(solid, indexA);
                        drawVertex(solid, indexB);
                        drawVertex(solid, indexC);
                    }
                }
                case QUADS -> {
                    GL11.glBegin(GL11.GL_QUADS);
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = part.getStartX() + i * 4;
                        int indexB = part.getStartX() + i * 4 + 1;
                        int indexC = part.getStartX() + i * 4 + 2;
                        int indexD = part.getStartX() + i * 4 + 3;
                        drawVertex(solid, indexA);
                        drawVertex(solid, indexB);
                        drawVertex(solid, indexC);
                        drawVertex(solid, indexD);
                    }
                    GL11.glEnd();
                }
            }
        }

    }

    private void drawVertex(Solid solid, int index) {
        GL11.glVertex3f(solid.getVertices().get(solid.getIndices().get(index)).x, solid.getVertices().get(solid.getIndices().get(index)).y, solid.getVertices().get(solid.getIndices().get(index)).z);
    }

    private void setupSkyBoxShader() throws Exception {
        skyBoxShader = new Shader();
        skyBoxShader.createVertexShader(FileUtils.loadResource("/shaders/sb_vertex.vs"));
        skyBoxShader.createFragmentShader(FileUtils.loadResource("/shaders/sb_fragment.fs"));
        skyBoxShader.link();

        skyBoxShader.createUniform("projectionMatrix");
        skyBoxShader.createUniform("modelViewMatrix");
        skyBoxShader.createUniform("texture_sampler");
        skyBoxShader.createUniform("ambientLight");
        skyBoxShader.createUniform("colour");
        skyBoxShader.createUniform("hasTexture");

        skyBoxShader.createUniform("depthsText");
        skyBoxShader.createUniform("screenSize");
    }

    private void setupGeometryShader() throws Exception {
        gBufferShader = new Shader();
        gBufferShader.createVertexShader(FileUtils.loadResource("/shaders/gbuffer_vertex.vs"));
        gBufferShader.createFragmentShader(FileUtils.loadResource("/shaders/gbuffer_fragment.fs"));
        gBufferShader.link();

        gBufferShader.createUniform("projectionMatrix");
        gBufferShader.createUniform("viewMatrix");
        gBufferShader.createUniform("texture_sampler");
        gBufferShader.createUniform("normalMap");
        gBufferShader.createMaterialUniform("material");
        gBufferShader.createUniform("isInstanced");
        gBufferShader.createUniform("modelNonInstancedMatrix");
        gBufferShader.createUniform("selectedNonInstanced");
        gBufferShader.createUniform("jointsMatrix");
        gBufferShader.createUniform("numCols");
        gBufferShader.createUniform("numRows");
    }

    private void setupDirLightShader() throws Exception {
        dirLightShader = new Shader();
        dirLightShader.createVertexShader(FileUtils.loadResource("/shaders/light_vertex.vs"));
        dirLightShader.createFragmentShader(FileUtils.loadResource("/shaders/dir_light_fragment.fs"));
        dirLightShader.link();

        dirLightShader.createUniform("modelMatrix");
        dirLightShader.createUniform("projectionMatrix");

        dirLightShader.createUniform("screenSize");
        dirLightShader.createUniform("positionsText");
        dirLightShader.createUniform("diffuseText");
        dirLightShader.createUniform("specularText");
        dirLightShader.createUniform("normalsText");
        dirLightShader.createUniform("shadowText");

        dirLightShader.createUniform("specularPower");
        dirLightShader.createUniform("ambientLight");
        dirLightShader.createDirectionalLightUniform("directionalLight");
    }

    private void setupPointLightShader() throws Exception {
        pointLightShader = new Shader();
        pointLightShader.createVertexShader(FileUtils.loadResource("/shaders/light_vertex.vs"));
        pointLightShader.createFragmentShader(FileUtils.loadResource("/shaders/point_light_fragment.fs"));
        pointLightShader.link();

        pointLightShader.createUniform("modelMatrix");
        pointLightShader.createUniform("projectionMatrix");

        pointLightShader.createUniform("screenSize");
        pointLightShader.createUniform("positionsText");
        pointLightShader.createUniform("diffuseText");
        pointLightShader.createUniform("specularText");
        pointLightShader.createUniform("normalsText");
        pointLightShader.createUniform("shadowText");

        pointLightShader.createUniform("specularPower");
        pointLightShader.createPointLightUniform("pointLight");
    }

    private void setupFogShader() throws Exception {
        fogShader = new Shader();
        fogShader.createVertexShader(FileUtils.loadResource("/shaders/light_vertex.vs"));
        fogShader.createFragmentShader(FileUtils.loadResource("/shaders/fog_fragment.fs"));
        fogShader.link();

        fogShader.createUniform("modelMatrix");
        fogShader.createUniform("viewMatrix");
        fogShader.createUniform("projectionMatrix");

        fogShader.createUniform("screenSize");
        fogShader.createUniform("positionsText");
        fogShader.createUniform("depthText");
        fogShader.createUniform("sceneText");

        fogShader.createFogUniform("fog");
        fogShader.createUniform("ambientLight");
        fogShader.createUniform("lightColour");
        fogShader.createUniform("lightIntensity");
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    private void renderGeometry(Window window, Camera camera, Scene scene) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, gBuffer.getGBufferId());

        clear();

        glDisable(GL_BLEND);

        gBufferShader.bind();

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        gBufferShader.setUniform("viewMatrix", viewMatrix);
        gBufferShader.setUniform("projectionMatrix", projectionMatrix);

        gBufferShader.setUniform("texture_sampler", 0);
        gBufferShader.setUniform("normalMap", 1);

        renderNonInstancedMeshes(scene);

        renderInstancedMeshes(scene, viewMatrix);

        gBufferShader.unbind();

        glEnable(GL_BLEND);
    }

    private void initLightRendering() {
        glBindFramebuffer(GL_FRAMEBUFFER, sceneBuffer.getBufferId());

        clear();

        glDisable(GL_DEPTH_TEST);

        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE, GL_ONE);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, gBuffer.getGBufferId());
    }

    private void endLightRendering() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    private void renderPointLights(Window window, Camera camera, Scene scene) {
        pointLightShader.bind();

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        pointLightShader.setUniform("modelMatrix", bufferPassModelMatrix);
        pointLightShader.setUniform("projectionMatrix", projectionMatrix);

        pointLightShader.setUniform("specularPower", specularPower);

        int[] textureIds = this.gBuffer.getTextureIds();
        int numTextures = textureIds != null ? textureIds.length : 0;
        for (int i = 0; i < numTextures; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }

        pointLightShader.setUniform("positionsText", 0);
        pointLightShader.setUniform("diffuseText", 1);
        pointLightShader.setUniform("specularText", 2);
        pointLightShader.setUniform("normalsText", 3);
        pointLightShader.setUniform("shadowText", 4);

        pointLightShader.setUniform("screenSize", (float) gBuffer.getWidth(), (float) gBuffer.getHeight());

        SceneLight sceneLight = scene.getSceneLight();
        PointLight[] pointLights = sceneLight.getPointLightList();
        int numPointLights = pointLights != null ? pointLights.length : 0;
        for (int i = 0; i < numPointLights; i++) {
            PointLight currPointLight = new PointLight(pointLights[i]);
            Vector3f lightPos = currPointLight.getPosition();
            tmpVec.set(lightPos, 1);
            tmpVec.mul(viewMatrix);
            lightPos.x = tmpVec.x;
            lightPos.y = tmpVec.y;
            lightPos.z = tmpVec.z;
            pointLightShader.setUniform("pointLight", currPointLight);

            bufferPassMesh.render();
        }

        pointLightShader.unbind();
    }

    private void renderDirectionalLight(Window window, Camera camera, Scene scene) {
        dirLightShader.bind();

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        dirLightShader.setUniform("modelMatrix", bufferPassModelMatrix);
        dirLightShader.setUniform("projectionMatrix", projectionMatrix);

        dirLightShader.setUniform("specularPower", specularPower);

        int[] textureIds = this.gBuffer.getTextureIds();
        int numTextures = textureIds != null ? textureIds.length : 0;
        for (int i = 0; i < numTextures; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }

        dirLightShader.setUniform("positionsText", 0);
        dirLightShader.setUniform("diffuseText", 1);
        dirLightShader.setUniform("specularText", 2);
        dirLightShader.setUniform("normalsText", 3);
        dirLightShader.setUniform("shadowText", 4);

        dirLightShader.setUniform("screenSize", (float) gBuffer.getWidth(), (float) gBuffer.getHeight());

        SceneLight sceneLight = scene.getSceneLight();
        dirLightShader.setUniform("ambientLight", sceneLight.getAmbientLight());

        DirectionalLight currDirLight = new DirectionalLight(sceneLight.getDirectionalLight());
        tmpVec.set(currDirLight.getDirection(), 0);
        tmpVec.mul(viewMatrix);
        currDirLight.setDirection(new Vector3f(tmpVec.x, tmpVec.y, tmpVec.z));
        dirLightShader.setUniform("directionalLight", currDirLight);

        bufferPassMesh.render();

        dirLightShader.unbind();
    }

    private void renderFog(Window window, Camera camera, Scene scene) {
        fogShader.bind();

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        fogShader.setUniform("modelMatrix", bufferPassModelMatrix);
        fogShader.setUniform("viewMatrix", viewMatrix);
        fogShader.setUniform("projectionMatrix", projectionMatrix);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getPositionTexture());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getDepthTexture());
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, sceneBuffer.getTextureId());

        fogShader.setUniform("positionsText", 0);
        fogShader.setUniform("depthText", 1);
        fogShader.setUniform("sceneText", 2);

        fogShader.setUniform("screenSize", (float) window.getWidth(), (float) window.getHeight());

        SceneLight sceneLight = scene.getSceneLight();
        fogShader.setUniform("ambientLight", sceneLight.getAmbientLight());
        DirectionalLight dirLight = sceneLight.getDirectionalLight();
        fogShader.setUniform("lightColour", dirLight.getColor());
        fogShader.setUniform("lightIntensity", dirLight.getIntensity());

        bufferPassMesh.render();

        fogShader.unbind();
    }


    private void renderSkyBox(Window window, Camera camera, Scene scene) {
        SkyBox skyBox = scene.getSkyBox();
        if (skyBox != null) {
            skyBoxShader.bind();

            skyBoxShader.setUniform("texture_sampler", 0);

            Matrix4f projectionMatrix = window.getProjectionMatrix();
            skyBoxShader.setUniform("projectionMatrix", projectionMatrix);
            Matrix4f viewMatrix = camera.getViewMatrix();
            float m30 = viewMatrix.m30();
            viewMatrix.m30(0);
            float m31 = viewMatrix.m31();
            viewMatrix.m31(0);
            float m32 = viewMatrix.m32();
            viewMatrix.m32(0);

            Mesh mesh = skyBox.getMesh();
            Matrix4f modelViewMatrix = transformation.buildModelViewMatrix(skyBox, viewMatrix);
            skyBoxShader.setUniform("modelViewMatrix", modelViewMatrix);
            skyBoxShader.setUniform("ambientLight", scene.getSceneLight().getSkyBoxLight());
            skyBoxShader.setUniform("colour", mesh.getMaterial().getDiffuseColour());
            skyBoxShader.setUniform("hasTexture", mesh.getMaterial().isTextured() ? 1 : 0);

            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, gBuffer.getDepthTexture());
            skyBoxShader.setUniform("screenSize", (float) window.getWidth(), (float) window.getHeight());
            skyBoxShader.setUniform("depthsText", 1);

            mesh.render();

            viewMatrix.m30(m30);
            viewMatrix.m31(m31);
            viewMatrix.m32(m32);
            skyBoxShader.unbind();
        }
    }

    private void renderNonInstancedMeshes(Scene scene) {
        gBufferShader.setUniform("isInstanced", 0);

        Map<Mesh, List<GameItem>> mapMeshes = scene.getGameMeshes();
        for (Mesh mesh : mapMeshes.keySet()) {
            gBufferShader.setUniform("material", mesh.getMaterial());

            Texture text = mesh.getMaterial().getTexture();
            if (text != null) {
                gBufferShader.setUniform("numCols", text.getNumCols());
                gBufferShader.setUniform("numRows", text.getNumRows());
            }

            mesh.renderList(mapMeshes.get(mesh), (GameItem gameItem) -> {
                        gBufferShader.setUniform("selectedNonInstanced", gameItem.isSelected() ? 1.0f : 0.0f);
                        Matrix4f modelMatrix = transformation.buildModelMatrix(gameItem);
                        gBufferShader.setUniform("modelNonInstancedMatrix", modelMatrix);
                    }
            );
        }
    }

    private void renderInstancedMeshes(Scene scene, Matrix4f viewMatrix) {
        gBufferShader.setUniform("isInstanced", 1);

        Map<InstancedMesh, List<GameItem>> mapMeshes = scene.getGameInstancedMeshes();
        for (InstancedMesh mesh : mapMeshes.keySet()) {
            Texture text = mesh.getMaterial().getTexture();
            if (text != null) {
                gBufferShader.setUniform("numCols", text.getNumCols());
                gBufferShader.setUniform("numRows", text.getNumRows());
            }

            gBufferShader.setUniform("material", mesh.getMaterial());

            filteredItems.clear();
            for (GameItem gameItem : mapMeshes.get(mesh)) {
                if (gameItem.isInsideFrustum()) {
                    filteredItems.add(gameItem);
                }
            }

            mesh.renderListInstanced(filteredItems, transformation, viewMatrix);
        }
    }

    public void cleanup() {
        if (skyBoxShader != null) {
            skyBoxShader.cleanup();
        }
        if (gBufferShader != null) {
            gBufferShader.cleanup();
        }
        if (dirLightShader != null) {
            dirLightShader.cleanup();
        }
        if (pointLightShader != null) {
            pointLightShader.cleanup();
        }
        if (gBuffer != null) {
            gBuffer.cleanUp();
        }
        if (bufferPassMesh != null) {
            bufferPassMesh.cleanUp();
        }
    }
}
