package org.example;

import org.joml.Vector3f;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import java.util.HashMap;
import java.util.Map;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

public class Chunk {
  private static final int CHUNK_SIZE = World.CHUNK_SIZE;
  private final Map<BlockPos, Block> blocks = new HashMap<>();
  private boolean isInitialized = false;
  private int vboId = -1;
  private int instanceVBO = -1;
  private static int vaoId = -1;
  private static boolean vaoInitialized = false;
  private static final int MAX_INSTANCES = 4096;

  public Chunk(int chunkX, int chunkZ, PerlinNoise noise) {
    // Add to Chunk constructor
    generateTerrain(chunkX, chunkZ, noise);
    System.out.println("Generating chunk at " + chunkX + "," + chunkZ + " with " + blocks.size() + " blocks");

  }

  private void generateTerrain(int chunkX, int chunkZ, PerlinNoise noise) {
    int maxHeight = 32;  // Increased from 16
    int minHeight = 4;   // Added minimum height

    for (int x = 0; x < CHUNK_SIZE; x++) {
      for (int z = 0; z < CHUNK_SIZE; z++) {
        // Use absolute coordinates for noise
        double nx = (chunkX * CHUNK_SIZE + x) / 32.0;  // Changed from 20.0
        double nz = (chunkZ * CHUNK_SIZE + z) / 32.0;

        // Apply multiple octaves with varying frequencies
        double height = noise.octaveNoise(nx, 0, nz, 4, 0.5);

        // Normalize to 0-1 range
        height = (height + 1.0) * 0.5;

        // Add variation
        double detailNoise = noise.noise(nx * 4, 0, nz * 4) * 0.1;  // Increased from 0.05
        height += detailNoise;

        // Clamp height to valid range
        height = Math.max(0, Math.min(1, height));

        // Scale to desired height
        int y = (int)(minHeight + height * maxHeight);

        // Create blocks
        for (int blockY = 0; blockY <= y; blockY++) {
          int blockType;
          if (blockY == y) {
            blockType = World.GRASS;
          } else if (blockY > y - 3) {
            blockType = World.DIRT;
          } else {
            blockType = World.STONE;
          }

          blocks.put(new BlockPos(x, blockY, z), new Block(blockType));
        }
      }
    }
  }

  public boolean isBlockAt(int x, int y, int z) {
    BlockPos pos = new BlockPos(x, y, z);
    Block block = blocks.get(pos);
    return block != null && block.type != World.AIR;
  }

  public void render(int chunkX, int chunkZ, Vector3f playerPosition, Map<Integer, Integer> blockTypeCount) {
    // Simple distance-based culling
    float chunkCenterX = chunkX * CHUNK_SIZE + CHUNK_SIZE / 2.0f;
    float chunkCenterZ = chunkZ * CHUNK_SIZE + CHUNK_SIZE / 2.0f;
    float dx = playerPosition.x - chunkCenterX;
    float dz = playerPosition.z - chunkCenterZ;
    float distSquared = dx * dx + dz * dz;

    // Skip if too far away (square of render distance × chunk size)
    if (distSquared > (World.RENDER_DISTANCE * CHUNK_SIZE) * (World.RENDER_DISTANCE * CHUNK_SIZE)) {
      return;
    }

    // Render visible blocks
    for (Map.Entry<BlockPos, Block> entry : blocks.entrySet()) {
      BlockPos pos = entry.getKey();
      Block block = entry.getValue();

      if (block.type == World.AIR) continue;

      // Skip block if all six faces are covered
      if (!isVisibleFace(pos.x, pos.y, pos.z, chunkX, chunkZ)) continue;

      // Count blocks by type for stats
      blockTypeCount.merge(block.type, 1, Integer::sum);

      // Render block with instanced rendering or direct calls depending on implementation
      renderBlock(pos.x + chunkX * CHUNK_SIZE, pos.y, pos.z + chunkZ * CHUNK_SIZE, block.type);
    }
  }

  private boolean isVisibleFace(int x, int y, int z, int chunkX, int chunkZ) {
    // Check if block has at least one exposed face
    int worldX = x + chunkX * CHUNK_SIZE;
    int worldZ = z + chunkZ * CHUNK_SIZE;

    return !isBlockAt(x+1, y, z) || !isBlockAt(x-1, y, z) ||
        !isBlockAt(x, y+1, z) || !isBlockAt(x, y-1, z) ||
        !isBlockAt(x, y, z+1) || !isBlockAt(x, y, z-1);
  }

