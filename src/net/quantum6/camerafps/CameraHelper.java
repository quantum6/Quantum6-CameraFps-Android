package net.quantum6.camerafps;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 *
 */
final class CameraHelper implements SurfaceHolder.Callback
{
    private final static String TAG         = "CameraHelper";

    private final static int PREVIEW_FORMAT = ImageFormat.NV21;

    private final static int MIN_FPS        = 10;
    private final static int MAX_FPS        = 60;
    
    private final static int PREVIEW_BUFFER_COUNT   = 3;
    
    private final static int MIN_VIDEO_SIZE = 120;

    private int frameRate                   = 30;

    private long mFpsStartTime  = 0;
    private int  mFpsCounter    = 0;
    public  int  mFpsCurrent    = 0;
    private final int FPS_MS_TIME  = 1000; 
    
    private int  mGsCounter     = 0;
    private int  GREEN_SCREEN_COUNT = 10;
    private boolean mResolutionChecked = false;
    
    private boolean useBackCamera = true;
    boolean         isInited                = false;


    SurfaceHolder       mPreviewHolder;

    private Camera      mCamera;
    List<Size>          mSupportedSizes;
    Camera.Size         mPreviewSize;

    CameraHelper()
    {
        //
    }

    public void changeResolution(int width, int height)
    {
        if (null != mPreviewSize && width == mPreviewSize.width && height == mPreviewSize.height)
        {
            return;
        }
        reset();
        initCamera(width, height);
    }
    
