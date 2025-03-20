package org.example;

import org.joml.Vector3f;

public class Physics {
  private static final float GRAVITY = 9.8f;
  private static final float TERMINAL_VELOCITY = 20.0f;
  private static final float JUMP_FORCE = 8.0f;
  private static final float PLAYER_HEIGHT = 1.8f;
  private static final float PLAYER_WIDTH = 0.6f;

  private final World world;
  private final Vector3f tempVec = new Vector3f();

  public Physics() {
    this.world = new World();
  }

  public World getWorld() {
    return world;
  }

  public void applyPhysics(Camera camera, float deltaTime) {
    if (deltaTime > 0.1f) {
      // Prevent large time steps that could cause physics glitches
      deltaTime = 0.1f;
    }

    Vector3f position = camera.getPosition();
    Vector3f velocity = camera.getVelocity();

    // Update chunks based on player position
    world.updateChunks(position);

    // Apply gravity if not on ground
    if (!isOnGround(camera)) {
      velocity.y -= GRAVITY * deltaTime;
      if (velocity.y < -TERMINAL_VELOCITY) {
        velocity.y = -TERMINAL_VELOCITY;
      }
    } else if (velocity.y < 0) {
      velocity.y = 0;
    }

    // Calculate movement - use smaller steps for better collision detection
    tempVec.set(velocity).mul(deltaTime);

    // Handle collisions separately for each axis for better stability
    handleAxisCollision(camera, tempVec, 0); // X axis
    handleAxisCollision(camera, tempVec, 1); // Y axis
    handleAxisCollision(camera, tempVec, 2); // Z axis

    // Apply final movement
    position.add(tempVec);
  }

  private void handleAxisCollision(Camera camera, Vector3f movement, int axis) {
    Vector3f position = camera.getPosition();
    float originalPosition = position.get(axis);

    // Move along just this axis
    position.setComponent(axis, originalPosition + movement.get(axis));

    // Check for collision
    if (checkPlayerCollision(position)) {
      // Restore position if collision occurred
      position.setComponent(axis, originalPosition);
      // Zero out movement on this axis
      movement.setComponent(axis, 0);

      // Zero velocity component if we hit something
      camera.getVelocity().setComponent(axis, 0);
    }
  }

  public void jump(Camera camera) {
    if (isOnGround(camera)) {
      camera.getVelocity().y = JUMP_FORCE;
    }
  }

  private boolean isOnGround(Camera camera) {
    Vector3f position = camera.getPosition();
    // Check slightly below the player
    return checkCollision(position.x, position.y - 0.1f, position.z);
  }

  private boolean checkPlayerCollision(Vector3f position) {
    float playerHalfWidth = PLAYER_WIDTH / 2;

    // Check collision at feet level and head level
    for (float offsetY : new float[]{0.1f, PLAYER_HEIGHT - 0.1f}) {
      for (float offsetX : new float[]{-playerHalfWidth, playerHalfWidth}) {
        for (float offsetZ : new float[]{-playerHalfWidth, playerHalfWidth}) {
          if (checkCollision(position.x + offsetX, position.y + offsetY, position.z + offsetZ)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean checkCollision(float x, float y, float z) {
    try {
      return world.isBlockAt((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
    } catch (Exception e) {
      // Gracefully handle any errors
      System.err.println("Error checking collision: " + e.getMessage());
      return false;
    }
  }

  public void cleanup() {
    world.cleanup();
  }
}