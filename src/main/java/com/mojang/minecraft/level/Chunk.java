package com.mojang.minecraft.level;

import com.mojang.minecraft.Textures;
import com.mojang.minecraft.phys.AABB;

import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEndList;
import static org.lwjgl.opengl.GL11.glGenLists;
import static org.lwjgl.opengl.GL11.glNewList;

public class Chunk {

    private static int TEXTURE = -1;
    private static final Tessellator TESSELLATOR = new Tessellator();

    public static int rebuiltThisFrame;
    public static int updates;

    private final Level level;
    public AABB boundingBox;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final int lists;
    private boolean dirty = true;

    public Chunk(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.level = level;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.lists = glGenLists(2);
        this.boundingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static int texture() {
        if (TEXTURE < 0) {
            TEXTURE = Textures.loadTexture("/terrain.png", GL_NEAREST);
        }
        return TEXTURE;
    }

    public void rebuild(int layer) {
        if (rebuiltThisFrame == 2) {
            return;
        }

        updates++;
        rebuiltThisFrame++;
        this.dirty = false;

        glNewList(this.lists + layer, GL_COMPILE);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture());
        TESSELLATOR.init();

        int surfaceY = this.level.depth * 2 / 3;

        for (int x = this.minX; x < this.maxX; ++x) {
            for (int y = this.minY; y < this.maxY; ++y) {
                for (int z = this.minZ; z < this.maxZ; ++z) {
                    if (this.level.isTile(x, y, z)) {
                        if (y == surfaceY) {
                            Tile.grass.render(TESSELLATOR, this.level, layer, x, y, z);
                        } else {
                            Tile.rock.render(TESSELLATOR, this.level, layer, x, y, z);
                        }
                    }
                }
            }
        }

        TESSELLATOR.flush();
        glDisable(GL_TEXTURE_2D);
        glEndList();
    }

    public void render(int layer) {
        if (this.dirty) {
            rebuild(0);
            rebuild(1);
        }
        glCallList(this.lists + layer);
    }

    public void setDirty() {
        this.dirty = true;
    }
}
