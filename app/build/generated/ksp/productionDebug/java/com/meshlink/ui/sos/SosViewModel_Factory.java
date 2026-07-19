package com.meshlink.ui.sos;

import com.meshlink.data.location.LocationProvider;
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
public final class SosViewModel_Factory implements Factory<SosViewModel> {
  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<LocationProvider> locationProvider;

  public SosViewModel_Factory(Provider<MeshRepository> meshRepositoryProvider,
      Provider<LocationProvider> locationProvider) {
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.locationProvider = locationProvider;
  }

  @Override
  public SosViewModel get() {
    return newInstance(meshRepositoryProvider.get(), locationProvider.get());
  }

  public static SosViewModel_Factory create(Provider<MeshRepository> meshRepositoryProvider,
      Provider<LocationProvider> locationProvider) {
    return new SosViewModel_Factory(meshRepositoryProvider, locationProvider);
  }

  public static SosViewModel newInstance(MeshRepository meshRepository,
      LocationProvider locationProvider) {
    return new SosViewModel(meshRepository, locationProvider);
  }
}
