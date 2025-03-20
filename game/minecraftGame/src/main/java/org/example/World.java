package org.example;

import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class World {
  // Constants
  public static final int AIR = 0;
  public static final int GRASS = 1;
  public static final int DIRT = 2;
  public static final int STONE = 3;
  public static final int CHUNK_SIZE = 16;
  public static final int RENDER_DISTANCE = 4      ;

  // Member variables
  private final ConcurrentHashMap<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();
  private final PerlinNoise noise;
  private final ExecutorService chunkLoader;
  private Vector3i lastPlayerChunk = new Vector3i(0, 0, 0);

  public World() {
    // Use a fixed thread pool with fewer threads to prevent overwhelming the system
    this.chunkLoader = Executors.newFixedThreadPool(2);
    this.noise = new PerlinNoise(12345);

    // Generate initial chunks synchronously to ensure they're available for rendering
    generateInitialChunks();
  }

  private void generateInitialChunks() {
    // Generate a smaller initial area to start faster
    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        Chunk chunk = new Chunk(x, z, noise);
        chunks.put(new ChunkPos(x, z), chunk);
      }
    }
  }

  public void updateChunks(Vector3f playerPosition) {
    // Get the chunk the player is in
    int playerChunkX = (int)Math.floor(playerPosition.x) >> 4;
    int playerChunkZ = (int)Math.floor(playerPosition.z) >> 4;
    Vector3i currentChunk = new Vector3i(playerChunkX, 0, playerChunkZ);

    // Only update chunks if the player has moved to a different chunk
    if (!currentChunk.equals(lastPlayerChunk)) {
      lastPlayerChunk.set(currentChunk);

      // Queue chunk generation for chunks in render distance
      for (int x = playerChunkX - RENDER_DISTANCE; x <= playerChunkX + RENDER_DISTANCE; x++) {
        for (int z = playerChunkZ - RENDER_DISTANCE; z <= playerChunkZ + RENDER_DISTANCE; z++) {
          final ChunkPos pos = new ChunkPos(x, z);
          if (!chunks.containsKey(pos)) {
            chunkLoader.submit(() -> {
              try {
                Chunk chunk = new Chunk(pos.x, pos.z, noise);
                chunks.put(pos, chunk);
              } catch (Exception e) {
                System.err.println("Error generating chunk at " + pos.x + "," + pos.z + ": " + e.getMessage());
              }
            });
          }
        }
      }
    }
  }

  public boolean isBlockAt(int x, int y, int z) {
    if (y < 0 || y > 255) return false;

    int chunkX = x >> 4;
    int chunkZ = z >> 4;
    ChunkPos pos = new ChunkPos(chunkX, chunkZ);
    Chunk chunk = chunks.get(pos);

    if (chunk == null) return false;

    // Convert to chunk-local coordinates
    int localX = x & 0xF;
    int localZ = z & 0xF;

    return chunk.isBlockAt(localX, y, localZ);
  }

  public void render(Vector3f playerPosition) {
    // Only render chunks within render distance
    int playerChunkX = (int)Math.floor(playerPosition.x) >> 4;
    int playerChunkZ = (int)Math.floor(playerPosition.z) >> 4;
    System.out.println("Rendering chunks: " + chunks.size());
    // Batch similar blocks together for fewer draw calls
    Map<Integer, Integer> blockTypeCount = new HashMap<>();

    for (int x = playerChunkX - RENDER_DISTANCE; x <= playerChunkX + RENDER_DISTANCE; x++) {
      for (int z = playerChunkZ - RENDER_DISTANCE; z <= playerChunkZ + RENDER_DISTANCE; z++) {
        Chunk chunk = chunks.get(new ChunkPos(x, z));
        if (chunk != null) {
          chunk.render(x, z, playerPosition, blockTypeCount);
        }
      }
    }
  }

  public void cleanup() {
    // Properly shut down the thread pool
    chunkLoader.shutdown();
    try {
      if (!chunkLoader.awaitTermination(2, TimeUnit.SECONDS)) {
        chunkLoader.shutdownNow();
      }
    } catch (InterruptedException e) {
      chunkLoader.shutdownNow();
    }
  }
}