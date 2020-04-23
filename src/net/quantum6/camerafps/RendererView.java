package net.quantum6.camerafps;

import java.nio.ByteBuffer;

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

    /**
     * NV21图像转RGB或BGR
     * @param input NV21格式图像数据
     * @param width 图像宽度
     * @param height 图像高度
     * @param output 输出图像缓冲区
     * @param isRGB 为{@code true}转为RGB图像,否则转为BGR图像
     */
    public void NV212RGBorBGR(byte[] input, int width, int height, byte[] output, boolean isRGB)
    {
        int nvOff = width * height ;
        int  i, j, yIndex = 0;
        int y, u, v;
        int r, g, b, nvIndex = 0;
        for(i = 0; i < height; i++){
            for(j = 0; j < width; j ++,++yIndex){
                nvIndex = (i / 2)  * width + j - j % 2;
                y = input[yIndex] & 0xff;
                u = input[nvOff + nvIndex ] & 0xff;
                v = input[nvOff + nvIndex + 1] & 0xff;

                // yuv to rgb
                r = y + ((351 * (v-128))>>8);  //r
                g = y - ((179 * (v-128) + 86 * (u-128))>>8); //g
                b = y + ((443 * (u-128))>>8); //b
                
                r = ((r>255) ?255 :(r<0)?0:r); 
                g = ((g>255) ?255 :(g<0)?0:g);
                b = ((b>255) ?255 :(b<0)?0:b);
                if(isRGB){
                    output[yIndex*3 + 0] = (byte) b;
                    output[yIndex*3 + 1] = (byte) g;
                    output[yIndex*3 + 2] = (byte) r;
                }else{
                    output[yIndex*3 + 0] = (byte) r;
                    output[yIndex*3 + 1] = (byte) g;
                    output[yIndex*3 + 2] = (byte) b;
                }
            }
        }
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
        
        NV212RGBorBGR(nv21, width, height, rgbBuffer, true);

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
