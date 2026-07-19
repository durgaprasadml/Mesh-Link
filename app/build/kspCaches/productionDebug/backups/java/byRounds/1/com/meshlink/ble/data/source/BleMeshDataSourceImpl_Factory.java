package com.meshlink.ble.data.source;

import com.meshlink.ble.data.BleAdvertiserManager;
import com.meshlink.ble.data.BleGattManager;
import com.meshlink.ble.data.BleScannerManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class BleMeshDataSourceImpl_Factory implements Factory<BleMeshDataSourceImpl> {
  private final Provider<BleScannerManager> scannerProvider;

  private final Provider<BleAdvertiserManager> advertiserProvider;

  private final Provider<BleGattManager> gattManagerProvider;

  public BleMeshDataSourceImpl_Factory(Provider<BleScannerManager> scannerProvider,
      Provider<BleAdvertiserManager> advertiserProvider,
      Provider<BleGattManager> gattManagerProvider) {
    this.scannerProvider = scannerProvider;
    this.advertiserProvider = advertiserProvider;
    this.gattManagerProvider = gattManagerProvider;
  }

  @Override
  public BleMeshDataSourceImpl get() {
    return newInstance(scannerProvider.get(), advertiserProvider.get(), gattManagerProvider.get());
  }

  public static BleMeshDataSourceImpl_Factory create(Provider<BleScannerManager> scannerProvider,
      Provider<BleAdvertiserManager> advertiserProvider,
      Provider<BleGattManager> gattManagerProvider) {
    return new BleMeshDataSourceImpl_Factory(scannerProvider, advertiserProvider, gattManagerProvider);
  }

  public static BleMeshDataSourceImpl newInstance(BleScannerManager scanner,
      BleAdvertiserManager advertiser, BleGattManager gattManager) {
    return new BleMeshDataSourceImpl(scanner, advertiser, gattManager);
  }
}
