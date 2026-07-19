package com.meshlink.video;

import com.meshlink.video.streaming.VideoSessionManager;
import com.meshlink.video.streaming.VideoStreamManager;
import com.meshlink.video.transport.VideoTransport;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.meshlink.di.IoDispatcher")
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
public final class VideoManager_Factory implements Factory<VideoManager> {
  private final Provider<VideoSessionManager> sessionManagerProvider;

  private final Provider<VideoTransport> transportProvider;

  private final Provider<VideoStreamManager> streamManagerProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public VideoManager_Factory(Provider<VideoSessionManager> sessionManagerProvider,
      Provider<VideoTransport> transportProvider,
      Provider<VideoStreamManager> streamManagerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
    this.transportProvider = transportProvider;
    this.streamManagerProvider = streamManagerProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public VideoManager get() {
    return newInstance(sessionManagerProvider.get(), transportProvider.get(), streamManagerProvider.get(), ioDispatcherProvider.get());
  }

  public static VideoManager_Factory create(Provider<VideoSessionManager> sessionManagerProvider,
      Provider<VideoTransport> transportProvider,
      Provider<VideoStreamManager> streamManagerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new VideoManager_Factory(sessionManagerProvider, transportProvider, streamManagerProvider, ioDispatcherProvider);
  }

  public static VideoManager newInstance(VideoSessionManager sessionManager,
      VideoTransport transport, VideoStreamManager streamManager,
      CoroutineDispatcher ioDispatcher) {
    return new VideoManager(sessionManager, transport, streamManager, ioDispatcher);
  }
}
