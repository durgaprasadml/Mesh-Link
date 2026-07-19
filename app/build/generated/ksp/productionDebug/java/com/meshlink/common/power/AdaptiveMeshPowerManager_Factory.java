package com.meshlink.common.power;

import com.meshlink.domain.repository.MeshRepository;
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
public final class AdaptiveMeshPowerManager_Factory implements Factory<AdaptiveMeshPowerManager> {
  private final Provider<PowerStateManager> powerStateManagerProvider;

  private final Provider<MeshRepository> meshRepositoryProvider;

  public AdaptiveMeshPowerManager_Factory(Provider<PowerStateManager> powerStateManagerProvider,
      Provider<MeshRepository> meshRepositoryProvider) {
    this.powerStateManagerProvider = powerStateManagerProvider;
    this.meshRepositoryProvider = meshRepositoryProvider;
  }

  @Override
  public AdaptiveMeshPowerManager get() {
    return newInstance(powerStateManagerProvider.get(), meshRepositoryProvider.get());
  }

  public static AdaptiveMeshPowerManager_Factory create(
      Provider<PowerStateManager> powerStateManagerProvider,
      Provider<MeshRepository> meshRepositoryProvider) {
    return new AdaptiveMeshPowerManager_Factory(powerStateManagerProvider, meshRepositoryProvider);
  }

  public static AdaptiveMeshPowerManager newInstance(PowerStateManager powerStateManager,
      MeshRepository meshRepository) {
    return new AdaptiveMeshPowerManager(powerStateManager, meshRepository);
  }
}
