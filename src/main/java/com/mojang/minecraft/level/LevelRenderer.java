package com.mojang.minecraft.level;

import com.mojang.minecraft.HitResult;
import com.mojang.minecraft.Player;
import com.mojang.minecraft.phys.AABB;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CURRENT_BIT;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glInitNames;
import static org.lwjgl.opengl.GL11.glPopName;
import static org.lwjgl.opengl.GL11.glPushName;

public class LevelRenderer implements LevelListener {

    private static final int CHUNK_SIZE = 16;

    private final Tessellator tessellator;
    private final Level level;
    private final Chunk[] chunks;

    private final int chunkAmountX;
    private final int chunkAmountY;
    private final int chunkAmountZ;

    public LevelRenderer(Level level) {
        level.addListener(this);

        this.tessellator = new Tessellator();
        this.level = level;

        this.chunkAmountX = level.width / CHUNK_SIZE;
        this.chunkAmountY = level.depth / CHUNK_SIZE;
        this.chunkAmountZ = level.height / CHUNK_SIZE;

        this.chunks = new Chunk[this.chunkAmountX * this.chunkAmountY * this.chunkAmountZ];

        for (int x = 0; x < this.chunkAmountX; x++) {
            for (int y = 0; y < this.chunkAmountY; y++) {
                for (int z = 0; z < this.chunkAmountZ; z++) {
                    int minChunkX = x * CHUNK_SIZE;
                    int minChunkY = y * CHUNK_SIZE;
                    int minChunkZ = z * CHUNK_SIZE;

                    int maxChunkX = Math.min(level.width, (x + 1) * CHUNK_SIZE);
                    int maxChunkY = Math.min(level.depth, (y + 1) * CHUNK_SIZE);
                    int maxChunkZ = Math.min(level.height, (z + 1) * CHUNK_SIZE);

                    Chunk chunk = new Chunk(level, minChunkX, minChunkY, minChunkZ,
                            maxChunkX, maxChunkY, maxChunkZ);
                    this.chunks[(x + y * this.chunkAmountX) * this.chunkAmountZ + z] = chunk;
                }
            }
        }
    }

    public void render(int layer) {
        Frustum frustum = Frustum.getFrustum();
        Chunk.rebuiltThisFrame = 0;

        for (Chunk chunk : this.chunks) {
            if (frustum.cubeInFrustum(chunk.boundingBox)) {
                chunk.render(layer);
            }
        }
    }

    public void setDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        minX /= CHUNK_SIZE;
        minY /= CHUNK_SIZE;
        minZ /= CHUNK_SIZE;
        maxX /= CHUNK_SIZE;
        maxY /= CHUNK_SIZE;
        maxZ /= CHUNK_SIZE;

        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        minZ = Math.max(minZ, 0);

        maxX = Math.min(maxX, this.chunkAmountX - 1);
        maxY = Math.min(maxY, this.chunkAmountY - 1);
        maxZ = Math.min(maxZ, this.chunkAmountZ - 1);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Chunk chunk = this.chunks[(x + y * this.chunkAmountX) * this.chunkAmountZ + z];
                    chunk.setDirty();
                }
            }
        }
    }

    public void pick(Player player) {
        float radius = 3.0F;
        AABB boundingBox = player.boundingBox.grow(radius, radius, radius);

        int minX = (int) boundingBox.minX;
        int maxX = (int) (boundingBox.maxX + 1.0f);
        int minY = (int) boundingBox.minY;
        int maxY = (int) (boundingBox.maxY + 1.0f);
        int minZ = (int) boundingBox.minZ;
        int maxZ = (int) (boundingBox.maxZ + 1.0f);

        glInitNames();
        for (int x = minX; x < maxX; x++) {
            glPushName(x);
            for (int y = minY; y < maxY; y++) {
                glPushName(y);
                for (int z = minZ; z < maxZ; z++) {
                    glPushName(z);
                    if (this.level.isSolidTile(x, y, z)) {
                        glPushName(0);
                        for (int face = 0; face < 6; face++) {
                            glPushName(face);
                            this.tessellator.init();
                            Tile.rock.renderFace(this.tessellator, x, y, z, face);
                            this.tessellator.flush();
                            glPopName();
                        }
                        glPopName();
                    }
                    glPopName();
                }
                glPopName();
            }
            glPopName();
        }
    }

    public void renderHit(HitResult hitResult) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_CURRENT_BIT);
        glColor4f(1.0f, 1.0f, 1.0f,
                (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.2f + 0.4f);

        this.tessellator.init();
        Tile.rock.renderFace(this.tessellator, hitResult.x, hitResult.y, hitResult.z, hitResult.face);
        this.tessellator.flush();

        glDisable(GL_BLEND);
    }

    @Override
    public void lightColumnChanged(int x, int z, int minY, int maxY) {
        setDirty(x - 1, minY - 1, z - 1, x + 1, maxY + 1, z + 1);
    }

    @Override
    public void tileChanged(int x, int y, int z) {
        setDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    @Override
    public void allChanged() {
        setDirty(0, 0, 0, this.level.width, this.level.depth, this.level.height);
    }
}
