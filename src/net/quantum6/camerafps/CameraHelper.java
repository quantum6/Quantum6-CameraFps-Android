package net.quantum6.camerafps;

import java.nio.ByteBuffer;
import java.util.List;

import net.quantum6.fps.FpsCounter;
import net.quantum6.kit.CameraDataThread;
import net.quantum6.kit.CameraKit;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 *
 */
final class CameraHelper
{
    private final static String TAG         = "CameraHelper";

    private final static int PREVIEW_FORMAT = ImageFormat.NV21;

    private final static int MIN_FPS        = 10;
    private final static int MAX_FPS        = 60;
    
    private final static int PREVIEW_BUFFER_COUNT   = 5;

    private int frameRate                   = 30;

    FpsCounter fpsCounter = new FpsCounter();
    
    private boolean useBackCamera = true;
    boolean         isInited                = false;


    SurfaceHolder       mPreviewHolder;

    private Camera      mCamera;
    List<Camera.Size>   mSupportedSizes;
    Camera.Size         mPreviewSize;
    
    SurfaceView previewVew;
    VideoRendererView rendererView;
    ByteBuffer byteBuffer;

    CameraHelper(SurfaceView previewVew, VideoRendererView displayView)
    {
        this.previewVew   = previewVew;
        this.rendererView = displayView;
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

            mCamera.startPreview();
            mCamera.setPreviewCallbackWithBuffer(dataThread);
            new Thread(dataThread).start();
            mCamera.startPreview();

            byteBuffer = ByteBuffer.allocateDirect((mPreviewSize.width * mPreviewSize.height * 3) >> 1);
            rendererView.setParams(false, byteBuffer,
                    mPreviewSize.width,
                    mPreviewSize.height,
                    15);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        isInited = true;
    }

    private CameraDataThread dataThread = new CameraDataThread()
    {
        @Override
        public void onCameraDataArrived(final byte[] data, Camera camera)
        {
            if (data == null || data.length == 0)
            {
                return;
            }

            fpsCounter.count();

            byteBuffer.rewind();
            byteBuffer.put(data);
            //((RendererView)rendererView).drawNV21(data, mPreviewSize.width, mPreviewSize.height);
            rendererView.requestRender();
        }
    };

    public int getFps()
    {
        return dataThread.getFps();
    }
    
    SurfaceHolder.Callback previewCallback = new SurfaceHolder.Callback()
    {
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
    };

    private void closeCamera()
    {
        if (null == mCamera)
        {
            return;
        }
        dataThread.stop();
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
