package org.example;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class Physics {
  private static final float GRAVITY = 9.8f;
  private static final float TERMINAL_VELOCITY = 20.0f;
  private static final float JUMP_FORCE = 8.0f;
  private static final float PLAYER_HEIGHT = 1.8f;
  private static final float PLAYER_WIDTH = 0.6f;

  private final Map<BlockPos, Block> blocks = new HashMap<>();

  public Physics() {
    // Initialize basic terrain
    for (int x = -10; x <= 10; x++) {
      for (int z = -10; z <= 10; z++) {
        blocks.put(new BlockPos(x, 0, z), new Block());
      }
    }
  }

  public boolean isBlockAt(int x, int y, int z) {
    return blocks.containsKey(new BlockPos(x, y, z));
  }

  public void applyPhysics(Camera camera, float deltaTime) {
    Vector3f position = camera.getPosition();
    Vector3f velocity = camera.getVelocity();

    // Apply gravity if not on ground
    if (!isOnGround(camera)) {
      velocity.y -= GRAVITY * deltaTime;
      // Terminal velocity
      if (velocity.y < -TERMINAL_VELOCITY) {
        velocity.y = -TERMINAL_VELOCITY;
      }
    } else if (velocity.y < 0) {
      velocity.y = 0;
    }

    // Calculate next position
    Vector3f nextPos = new Vector3f(
        position.x + velocity.x * deltaTime,
        position.y + velocity.y * deltaTime,
        position.z + velocity.z * deltaTime
    );

    // Check collisions and adjust position
    handleCollisions(camera, nextPos);

    // Update position
    position.set(nextPos);
  }

  public void jump(Camera camera) {
    if (isOnGround(camera)) {
      camera.getVelocity().y = JUMP_FORCE;
    }
  }

  private boolean isOnGround(Camera camera) {
    Vector3f position = camera.getPosition();
    return isBlockAt((int)Math.floor(position.x), (int)Math.floor(position.y - 0.1f), (int)Math.floor(position.z));
  }

  private void handleCollisions(Camera camera, Vector3f nextPos) {
    // Check X-axis collision
    if (checkCollision(nextPos.x, camera.getPosition().y, camera.getPosition().z)) {
      nextPos.x = camera.getPosition().x;
      camera.getVelocity().x = 0;
    }

    // Check Y-axis collision
    if (checkCollision(nextPos.x, nextPos.y, camera.getPosition().z)) {
      if (camera.getVelocity().y > 0) {
        nextPos.y = (float)Math.floor(nextPos.y + PLAYER_HEIGHT) - PLAYER_HEIGHT;
      } else {
        nextPos.y = (float)Math.ceil(nextPos.y) + 0.01f;
      }
      camera.getVelocity().y = 0;
    }

    // Check Z-axis collision
    if (checkCollision(nextPos.x, nextPos.y, nextPos.z)) {
      nextPos.z = camera.getPosition().z;
      camera.getVelocity().z = 0;
    }
  }

  private boolean checkCollision(float x, float y, float z) {
    // Check for collisions with blocks around the player
    float playerHalfWidth = PLAYER_WIDTH / 2;

    for (float checkX = x - playerHalfWidth; checkX <= x + playerHalfWidth; checkX += playerHalfWidth) {
      for (float checkZ = z - playerHalfWidth; checkZ <= z + playerHalfWidth; checkZ += playerHalfWidth) {
        for (float checkY = y; checkY <= y + PLAYER_HEIGHT; checkY += PLAYER_HEIGHT) {
          if (isBlockAt((int)Math.floor(checkX), (int)Math.floor(checkY), (int)Math.floor(checkZ))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  // Simple block data class
  private static class Block {
  }

  // Position class for the HashMap
  private static class BlockPos {
    final int x, y, z;

    BlockPos(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BlockPos)) return false;
      BlockPos blockPos = (BlockPos) o;
      return x == blockPos.x && y == blockPos.y && z == blockPos.z;
    }

    @Override
    public int hashCode() {
      int result = x;
      result = 31 * result + y;
      result = 31 * result + z;
      return result;
    }
  }
}