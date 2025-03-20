package org.example;

public class BlockPos {
  public final int x, y, z;

  public BlockPos(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
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