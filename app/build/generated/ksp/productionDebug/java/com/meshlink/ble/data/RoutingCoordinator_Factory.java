package com.meshlink.ble.data;

import com.meshlink.domain.repository.UserRepository;
import com.meshlink.routing.data.MeshRouter;
import com.meshlink.security.data.MeshCryptoManager;
import com.meshlink.security.data.RekeyManager;
import com.meshlink.security.data.SessionManager;
import com.meshlink.security.data.TrustManager;
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
public final class RoutingCoordinator_Factory implements Factory<RoutingCoordinator> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<TrustManager> trustManagerProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<RekeyManager> rekeyManagerProvider;

  private final Provider<MeshRouter> meshRouterProvider;

  private final Provider<BleConnectionManager> connectionManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  public RoutingCoordinator_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<TrustManager> trustManagerProvider, Provider<SessionManager> sessionManagerProvider,
      Provider<RekeyManager> rekeyManagerProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<BleConnectionManager> connectionManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.trustManagerProvider = trustManagerProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.rekeyManagerProvider = rekeyManagerProvider;
    this.meshRouterProvider = meshRouterProvider;
    this.connectionManagerProvider = connectionManagerProvider;
    this.discoveryManagerProvider = discoveryManagerProvider;
  }

  @Override
  public RoutingCoordinator get() {
    return newInstance(userRepositoryProvider.get(), cryptoManagerProvider.get(), trustManagerProvider.get(), sessionManagerProvider.get(), rekeyManagerProvider.get(), meshRouterProvider.get(), connectionManagerProvider.get(), discoveryManagerProvider.get());
  }

  public static RoutingCoordinator_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<TrustManager> trustManagerProvider, Provider<SessionManager> sessionManagerProvider,
      Provider<RekeyManager> rekeyManagerProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<BleConnectionManager> connectionManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider) {
    return new RoutingCoordinator_Factory(userRepositoryProvider, cryptoManagerProvider, trustManagerProvider, sessionManagerProvider, rekeyManagerProvider, meshRouterProvider, connectionManagerProvider, discoveryManagerProvider);
  }

  public static RoutingCoordinator newInstance(UserRepository userRepository,
      MeshCryptoManager cryptoManager, TrustManager trustManager, SessionManager sessionManager,
      RekeyManager rekeyManager, MeshRouter meshRouter, BleConnectionManager connectionManager,
      DiscoveryManager discoveryManager) {
    return new RoutingCoordinator(userRepository, cryptoManager, trustManager, sessionManager, rekeyManager, meshRouter, connectionManager, discoveryManager);
  }
}