    private static boolean setCameraFocus(Camera.Parameters parameters, String selected)
    {
        List<String> modes = parameters.getSupportedFocusModes();
        try
        {
            for (String mode : modes)
            {
                //优先使用这个对焦方式。
                if (null != mode && mode.equals(selected))
                {
                    parameters.setFocusMode(mode);
                    Log.e(TAG, "setCameraFocus="+mode);
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    private static void setCameraFocus(Camera.Parameters parameters)
    {
        String[] selectedModes =
            {
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            };
        for (String mode : selectedModes)
        {
            if (setCameraFocus(parameters, mode))
            {
                return;
            }
        }
    }
    
    public void toggleCamera()
    {
        if (Camera.getNumberOfCameras() > 1)
        {
            useBackCamera = !useBackCamera;
            mPreviewSize = null;
        }
    }

    private void initCamera(int width, int height)
    {
        if (null != mCamera)
        {
            return;
        }
        
        if (null == mCamera)
        {
            try
            {
                mCamera = Camera.open(useBackCamera ? 0 : 1);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters)
            {
                return;
            }
            mSupportedSizes = parameters.getSupportedPreviewSizes();
            Collections.sort(mSupportedSizes, new SizeComparator());
            for (int i = 0; i < mSupportedSizes.size(); i++)
            {
                Size size = mSupportedSizes.get(i);
                if (       size.width  < MIN_VIDEO_SIZE
                        || size.height < MIN_VIDEO_SIZE)
                {
                    //mSupportedSizes.remove(i);
                }
                Log.d(TAG, "i=" + i + ", " + size.width + ", " + size.height);
            }
            
            List<Size>  picSizes = parameters.getSupportedPictureSizes();
            for (int i = 0; i < picSizes.size(); i++)
            {
                Size size = picSizes.get(i);
                if (       size.width  < MIN_VIDEO_SIZE
                        || size.height < MIN_VIDEO_SIZE)
                {
                    //mSupportedSizes.remove(i);
                }
                Log.d(TAG, "i=" + i + ", " + size.width + ", " + size.height);
            }
            
            picSizes = parameters.getSupportedVideoSizes();
            if (null != picSizes)
            {
                for (int i = 0; i < picSizes.size(); i++)
                {
                    Size size = picSizes.get(i);
                    if (       size.width  < MIN_VIDEO_SIZE
                            || size.height < MIN_VIDEO_SIZE)
                    {
                        //mSupportedSizes.remove(i);
                    }
                    Log.d(TAG, "i=" + i + ", " + size.width + ", " + size.height);
                }
            }
            
            if (0 == width || 0 == height)
            {
                Size size = mSupportedSizes.get(0);
                width = size.width;
                height= size.height;
            }

            parameters.setPreviewSize(width, height);
            parameters.setPreviewFormat(PREVIEW_FORMAT);
            parameters.setPreviewFpsRange(MIN_FPS*1000, MAX_FPS*1000);
            parameters.setPreviewFrameRate(frameRate);
            setCameraFocus(parameters);
            
            Log.d(TAG, "getSupportedAntibanding()" +parameters.getSupportedAntibanding() );
            Log.d(TAG, "getSupportedColorEffects()"+parameters.getSupportedColorEffects());
            Log.d(TAG, "getSupportedFlashModes()"  +parameters.getSupportedFlashModes()  );
            Log.d(TAG, "getSupportedFocusModes()"  +parameters.getSupportedFocusModes()  );
            Log.d(TAG, "getSupportedSceneModes()"  +parameters.getSupportedSceneModes()  );
            Log.d(TAG, "getSupportedWhiteBalance()"+parameters.getSupportedWhiteBalance());
            
            Log.d(TAG, ""+parameters.getSupportedPreviewFpsRange());
            Log.d(TAG, ""+parameters.getSupportedPreviewFrameRates());
 
            Log.d(TAG, ""+parameters.getSupportedPictureSizes());
            Log.d(TAG, ""+parameters.getPreferredPreviewSizeForVideo());
            Log.d(TAG, ""+parameters.getSupportedPreviewSizes());

            mCamera.setParameters(parameters);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters)
            {
                return;
            }

            mPreviewSize = parameters.getPreviewSize();
            mCamera.setPreviewDisplay(mPreviewHolder);
            int bufSize = mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(PREVIEW_FORMAT) / 8;
            Log.d(TAG, "----" + bufSize + ", " + mPreviewSize.width + ", " + mPreviewSize.height);

            for (int i = 0; i < PREVIEW_BUFFER_COUNT; i++)
            {
                mCamera.addCallbackBuffer(new byte[bufSize]);
            }

            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback()
            {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera)
                {
                    if (null != data && data.length != 0)
                    {
                        processData(data, camera);
                    }
                    if (null != mCamera)
                    {
                        mCamera.addCallbackBuffer(data);
                    }
                }
            });

            mCamera.startPreview();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        isInited = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        Log.d(TAG, "surfaceCreated()");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Log.d(TAG, "surfaceChanged()");
        initCamera(0, 0);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.d(TAG, "surfaceDestroyed()");
        release();
    }
    
    private void calculateFps()
    {
        long currentTime = System.currentTimeMillis();
        if (0 == mFpsStartTime)
        {
            mFpsCurrent   = 0;
            mFpsCounter   = 1;
            mFpsStartTime = currentTime;
            return;
        }
        
        if (currentTime - mFpsStartTime > FPS_MS_TIME)
        {
            mFpsCurrent   = mFpsCounter;
            mFpsCounter   = 1;
            mFpsStartTime = currentTime;
            Log.d(TAG, "Preview mFpsCurrent="+mFpsCurrent);
        }
        else
        {
            mFpsCounter++;
        }
    }

    static public void decodeYUV420SPToRGB(int[] rgb, byte[] yuv420sp, int width, int height)
    {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++)
        {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++)
            {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0)
                {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    private boolean checkResolution(final byte[] data)
    {
        if (this.mResolutionChecked)
        {
            return false;
        }
        
        for (int i=0; i<data.length; i++)
        {
            if (0 != data[i])
            {
                mResolutionChecked = true;
                mGsCounter = 0;
                return false;
            }
        }
        
        this.mGsCounter++;
        if (this.mGsCounter < GREEN_SCREEN_COUNT)
        {
            return false;
        }
        
        for (int i = mSupportedSizes.size()-1; i >= 0; i--)
        {
            Size size = mSupportedSizes.get(i);
            if (       size.width  < this.mPreviewSize.width
                    || size.height < this.mPreviewSize.height)
            {
                this.changeResolution(size.width, size.height);
                return true;
            }
        }
        return false;
    }
    
    public void processData(final byte[] data, Camera camera)
    {
        calculateFps();
        Log.d(TAG, "mFpsCounter="+mFpsCounter+", data="+data.length);
        
        if (checkResolution(data))
        {
            return;
        }

        //YuvImage image = new YuvImage(data, PREVIEW_FORMAT, mPreviewSize.width, mPreviewSize.height, null);
        //setData(image, mPreviewSize.width, mPreviewSize.height);
        
        /*ByteBuffer mByteBuffer = ByteBuffer.allocate(data.length);
        mByteBuffer.clear();
        mByteBuffer.put(data);
        mByteBuffer.rewind();
        
        int[] rgb = new int[mPreviewSize.width* mPreviewSize.height*4];
        decodeYUV420SPToRGB(rgb, data, mPreviewSize.width, mPreviewSize.height);
        for (int i=0; i<10; i++)
        {
            Log.d(TAG, "rgb="
                    +", "+rgb[i*10+0]+", "+rgb[i*10+1]
                    +", "+rgb[i*10+2]+", "+rgb[i*10+3]
                    +", "+rgb[i*10+4]+", "+rgb[i*10+5]
                    +", "+rgb[i*10+6]+", "+rgb[i*10+7]
                    +", "+rgb[i*10+8]+", "+rgb[i*10+9]
                    );
        }*/
        
/*        Bitmap mBitmap = Bitmap.createBitmap(mPreviewSize.width, mPreviewSize.height, Bitmap.Config.RGB_565);
        Log.d(TAG, "mBitmap="+mBitmap.getWidth()+", "+mBitmap.getHeight()
                +", "+mBitmap.getByteCount()
                +", "+mBitmap.getRowBytes());
        mBitmap.copyPixelsFromBuffer(mByteBuffer);
        for (int i=0; i<10; i++)
        {
            Log.d(TAG, "count"
                    +", "+mBitmap.getPixel(i, 0)
                    +", "+mBitmap.getPixel(i, 1)
                    +", "+mBitmap.getPixel(i, 2)
                    +", "+mBitmap.getPixel(i, 3)
                    +", "+mBitmap.getPixel(i, 4)
                    +", "+mBitmap.getPixel(i, 5)
                    +", "+mBitmap.getPixel(i, 6)
                    +", "+mBitmap.getPixel(i, 7)
                    +", "+mBitmap.getPixel(i, 8)
                    +", "+mBitmap.getPixel(i, 9)
                    );
        }
*/
    }

    private void closeCamera()
    {
        if (null == mCamera)
        {
            return;
        }
        try
        {
            mCamera.setPreviewCallback(null); // 锛侊紒杩欎釜蹇呴』鍦ㄥ墠锛屼笉鐒堕��鍑哄嚭閿�
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void reset()
    {
        closeCamera();

        isInited            = false;
        mPreviewSize        = null;

        mFpsStartTime   = 0;
        mFpsCounter     = 0;
        mFpsCurrent     = 0;
        
        mResolutionChecked = false;
        mGsCounter      = 0;
    }
    
    public void release()
    {
        reset();
        
        mPreviewHolder  = null;
    }

    /**
     * @author PC
     * 
     */
    private class SizeComparator implements Comparator<Size>
    {
        @Override
        public int compare(Size arg0, Size arg1)
        {
            if (arg0.width > arg1.width)
            {
                return 1;
            }
            if (arg0.width < arg1.width)
            {
                return -1;
            }
            if (arg0.height == arg1.height)
            {
                return 0;
            }
            return (arg0.height > arg1.height) ? 1 : -1;
        }
    }

}
