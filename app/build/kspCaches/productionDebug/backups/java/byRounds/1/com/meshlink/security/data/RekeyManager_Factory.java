package com.meshlink.security.data;

import com.meshlink.domain.repository.UserRepository;
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
public final class RekeyManager_Factory implements Factory<RekeyManager> {
  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  public RekeyManager_Factory(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.defaultDispatcherProvider = defaultDispatcherProvider;
  }

  @Override
  public RekeyManager get() {
    return newInstance(cryptoManagerProvider.get(), sessionManagerProvider.get(), userRepositoryProvider.get(), defaultDispatcherProvider.get());
  }

  public static RekeyManager_Factory create(Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    return new RekeyManager_Factory(cryptoManagerProvider, sessionManagerProvider, userRepositoryProvider, defaultDispatcherProvider);
  }

  public static RekeyManager newInstance(MeshCryptoManager cryptoManager,
      SessionManager sessionManager, UserRepository userRepository,
      CoroutineDispatcher defaultDispatcher) {
    return new RekeyManager(cryptoManager, sessionManager, userRepository, defaultDispatcher);
  }
}
