package engine.core;

import engine.graphics.InstancedMesh;
import engine.graphics.Mesh;
import engine.items.complex.GameItem;
import engine.items.complex.SkyBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {

    private final Map<Mesh, List<GameItem>> meshMap;

    private final Map<InstancedMesh, List<GameItem>> instancedMeshMap;

    private SkyBox skyBox;

    private SceneLight sceneLight;

    public Scene() {
        meshMap = new HashMap();
        instancedMeshMap = new HashMap();
    }

    public Map<Mesh, List<GameItem>> getGameMeshes() {
        return meshMap;
    }

    public Map<InstancedMesh, List<GameItem>> getGameInstancedMeshes() {
        return instancedMeshMap;
    }

    public void setGameItems(GameItem[] gameItems) {
        int numGameItems = gameItems != null ? gameItems.length : 0;
        for (int i = 0; i < numGameItems; i++) {
            GameItem gameItem = gameItems[i];
            Mesh[] meshes = gameItem.getMeshes();
            for (Mesh mesh : meshes) {
                boolean instancedMesh = mesh instanceof InstancedMesh;
                List<GameItem> list = instancedMesh ? instancedMeshMap.get(mesh) : meshMap.get(mesh);
                if (list == null) {
                    list = new ArrayList<>();
                    if (instancedMesh) {
                        instancedMeshMap.put((InstancedMesh) mesh, list);
                    } else {
                        meshMap.put(mesh, list);
                    }
                }
                list.add(gameItem);
            }
        }
    }

    public void cleanup() {
        for (Mesh mesh : meshMap.keySet()) {
            mesh.cleanUp();
        }
        for (Mesh mesh : instancedMeshMap.keySet()) {
            mesh.cleanUp();
        }
    }

    public SkyBox getSkyBox() {
        return skyBox;
    }

    public void setSkyBox(SkyBox skyBox) {
        this.skyBox = skyBox;
    }

    public SceneLight getSceneLight() {
        return sceneLight;
    }

    public void setSceneLight(SceneLight sceneLight) {
        this.sceneLight = sceneLight;
    }
}
