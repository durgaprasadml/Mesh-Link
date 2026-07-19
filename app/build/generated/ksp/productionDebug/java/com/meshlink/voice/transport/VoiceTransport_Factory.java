package com.meshlink.voice.transport;

import com.meshlink.security.data.MeshCryptoManager;
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
public final class VoiceTransport_Factory implements Factory<VoiceTransport> {
  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public VoiceTransport_Factory(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public VoiceTransport get() {
    return newInstance(cryptoManagerProvider.get(), ioDispatcherProvider.get());
  }

  public static VoiceTransport_Factory create(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new VoiceTransport_Factory(cryptoManagerProvider, ioDispatcherProvider);
  }

  public static VoiceTransport newInstance(MeshCryptoManager cryptoManager,
      CoroutineDispatcher ioDispatcher) {
    return new VoiceTransport(cryptoManager, ioDispatcher);
  }
}
