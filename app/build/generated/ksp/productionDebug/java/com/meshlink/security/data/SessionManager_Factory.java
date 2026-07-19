package com.meshlink.security.data;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.meshlink.di.DefaultDispatcher")
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
public final class SessionManager_Factory implements Factory<SessionManager> {
  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<TrustManager> trustManagerProvider;

  private final Provider<MeshSecurityMonitor> securityMonitorProvider;

  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  public SessionManager_Factory(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<TrustManager> trustManagerProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.trustManagerProvider = trustManagerProvider;
    this.securityMonitorProvider = securityMonitorProvider;
    this.defaultDispatcherProvider = defaultDispatcherProvider;
  }

  @Override
  public SessionManager get() {
    return newInstance(cryptoManagerProvider.get(), trustManagerProvider.get(), securityMonitorProvider.get(), defaultDispatcherProvider.get());
  }

  public static SessionManager_Factory create(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<TrustManager> trustManagerProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    return new SessionManager_Factory(cryptoManagerProvider, trustManagerProvider, securityMonitorProvider, defaultDispatcherProvider);
  }

  public static SessionManager newInstance(MeshCryptoManager cryptoManager,
      TrustManager trustManager, MeshSecurityMonitor securityMonitor,
      CoroutineDispatcher defaultDispatcher) {
    return new SessionManager(cryptoManager, trustManager, securityMonitor, defaultDispatcher);
  }
}
