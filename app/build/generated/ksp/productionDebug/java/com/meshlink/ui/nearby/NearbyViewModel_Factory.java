package com.meshlink.ui.nearby;

import com.meshlink.domain.repository.MeshRepository;
import com.meshlink.domain.repository.UserRepository;
import com.meshlink.wifi.data.WifiDirectManager;
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
  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<WifiDirectManager> wifiDirectManagerProvider;

  public NearbyViewModel_Factory(Provider<MeshRepository> meshRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider) {
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.wifiDirectManagerProvider = wifiDirectManagerProvider;
  }

  @Override
  public NearbyViewModel get() {
    return newInstance(meshRepositoryProvider.get(), userRepositoryProvider.get(), wifiDirectManagerProvider.get());
  }

  public static NearbyViewModel_Factory create(Provider<MeshRepository> meshRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider) {
    return new NearbyViewModel_Factory(meshRepositoryProvider, userRepositoryProvider, wifiDirectManagerProvider);
  }

  public static NearbyViewModel newInstance(MeshRepository meshRepository,
      UserRepository userRepository, WifiDirectManager wifiDirectManager) {
    return new NearbyViewModel(meshRepository, userRepository, wifiDirectManager);
  }
}
