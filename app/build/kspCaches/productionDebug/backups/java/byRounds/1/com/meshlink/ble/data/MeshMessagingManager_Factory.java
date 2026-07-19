package com.meshlink.ble.data;

import android.content.Context;
import com.meshlink.data.location.LocationProvider;
import com.meshlink.database.data.local.ChatDao;
import com.meshlink.domain.repository.UserRepository;
import com.meshlink.media.data.MediaTransferManager;
import com.meshlink.routing.data.MeshRouter;
import com.meshlink.security.data.MeshCryptoManager;
import com.meshlink.security.data.MeshSecurityMonitor;
import com.meshlink.security.data.RekeyManager;
import com.meshlink.security.data.SessionManager;
import com.meshlink.security.data.TrustManager;
import com.meshlink.transfer.TransferManager;
import com.meshlink.video.transport.VideoTransport;
import com.meshlink.voice.transport.VoiceTransport;
import com.meshlink.wifi.data.WifiDirectManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class MeshMessagingManager_Factory implements Factory<MeshMessagingManager> {
  private final Provider<Context> contextProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<MeshRouter> meshRouterProvider;

  private final Provider<TransferManager> transferManagerProvider;

  private final Provider<MediaTransferManager> mediaTransferManagerProvider;

  private final Provider<WifiDirectManager> wifiDirectManagerProvider;

  private final Provider<MeshSecurityMonitor> securityMonitorProvider;

  private final Provider<LocationProvider> locationProvider;

  private final Provider<RoutingCoordinator> routingCoordinatorProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<TrustManager> trustManagerProvider;

  private final Provider<RekeyManager> rekeyManagerProvider;

  private final Provider<VoiceTransport> voiceTransportProvider;

  private final Provider<VideoTransport> videoTransportProvider;

  private final Provider<BleConnectionManager> connectionManagerProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  public MeshMessagingManager_Factory(Provider<Context> contextProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<ChatDao> chatDaoProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<TransferManager> transferManagerProvider,
      Provider<MediaTransferManager> mediaTransferManagerProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<LocationProvider> locationProvider,
      Provider<RoutingCoordinator> routingCoordinatorProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<TrustManager> trustManagerProvider,
      Provider<RekeyManager> rekeyManagerProvider, Provider<VoiceTransport> voiceTransportProvider,
      Provider<VideoTransport> videoTransportProvider,
      Provider<BleConnectionManager> connectionManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider) {
    this.contextProvider = contextProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.meshRouterProvider = meshRouterProvider;
    this.transferManagerProvider = transferManagerProvider;
    this.mediaTransferManagerProvider = mediaTransferManagerProvider;
    this.wifiDirectManagerProvider = wifiDirectManagerProvider;
    this.securityMonitorProvider = securityMonitorProvider;
    this.locationProvider = locationProvider;
    this.routingCoordinatorProvider = routingCoordinatorProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.trustManagerProvider = trustManagerProvider;
    this.rekeyManagerProvider = rekeyManagerProvider;
    this.voiceTransportProvider = voiceTransportProvider;
    this.videoTransportProvider = videoTransportProvider;
    this.connectionManagerProvider = connectionManagerProvider;
    this.discoveryManagerProvider = discoveryManagerProvider;
  }

  @Override
  public MeshMessagingManager get() {
    return newInstance(contextProvider.get(), userRepositoryProvider.get(), chatDaoProvider.get(), cryptoManagerProvider.get(), meshRouterProvider.get(), transferManagerProvider.get(), mediaTransferManagerProvider.get(), wifiDirectManagerProvider.get(), securityMonitorProvider.get(), locationProvider.get(), routingCoordinatorProvider.get(), sessionManagerProvider.get(), trustManagerProvider.get(), rekeyManagerProvider.get(), voiceTransportProvider.get(), videoTransportProvider.get(), connectionManagerProvider.get(), discoveryManagerProvider.get());
  }

  public static MeshMessagingManager_Factory create(Provider<Context> contextProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<ChatDao> chatDaoProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<TransferManager> transferManagerProvider,
      Provider<MediaTransferManager> mediaTransferManagerProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<LocationProvider> locationProvider,
      Provider<RoutingCoordinator> routingCoordinatorProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<TrustManager> trustManagerProvider,
      Provider<RekeyManager> rekeyManagerProvider, Provider<VoiceTransport> voiceTransportProvider,
      Provider<VideoTransport> videoTransportProvider,
      Provider<BleConnectionManager> connectionManagerProvider,
      Provider<DiscoveryManager> discoveryManagerProvider) {
    return new MeshMessagingManager_Factory(contextProvider, userRepositoryProvider, chatDaoProvider, cryptoManagerProvider, meshRouterProvider, transferManagerProvider, mediaTransferManagerProvider, wifiDirectManagerProvider, securityMonitorProvider, locationProvider, routingCoordinatorProvider, sessionManagerProvider, trustManagerProvider, rekeyManagerProvider, voiceTransportProvider, videoTransportProvider, connectionManagerProvider, discoveryManagerProvider);
  }

  public static MeshMessagingManager newInstance(Context context, UserRepository userRepository,
      ChatDao chatDao, MeshCryptoManager cryptoManager, MeshRouter meshRouter,
      TransferManager transferManager, MediaTransferManager mediaTransferManager,
      WifiDirectManager wifiDirectManager, MeshSecurityMonitor securityMonitor,
      LocationProvider locationProvider, RoutingCoordinator routingCoordinator,
      SessionManager sessionManager, TrustManager trustManager, RekeyManager rekeyManager,
      VoiceTransport voiceTransport, VideoTransport videoTransport,
      BleConnectionManager connectionManager, DiscoveryManager discoveryManager) {
    return new MeshMessagingManager(context, userRepository, chatDao, cryptoManager, meshRouter, transferManager, mediaTransferManager, wifiDirectManager, securityMonitor, locationProvider, routingCoordinator, sessionManager, trustManager, rekeyManager, voiceTransport, videoTransport, connectionManager, discoveryManager);
  }
}
