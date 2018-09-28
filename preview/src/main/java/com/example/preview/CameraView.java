package com.example.preview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

public class CameraView extends CameraDevice.StateCallback {
    static final String TAG = "CAM";

    Handler mHandler;
    CameraManager cm;
    CameraDevice device;
    MStateCallback mStateCallback;
    MCaptureCallback mCaptureCallback;
    ArrayList<Surface> surfaces;
    Context ctx;
    String cameraId=null;
    

    public CameraView(Context context, SurfaceHolder surfaceHolder ) {
        cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        ctx = context;
        surfaces = new ArrayList<Surface>();
        try {
            String[] camera = cm.getCameraIdList();
            for (String cam : camera) {
                CameraCharacteristics characteristics = cm.getCameraCharacteristics(cam);
                cameraId = cam;
                int face = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.i(TAG, "cam " + cam +" facing "+face);
                //CameraMetadata.LENS_FACING_FRONT
                if (CameraMetadata.LENS_FACING_BACK == face) {
                    Log.i(TAG,"set camera "+cam);
                    StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] size = configs.getOutputSizes(SurfaceHolder.class);
                    surfaceHolder.setFixedSize(size[0].getWidth(),size[0].getHeight());
                    surfaces.add(surfaceHolder.getSurface());
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "err " + e);
        }
        mStateCallback = new MStateCallback();
        mCaptureCallback = new MCaptureCallback();
        createHandler();
        try {
            if (ctx.checkCallingOrSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                return;
            }
            Log.i(TAG, "openCamera");
            cm.openCamera(cameraId, this, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "can not access camera " + e);
        }
    }

    void createHandler() {
        HandlerThread thread = new HandlerThread("cam");
        thread.start();
        Looper looper = thread.getLooper();
        mHandler = new Handler(looper);
    }

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        Log.i(TAG, "onOpened ");
        device = cameraDevice;
        try {
            device.createCaptureSession(surfaces, mStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "can not access camera " + e);
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {

    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int i) {

    }

    class MStateCallback extends CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.i(TAG, "onConfigured ");
            CaptureRequest request = null;
            try {
                CaptureRequest.Builder builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(surfaces.get(0));
                request = builder.build();
            } catch (CameraAccessException e) {
                Log.e(TAG, "can not access camera  " + e);
                return;
            }
            try {
                cameraCaptureSession.setRepeatingRequest (request,mCaptureCallback,mHandler);
                //cameraCaptureSession.capture(request, mCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

        }
    }

    class MCaptureCallback extends CameraCaptureSession.CaptureCallback {

    }

}
