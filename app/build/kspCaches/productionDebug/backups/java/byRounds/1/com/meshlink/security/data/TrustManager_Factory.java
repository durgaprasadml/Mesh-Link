package com.meshlink.security.data;

import com.meshlink.database.data.local.TrustDao;
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
public final class TrustManager_Factory implements Factory<TrustManager> {
  private final Provider<TrustDao> trustDaoProvider;

  private final Provider<MeshSecurityMonitor> securityMonitorProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public TrustManager_Factory(Provider<TrustDao> trustDaoProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.trustDaoProvider = trustDaoProvider;
    this.securityMonitorProvider = securityMonitorProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public TrustManager get() {
    return newInstance(trustDaoProvider.get(), securityMonitorProvider.get(), ioDispatcherProvider.get());
  }

  public static TrustManager_Factory create(Provider<TrustDao> trustDaoProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new TrustManager_Factory(trustDaoProvider, securityMonitorProvider, ioDispatcherProvider);
  }

  public static TrustManager newInstance(TrustDao trustDao, MeshSecurityMonitor securityMonitor,
      CoroutineDispatcher ioDispatcher) {
    return new TrustManager(trustDao, securityMonitor, ioDispatcher);
  }
}
