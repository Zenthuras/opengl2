package engine.items.complex;

import engine.graphics.Material;
import engine.graphics.Mesh;
import engine.items.complex.GameItem;
import engine.loaders.MeshLoader;
import org.joml.Vector4f;

public class SkyBox extends GameItem {

    public SkyBox(String objModel, Vector4f colour) throws Exception {
        super();
        Mesh skyBoxMesh = MeshLoader.load(objModel, "", 0)[0];
        Material material = new Material(colour, 0);
        skyBoxMesh.setMaterial(material);
        setMesh(skyBoxMesh);
        setPosition(0, 0, 0);
    }
}
