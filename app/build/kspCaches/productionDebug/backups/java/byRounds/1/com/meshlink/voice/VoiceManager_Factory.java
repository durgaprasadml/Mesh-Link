package com.meshlink.voice;

import com.meshlink.voice.streaming.AudioStreamer;
import com.meshlink.voice.streaming.VoiceSessionManager;
import com.meshlink.voice.transport.VoiceTransport;
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
public final class VoiceManager_Factory implements Factory<VoiceManager> {
  private final Provider<VoiceSessionManager> sessionManagerProvider;

  private final Provider<VoiceTransport> transportProvider;

  private final Provider<AudioStreamer> streamerProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public VoiceManager_Factory(Provider<VoiceSessionManager> sessionManagerProvider,
      Provider<VoiceTransport> transportProvider, Provider<AudioStreamer> streamerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
    this.transportProvider = transportProvider;
    this.streamerProvider = streamerProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public VoiceManager get() {
    return newInstance(sessionManagerProvider.get(), transportProvider.get(), streamerProvider.get(), ioDispatcherProvider.get());
  }

  public static VoiceManager_Factory create(Provider<VoiceSessionManager> sessionManagerProvider,
      Provider<VoiceTransport> transportProvider, Provider<AudioStreamer> streamerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new VoiceManager_Factory(sessionManagerProvider, transportProvider, streamerProvider, ioDispatcherProvider);
  }

  public static VoiceManager newInstance(VoiceSessionManager sessionManager,
      VoiceTransport transport, AudioStreamer streamer, CoroutineDispatcher ioDispatcher) {
    return new VoiceManager(sessionManager, transport, streamer, ioDispatcher);
  }
}
