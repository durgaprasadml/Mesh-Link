package com.meshlink.video.streaming;

import com.meshlink.video.camera.CameraController;
import com.meshlink.video.codec.VideoCodecManager;
import com.meshlink.video.transport.VideoTransport;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class VideoStreamManager_Factory implements Factory<VideoStreamManager> {
  private final Provider<CameraController> cameraControllerProvider;

  private final Provider<VideoCodecManager> codecManagerProvider;

  private final Provider<VideoTransport> transportProvider;

  public VideoStreamManager_Factory(Provider<CameraController> cameraControllerProvider,
      Provider<VideoCodecManager> codecManagerProvider,
      Provider<VideoTransport> transportProvider) {
    this.cameraControllerProvider = cameraControllerProvider;
    this.codecManagerProvider = codecManagerProvider;
    this.transportProvider = transportProvider;
  }

  @Override
  public VideoStreamManager get() {
    return newInstance(cameraControllerProvider.get(), codecManagerProvider.get(), transportProvider.get());
  }

  public static VideoStreamManager_Factory create(
      Provider<CameraController> cameraControllerProvider,
      Provider<VideoCodecManager> codecManagerProvider,
      Provider<VideoTransport> transportProvider) {
    return new VideoStreamManager_Factory(cameraControllerProvider, codecManagerProvider, transportProvider);
  }

  public static VideoStreamManager newInstance(CameraController cameraController,
      VideoCodecManager codecManager, VideoTransport transport) {
    return new VideoStreamManager(cameraController, codecManager, transport);
  }
}
