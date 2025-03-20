package org.example;

import java.util.Random;

public class PerlinNoise {
  private final int[] permutation;
  private final int seed;

  public PerlinNoise(int seed) {
    this.seed = seed;
    Random random = new Random(seed);
    permutation = new int[512];

    // Initialize the permutation array
    for (int i = 0; i < 256; i++) {
      permutation[i] = i;
    }

    // Shuffle permutation array
    for (int i = 0; i < 256; i++) {
      int j = random.nextInt(256);
      int temp = permutation[i];
      permutation[i] = permutation[j];
      permutation[j] = temp;
    }

    // Duplicate the permutation array
    for (int i = 0; i < 256; i++) {
      permutation[i + 256] = permutation[i];
    }
  }

  public double noise(double x, double y, double z) {
    // Find unit cube that contains point
    int X = (int) Math.floor(x) & 255;
    int Y = (int) Math.floor(y) & 255;
    int Z = (int) Math.floor(z) & 255;

    // Find relative x, y, z of point in cube
    x -= Math.floor(x);
    y -= Math.floor(y);
    z -= Math.floor(z);

    // Compute fade curves for each of x, y, z
    double u = fade(x);
    double v = fade(y);
    double w = fade(z);

    // Hash coordinates of the 8 cube corners
    int A = permutation[X] + Y;
    int AA = permutation[A] + Z;
    int AB = permutation[A + 1] + Z;
    int B = permutation[X + 1] + Y;
    int BA = permutation[B] + Z;
    int BB = permutation[B + 1] + Z;

    // Add blended results from 8 corners of cube
    return lerp(w, lerp(v, lerp(u, grad(permutation[AA], x, y, z),
                grad(permutation[BA], x - 1, y, z)),
            lerp(u, grad(permutation[AB], x, y - 1, z),
                grad(permutation[BB], x - 1, y - 1, z))),
        lerp(v, lerp(u, grad(permutation[AA + 1], x, y, z - 1),
                grad(permutation[BA + 1], x - 1, y, z - 1)),
            lerp(u, grad(permutation[AB + 1], x, y - 1, z - 1),
                grad(permutation[BB + 1], x - 1, y - 1, z - 1))));
  }

  private double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private double lerp(double t, double a, double b) {
    return a + t * (b - a);
  }

  private double grad(int hash, double x, double y, double z) {
    // Convert low 4 bits of hash to 12 gradient directions
    int h = hash & 15;
    double u = h < 8 ? x : y;
    double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
  }

  // Generate octaved noise for more realistic terrain
  public double octaveNoise(double x, double y, double z, int octaves, double persistence) {
    double total = 0;
    double frequency = 1;
    double amplitude = 1;
    double maxValue = 0;

    for (int i = 0; i < octaves; i++) {
      total += noise(x * frequency, y * frequency, z * frequency) * amplitude;
      maxValue += amplitude;
      amplitude *= persistence;
      frequency *= 2;
    }

    return total / maxValue;
  }
}