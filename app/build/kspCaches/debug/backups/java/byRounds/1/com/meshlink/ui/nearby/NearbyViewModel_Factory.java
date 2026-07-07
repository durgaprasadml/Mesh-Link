package com.meshlink.ui.nearby;

import com.meshlink.data.repository.BleRepository;
import com.meshlink.data.wifi.WifiDirectManager;
import com.meshlink.domain.repository.UserRepository;
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
public final class NearbyViewModel_Factory implements Factory<NearbyViewModel> {
  private final Provider<BleRepository> bleRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<WifiDirectManager> wifiDirectManagerProvider;

  public NearbyViewModel_Factory(Provider<BleRepository> bleRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider) {
    this.bleRepositoryProvider = bleRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.wifiDirectManagerProvider = wifiDirectManagerProvider;
  }

  @Override
  public NearbyViewModel get() {
    return newInstance(bleRepositoryProvider.get(), userRepositoryProvider.get(), wifiDirectManagerProvider.get());
  }

  public static NearbyViewModel_Factory create(Provider<BleRepository> bleRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider) {
    return new NearbyViewModel_Factory(bleRepositoryProvider, userRepositoryProvider, wifiDirectManagerProvider);
  }

  public static NearbyViewModel newInstance(BleRepository bleRepository,
      UserRepository userRepository, WifiDirectManager wifiDirectManager) {
    return new NearbyViewModel(bleRepository, userRepository, wifiDirectManager);
  }
}
