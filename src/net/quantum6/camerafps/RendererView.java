package net.quantum6.camerafps;

import java.nio.ByteBuffer;

import net.quantum6.mediacodec.MediaCodecKit;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

public class RendererView extends SurfaceView implements SurfaceHolder.Callback
{
    private static String TAG = RendererView.class.getCanonicalName();
    
    private Rect m_srcRect;
    private Rect m_dstRect;
    private SurfaceHolder m_surfaceHolder;
    private Canvas m_canvas;
    
    private byte[] rgbBuffer;
    private ByteBuffer dataBuffer;
    private Bitmap videoBitmap;

    private int srcWidth  = 0;
    private int srcHeight = 0;

    public RendererView(Context context)
    {
        super(context);
        Log.i(TAG, "MySurfaceView Constructor");
        m_surfaceHolder = this.getHolder();
        m_surfaceHolder.addCallback(this);
    }

    public void drawNV21(byte[] nv21, int width, int height)
    {
        if (width != srcWidth || height != srcHeight)
        {
            srcWidth  = width;
            srcHeight = height;
            rgbBuffer = new byte[width*height*4];
            dataBuffer = ByteBuffer.allocate(width*height*4);
            videoBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            m_dstRect = new Rect(0, 0, getWidth(), getHeight());
            m_srcRect = new Rect(0, 0, srcWidth, srcHeight);
        }
        
        MediaCodecKit.NV21ToRGBA(nv21, width, height, rgbBuffer, true);
        //MediaCodecKit.YV12ToBGR24_Table(nv21, rgbBuffer, width, height);

        dataBuffer.rewind();
        dataBuffer.put(rgbBuffer);
        dataBuffer.position(0);

        videoBitmap.copyPixelsFromBuffer(dataBuffer);

        m_canvas = m_surfaceHolder.lockCanvas();

        m_canvas.drawColor(Color.BLACK);

        m_canvas.drawBitmap(videoBitmap, m_srcRect, m_dstRect, null);

        if (m_canvas != null)
        {
            m_surfaceHolder.unlockCanvasAndPost(m_canvas);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        //
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        //
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        //
    }
}
