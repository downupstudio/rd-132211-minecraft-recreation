package com.mojang.minecraft.level;

import com.mojang.minecraft.phys.AABB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Level {

    public final int width;
    public final int height;
    public final int depth;

    private final byte[] blocks;
    private final int[] lightDepths;
    private final ArrayList<LevelListener> levelListeners = new ArrayList<LevelListener>();

    public Level(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;

        this.blocks = new byte[width * height * depth];
        this.lightDepths = new int[width * height];

        int surfaceY = depth * 2 / 3;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < depth; y++) {
                for (int z = 0; z < height; z++) {
                    int index = (y * this.height + z) * this.width + x;
                    this.blocks[index] = (byte) ((y <= surfaceY) ? 1 : 0);
                }
            }
        }

        calcLightDepths(0, 0, width, height);
        load();
    }

    public void load() {
        File file = new File("level.dat");
        if (!file.exists()) {
            return;
        }

        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new GZIPInputStream(new FileInputStream(file)));
            int offset = 0;
            while (offset < this.blocks.length) {
                int read = dis.read(this.blocks, offset, this.blocks.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            calcLightDepths(0, 0, this.width, this.height);

            for (LevelListener levelListener : this.levelListeners) {
                levelListener.allChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void save() {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new GZIPOutputStream(new FileOutputStream("level.dat")));
            dos.write(this.blocks);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void calcLightDepths(int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX; x < minX + maxX; x++) {
            for (int z = minZ; z < minZ + maxZ; z++) {
                int prevDepth = this.lightDepths[x + z * this.width];

                int depth = this.depth - 1;
                while (depth > 0 && !isLightBlocker(x, depth, z)) {
                    depth--;
                }

                this.lightDepths[x + z * this.width] = depth;

                if (prevDepth != depth) {
                    int minTileChangeY = Math.min(prevDepth, depth);
                    int maxTileChangeY = Math.max(prevDepth, depth);
                    for (LevelListener levelListener : this.levelListeners) {
                        levelListener.lightColumnChanged(x, z, minTileChangeY, maxTileChangeY);
                    }
                }
            }
        }
    }

    public boolean isTile(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return false;
        }
        int index = (y * this.height + z) * this.width + x;
        return this.blocks[index] == 1;
    }

    public int getTile(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return 0;
        }
        return this.blocks[(y * this.height + z) * this.width + x] & 0xFF;
    }

    public boolean isSolidTile(int x, int y, int z) {
        return isTile(x, y, z);
    }

    public boolean isLightBlocker(final int x, final int y, final int z) {
        return this.isSolidTile(x, y, z);
    }

    public float getBrightness(int x, int y, int z) {
        float dark = 0.8F;
        float light = 1.0F;

        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return light;
        }
        if (y < this.lightDepths[x + z * this.width]) {
            return dark;
        }
        return light;
    }

    @SuppressWarnings("rawtypes")
    public ArrayList getCubes(AABB boundingBox) {
        ArrayList boundingBoxList = new ArrayList();

        int minX = (int) (Math.floor(boundingBox.minX) - 1);
        int maxX = (int) (Math.ceil(boundingBox.maxX) + 1);
        int minY = (int) (Math.floor(boundingBox.minY) - 1);
        int maxY = (int) (Math.ceil(boundingBox.maxY) + 1);
        int minZ = (int) (Math.floor(boundingBox.minZ) - 1);
        int maxZ = (int) (Math.ceil(boundingBox.maxZ) + 1);

        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        minZ = Math.max(0, minZ);

        maxX = Math.min(this.width, maxX);
        maxY = Math.min(this.depth, maxY);
        maxZ = Math.min(this.height, maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (isSolidTile(x, y, z)) {
                        boundingBoxList.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
        return boundingBoxList;
    }

    public void setTile(int x, int y, int z, int id) {
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return;
        }

        this.blocks[(y * this.height + z) * this.width + x] = (byte) id;
        this.calcLightDepths(x, z, 1, 1);

        for (LevelListener levelListener : this.levelListeners) {
            levelListener.tileChanged(x, y, z);
        }
    }

    public void addListener(LevelListener levelListener) {
        this.levelListeners.add(levelListener);
    }
}
