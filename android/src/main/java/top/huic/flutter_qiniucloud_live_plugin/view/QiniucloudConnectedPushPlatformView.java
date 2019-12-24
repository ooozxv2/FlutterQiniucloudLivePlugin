package top.huic.flutter_qiniucloud_live_plugin.view;


import android.content.Context;
import android.util.Log;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.qiniu.pili.droid.rtcstreaming.RTCAudioSource;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCSurfaceView;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.MediaStreamingManager;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.WatermarkSetting;

import java.net.URISyntaxException;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;
import top.huic.flutter_qiniucloud_live_plugin.listener.QiniucloudPlayerListener;
import top.huic.flutter_qiniucloud_live_plugin.listener.QiniuicloudConnectedPushListener;
import top.huic.flutter_qiniucloud_live_plugin.util.CommonUtil;
import top.huic.flutter_qiniucloud_live_plugin.widget.CameraPreviewFrameView;
import top.huic.flutter_qiniucloud_live_plugin.widget.MediaController;

/**
 * 七牛云连麦推流视图
 */
public class QiniucloudConnectedPushPlatformView extends PlatformViewFactory implements PlatformView, MethodChannel.MethodCallHandler {

    /**
     * 日志标签
     */
    private static final String TAG = QiniucloudConnectedPushPlatformView.class.getName();

    /**
     * 全局上下文
     */
    private Context context;

    /**
     * 消息器
     */
    private BinaryMessenger messenger;

    /**
     * 全局标识
     */
    public static final String SIGN = "plugins.huic.top/QiniucloudConnectedPush";

    /**
     * 本地预览内容
     */
    private CameraPreviewFrameView view;

    /**
     * 流管理器
     */
    private RTCMediaStreamingManager manager;

    /**
     * 推流参数
     */
    private StreamingProfile streamingProfile;

    /**
     * 初始化视图工厂，注册视图时调用
     */
    public QiniucloudConnectedPushPlatformView(Context context, BinaryMessenger messenger) {
        super(StandardMessageCodec.INSTANCE);
        this.context = context;
        this.messenger = messenger;
    }

    /**
     * 初始化组件，同时也初始化七牛云推流
     * 每个组件被实例化时调用
     */
    private QiniucloudConnectedPushPlatformView(Context context) {
        super(StandardMessageCodec.INSTANCE);
        this.context = context;
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        switch (call.method) {
            case "startCapture":
                this.startCapture(call, result);
                break;
            case "stopCapture":
                this.stopCapture(call, result);
                break;
            case "startConference":
                this.startConference(call, result);
                break;
            case "stopConference":
                this.stopConference(call, result);
                break;
            case "startStreaming":
                this.startStreaming(call, result);
                break;
            case "stopStreaming":
                this.stopStreaming(call, result);
                break;
            case "destroy":
                this.destroy(call, result);
                break;
            case "isZoomSupported":
                this.isZoomSupported(call, result);
                break;
            case "setZoomValue":
                this.setZoomValue(call, result);
                break;
            case "getZoom":
                this.getZoom(call, result);
                break;
            case "getMaxZoom":
                this.getMaxZoom(call, result);
                break;
            case "turnLightOn":
                this.turnLightOn(call, result);
                break;
            case "turnLightOff":
                this.turnLightOff(call, result);
                break;
            case "switchCamera":
                this.switchCamera(call, result);
                break;
            case "mute":
                this.mute(call, result);
                break;
            case "kickoutUser":
                this.kickoutUser(call, result);
                break;
            case "setConferenceOptions":
                this.setConferenceOptions(call, result);
                break;
            case "setStreamingProfile":
                this.setStreamingProfile(call, result);
                break;
            case "getParticipantsCount":
                this.getParticipantsCount(call, result);
                break;
            case "getParticipants":
                this.getParticipants(call, result);
                break;
            case "setPreviewMirror":
                this.setPreviewMirror(call, result);
                break;
            case "setEncodingMirror":
                this.setEncodingMirror(call, result);
                break;
            case "startPlayback":
                this.startPlayback(call, result);
                break;
            case "stopPlayback":
                this.stopPlayback(call, result);
                break;
            case "updateWatermarkSetting":
                this.updateWatermarkSetting(call, result);
                break;
            case "updateFaceBeautySetting":
                this.updateFaceBeautySetting(call, result);
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args) {
        Map<String, Object> params = (Map<String, Object>) args;
        QiniucloudConnectedPushPlatformView view = new QiniucloudConnectedPushPlatformView(context);
        // 绑定方法监听器
        MethodChannel methodChannel = new MethodChannel(messenger, SIGN + "_" + viewId);
        methodChannel.setMethodCallHandler(view);
        // 初始化
        view.init(params, methodChannel);
        return view;
    }

    @Override
    public void dispose() {

    }

    @Override
    public View getView() {
        return view;
    }

    /**
     * 初始化
     *
     * @param params        参数
     * @param methodChannel 方法通道
     */
    private void init(Map<String, Object> params, MethodChannel methodChannel) {
        // 连麦参数
        String conferenceOptionsStr = (String) params.get("conferenceOptions");
        // 相机参数
        String cameraSettingStr = (String) params.get("cameraStreamingSetting");
        // 推流参数(仅主播)
        String streamingProfileStr = (String) params.get("streamingProfile");
        Map<String, Object> cameraSettingMap = JSON.parseObject(cameraSettingStr);

        // 初始化视图
        view = new CameraPreviewFrameView(context);
        manager = new RTCMediaStreamingManager(context, view, AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC);

        QiniuicloudConnectedPushListener listener = new QiniuicloudConnectedPushListener(context, methodChannel);
        manager.setConferenceStateListener(listener);
        manager.setStreamingSessionListener(listener);
        manager.setStreamingStateListener(listener);
        manager.setUserEventListener(listener);
        manager.setStreamStatusCallback(listener);
        manager.setAudioSourceCallback(listener);

        // TODO 添加窗口具体含义待验证
        // mMediaStreamingManager.addRemoteWindow(windowA);
        // mMediaStreamingManager.addRemoteWindow(windowB);

        // 预览设置
        CameraStreamingSetting cameraStreamingSetting = JSON.parseObject(cameraSettingStr, CameraStreamingSetting.class);
        if (cameraStreamingSetting == null) {
            Log.e(TAG, "init: 相机信息初始化失败!");
        } else {
            // 美颜过滤(设置美颜后，启用美颜过滤，没设置美颜，则自动过滤空)
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY);

            // 美颜设置
            Map faceBeauty = (Map) cameraSettingMap.get("faceBeauty");
            if (faceBeauty != null) {
                cameraStreamingSetting.setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(Float.valueOf(faceBeauty.get("beautyLevel").toString()), Float.valueOf(faceBeauty.get("whiten").toString()), Float.valueOf(faceBeauty.get("redden").toString())));
            }
        }

