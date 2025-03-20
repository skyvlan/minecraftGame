package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {
  private long window;
  private int width = 800;
  private int height = 600;

  private int vaoId;
  private int vboId;
  private int shaderProgram;

  private Camera camera;
  private World world;
  private Physics physics;

  private void cleanup() {
    if (physics != null) {
      physics.cleanup();
    }

    glDeleteProgram(shaderProgram);
    glfwDestroyWindow(window);
    glfwTerminate();
  }

  public void run() {
    init();
    loop();

    // Free the window callbacks and destroy the window
    glfwFreeCallbacks(window);
    glfwDestroyWindow(window);

    // Terminate GLFW and free the error callback
    glfwTerminate();
    glfwSetErrorCallback(null).free();
  }

  private void init() {
    // Setup an error callback
    GLFWErrorCallback.createPrint(System.err).set();
    physics = new Physics();
    world = physics.getWorld();
    // Initialize GLFW
    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW");

    // Configure GLFW
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

    // Create the window
    window = glfwCreateWindow(width, height, "Simple Minecraft Clone", NULL, NULL);
    if (window == NULL)
      throw new RuntimeException("Failed to create the GLFW window");

    // Setup key callback
    glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
      if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
        glfwSetWindowShouldClose(window, true);
    });

    // Get the thread stack and push a new frame
    try (MemoryStack stack = stackPush()) {
      IntBuffer pWidth = stack.mallocInt(1);
      IntBuffer pHeight = stack.mallocInt(1);

      // Get the window size passed to glfwCreateWindow
      glfwGetWindowSize(window, pWidth, pHeight);

      // Get the resolution of the primary monitor
      GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

      // Center the window
      glfwSetWindowPos(
          window,
          (vidmode.width() - pWidth.get(0)) / 2,
          (vidmode.height() - pHeight.get(0)) / 2
      );
    } // the stack frame is popped automatically

    // Make the OpenGL context current
    glfwMakeContextCurrent(window);
    // Enable v-sync
    glfwSwapInterval(1);

    // Make the window visible
    glfwShowWindow(window);

    // This line is critical for LWJGL's interoperation with GLFW's
    // OpenGL context, or any context that is managed externally.
    GL.createCapabilities();

    // Set the clear color
    glClearColor(0.5f, 0.7f, 1.0f, 0.0f);
    glEnable(GL_DEPTH_TEST);

    // Initialize game components
    camera = new Camera(width, height);

    // Setup shaders
    setupShaders();
  }

  private void setupShaders() {
    // Vertex shader
    int vertexShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShader,
        "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "layout (location = 1) in vec3 aColor;\n" +
            "out vec3 ourColor;\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 view;\n" +
            "uniform mat4 model;\n" +
            "void main()\n" +
            "{\n" +
            "   gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
            "   ourColor = aColor;\n" +
            "}\n"
    );
    glCompileShader(vertexShader);

    // Fragment shader
    int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragmentShader,
        "#version 330 core\n" +
            "in vec3 ourColor;\n" +
            "out vec4 FragColor;\n" +
            "void main()\n" +
            "{\n" +
            "   FragColor = vec4(ourColor, 1.0);\n" +
            "}\n"
    );
    glCompileShader(fragmentShader);

    // Link shaders
    shaderProgram = glCreateProgram();
    glAttachShader(shaderProgram, vertexShader);
    glAttachShader(shaderProgram, fragmentShader);
    glLinkProgram(shaderProgram);

    // Delete the shaders as they're linked into our program now and no longer necessary
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
  }

  private void loop() {
    // Initialize input handler
    Input input = new Input(window, camera, physics);

    // Set up timing
    double lastTime = glfwGetTime();
    double deltaTime;

    // Run the rendering loop until the user has attempted to close the window
    while (!glfwWindowShouldClose(window)) {
      // Calculate delta time
      double currentTime = glfwGetTime();
      deltaTime = currentTime - lastTime;
      lastTime = currentTime;

      // Process input
      input.processInput(deltaTime);

      physics.applyPhysics(camera, (float) deltaTime);
      // Render
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

      // Use our shader program
      glUseProgram(shaderProgram);

      // Update camera matrices
      Matrix4f projection = camera.getProjectionMatrix();
      Matrix4f view = camera.getViewMatrix();
      if (camera.getPosition().y <= -10){
        camera.getPosition().y = 10;
        camera.getPosition().x = 0;
        camera.getPosition().z = 0;
      }
      // Set shader uniforms
      int projectionLoc = glGetUniformLocation(shaderProgram, "projection");
      int viewLoc = glGetUniformLocation(shaderProgram, "view");
      int modelLoc = glGetUniformLocation(shaderProgram, "model");

      // Upload matrices to GPU
      try (MemoryStack stack = stackPush()) {
        FloatBuffer projBuffer = stack.mallocFloat(16);
        FloatBuffer viewBuffer = stack.mallocFloat(16);
        FloatBuffer modelBuffer = stack.mallocFloat(16);

        projection.get(projBuffer);
        view.get(viewBuffer);
        new Matrix4f().get(modelBuffer); // Identity matrix for now

        glUniformMatrix4fv(projectionLoc, false, projBuffer);
        glUniformMatrix4fv(viewLoc, false, viewBuffer);
        glUniformMatrix4fv(modelLoc, false, modelBuffer);
      }

      // Render the world
      world.render(camera.getPosition());

      // Swap buffers and poll for window events
      glfwSwapBuffers(window);
      glfwPollEvents();
    }
  }

  public static void main(String[] args) {
    new Main().run();
  }
}