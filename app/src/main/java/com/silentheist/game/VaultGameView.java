package com.silentheist.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.MotionEvent;
import android.view.View;

public class VaultGameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SoundPool sounds;
    private final int sndTick, sndLock, sndScrape, sndAlarm, sndOpen;
    private final Vibrator vibrator;
    private final float[] target = {118f, 292f, 43f};
    private final int[] requiredDirection = {1, -1, 1};
    private final String[] directionLabel = {"TURN RIGHT", "TURN LEFT", "TURN RIGHT"};
    private final RectF resetButton = new RectF();

    private float cx, cy, radius, dialPosition, lastTouchAngle, noise;
    private long lastMoveNs, lastFrameNs, wonAtMs;
    private int stage, lastTickBucket = -1;
    private boolean dragging, failed, won;

    public VaultGameView(Context context) {
        super(context);
        setFocusable(true);
        setBackgroundColor(Color.rgb(8, 9, 11));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sounds = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(attrs).build();
        sndTick = sounds.load(context, R.raw.tick, 1);
        sndLock = sounds.load(context, R.raw.lock_click, 1);
        sndScrape = sounds.load(context, R.raw.scrape, 1);
        sndAlarm = sounds.load(context, R.raw.alarm, 1);
        sndOpen = sounds.load(context, R.raw.vault_open, 1);

        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager != null ? manager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        sounds.release();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cx = w * 0.5f;
        cy = h * 0.56f;
        radius = Math.min(w * 0.37f, h * 0.245f);
        float d = getResources().getDisplayMetrics().density;
        resetButton.set(cx - 140f*d, h - 76f*d, cx + 140f*d, h - 24f*d);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now = System.nanoTime();
        if (lastFrameNs == 0) lastFrameNs = now;
        float dt = Math.min(.05f, (now-lastFrameNs)/1_000_000_000f);
        lastFrameNs = now;
        if (!dragging && !failed && !won) noise = Math.max(0f, noise - 7.5f*dt);
        drawHeader(c);
        drawNoise(c);
        drawVault(c);
        drawFooter(c);
        if (failed || won || noise > 0f) postInvalidateDelayed(16);
    }

    private void drawHeader(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        p.setColor(Color.rgb(216,195,139)); p.setTextSize(13f*d);
        c.drawText("MISSION 01  ·  THE QUIET VAULT", getWidth()/2f, 42f*d, p);
        p.setColor(Color.rgb(236,238,240)); p.setTextSize(26f*d);
        c.drawText(failed ? "YOU WERE HEARD" : won ? "VAULT OPEN" : "Crack the safe silently", getWidth()/2f, 78f*d, p);
        p.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        p.setColor(Color.rgb(145,150,158)); p.setTextSize(14f*d);
        String sub = failed ? "Too much noise. The guard is coming." : won ? "Perfect. The diamond is yours." : directionLabel[Math.min(stage,2)] + "  ·  move slowly and listen";
        c.drawText(sub, getWidth()/2f, 104f*d, p);
    }

    private void drawNoise(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float l=28f*d, r=getWidth()-28f*d, t=132f*d, h=11f*d;
        p.setColor(Color.rgb(29,32,37)); c.drawRoundRect(l,t,r,t+h,h/2,h/2,p);
        int col = noise<35 ? Color.rgb(120,170,145) : noise<70 ? Color.rgb(208,180,105) : Color.rgb(205,96,82);
        p.setColor(col); c.drawRoundRect(l,t,Math.max(l+2f,l+(r-l)*Math.min(1f,noise/100f)),t+h,h/2,h/2,p);
        p.setTextSize(11f*d); p.setColor(col); p.setTextAlign(Paint.Align.RIGHT);
        String s=noise<35?"UNDETECTED":noise<60?"SUSPICIOUS":noise<85?"SEARCHING":"DANGER";
        c.drawText(s+"  "+Math.round(noise)+"%",r,t-8f*d,p);
    }

    private void drawVault(Canvas c) {
        float d=getResources().getDisplayMetrics().density;
        p.setColor(Color.rgb(18,20,24));
        c.drawRoundRect(cx-radius*1.28f,cy-radius*1.30f,cx+radius*1.28f,cy+radius*1.30f,28f*d,28f*d,p);
        stroke.setColor(Color.rgb(47,51,58)); stroke.setStrokeWidth(1.5f*d);
        c.drawRoundRect(cx-radius*1.28f,cy-radius*1.30f,cx+radius*1.28f,cy+radius*1.30f,28f*d,28f*d,stroke);

        if (won) {
            float t=Math.min(1f,(System.currentTimeMillis()-wonAtMs)/700f);
            p.setColor(Color.rgb(10,11,13)); c.drawCircle(cx,cy,radius*.98f,p);
            float s=radius*(.26f+.08f*t);
            Path diamond=new Path();
            diamond.moveTo(cx,cy-s); diamond.lineTo(cx+s*.78f,cy-s*.25f); diamond.lineTo(cx+s*.58f,cy+s*.74f);
            diamond.lineTo(cx,cy+s); diamond.lineTo(cx-s*.58f,cy+s*.74f); diamond.lineTo(cx-s*.78f,cy-s*.25f); diamond.close();
            p.setColor(Color.rgb(216,195,139)); c.drawPath(diamond,p); return;
        }

        p.setColor(Color.rgb(37,40,46)); c.drawCircle(cx,cy,radius,p);
        stroke.setColor(Color.rgb(86,91,99)); stroke.setStrokeWidth(2f*d); c.drawCircle(cx,cy,radius,stroke);
        c.save(); c.rotate(dialPosition,cx,cy);
        for(int i=0;i<60;i++) {
            float a=(float)Math.toRadians(i*6f-90f), outer=radius*.90f, inner=radius*(i%5==0?.74f:.81f);
            stroke.setColor(i%5==0?Color.rgb(205,207,211):Color.rgb(104,108,116)); stroke.setStrokeWidth((i%5==0?2.2f:1.2f)*d);
            c.drawLine(cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,cx+(float)Math.cos(a)*outer,cy+(float)Math.sin(a)*outer,stroke);
        }
        p.setColor(Color.rgb(216,195,139)); c.drawCircle(cx,cy-radius*.61f,4.5f*d,p); c.restore();
        p.setColor(Color.rgb(14,15,18)); c.drawCircle(cx,cy,radius*.48f,p);
        p.setColor(Color.rgb(214,216,220)); c.drawCircle(cx,cy,radius*.105f,p);
        p.setColor(Color.rgb(32,34,39)); c.drawCircle(cx,cy,radius*.055f,p);

        Path pointer=new Path(); pointer.moveTo(cx,cy-radius*1.07f); pointer.lineTo(cx-9f*d,cy-radius*.94f); pointer.lineTo(cx+9f*d,cy-radius*.94f); pointer.close();
        p.setColor(Color.rgb(216,195,139)); c.drawPath(pointer,p);

        float y=cy+radius*1.10f;
        for(int i=0;i<3;i++) { p.setColor(i<stage?Color.rgb(216,195,139):Color.rgb(69,73,80)); c.drawCircle(cx+(i-1)*34f*d,y,6f*d,p); }
    }

    private void drawFooter(Canvas c) {
        float d=getResources().getDisplayMetrics().density;
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(12f*d); p.setColor(Color.rgb(128,133,141));
        if(!failed&&!won) c.drawText("Tip: the click gets sharper near the hidden lock point.",cx,getHeight()-92f*d,p);
        if(failed||won) {
            p.setColor(Color.rgb(34,37,42)); c.drawRoundRect(resetButton,18f*d,18f*d,p);
            p.setColor(Color.rgb(235,237,239)); p.setTextSize(14f*d); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            c.drawText("RESTART MISSION",cx,resetButton.centerY()+5f*d,p);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN) {
            if((failed||won)&&resetButton.contains(e.getX(),e.getY())) { resetGame(); return true; }
            if(failed||won) return true;
            if(distance(e.getX(),e.getY(),cx,cy)<=radius*1.05f) {
                dragging=true; lastTouchAngle=touchAngle(e.getX(),e.getY()); lastMoveNs=System.nanoTime(); return true;
            }
        } else if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&dragging) {
            long now=System.nanoTime(); float a=touchAngle(e.getX(),e.getY()); float diff=shortestDelta(lastTouchAngle,a);
            float dt=Math.max(.004f,(now-lastMoveNs)/1_000_000_000f), speed=Math.abs(diff)/dt; int dir=diff>=0?1:-1;
            dialPosition=normalize(dialPosition+diff); lastTouchAngle=a; lastMoveNs=now;
            if(speed>48f) {
                noise+=(speed-48f)*dt*.12f;
                if(speed>150f&&Math.random()<.18) { sounds.play(sndScrape,.20f,.20f,1,0,.82f); vibrate(10,55); }
            } else noise=Math.max(0f,noise-2.2f*dt);
            playDialFeedback(); checkLock(dir,speed);
            if(noise>=100f) failMission(); invalidate(); return true;
        } else if((e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)&&dragging) {
            dragging=false; invalidate(); return true;
        }
        return true;
    }

    private void playDialFeedback() {
        if(stage>=3) return;
        int bucket=(int)(dialPosition/6f); if(bucket==lastTickBucket) return; lastTickBucket=bucket;
        float dist=angularDistance(dialPosition,target[stage]), closeness=1f-Math.min(1f,dist/80f);
        sounds.play(sndTick,.08f+closeness*.20f,.08f+closeness*.20f,0,0,.72f+closeness*.72f);
        if(dist<22f) vibrate(4,20+(int)(closeness*40));
    }

    private void checkLock(int direction,float speed) {
        if(stage>=3||direction!=requiredDirection[stage]) return;
        if(angularDistance(dialPosition,target[stage])<=4.2f&&speed<95f) {
            sounds.play(sndLock,.72f,.72f,1,0,1f); vibrate(32,130); stage++; lastTickBucket=-1;
            if(stage>=3) { won=true; dragging=false; wonAtMs=System.currentTimeMillis(); sounds.play(sndOpen,.8f,.8f,1,0,1f); vibrate(85,170); }
        }
    }

    private void failMission() { if(failed)return; noise=100f; failed=true; dragging=false; sounds.play(sndAlarm,.7f,.7f,2,0,1f); vibrate(240,210); }
    private void resetGame() { dialPosition=0; noise=0; stage=0; failed=false; won=false; dragging=false; lastTickBucket=-1; invalidate(); }
    private void vibrate(long ms,int amp) { if(vibrator==null||!vibrator.hasVibrator())return; vibrator.vibrate(VibrationEffect.createOneShot(ms,Math.max(1,Math.min(255,amp)))); }
    private float touchAngle(float x,float y){return(float)Math.toDegrees(Math.atan2(y-cy,x-cx));}
    private float shortestDelta(float from,float to){float d=to-from;while(d>180)d-=360;while(d<-180)d+=360;return d;}
    private float normalize(float a){a%=360;if(a<0)a+=360;return a;}
    private float angularDistance(float a,float b){float d=Math.abs(normalize(a)-normalize(b));return Math.min(d,360-d);}
    private float distance(float x1,float y1,float x2,float y2){float dx=x1-x2,dy=y1-y2;return(float)Math.sqrt(dx*dx+dy*dy);}
}
