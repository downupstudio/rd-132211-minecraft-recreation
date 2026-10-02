package com.mojang.minecraft;

import com.mojang.minecraft.level.Chunk;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.LevelRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import javax.swing.JOptionPane;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FOG;
import static org.lwjgl.opengl.GL11.GL_FOG_COLOR;
import static org.lwjgl.opengl.GL11.GL_FOG_END;
import static org.lwjgl.opengl.GL11.GL_FOG_MODE;
import static org.lwjgl.opengl.GL11.GL_FOG_START;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_RENDER;
import static org.lwjgl.opengl.GL11.GL_SELECT;
import static org.lwjgl.opengl.GL11.GL_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glClearDepth;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFog;
import static org.lwjgl.opengl.GL11.glFogf;
import static org.lwjgl.opengl.GL11.glFogi;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glRenderMode;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glSelectBuffer;
import static org.lwjgl.opengl.GL11.glShadeModel;
import static org.lwjgl.opengl.GL11.glTranslated;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.util.glu.GLU.gluPerspective;
import static org.lwjgl.util.glu.GLU.gluPickMatrix;

public class RubyDung implements Runnable {

    private final Timer timer = new Timer(60);

    private Level level;
    private LevelRenderer levelRenderer;
    private Player player;

    private final FloatBuffer fogColor = BufferUtils.createFloatBuffer(4);

    private final int width = 854;
    private final int height = 480;

    private final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
    private final IntBuffer selectBuffer = BufferUtils.createIntBuffer(2000);
    private HitResult hitResult;

    public void init() throws LWJGLException {
        this.fogColor.put(new float[]{
                14 / 255.0F,
                11 / 255.0F,
                10 / 255.0F,
                255 / 255.0F
        }).flip();

        Display.setDisplayMode(new DisplayMode(this.width, this.height));
        Display.setTitle("Minecraft");
        Display.create();
        Keyboard.create();
        Mouse.create();

        glEnable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);
        glClearColor(0.5F, 0.8F, 1.0F, 0.0F);
        glClearDepth(1.0);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glDepthFunc(GL_LEQUAL);

        this.level = new Level(256, 256, 64);
        this.levelRenderer = new LevelRenderer(this.level);
        this.player = new Player(this.level);

        Mouse.setGrabbed(true);
    }

    public void destroy() {
        this.level.save();
        Mouse.destroy();
        Keyboard.destroy();
        Display.destroy();
    }

    @Override
    public void run() {
        try {
            init();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e, "Failed to start Minecraft", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        int frames = 0;
        long lastTime = System.currentTimeMillis();

        try {
            while (!Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && !Display.isCloseRequested()) {
                this.timer.advanceTime();

                for (int i = 0; i < this.timer.ticks; ++i) {
                    tick();
                }

                render(this.timer.partialTicks);
                frames++;

                while (System.currentTimeMillis() >= lastTime + 1000L) {
                    System.out.println(frames + " fps, " + Chunk.updates);
                    Chunk.updates = 0;
                    lastTime += 1000L;
                    frames = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            destroy();
        }
    }

    private void tick() {
        this.player.tick();
    }

    private void moveCameraToPlayer(float partialTicks) {
        Player player = this.player;

        glTranslatef(0.0f, 0.0f, -0.3f);
        glRotatef(player.xRotation, 1.0f, 0.0f, 0.0f);
        glRotatef(player.yRotation, 0.0f, 1.0f, 0.0f);

        double x = this.player.prevX + (this.player.x - this.player.prevX) * partialTicks;
        double y = this.player.prevY + (this.player.y - this.player.prevY) * partialTicks;
        double z = this.player.prevZ + (this.player.z - this.player.prevZ) * partialTicks;

        glTranslated(-x, -y, -z);
    }

    private void setupCamera(float partialTicks) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective(70, width / (float) height, 0.05F, 1000);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        moveCameraToPlayer(partialTicks);
    }

    private void setupPickCamera(float partialTicks, int x, int y) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        this.viewportBuffer.clear();
        glGetInteger(GL_VIEWPORT, this.viewportBuffer);
        this.viewportBuffer.flip();
        this.viewportBuffer.limit(16);

        gluPickMatrix(x, y, 5.0f, 5.0f, this.viewportBuffer);
        gluPerspective(70.0f, this.width / (float) this.height, 0.05f, 1000.0f);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        moveCameraToPlayer(partialTicks);
    }

    private void pick(float partialTicks) {
        this.selectBuffer.clear();
        glSelectBuffer(this.selectBuffer);
        glRenderMode(GL_SELECT);

        this.setupPickCamera(partialTicks, this.width / 2, this.height / 2);
        this.levelRenderer.pick(this.player);

        this.selectBuffer.flip();
        this.selectBuffer.limit(this.selectBuffer.capacity());

        long closest = 0L;
        int[] names = new int[10];
        int hitNameCount = 0;

        int hits = glRenderMode(GL_RENDER);
        for (int hitIndex = 0; hitIndex < hits; hitIndex++) {
            int nameCount = this.selectBuffer.get();
            long minZ = this.selectBuffer.get();
            this.selectBuffer.get();

            if (minZ < closest || hitIndex == 0) {
                closest = minZ;
                hitNameCount = nameCount;
                for (int nameIndex = 0; nameIndex < nameCount; nameIndex++) {
                    names[nameIndex] = this.selectBuffer.get();
                }
            } else {
                for (int nameIndex = 0; nameIndex < nameCount; ++nameIndex) {
                    this.selectBuffer.get();
                }
            }
        }

        if (hitNameCount > 0) {
            this.hitResult = new HitResult(names[0], names[1], names[2], names[3], names[4]);
        } else {
            this.hitResult = null;
        }
    }

    private void render(float partialTicks) {
        float motionX = Mouse.getDX();
        float motionY = Mouse.getDY();
        this.player.turn(motionX, motionY);

        pick(partialTicks);

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && this.hitResult != null) {
                this.level.setTile(this.hitResult.x, this.hitResult.y, this.hitResult.z, 0);
            }

            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState() && this.hitResult != null) {
                int x = this.hitResult.x;
                int y = this.hitResult.y;
                int z = this.hitResult.z;

                if (this.hitResult.face == 0) {
                    y--;
                }
                if (this.hitResult.face == 1) {
                    y++;
                }
                if (this.hitResult.face == 2) {
                    z--;
                }
                if (this.hitResult.face == 3) {
                    z++;
                }
                if (this.hitResult.face == 4) {
                    x--;
                }
                if (this.hitResult.face == 5) {
                    x++;
                }

                this.level.setTile(x, y, z, 1);
            }
        }

        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();
            if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)
                    && Keyboard.getEventKeyState()) {
                this.level.save();
            }
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        setupCamera(partialTicks);

        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, -10);
        glFogf(GL_FOG_END, 20);
        glFog(GL_FOG_COLOR, this.fogColor);
        glDisable(GL_FOG);

        this.levelRenderer.render(0);

        glEnable(GL_FOG);
        this.levelRenderer.render(1);

        glDisable(GL_TEXTURE_2D);

        if (this.hitResult != null) {
            this.levelRenderer.renderHit(this.hitResult);
        }

        glDisable(GL_FOG);
        Display.update();
    }

    public static void main(String[] args) {
        NativeLoader.load();
        new Thread(new RubyDung(), "Minecraft main thread").start();
    }
}
