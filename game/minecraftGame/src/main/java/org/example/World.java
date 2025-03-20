package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class World {
  private int vaoId;
  private int vboId;
  private int vertexCount;

  public World() {
    // Initialize the world with a simple flat terrain
    createTerrain();
  }

  private void createTerrain() {
    // Create a simple flat terrain of blocks
    float[] vertices = {
        // Positions             // Colors
        // Bottom face
        -0.5f, -0.5f, -0.5f,     0.5f, 0.5f, 0.5f,
        0.5f, -0.5f, -0.5f,     0.5f, 0.5f, 0.5f,
        0.5f, -0.5f,  0.5f,     0.5f, 0.5f, 0.5f,
        0.5f, -0.5f,  0.5f,     0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f,  0.5f,     0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, -0.5f,     0.5f, 0.5f, 0.5f,

        // Top face
        -0.5f,  0.5f, -0.5f,     0.0f, 1.0f, 0.0f,
        0.5f,  0.5f, -0.5f,     0.0f, 1.0f, 0.0f,
        0.5f,  0.5f,  0.5f,     0.0f, 1.0f, 0.0f,
        0.5f,  0.5f,  0.5f,     0.0f, 1.0f, 0.0f,
        -0.5f,  0.5f,  0.5f,     0.0f, 1.0f, 0.0f,
        -0.5f,  0.5f, -0.5f,     0.0f, 1.0f, 0.0f,

        // Front face
        -0.5f, -0.5f,  0.5f,     0.8f, 0.6f, 0.4f,
        0.5f, -0.5f,  0.5f,     0.8f, 0.6f, 0.4f,
        0.5f,  0.5f,  0.5f,     0.8f, 0.6f, 0.4f,
        0.5f,  0.5f,  0.5f,     0.8f, 0.6f, 0.4f,
        -0.5f,  0.5f,  0.5f,     0.8f, 0.6f, 0.4f,
        -0.5f, -0.5f,  0.5f,     0.8f, 0.6f, 0.4f,

        // Back face
        -0.5f, -0.5f, -0.5f,     0.8f, 0.6f, 0.4f,
        0.5f, -0.5f, -0.5f,     0.8f, 0.6f, 0.4f,
        0.5f,  0.5f, -0.5f,     0.8f, 0.6f, 0.4f,
        0.5f,  0.5f, -0.5f,     0.8f, 0.6f, 0.4f,
        -0.5f,  0.5f, -0.5f,     0.8f, 0.6f, 0.4f,
        -0.5f, -0.5f, -0.5f,     0.8f, 0.6f, 0.4f,

        // Left face
        -0.5f, -0.5f, -0.5f,     0.6f, 0.6f, 0.6f,
        -0.5f, -0.5f,  0.5f,     0.6f, 0.6f, 0.6f,
        -0.5f,  0.5f,  0.5f,     0.6f, 0.6f, 0.6f,
        -0.5f,  0.5f,  0.5f,     0.6f, 0.6f, 0.6f,
        -0.5f,  0.5f, -0.5f,     0.6f, 0.6f, 0.6f,
        -0.5f, -0.5f, -0.5f,     0.6f, 0.6f, 0.6f,

        // Right face
        0.5f, -0.5f, -0.5f,     0.6f, 0.6f, 0.6f,
        0.5f, -0.5f,  0.5f,     0.6f, 0.6f, 0.6f,
        0.5f,  0.5f,  0.5f,     0.6f, 0.6f, 0.6f,
        0.5f,  0.5f,  0.5f,     0.6f, 0.6f, 0.6f,
        0.5f,  0.5f, -0.5f,     0.6f, 0.6f, 0.6f,
        0.5f, -0.5f, -0.5f,     0.6f, 0.6f, 0.6f
    };

    vertexCount = vertices.length / 6; // 6 elements per vertex (3 position, 3 color)

    // Create and bind VAO
    vaoId = glGenVertexArrays();
    glBindVertexArray(vaoId);

    // Create and bind VBO, and upload vertex data
    vboId = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vboId);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

    // Position attribute
    glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
    glEnableVertexAttribArray(0);

    // Color attribute
    glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
    glEnableVertexAttribArray(1);

    // Unbind
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  public void render() {
    glBindVertexArray(vaoId);

    // Draw multiple blocks to create a simple terrain
    for (int x = -10; x <= 10; x++) {
      for (int z = -10; z <= 10; z++) {
        // Draw a block at (x, 0, z)
        Matrix4f model = new Matrix4f().translate(x, 0, z);

        // Get uniform location
        int modelLoc = glGetUniformLocation(glGetInteger(GL_CURRENT_PROGRAM), "model");

        // Upload matrix to shader
        try (MemoryStack stack = MemoryStack.stackPush()) {
          FloatBuffer modelBuffer = stack.mallocFloat(16);
          model.get(modelBuffer);
          glUniformMatrix4fv(modelLoc, false, modelBuffer);
        }

        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
      }
    }

    glBindVertexArray(0);
  }
}