  private void renderBlock(int x, int y, int z, int blockType) {
    // Get the current shader program
    int program = glGetInteger(GL_CURRENT_PROGRAM);
    int modelLoc = glGetUniformLocation(program, "model");

    // This uniform doesn't exist in your shader!
    // int colorLoc = glGetUniformLocation(program, "blockColor");

    // Use the existing "ourColor" instead
    Vector3f color = new Vector3f();
    switch (blockType) {
      case World.GRASS:
        color.set(0.0f, 0.8f, 0.0f);
        break;
      case World.DIRT:
        color.set(0.6f, 0.4f, 0.2f);
        break;
      case World.STONE:
        color.set(0.5f, 0.5f, 0.5f);
        break;
    }

    // Create and upload model matrix
    try (MemoryStack stack = MemoryStack.stackPush()) {
      Matrix4f model = new Matrix4f().translate(x, y, z);
      FloatBuffer modelBuffer = stack.mallocFloat(16);
      model.get(modelBuffer);
      glUniformMatrix4fv(modelLoc, false, modelBuffer);

      // Draw a unit cube centered at the origin
      renderCube(color);
    }
  }

  private void renderCube(Vector3f color) {
    // Create static VAO for cube if not already initialized
    if (!vaoInitialized) {
      // Define vertices for a unit cube centered at the origin
      float[] vertices = {
          // Front face
          -0.5f, -0.5f,  0.5f,
          0.5f, -0.5f,  0.5f,
          0.5f,  0.5f,  0.5f,
          -0.5f,  0.5f,  0.5f,
          // Back face
          -0.5f, -0.5f, -0.5f,
          -0.5f,  0.5f, -0.5f,
          0.5f,  0.5f, -0.5f,
          0.5f, -0.5f, -0.5f,
          // Top face
          -0.5f,  0.5f, -0.5f,
          -0.5f,  0.5f,  0.5f,
          0.5f,  0.5f,  0.5f,
          0.5f,  0.5f, -0.5f,
          // Bottom face
          -0.5f, -0.5f, -0.5f,
          0.5f, -0.5f, -0.5f,
          0.5f, -0.5f,  0.5f,
          -0.5f, -0.5f,  0.5f,
          // Right face
          0.5f, -0.5f, -0.5f,
          0.5f,  0.5f, -0.5f,
          0.5f,  0.5f,  0.5f,
          0.5f, -0.5f,  0.5f,
          // Left face
          -0.5f, -0.5f, -0.5f,
          -0.5f, -0.5f,  0.5f,
          -0.5f,  0.5f,  0.5f,
          -0.5f,  0.5f, -0.5f
      };

      // Define indices for cube
      int[] indices = {
          0, 1, 2, 2, 3, 0,       // Front face
          4, 5, 6, 6, 7, 4,       // Back face
          8, 9, 10, 10, 11, 8,    // Top face
          12, 13, 14, 14, 15, 12, // Bottom face
          16, 17, 18, 18, 19, 16, // Right face
          20, 21, 22, 22, 23, 20  // Left face
      };

      // Create VAO and VBO
      vaoId = glGenVertexArrays();
      glBindVertexArray(vaoId);

      // Store vertex data
      int vbo = glGenBuffers();
      glBindBuffer(GL_ARRAY_BUFFER, vbo);
      glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
      glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
      glEnableVertexAttribArray(0);

      // Store indices
      int ebo = glGenBuffers();
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
      glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

      // Unbind VAO
      glBindVertexArray(0);
      vaoInitialized = true;
    }

    // Bind VAO and render
    glBindVertexArray(vaoId);

    // Set color attribute
    glVertexAttrib3f(1, color.x, color.y, color.z);

    // Draw cube
    glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);

    // Unbind VAO
    glBindVertexArray(0);
  }

  public void cleanup() {
    if (vboId != -1) {
      glDeleteBuffers(vboId);
      vboId = -1;
    }
    if (instanceVBO != -1) {
      glDeleteBuffers(instanceVBO);
      instanceVBO = -1;
    }
  }
}