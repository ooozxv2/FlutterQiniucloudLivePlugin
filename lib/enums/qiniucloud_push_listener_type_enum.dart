/// 七牛云推流监听类型枚举
enum QiniucloudPushListenerTypeEnum {
  /// 回调音频采集 PCM 数据
  AudioSourceAvailable,

  /// 根据StreamingProfile.StreamStatusConfig.getIntervalMs（）调用
  StreamStatusChanged,

  /// 录音失败时调用。
  RecordAudioFailedHandled,

  /// 重新启动流式处理通知。
  RestartStreamingHandled,

  /// 在构造相机对象后调用。
  PreviewSizeSelected,

  /// 自定义预览fps，在构造相机对象后调用。
  PreviewFpsSelected,

  /// 状态发生改变
  StateChanged,
}
