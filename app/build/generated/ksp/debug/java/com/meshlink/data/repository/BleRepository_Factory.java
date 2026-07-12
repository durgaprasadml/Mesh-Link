package com.meshlink.data.repository;

import android.content.Context;
import com.meshlink.data.ble.BleAdvertiserManager;
import com.meshlink.data.ble.BleGattManager;
import com.meshlink.data.ble.BleScannerManager;
import com.meshlink.data.ble.MeshRouter;
import com.meshlink.data.crypto.MeshCryptoManager;
import com.meshlink.data.local.ChatDao;
import com.meshlink.data.location.LocationProvider;
import com.meshlink.data.media.MediaTransferManager;
import com.meshlink.domain.repository.UserRepository;
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
public final class BleRepository_Factory implements Factory<BleRepository> {
  private final Provider<BleScannerManager> scannerProvider;

  private final Provider<BleAdvertiserManager> advertiserProvider;

  private final Provider<BleGattManager> gattManagerProvider;

  private final Provider<MeshRouter> meshRouterProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<MediaTransferManager> mediaTransferManagerProvider;

  private final Provider<LocationProvider> locationProvider;

  private final Provider<MeshCryptoManager> cryptoManagerProvider;

  private final Provider<Context> contextProvider;

  public BleRepository_Factory(Provider<BleScannerManager> scannerProvider,
      Provider<BleAdvertiserManager> advertiserProvider,
      Provider<BleGattManager> gattManagerProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<ChatDao> chatDaoProvider, Provider<UserRepository> userRepositoryProvider,
      Provider<MediaTransferManager> mediaTransferManagerProvider,
      Provider<LocationProvider> locationProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider, Provider<Context> contextProvider) {
    this.scannerProvider = scannerProvider;
    this.advertiserProvider = advertiserProvider;
    this.gattManagerProvider = gattManagerProvider;
    this.meshRouterProvider = meshRouterProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.mediaTransferManagerProvider = mediaTransferManagerProvider;
    this.locationProvider = locationProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public BleRepository get() {
    return newInstance(scannerProvider.get(), advertiserProvider.get(), gattManagerProvider.get(), meshRouterProvider.get(), chatDaoProvider.get(), userRepositoryProvider.get(), mediaTransferManagerProvider.get(), locationProvider.get(), cryptoManagerProvider.get(), contextProvider.get());
  }

  public static BleRepository_Factory create(Provider<BleScannerManager> scannerProvider,
      Provider<BleAdvertiserManager> advertiserProvider,
      Provider<BleGattManager> gattManagerProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<ChatDao> chatDaoProvider, Provider<UserRepository> userRepositoryProvider,
      Provider<MediaTransferManager> mediaTransferManagerProvider,
      Provider<LocationProvider> locationProvider,
      Provider<MeshCryptoManager> cryptoManagerProvider, Provider<Context> contextProvider) {
    return new BleRepository_Factory(scannerProvider, advertiserProvider, gattManagerProvider, meshRouterProvider, chatDaoProvider, userRepositoryProvider, mediaTransferManagerProvider, locationProvider, cryptoManagerProvider, contextProvider);
  }

  public static BleRepository newInstance(BleScannerManager scanner,
      BleAdvertiserManager advertiser, BleGattManager gattManager, MeshRouter meshRouter,
      ChatDao chatDao, UserRepository userRepository, MediaTransferManager mediaTransferManager,
      LocationProvider locationProvider, MeshCryptoManager cryptoManager, Context context) {
    return new BleRepository(scanner, advertiser, gattManager, meshRouter, chatDao, userRepository, mediaTransferManager, locationProvider, cryptoManager, context);
  }
}
