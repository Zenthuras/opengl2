package engine.graphics;

import engine.utils.FileUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {

    private final int id;

    private final int width;

    private final int height;

    public Texture(String fileName) throws Exception {
        this(FileUtils.ioResourceToByteBuffer(fileName, 1024));
    }

    public Texture(ByteBuffer imageData) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer avChannels = stack.mallocInt(1);

            ByteBuffer decodedImage = stbi_load_from_memory(imageData, w, h, avChannels, 4);

            this.width = w.get();
            this.height = h.get();

            this.id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, this.id);

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.width, this.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, decodedImage);
            glGenerateMipmap(GL_TEXTURE_2D);

            assert decodedImage != null;
            stbi_image_free(decodedImage);
        }
    }

    public int getNumCols() {
        return 1;
    }

    public int getNumRows() {
        return 1;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getId() {
        return id;
    }

    public void cleanup() {
        glDeleteTextures(id);
    }
}
