package org.example;

import static org.lwjgl.glfw.GLFW.*;

public class Input {
  private long window;
  private Camera camera;
  private Physics physics;
  private boolean firstMouse = true;
  private double lastX, lastY;
  private boolean spacePressed = false;

  public Input(long window, Camera camera, Physics physics) {
    this.window = window;
    this.camera = camera;
    this.physics = physics;

    // Capture the cursor
    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

    // Set up mouse callback
    glfwSetCursorPosCallback(window, (windowHandle, xpos, ypos) -> {
      if (firstMouse) {
        lastX = xpos;
        lastY = ypos;
        firstMouse = false;
      }

      double xOffset = xpos - lastX;
      double yOffset = ypos - lastY; // Not reversed for natural movement
      lastX = xpos;
      lastY = ypos;

      camera.look((float) xOffset, (float) -yOffset);
    });
  }

  public void processInput(double deltaTime) {
    // Reset horizontal velocity
    camera.getVelocity().x = 0;
    camera.getVelocity().z = 0;

    // Process keyboard input for camera movement
    if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
      camera.moveForward((float) deltaTime);
      System.out.println("X:" + camera.getPosition().x);
      System.out.println("Y:" + camera.getPosition().y);
      System.out.println("Z:" + camera.getPosition().z);

    if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
      camera.moveBackward((float) deltaTime);
    if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
      camera.moveLeft((float) deltaTime);
    if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
      camera.moveRight((float) deltaTime);

    // Jump with space bar
    if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && !spacePressed) {
      physics.jump(camera);
      spacePressed = true;
    }
    if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_RELEASE) {
      spacePressed = false;
    }
  }
}