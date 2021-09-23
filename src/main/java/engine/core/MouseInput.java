package engine.core;

import org.joml.Vector2d;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public class MouseInput {

    private final Vector2d previousPos;

    private final Vector2d currentPos;

    private final Vector2f displayVector;

    private boolean inWindow = false;

    private boolean rightButtonPressed = false;

    public MouseInput() {
        previousPos = new Vector2d(-1, -1);
        currentPos = new Vector2d(0, 0);
        displayVector = new Vector2f();
    }

    public void init(Window window) {
        glfwSetCursorPosCallback(window.getWindowHandle(), (windowHandle, xPos, yPos) -> {
            currentPos.x = xPos;
            currentPos.y = yPos;
        });
        glfwSetCursorEnterCallback(window.getWindowHandle(), (windowHandle, entered) -> inWindow = entered);
        glfwSetMouseButtonCallback(window.getWindowHandle(), (windowHandle, button, action, mode) -> rightButtonPressed = button == GLFW_MOUSE_BUTTON_2 && action == GLFW_PRESS);
    }

    public Vector2f getDisplayVector() {
        return displayVector;
    }

    public void input() {
        displayVector.x = 0;
        displayVector.y = 0;
        if (previousPos.x > 0 && previousPos.y > 0 && inWindow) {
            double deltaX = currentPos.x - previousPos.x;
            double deltaY = currentPos.y - previousPos.y;
            boolean rotateX = deltaX != 0;
            boolean rotateY = deltaY != 0;
            if (rotateX) {
                displayVector.y = (float) deltaX;
            }
            if (rotateY) {
                displayVector.x = (float) deltaY;
            }
        }
        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;
    }

    public boolean isRightButtonPressed() {
        return rightButtonPressed;
    }
}
