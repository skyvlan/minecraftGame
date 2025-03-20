package org.example;

public class ChunkPos {
  public final int x, z;

  public ChunkPos(int x, int z) {
    this.x = x;
    this.z = z;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChunkPos chunkPos = (ChunkPos) o;
    return x == chunkPos.x && z == chunkPos.z;
  }

  @Override
  public int hashCode() {
    int result = x;
    result = 31 * result + z;
    return result;
  }

  @Override
  public String toString() {
    return "[" + x + "," + z + "]";
  }
}