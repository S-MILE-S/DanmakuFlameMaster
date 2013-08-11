/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.DrawTask;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.renderer.android.DanmakuRenderer;

public class DanmakuSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mSurfaceHolder;

    private HandlerThread mDrawThread;

    private DrawHandler handler;

    private long startTime;

    private float cx, cy;

    private long avgDuration;

    private long maxDuration;

    private DanmakuTimer timer;

    private DanmakuRenderer renderer;

    private DrawTask drawTask;

    private long mTimeBase;

    private boolean isSurfaceCreated;

    public DanmakuSurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setZOrderOnTop(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        if (timer == null) {
            timer = new DanmakuTimer();
        }
    }

    public DanmakuSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DanmakuSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startDraw();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isSurfaceCreated) {
                if (handler == null || handler.isStop())
                    startDraw();
                else
                    stopDraw();
            }
        }

        return true;
    }

    private void stopDraw() {
        quitDrawThread();
    }

    private void quitDrawThread() {
        if (handler != null) {
            handler.quit();
            handler = null;
        }
        if (mDrawThread != null) {
            mDrawThread.quit();
            mDrawThread = null;
        }
    }

    private void startDraw() {
        mDrawThread = new HandlerThread("draw thread");
        mDrawThread.start();
        handler = new DrawHandler(mDrawThread.getLooper());
        handler.sendEmptyMessage(DrawHandler.START);
    }

    void drawDanmakus() {
        long stime = System.currentTimeMillis();
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {

            DrawHelper.clearCanvas(canvas);
            if (drawTask == null)
                drawTask = new DrawTask(timer, getContext(), canvas.getWidth(), canvas.getHeight());
            drawTask.draw(canvas);

            long dtime = System.currentTimeMillis() - stime;
            String fps = String.format("%.2f fps", 1000 / (float) dtime);
            DrawHelper.drawText(canvas, fps);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }

    }

    public class DrawHandler extends Handler {

        private static final int START = 1;

        private static final int UPDATE = 2;

        private boolean quitFlag;

        public DrawHandler(Looper looper) {
            super(looper);
        }

        public void quit() {
            quitFlag = true;
        }

        public boolean isStop() {
            return quitFlag == true;
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case START:
                    quitFlag = false;
                    mTimeBase = System.currentTimeMillis();
                    timer.update(0);
                    sendEmptyMessage(UPDATE);
                    break;
                case UPDATE:
                    if (!quitFlag) {
                        long d = timer.update(System.currentTimeMillis() - mTimeBase);
                        if(d<5){
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        drawDanmakus();
                        sendEmptyMessage(UPDATE);

                    }
                    break;
            }
        }

    }



}
