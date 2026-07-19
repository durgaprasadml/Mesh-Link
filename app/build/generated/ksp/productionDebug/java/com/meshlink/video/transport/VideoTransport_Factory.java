package com.meshlink.video.transport;

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
public final class VideoTransport_Factory implements Factory<VideoTransport> {
  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public VideoTransport_Factory(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public VideoTransport get() {
    return newInstance(cryptoManagerProvider.get(), ioDispatcherProvider.get());
  }

  public static VideoTransport_Factory create(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new VideoTransport_Factory(cryptoManagerProvider, ioDispatcherProvider);
  }

  public static VideoTransport newInstance(MeshCryptoManager cryptoManager,
      CoroutineDispatcher ioDispatcher) {
    return new VideoTransport(cryptoManager, ioDispatcher);
  }
}
