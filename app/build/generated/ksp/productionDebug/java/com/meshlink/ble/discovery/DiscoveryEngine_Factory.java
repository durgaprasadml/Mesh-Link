package com.meshlink.ble.discovery;

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
public final class DiscoveryEngine_Factory implements Factory<DiscoveryEngine> {
  private final Provider<BatteryAwareScanner> batteryAwareScannerProvider;

  public DiscoveryEngine_Factory(Provider<BatteryAwareScanner> batteryAwareScannerProvider) {
    this.batteryAwareScannerProvider = batteryAwareScannerProvider;
  }

  @Override
  public DiscoveryEngine get() {
    return newInstance(batteryAwareScannerProvider.get());
  }

  public static DiscoveryEngine_Factory create(
      Provider<BatteryAwareScanner> batteryAwareScannerProvider) {
    return new DiscoveryEngine_Factory(batteryAwareScannerProvider);
  }

  public static DiscoveryEngine newInstance(BatteryAwareScanner batteryAwareScanner) {
    return new DiscoveryEngine(batteryAwareScanner);
  }
}
