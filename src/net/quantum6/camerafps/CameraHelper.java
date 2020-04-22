package net.quantum6.camerafps;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.quantum6.fps.FpsCounter;
import net.quantum6.kit.CameraKit;

import android.graphics.ImageFormat;
import android.hardware.Camera;
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
    
    private final static int PREVIEW_BUFFER_COUNT   = 5;
    
    private final static int MIN_VIDEO_SIZE = 120;

    private int frameRate                   = 30;

    FpsCounter fpsCounter = new FpsCounter();
    
    private boolean useBackCamera = true;
    boolean         isInited                = false;


    SurfaceHolder       mPreviewHolder;

    private Camera      mCamera;
    List<Camera.Size>   mSupportedSizes;
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
    
    public void toggleCamera()
    {
        if (Camera.getNumberOfCameras() > 1)
        {
            useBackCamera   = !useBackCamera;
            mPreviewSize    = null;
            mSupportedSizes = null;
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
            mSupportedSizes = CameraKit.getSupportedSizes(parameters);
            
            Camera.Size size = CameraKit.getCameraBestPreviewSize(parameters, width, height);
            width = size.width;
            height= size.height;

            parameters.setPreviewSize(width, height);
            parameters.setPreviewFormat(PREVIEW_FORMAT);
            parameters.setPreviewFpsRange(MIN_FPS*1000, MAX_FPS*1000);
            parameters.setPreviewFrameRate(frameRate);
            CameraKit.setCameraFocus(parameters);
            
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
    
    public void processData(final byte[] data, Camera camera)
    {
        fpsCounter.count();
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
    }
    
    public void release()
    {
        reset();
        
        mPreviewHolder  = null;
    }

}
