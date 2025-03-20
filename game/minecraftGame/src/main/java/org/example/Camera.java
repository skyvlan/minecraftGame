package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
  private Vector3f position;
  private Vector3f velocity;
  private Vector3f front;
  private Vector3f up;
  private float yaw;
  private float pitch;
  private float fov;
  private int width;
  private int height;
  private float moveSpeed = 100.0f;

  public Camera(int width, int height) {
    this.width = width;
    this.height = height;
    position = new Vector3f(0.0f, 2.0f, 0.0f);
    velocity = new Vector3f(0.0f, 0.0f, 0.0f);
    front = new Vector3f(0.0f, 0.0f, -1.0f);
    up = new Vector3f(0.0f, 1.0f, 0.0f);
    yaw = -90.0f;
    pitch = 0.0f;
    fov = 45.0f;
  }

  public Matrix4f getViewMatrix() {
    Vector3f target = new Vector3f();
    position.add(front, target);
    return new Matrix4f().lookAt(position, target, up);
  }

  public Matrix4f getProjectionMatrix() {
    return new Matrix4f().perspective((float) Math.toRadians(fov), (float) width / height, 0.1f, 1000.0f);
  }

  public void moveForward(float deltaTime) {
    Vector3f movement = new Vector3f(front.x, 0, front.z).normalize().mul(moveSpeed * deltaTime);
    velocity.x = movement.x;
    velocity.z = movement.z;
  }

  public void moveBackward(float deltaTime) {
    Vector3f movement = new Vector3f(front.x, 0, front.z).normalize().mul(moveSpeed * deltaTime);
    velocity.x = -movement.x;
    velocity.z = -movement.z;
  }

  public void moveLeft(float deltaTime) {
    Vector3f right = new Vector3f();
    front.cross(up, right).normalize();
    velocity.x = -right.x * moveSpeed * deltaTime;
    velocity.z = -right.z * moveSpeed * deltaTime;
  }

  public void moveRight(float deltaTime) {
    Vector3f right = new Vector3f();
    front.cross(up, right).normalize();
    velocity.x = right.x * moveSpeed * deltaTime;
    velocity.z = right.z * moveSpeed * deltaTime;
  }

  public void look(float xOffset, float yOffset) {
    float sensitivity = 0.1f;
    xOffset *= sensitivity;
    yOffset *= sensitivity;

    // Negate yOffset to fix vertical inversion
//    yOffset = -yOffset;
    // If horizontal movement is also inverted, uncomment the line below
    // xOffset = -xOffset;

    yaw += xOffset;
    pitch += yOffset;

    // Constrain pitch
    if (pitch > 89.0f) pitch = 89.0f;
    if (pitch < -89.0f) pitch = -89.0f;

    // Update front vector
    front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
    front.y = (float) Math.sin(Math.toRadians(pitch));
    front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
    front.normalize();
  }

  public Vector3f getPosition() {
    return position;
  }

  public Vector3f getVelocity() {
    return velocity;
  }
}