        // 推流设置
        if (streamingProfileStr != null) {
            streamingProfile = JSON.parseObject(streamingProfileStr, StreamingProfile.class);
        }

        manager.prepare(cameraStreamingSetting, null, null, streamingProfile);
    }

    /**
     * 打开摄像头和麦克风采集
     */
    private void startCapture(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.startCapture());
    }


    /**
     * 关闭摄像头和麦克风采集
     */
    private void stopCapture(MethodCall call, final MethodChannel.Result result) {
        manager.stopCapture();
        result.success(null);
    }

    /**
     * 开始连麦
     */
    private void startConference(MethodCall call, final MethodChannel.Result result) {
        String userId = CommonUtil.getParam(call, result, "userId");
        String roomName = CommonUtil.getParam(call, result, "roomName");
        String roomToken = CommonUtil.getParam(call, result, "roomToken");
        manager.startConference(userId, roomName, roomToken, new RTCStartConferenceCallback() {
            @Override
            public void onStartConferenceSuccess() {
                result.success(null);
            }

            @Override
            public void onStartConferenceFailed(int i) {
                result.error(String.valueOf(i), "", "");
            }
        });
    }

    /**
     * 停止连麦
     */
    private void stopConference(MethodCall call, final MethodChannel.Result result) {
        manager.stopConference();
        result.success(null);
    }

    /**
     * 开始推流
     */
    private void startStreaming(MethodCall call, final MethodChannel.Result result) {
        String publishUrl = call.argument("publishUrl");
        if (publishUrl != null) {
            try {
                streamingProfile.setPublishUrl(publishUrl);
            } catch (URISyntaxException e) {
                Log.e(TAG, "setStreamingProfile: setPublishUrl Error", e);
                result.error("0", e.toString(), e.getMessage());
                return;
            }
            manager.setStreamingProfile(streamingProfile);
        }
        result.success(manager.startStreaming());
    }

    /**
     * 停止推流
     */
    private void stopStreaming(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.stopStreaming());
    }

    /**
     * 销毁
     */
    private void destroy(MethodCall call, final MethodChannel.Result result) {
        manager.destroy();
        result.success(null);
    }

    /**
     * 查询是否支持缩放
     */
    private void isZoomSupported(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.isZoomSupported());
    }

    /**
     * 设置缩放比例
     */
    private void setZoomValue(MethodCall call, final MethodChannel.Result result) {
        int value = CommonUtil.getParam(call, result, "value");
        manager.setZoomValue(value);
        result.success(null);
    }

    /**
     * 获得最大缩放比例
     */
    private void getMaxZoom(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.getMaxZoom());
    }

    /**
     * 获得缩放比例
     */
    private void getZoom(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.getZoom());
    }

    /**
     * 开启闪光灯
     */
    private void turnLightOn(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.turnLightOn());
    }

    /**
     * 关闭闪光灯
     */
    private void turnLightOff(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.turnLightOff());
    }

    /**
     * 切换摄像头
     */
    private void switchCamera(MethodCall call, final MethodChannel.Result result) {
        String target = CommonUtil.getParam(call, result, "target");
        CameraStreamingSetting.CAMERA_FACING_ID id = CameraStreamingSetting.CAMERA_FACING_ID.valueOf(target);
        result.success(manager.switchCamera(id));
    }

    /**
     * 切换静音
     */
    private void mute(MethodCall call, final MethodChannel.Result result) {
        boolean mute = CommonUtil.getParam(call, result, "mute");
        String audioSource = CommonUtil.getParam(call, result, "audioSource");
        if (mute) {
            manager.mute(RTCAudioSource.valueOf(audioSource));
        } else {
            manager.unMute(RTCAudioSource.valueOf(audioSource));

        }
        result.success(null);
    }


    /**
     * 根据用户ID踢人
     */
    private void kickoutUser(MethodCall call, final MethodChannel.Result result) {
        String userId = CommonUtil.getParam(call, result, "userId");
        result.success(manager.kickoutUser(userId));
    }

    /**
     * 设置连麦参数
     */
    private void setConferenceOptions(MethodCall call, final MethodChannel.Result result) {
        manager.setConferenceOptions(JSON.parseObject(CommonUtil.getParam(call, result, "conferenceOptions").toString(), RTCConferenceOptions.class));
        result.success(null);
    }


    /**
     * 更新推流参数
     */
    private void setStreamingProfile(MethodCall call, final MethodChannel.Result result) {
        this.streamingProfile = JSON.parseObject(CommonUtil.getParam(call, result, "streamingProfile").toString(), StreamingProfile.class);
        manager.setStreamingProfile(streamingProfile);
        result.success(null);
    }

    /**
     * 获取参与连麦的人数，不包括自己
     */
    private void getParticipantsCount(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.getParticipantsCount());
    }

    /**
     * 获取参与连麦的用户ID列表，不包括自己
     */
    private void getParticipants(MethodCall call, final MethodChannel.Result result) {
        result.success(JSON.toJSONString(manager.getParticipants()));
    }

    /**
     * 设置预览镜像
     */
    private void setPreviewMirror(MethodCall call, final MethodChannel.Result result) {
        boolean mirror = CommonUtil.getParam(call, result, "mirror");
        result.success(manager.setPreviewMirror(mirror));
    }

    /**
     * 设置推流镜像
     */
    private void setEncodingMirror(MethodCall call, final MethodChannel.Result result) {
        boolean mirror = CommonUtil.getParam(call, result, "mirror");
        result.success(manager.setEncodingMirror(mirror));
    }

    /**
     * 开启耳返
     */
    private void startPlayback(MethodCall call, final MethodChannel.Result result) {
        result.success(manager.startPlayback());
    }

    /**
     * 关闭耳返
     */
    private void stopPlayback(MethodCall call, final MethodChannel.Result result) {
        manager.stopPlayback();
        result.success(null);
    }

    /**
     * 更新动态水印
     */
    private void updateWatermarkSetting(MethodCall call, final MethodChannel.Result result) {
        WatermarkSetting watermarkSetting = new WatermarkSetting(context);
        watermarkSetting.setResourcePath(CommonUtil.getParam(call, result, "resourcePath").toString());
        watermarkSetting.setSize(WatermarkSetting.WATERMARK_SIZE.valueOf(CommonUtil.getParam(call, result, "size").toString()));
        watermarkSetting.setLocation(WatermarkSetting.WATERMARK_LOCATION.valueOf(CommonUtil.getParam(call, result, "location").toString()));
        watermarkSetting.setAlpha(Integer.valueOf(CommonUtil.getParam(call, result, "alpha").toString()));
        manager.updateWatermarkSetting(watermarkSetting);
        result.success(null);
    }

    /**
     * 更新美颜设置
     */
    private void updateFaceBeautySetting(MethodCall call, final MethodChannel.Result result) {
        double beautyLevel = CommonUtil.getParam(call, result, "beautyLevel");
        double redden = CommonUtil.getParam(call, result, "redden");
        double whiten = CommonUtil.getParam(call, result, "whiten");
        manager.setVideoFilterType(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY);
        manager.updateFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting((float) beautyLevel, (float) whiten, (float) redden));
        result.success(null);
    }
}
