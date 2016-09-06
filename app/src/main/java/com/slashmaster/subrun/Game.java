package com.slashmaster.subrun;

//Stefan Dinic (Slashmaster) 1.8.2016.

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.media.MediaPlayer;

public class Game extends Activity {

    MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Turn title off
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Set to full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new GamePanel(this));

        //Adding background music
        mPlayer = MediaPlayer.create(Game.this, R.raw.backgroundmusic);
        mPlayer.setLooping(true);
        mPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.release();
        finish();
    }
}
