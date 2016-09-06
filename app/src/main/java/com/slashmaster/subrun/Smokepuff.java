package com.slashmaster.subrun;

//Stefan Dinic (Slashmaster) 2.8.2016.

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Smokepuff extends GameObject {

    public int r;

    public Smokepuff(int x, int y) {

        r = 5;
        super.x = x;
        super.y = y;
    }

    public void update() {

        x -= 10;
    }

    public void draw(Canvas canvas) {

        //Creating and painting smoke puffs
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);

        //creating and drawing three smoke puffs following the submarine
        canvas.drawCircle(x - r, y - r, r, paint);
        canvas.drawCircle(x - r + 2, y - r - 2, r, paint);
        canvas.drawCircle(x - r + 4, y - r + 1, r, paint);
    }
}
