package ro.atm.corden.util.webrtc.usb_camera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import org.webrtc.CapturerObserver;
import org.webrtc.EglBase;
import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Used to capture video stream from UVC Camera connected through USB cable to the mobile phone
 */
public class UsbCapturer implements VideoCapturer, USBMonitor.OnDeviceConnectListener, IFrameCallback {
    private Context context;
    private USBMonitor monitor;
    private SurfaceViewRenderer svVideoRender;
    private SurfaceTextureHelper surfaceTextureHelper;
    private CapturerObserver capturerObserver;
    private Executor executor = Executors.newSingleThreadExecutor();

    public UsbCapturer(final Context context, SurfaceViewRenderer svVideoRender) {
        this.context = context;
        this.svVideoRender = svVideoRender;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                monitor = new USBMonitor(context, UsbCapturer.this);
                monitor.register();
            }
        });
    }

    public UsbCapturer(final Context context) {
        this.context = context;
        this.svVideoRender = new SurfaceViewRenderer(context);
        new Handler(Looper.getMainLooper())
                .post(() -> {
                    EglBase eglBase = EglBase.create();
                    svVideoRender.init(eglBase.getEglBaseContext(), null);
                    svVideoRender.setEnableHardwareScaler(true);
                });

        executor.execute(new Runnable() {
            @Override
            public void run() {
                monitor = new USBMonitor(context, UsbCapturer.this);
                monitor.register();
            }
        });
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int i, int i1, int i2) {

    }

    @Override
    public void stopCapture() throws InterruptedException {
        if (camera != null) {
            camera.stopPreview();
            camera.close();
            camera.destroy();
        }
    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {
        camera.setPreviewSize(i, i1, i2);
    }

    @Override
    public void dispose() {
        monitor.unregister();
        monitor.destroy();

    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    @Override
    public void onAttach(UsbDevice device) {
        monitor.requestPermission(device);
    }

    @Override
    public void onDettach(UsbDevice device) {
    }

    UVCCamera camera;

    @Override
    public void onConnect(UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                camera = new UVCCamera();
                camera.open(ctrlBlock);
                try {
                    camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                } catch (final IllegalArgumentException e) {
                    try {
                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                    } catch (final IllegalArgumentException e1) {
                        camera.destroy();
                        camera = null;
                    }
                }
                camera.setPreviewDisplay(svVideoRender.getHolder().getSurface());
                camera.setFrameCallback(UsbCapturer.this, UVCCamera.PIXEL_FORMAT_NV21);
                camera.startPreview();
            }
        });
    }

    @Override
    public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
    }

    @Override
    public void onCancel(UsbDevice device) {
    }

    @Override
    public void onFrame(final ByteBuffer frame) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] imageArray = new byte[frame.remaining()];
                frame.get(imageArray); //without this line only a green image was transferred
                long timestampNS = System.nanoTime();
                NV21Buffer buffer = new NV21Buffer(imageArray, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, null);
                VideoFrame videoFrame = new VideoFrame(buffer, 0, timestampNS);
                capturerObserver.onFrameCaptured(videoFrame);
            }
        });
    }
}