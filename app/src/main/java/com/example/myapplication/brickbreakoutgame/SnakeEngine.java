package com.example.myapplication.brickbreakoutgame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class SnakeEngine extends SurfaceView implements Runnable {

    private Thread thread = null;
    private Context context;
    private SoundPool soundPool;

    private int eat_bob = -1;
    private int snake_crash = -1;

    public enum Heading {UP, RIGHT, DOWN, LEFT};
    private Heading heading = Heading.RIGHT;

    private int screenX, screenY;
    private int snakeLength;
    private int bobX, bobY;
    private int blockSize, numBlocksHigh;

    private final int numBlocksWide = 50;

    private long nextFrameTime;
    private final long FPS = 10;
    private final long MILLIS_PER_SECOND = 1000;

    private int score;
    private int[] snakeXs, snakeYs;

    private volatile boolean isPlaying;
    private Canvas canvas;

    private SurfaceHolder surfaceHolder;
    private Paint paint;

    public SnakeEngine(Context context, Point size) {
        super(context);
        context = context;

        screenX = size.x;
        screenY = size.y;

        blockSize = screenX / numBlocksWide;
        numBlocksHigh = screenY / blockSize;
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("get_mouse_sound.ogg");
            eat_bob = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("death_sound.ogg");
            snake_crash = soundPool.load(descriptor, 0);

       } catch (IOException e) {
        }

        surfaceHolder = getHolder();
        paint = new Paint();
        snakeXs = new int[200];
        snakeYs = new int[200];
        newGame();
    }

    @Override
    public void run() {
        while (isPlaying) {
            if(updateRequired()) {
                update();
                draw();
            }
        }
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void newGame() {
        snakeLength = 1;
        snakeXs[0] = numBlocksWide / 2;
        snakeYs[0] = numBlocksHigh / 2;
        spawnBob();
        score = 0;
        nextFrameTime = System.currentTimeMillis();
    }

    public void spawnBob() {
        Random random = new Random();
        bobX = random.nextInt(numBlocksWide - 1) + 1;
        bobY = random.nextInt(numBlocksHigh - 1) + 1;
    }

    private void eatBob(){
        snakeLength++;
        spawnBob();
        score = score + 1;
        soundPool.play(eat_bob, 1, 1, 0, 0, 1);
    }

    private void moveSnake(){
        for (int i = snakeLength; i > 0; i--) {
            snakeXs[i] = snakeXs[i - 1];
            snakeYs[i] = snakeYs[i - 1];
        }
        switch (heading) {
            case UP:
                snakeYs[0]--;
                break;

            case RIGHT:
                snakeXs[0]++;
                break;

            case DOWN:
                snakeYs[0]++;
                break;

            case LEFT:
                snakeXs[0]--;
                break;
        }
    }

    private boolean detectDeath(){
        boolean dead = false;
        if (snakeXs[0] == -1) dead = true;
        if (snakeXs[0] >= numBlocksWide) dead = true;
        if (snakeYs[0] == -1) dead = true;
        if (snakeYs[0] == numBlocksHigh) dead = true;
        for (int i = snakeLength - 1; i > 0; i--) {
            if ((i > 4) && (snakeXs[0] == snakeXs[i]) && (snakeYs[0] == snakeYs[i])) {
                dead = true;
            }
        }
        return dead;
    }

    public void update() {
        if (snakeXs[0] == bobX && snakeYs[0] == bobY) {
            eatBob();
        }

        moveSnake();

        if (detectDeath()) {
            soundPool.play(snake_crash, 1, 1, 0, 0, 1);
            newGame();
        }
    }

    public void draw() {
        // Get a lock on the canvas
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();
            canvas.drawColor(Color.argb(200, 22, 8, 82));
            paint.setColor(Color.argb(255, 255, 255, 255));
            paint.setTextSize(60);
            canvas.drawText("Snake Game", 10, 70, paint);
            canvas.drawText("Score: " + score, 900, 100, paint);

            for (int i = 0; i < snakeLength; i++) {
                canvas.drawRect(snakeXs[i] * blockSize,
                        (snakeYs[i] * blockSize),
                        (snakeXs[i] * blockSize) + blockSize,
                        (snakeYs[i] * blockSize) + blockSize,
                        paint);
            }

            paint.setColor(Color.argb(255, 255, 0, 0));

            canvas.drawRect(bobX * blockSize,
                    (bobY * blockSize),
                    (bobX * blockSize) + blockSize,
                    (bobY * blockSize) + blockSize,
                    paint);

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public boolean updateRequired() {
        if(nextFrameTime <= System.currentTimeMillis()){
            nextFrameTime =System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (motionEvent.getX() >= screenX / 2) {
                    switch(heading){
                        case UP:
                            heading = Heading.LEFT;
                            break;
                        case RIGHT:
                            heading = Heading.UP;
                            break;
                        case DOWN:
                            heading = Heading.RIGHT;
                            break;
                        case LEFT:
                            heading = Heading.DOWN;
                            break;
                    }
                } else {
                    switch(heading){
                        case UP:
                            heading = Heading.RIGHT;
                            break;
                        case LEFT:
                            heading = Heading.UP;
                            break;
                        case DOWN:
                            heading = Heading.LEFT;
                            break;
                        case RIGHT:
                            heading = Heading.DOWN;
                            break;
                    }
                }
        }
        return true;
    }
}