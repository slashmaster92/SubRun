package com.slashmaster.subrun;

//Stefan Dinic (Slashmaster) 1.8.2016.

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {

    public static final int WIDTH = 859;
    public static final int HEIGHT= 480;
    public static final int MOVESPEED = -5;

    private MainThread thread;
    private Background bg;
    private Player player;
    private Random rand = new Random();
    private int maxBorderHeight;
    private int minBorderHeight;

    private boolean topDown = true;
    private boolean botDown = true;
    private boolean newGameCreated;

    //Increase to slow down difficulty progression, decrease to speed up
    private int progressDenom = 20;

    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean dissapear;
    private boolean started;
    private int best;

    private long missileStartTime;
    private long smokeStartTime;

    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topborder;
    private ArrayList<BotBorder> botborder;

    //Adding sound variables
    private SoundPool sounds;
    private int sExplosion;

    public GamePanel(Context context) {
        super(context);

        //add the callback to the SurfaceHolder to intercept events
        getHolder().addCallback(this);

        //Make gamePanel focusable so it can handle events
        setFocusable(true);

        //Creating sound pool
        sounds = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        sExplosion = sounds.load(context, R.raw.subexplosion, 1);


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        boolean retry = true;
        int counter = 0;

        while(retry && counter < 1000) {
            counter++;

            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.background));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.player), 74, 49, 3);

        smoke = new ArrayList<Smokepuff>();
        missiles = new ArrayList<Missile>();
        topborder = new ArrayList<TopBorder>();
        botborder = new ArrayList<BotBorder>();
        smokeStartTime = System.nanoTime();
        missileStartTime = System.nanoTime();

        thread = new MainThread(getHolder(), this);

        //We can safely start the game loop
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!player.getPlaying() && newGameCreated && reset) {
                player.setPlaying(true);
                player.setUp(true);
            }

            if (player.getPlaying()) {

                if (!started) started = true;
                reset = false;
                player.setUp(true);
            }

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update() {

        if (player.getPlaying()) {

            if (botborder.isEmpty()) {
                player.setPlaying(false);
                return;
            }

            if (topborder.isEmpty()) {
                player.setPlaying(false);
                return;
            }

            bg.update();
            player.update();

            //Calculate the threshold of height the border can have based on the score
            //Max and min border are updated, and the border switched direction when
            //either max or min is met

            maxBorderHeight = 30 + player.getScore() / progressDenom;

            //Cap max border height so that borders can only take up a total of 1/2 the screen
            if (maxBorderHeight > HEIGHT / 4) maxBorderHeight = HEIGHT / 4;
            minBorderHeight = 5 + player.getScore() / progressDenom;

            //Check Bottom border collision
            for (int i = 0; i < botborder.size(); i++) {
                if (collision(botborder.get(i), player))
                    player.setPlaying(false);
            }

            //Check top border for collision
            for (int i = 0; i < topborder.size(); i++) {
                if (collision(topborder.get(i), player))
                    player.setPlaying(false);
            }

            //Update top border
            this.updateTopBorder();

            //Update Bottom border
            this.updateBottomBorder();

            //Add missiles on timer
            long missileElapsed = (System.nanoTime() - missileStartTime) / 1000000;

            if (missileElapsed > (2000 - player.getScore() / 4)) {
                System.out.println("Making missile!");

                //First missile always goes down the middle
                if (missiles.size() == 0) {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.
                            missile), WIDTH + 10, HEIGHT / 2, 45, 15, player.getScore(), 13));
                } else {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile),
                            WIDTH + 10, (int)(rand.nextDouble() * (HEIGHT - (maxBorderHeight * 2)) + maxBorderHeight),
                            45, 15, player.getScore(), 13));
                }

                //Reset Timer
                missileStartTime = System.nanoTime();
            }

            //Loop through every missile and check for collision and remove
            for (int i = 0; i < missiles.size(); i++) {

                //update missile
                missiles.get(i).update();

                if (collision(missiles.get(i), player)) {
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }

                //remove missile if it is way off the screen
                if (missiles.get(i).getX() < -100) {
                    missiles.remove(i);
                    break;
                }
            }


            //Add smoke puffs on timer
            long elapsed = (System.nanoTime() - smokeStartTime) / 1000000;

            if (elapsed > 120) {
                smoke.add(new Smokepuff(player.getX(), player.getY() + 10));
                smokeStartTime = System.nanoTime();
            }

            for (int i = 0; i < smoke.size(); i++) {

                smoke.get(i).update();

                if (smoke.get(i).getX() < -10) {
                    smoke.remove(i);
                }
            }
        } else {
            player.resetDY();

            if (!reset) {

                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                dissapear = true;

                explosion = new Explosion(BitmapFactory.decodeResource(getResources(), R.drawable.explosion),
                        player.getX(), player.getY() - 30, 120, 120, 25);

                sounds.play(sExplosion, 1.0f, 1.0f, 0, 0, 1.5f);
            }

            explosion.update();

            long resetElapsed = (System.nanoTime() - startReset) / 1000000;

            if (resetElapsed > 2500 && !newGameCreated) {
                newGame();
            }
        }
        if (player.getScore() > best) {
            best = player.getScore();
        }
    }

    //Checking for collision between two objects, player and missile
    public boolean collision(GameObject a, GameObject b) {

        if (Rect.intersects(a.getRectangle(), b.getRectangle())) {
            return true;
        }

        return false;
    }

    @Override
    public void draw(Canvas canvas) {

        final float scaleFactorX = getWidth() / (WIDTH * 1.f);
        final float scaleFactorY = getHeight() / (HEIGHT * 1.f);

        if (canvas != null) {

            final int savedState = canvas.save();

            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);

            if (!dissapear) {
                player.draw(canvas);
            }
            //Draw smoke puffs
            for (Smokepuff sp: smoke) {
                if (!dissapear) {
                    sp.draw(canvas);
                }
            }
            //Draw missiles
            for (Missile m: missiles) {
                m.draw(canvas);
            }
            //Draw Top Border
            for (TopBorder tb: topborder) {
                tb.draw(canvas);
            }
            //Draw Bottom Border
            for (BotBorder bb: botborder) {
                bb.draw(canvas);
            }
            //Draw explosion
            if (started) {
                explosion.draw(canvas);
            }

            drawText(canvas);

            canvas.restoreToCount(savedState);
        }
    }

    public void updateTopBorder() {

        //Every 50 points, insert randomly placed top blocks that break the pattern
        /*if (player.getScore() % 50 == 0) {

            topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.border),
                    topborder.get(topborder.size() - 1).getX() + 20, 0, (int)((rand.nextDouble() * (maxBorderHeight)) + 1)));
        }*/

        for (int i = 0; i < topborder.size(); i++) {
            topborder.get(i).update();

            if (topborder.get(i).getX() < -20) {
                topborder.remove(i);

                //remove top down which determines the direction the border is moving (up or down)
                if (topborder.get(topborder.size() - 1).getHeight() >= maxBorderHeight) {
                    topDown = false;
                }

                if (topborder.get(topborder.size() - 1).getHeight() <= minBorderHeight) {
                    topDown = true;
                }

                //New border added will have larger height
                if (topDown) {
                    topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(),
                            R.drawable.border), topborder.get(topborder.size() - 1).getX() + 20, 0,
                            topborder.get(topborder.size() - 1).getHeight() + 1));
                }
                //New Border added will have smaller height
                else {
                    topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(),
                            R.drawable.border), topborder.get(topborder.size() - 1).getX() + 20, 0,
                            topborder.get(topborder.size() - 1).getHeight() - 1));
                }
            }
        }
    }

    public void updateBottomBorder() {

        //Update Bottom border
        for (int i = 0; i < botborder.size(); i++) {
            botborder.get(i).update();

            //If border is moving off screen, remove it and add a corresponding new one
            if (botborder.get(i).getX() < -20) {
                botborder.remove(i);

                //Determines if the border is moving up or down
                if (botborder.get(botborder.size() - 1).getY() <= HEIGHT - 10) {
                    botDown = true;
                }

                if (botborder.get(botborder.size() - 1).getY() >= HEIGHT - 10) {
                    botDown = false;
                }

                if (botDown) {
                    botborder.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.border),
                            botborder.get(botborder.size() - 1).getX() + 20, botborder.get(botborder.size() - 1)
                            .getY() + 1));
                } else {
                    botborder.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.border),
                            botborder.get(botborder.size() - 1).getX() + 20, botborder.get(botborder.size() - 1)
                            .getY() -1));
                }
            }
        }
    }

    public void newGame() {

        dissapear = false;

        botborder.clear();
        topborder.clear();
        missiles.clear();
        smoke.clear();

        minBorderHeight = 5;
        maxBorderHeight = 30;

        player.resetDY();

        player.setY(HEIGHT / 2);

        player.resetScore();


        //Create initial Border

        //Initial Top border
        for (int i = 0;  i * 20 < WIDTH + 40; i++) {

            //First top border create
            if (i ==0) {
                topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(),
                        R.drawable.border), i * 20, 0, 10));
            } else {
                topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(),
                        R.drawable.border), i * 20, 0, topborder.get(i - 1).getHeight() + 1));
            }
        }

        //Initial bottom border
        for (int i = 0; i * 20 < WIDTH + 40; i++) {

            //First border ever created
            if (i == 0) {
                botborder.add(new BotBorder(BitmapFactory.decodeResource(getResources(),
                        R.drawable.border), i * 20, HEIGHT - minBorderHeight));
            } else {
                botborder.add(new BotBorder(BitmapFactory.decodeResource(getResources(),
                        R.drawable.border), i * 20, HEIGHT - minBorderHeight /*botborder.get(i - 1).getY() - 1)*/));
            }
        }

        newGameCreated = true;
    }

    public void drawText(Canvas canvas) {

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("DISTANCE: " + (player.getScore()), 10, HEIGHT / 2 - 215, paint);
        canvas.drawText("BEST: " + best, WIDTH - 120, HEIGHT / 2 - 215, paint);

        if (!player.getPlaying() && newGameCreated && reset) {

            Paint paint1 = new Paint();
            paint1.setColor(Color.RED);
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("PRESS TO START", WIDTH / 2 - 50, HEIGHT / 2, paint1);

            paint1.setColor(Color.BLACK);
            paint1.setTextSize(20);
            canvas.drawText("PRESS AND HOLD TO GO UP", WIDTH / 2 - 50, HEIGHT / 2 + 20, paint1);
            canvas.drawText("RELEASE TO GO DOWN", WIDTH / 2 - 50, HEIGHT / 2 + 40, paint1);
        }
    }
}
