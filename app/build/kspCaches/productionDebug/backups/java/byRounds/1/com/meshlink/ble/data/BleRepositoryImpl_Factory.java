package com.meshlink.ble.data;

import android.app.Application;
import android.content.Context;
import com.meshlink.ble.data.source.BleMeshDataSource;
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
import com.meshlink.wifi.data.WifiSocketTransport;
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
public final class BleRepositoryImpl_Factory implements Factory<BleRepositoryImpl> {
  private final Provider<Application> applicationProvider;

  private final Provider<BleMeshDataSource> bleDataSourceProvider;

  private final Provider<MeshRouter> meshRouterProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<TransferManager> transferManagerProvider;

  private final Provider<MediaTransferManager> mediaTransferManagerProvider;

  private final Provider<LocationProvider> locationProvider;

  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<WifiDirectManager> wifiDirectManagerProvider;

  private final Provider<WifiSocketTransport> wifiSocketTransportProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<RekeyManager> rekeyManagerProvider;

  private final Provider<TrustManager> trustManagerProvider;

  private final Provider<MeshSecurityMonitor> securityMonitorProvider;

  private final Provider<DiscoveryManager> discoveryManagerProvider;

  private final Provider<BleConnectionManager> connectionManagerProvider;

  private final Provider<RoutingCoordinator> routingCoordinatorProvider;

  private final Provider<MeshMessagingManager> meshMessagingManagerProvider;

  private final Provider<VoiceTransport> voiceTransportProvider;

  private final Provider<VideoTransport> videoTransportProvider;

  private final Provider<Context> contextProvider;

  public BleRepositoryImpl_Factory(Provider<Application> applicationProvider,
      Provider<BleMeshDataSource> bleDataSourceProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<ChatDao> chatDaoProvider, Provider<UserRepository> userRepositoryProvider,
      Provider<TransferManager> transferManagerProvider,
      Provider<MediaTransferManager> mediaTransferManagerProvider,
      Provider<LocationProvider> locationProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<WifiSocketTransport> wifiSocketTransportProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<RekeyManager> rekeyManagerProvider,
      Provider<TrustManager> trustManagerProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<BleConnectionManager> connectionManagerProvider,
      Provider<RoutingCoordinator> routingCoordinatorProvider,
      Provider<MeshMessagingManager> meshMessagingManagerProvider,
      Provider<VoiceTransport> voiceTransportProvider,
      Provider<VideoTransport> videoTransportProvider, Provider<Context> contextProvider) {
    this.applicationProvider = applicationProvider;
    this.bleDataSourceProvider = bleDataSourceProvider;
    this.meshRouterProvider = meshRouterProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.transferManagerProvider = transferManagerProvider;
    this.mediaTransferManagerProvider = mediaTransferManagerProvider;
    this.locationProvider = locationProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.wifiDirectManagerProvider = wifiDirectManagerProvider;
    this.wifiSocketTransportProvider = wifiSocketTransportProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.rekeyManagerProvider = rekeyManagerProvider;
    this.trustManagerProvider = trustManagerProvider;
    this.securityMonitorProvider = securityMonitorProvider;
    this.discoveryManagerProvider = discoveryManagerProvider;
    this.connectionManagerProvider = connectionManagerProvider;
    this.routingCoordinatorProvider = routingCoordinatorProvider;
    this.meshMessagingManagerProvider = meshMessagingManagerProvider;
    this.voiceTransportProvider = voiceTransportProvider;
    this.videoTransportProvider = videoTransportProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public BleRepositoryImpl get() {
    return newInstance(applicationProvider.get(), bleDataSourceProvider.get(), meshRouterProvider.get(), chatDaoProvider.get(), userRepositoryProvider.get(), transferManagerProvider.get(), mediaTransferManagerProvider.get(), locationProvider.get(), cryptoManagerProvider.get(), wifiDirectManagerProvider.get(), wifiSocketTransportProvider.get(), sessionManagerProvider.get(), rekeyManagerProvider.get(), trustManagerProvider.get(), securityMonitorProvider.get(), discoveryManagerProvider.get(), connectionManagerProvider.get(), routingCoordinatorProvider.get(), meshMessagingManagerProvider.get(), voiceTransportProvider.get(), videoTransportProvider.get(), contextProvider.get());
  }

  public static BleRepositoryImpl_Factory create(Provider<Application> applicationProvider,
      Provider<BleMeshDataSource> bleDataSourceProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<ChatDao> chatDaoProvider, Provider<UserRepository> userRepositoryProvider,
      Provider<TransferManager> transferManagerProvider,
      Provider<MediaTransferManager> mediaTransferManagerProvider,
      Provider<LocationProvider> locationProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<WifiSocketTransport> wifiSocketTransportProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<RekeyManager> rekeyManagerProvider,
      Provider<TrustManager> trustManagerProvider,
      Provider<MeshSecurityMonitor> securityMonitorProvider,
      Provider<DiscoveryManager> discoveryManagerProvider,
      Provider<BleConnectionManager> connectionManagerProvider,
      Provider<RoutingCoordinator> routingCoordinatorProvider,
      Provider<MeshMessagingManager> meshMessagingManagerProvider,
      Provider<VoiceTransport> voiceTransportProvider,
      Provider<VideoTransport> videoTransportProvider, Provider<Context> contextProvider) {
    return new BleRepositoryImpl_Factory(applicationProvider, bleDataSourceProvider, meshRouterProvider, chatDaoProvider, userRepositoryProvider, transferManagerProvider, mediaTransferManagerProvider, locationProvider, cryptoManagerProvider, wifiDirectManagerProvider, wifiSocketTransportProvider, sessionManagerProvider, rekeyManagerProvider, trustManagerProvider, securityMonitorProvider, discoveryManagerProvider, connectionManagerProvider, routingCoordinatorProvider, meshMessagingManagerProvider, voiceTransportProvider, videoTransportProvider, contextProvider);
  }

  public static BleRepositoryImpl newInstance(Application application,
      BleMeshDataSource bleDataSource, MeshRouter meshRouter, ChatDao chatDao,
      UserRepository userRepository, TransferManager transferManager,
      MediaTransferManager mediaTransferManager, LocationProvider locationProvider,
      MeshCryptoManager cryptoManager, WifiDirectManager wifiDirectManager,
      WifiSocketTransport wifiSocketTransport, SessionManager sessionManager,
      RekeyManager rekeyManager, TrustManager trustManager, MeshSecurityMonitor securityMonitor,
      DiscoveryManager discoveryManager, BleConnectionManager connectionManager,
      RoutingCoordinator routingCoordinator, MeshMessagingManager meshMessagingManager,
      VoiceTransport voiceTransport, VideoTransport videoTransport, Context context) {
    return new BleRepositoryImpl(application, bleDataSource, meshRouter, chatDao, userRepository, transferManager, mediaTransferManager, locationProvider, cryptoManager, wifiDirectManager, wifiSocketTransport, sessionManager, rekeyManager, trustManager, securityMonitor, discoveryManager, connectionManager, routingCoordinator, meshMessagingManager, voiceTransport, videoTransport, context);
  }
}
