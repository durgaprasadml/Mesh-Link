package com.meshlink.ui.mesh;

import com.meshlink.data.repository.BleRepository;
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
public final class MeshDebugViewModel_Factory implements Factory<MeshDebugViewModel> {
  private final Provider<BleRepository> bleRepositoryProvider;

  public MeshDebugViewModel_Factory(Provider<BleRepository> bleRepositoryProvider) {
    this.bleRepositoryProvider = bleRepositoryProvider;
  }

  @Override
  public MeshDebugViewModel get() {
    return newInstance(bleRepositoryProvider.get());
  }

  public static MeshDebugViewModel_Factory create(Provider<BleRepository> bleRepositoryProvider) {
    return new MeshDebugViewModel_Factory(bleRepositoryProvider);
  }

  public static MeshDebugViewModel newInstance(BleRepository bleRepository) {
    return new MeshDebugViewModel(bleRepository);
  }
}
