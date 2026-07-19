package com.meshlink.voice.streaming;

import com.meshlink.voice.audio.AudioEngine;
import com.meshlink.voice.codec.JitterBuffer;
import com.meshlink.voice.codec.VoiceCodecManager;
import com.meshlink.voice.transport.VoiceTransport;
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
public final class AudioStreamer_Factory implements Factory<AudioStreamer> {
  private final Provider<AudioEngine> audioEngineProvider;

  private final Provider<VoiceCodecManager> codecManagerProvider;

  private final Provider<JitterBuffer> jitterBufferProvider;

  private final Provider<VoiceTransport> transportProvider;

  public AudioStreamer_Factory(Provider<AudioEngine> audioEngineProvider,
      Provider<VoiceCodecManager> codecManagerProvider, Provider<JitterBuffer> jitterBufferProvider,
      Provider<VoiceTransport> transportProvider) {
    this.audioEngineProvider = audioEngineProvider;
    this.codecManagerProvider = codecManagerProvider;
    this.jitterBufferProvider = jitterBufferProvider;
    this.transportProvider = transportProvider;
  }

  @Override
  public AudioStreamer get() {
    return newInstance(audioEngineProvider.get(), codecManagerProvider.get(), jitterBufferProvider.get(), transportProvider.get());
  }

  public static AudioStreamer_Factory create(Provider<AudioEngine> audioEngineProvider,
      Provider<VoiceCodecManager> codecManagerProvider, Provider<JitterBuffer> jitterBufferProvider,
      Provider<VoiceTransport> transportProvider) {
    return new AudioStreamer_Factory(audioEngineProvider, codecManagerProvider, jitterBufferProvider, transportProvider);
  }

  public static AudioStreamer newInstance(AudioEngine audioEngine, VoiceCodecManager codecManager,
      JitterBuffer jitterBuffer, VoiceTransport transport) {
    return new AudioStreamer(audioEngine, codecManager, jitterBuffer, transport);
  }
}
