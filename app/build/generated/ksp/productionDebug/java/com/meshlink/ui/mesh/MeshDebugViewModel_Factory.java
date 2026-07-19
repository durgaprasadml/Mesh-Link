package com.meshlink.ui.mesh;

import com.meshlink.domain.repository.MeshRepository;
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
  private final Provider<MeshRepository> meshRepositoryProvider;

  public MeshDebugViewModel_Factory(Provider<MeshRepository> meshRepositoryProvider) {
    this.meshRepositoryProvider = meshRepositoryProvider;
  }

  @Override
  public MeshDebugViewModel get() {
    return newInstance(meshRepositoryProvider.get());
  }

  public static MeshDebugViewModel_Factory create(Provider<MeshRepository> meshRepositoryProvider) {
    return new MeshDebugViewModel_Factory(meshRepositoryProvider);
  }

  public static MeshDebugViewModel newInstance(MeshRepository meshRepository) {
    return new MeshDebugViewModel(meshRepository);
  }
}
