package com.meshlink.data.ble;

import com.meshlink.data.analytics.MeshAnalytics;
import com.meshlink.data.local.RelayDao;
import com.meshlink.data.wifi.WifiDirectManager;
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
public final class MeshRouter_Factory implements Factory<MeshRouter> {
  private final Provider<BleGattManager> gattManagerProvider;

  private final Provider<WifiDirectManager> wifiDirectManagerProvider;

  private final Provider<MeshAnalytics> analyticsProvider;

  private final Provider<RelayDao> relayDaoProvider;

  public MeshRouter_Factory(Provider<BleGattManager> gattManagerProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<MeshAnalytics> analyticsProvider, Provider<RelayDao> relayDaoProvider) {
    this.gattManagerProvider = gattManagerProvider;
    this.wifiDirectManagerProvider = wifiDirectManagerProvider;
    this.analyticsProvider = analyticsProvider;
    this.relayDaoProvider = relayDaoProvider;
  }

  @Override
  public MeshRouter get() {
    return newInstance(gattManagerProvider.get(), wifiDirectManagerProvider.get(), analyticsProvider.get(), relayDaoProvider.get());
  }

  public static MeshRouter_Factory create(Provider<BleGattManager> gattManagerProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<MeshAnalytics> analyticsProvider, Provider<RelayDao> relayDaoProvider) {
    return new MeshRouter_Factory(gattManagerProvider, wifiDirectManagerProvider, analyticsProvider, relayDaoProvider);
  }

  public static MeshRouter newInstance(BleGattManager gattManager,
      WifiDirectManager wifiDirectManager, MeshAnalytics analytics, RelayDao relayDao) {
    return new MeshRouter(gattManager, wifiDirectManager, analytics, relayDao);
  }
}
