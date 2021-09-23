package game;

import engine.core.*;
import engine.graphics.Camera;
import engine.graphics.Mesh;
import engine.graphics.Renderer;
import engine.graphics.lights.Attenuation;
import engine.graphics.lights.DirectionalLight;
import engine.graphics.lights.PointLight;
import engine.items.complex.GameItem;
import engine.items.complex.SkyBox;
import engine.loaders.MeshLoader;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;

public class Room implements Game {

    private static final float MOUSE_SENSITIVITY = 0.2f;

    private final Vector3f cameraInc;

    private final Renderer renderer;

    private final Camera camera;

    private Scene scene;

    private static final float CAMERA_POS_STEP = 0.40f;

    private float angleInc;

    private float lightAngle;

    private boolean firstTime;

    private Vector3f pointLightPos;

    public Room() {
        renderer = new Renderer();
        camera = new Camera();
        cameraInc = new Vector3f(0.0f, 0.0f, 0.0f);
        angleInc = 0;
        lightAngle = 90;
        firstTime = true;
    }

    @Override
    public void init(Window window) throws Exception {
        renderer.init(window);

        scene = new Scene();

        Mesh[] houseMesh = MeshLoader.load("models/house/house.obj", "models/house");
        GameItem house = new GameItem(houseMesh);

        Mesh[] chairMesh = MeshLoader.load("models/chair/chair.obj", "models/chair");
        GameItem chair = new GameItem(chairMesh);
        chair.setScale(0.05f);
        chair.getPosition().x = -7;
        chair.getPosition().y = 1;
        chair.getPosition().z = -12;

        Mesh[] tableMesh = MeshLoader.load("models/table/table.obj", "models/table");
        GameItem table = new GameItem(tableMesh);

        Mesh[] sofaMesh = MeshLoader.load("models/sofa/sofa.obj", "models/sofa");
        GameItem sofa = new GameItem(sofaMesh);
        sofa.setScale(5);
        sofa.getPosition().z = 20;

        Mesh[] tvMesh = MeshLoader.load("models/tv.obj", "models/");
        GameItem tv = new GameItem(tvMesh);
        tv.setScale(3);
        tv.getPosition().z = 27;

        scene.setGameItems(new GameItem[]{house, chair, table, sofa, tv});

        float skyBoxScale = 100.0f;
        SkyBox skyBox = new SkyBox("models/skybox.obj", new Vector4f(0.65f, 0.65f, 0.65f, 1.0f));
        skyBox.setScale(skyBoxScale);
        scene.setSkyBox(skyBox);

        setupLights();

        camera.getPosition().x = 0;
        camera.getPosition().y = 5;
        camera.getPosition().z = -30;
        camera.getRotation().x = 10;
        camera.getRotation().y = 180;
    }

    private void setupLights() {
        SceneLight sceneLight = new SceneLight();
        scene.setSceneLight(sceneLight);

        sceneLight.setAmbientLight(new Vector3f(0.3f, 0.3f, 0.3f));
        sceneLight.setSkyBoxLight(new Vector3f(1.0f, 1.0f, 1.0f));

        float lightIntensity = 1.0f;
        Vector3f lightDirection = new Vector3f(0, 1, 1);
        DirectionalLight directionalLight = new DirectionalLight(new Vector3f(1, 1, 1), lightDirection, lightIntensity);
        sceneLight.setDirectionalLight(directionalLight);

        pointLightPos = new Vector3f(0.0f, 25.0f, 0.0f);
        Vector3f pointLightColour = new Vector3f(0.0f, 1.0f, 0.0f);
        Attenuation attenuation = new Attenuation(1, 0.0f, 0);
        PointLight pointLight = new PointLight(pointLightColour, pointLightPos, lightIntensity, attenuation);
        sceneLight.setPointLightList(new PointLight[]{pointLight});
    }

    @Override
    public void input(Window window, MouseInput mouseInput) {
        cameraInc.set(0, 0, 0);
        if (window.isKeyPressed(GLFW_KEY_W)) {
            cameraInc.z = -1;
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            cameraInc.z = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            cameraInc.x = -1;
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            cameraInc.x = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_Z)) {
            cameraInc.y = -1;
        } else if (window.isKeyPressed(GLFW_KEY_X)) {
            cameraInc.y = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_LEFT)) {
            angleInc -= 0.05f;
        } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
            angleInc += 0.05f;
        } else {
            angleInc = 0;
        }
        if (window.isKeyPressed(GLFW_KEY_UP)) {
            pointLightPos.y += 0.5f;
        } else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
            pointLightPos.y -= 0.5f;
        }
    }

    @Override
    public void update(float interval, MouseInput mouseInput, Window window) {
        if (mouseInput.isRightButtonPressed()) {
            Vector2f rotVec = mouseInput.getDisplayVector();
            camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);
        }

        camera.movePosition(cameraInc.x * CAMERA_POS_STEP, cameraInc.y * CAMERA_POS_STEP, cameraInc.z * CAMERA_POS_STEP);

        lightAngle += angleInc;
        if (lightAngle < 0) {
            lightAngle = 0;
        } else if (lightAngle > 180) {
            lightAngle = 180;
        }
        float zValue = (float) Math.cos(Math.toRadians(lightAngle));
        float yValue = (float) Math.sin(Math.toRadians(lightAngle));
        Vector3f lightDirection = this.scene.getSceneLight().getDirectionalLight().getDirection();
        lightDirection.x = 0;
        lightDirection.y = yValue;
        lightDirection.z = zValue;
        lightDirection.normalize();

        camera.updateViewMatrix();
    }

    @Override
    public void render(Window window) {
        if (firstTime) {
            firstTime = false;
        }
        renderer.render(window, camera, scene);
    }

    @Override
    public void cleanup() {
        renderer.cleanup();

        scene.cleanup();
    }
}
