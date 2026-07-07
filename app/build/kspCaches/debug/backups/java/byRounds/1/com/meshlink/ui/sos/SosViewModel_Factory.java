package com.meshlink.ui.sos;

import com.meshlink.data.location.LocationProvider;
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
public final class SosViewModel_Factory implements Factory<SosViewModel> {
  private final Provider<BleRepository> bleRepositoryProvider;

  private final Provider<LocationProvider> locationProvider;

  public SosViewModel_Factory(Provider<BleRepository> bleRepositoryProvider,
      Provider<LocationProvider> locationProvider) {
    this.bleRepositoryProvider = bleRepositoryProvider;
    this.locationProvider = locationProvider;
  }

  @Override
  public SosViewModel get() {
    return newInstance(bleRepositoryProvider.get(), locationProvider.get());
  }

  public static SosViewModel_Factory create(Provider<BleRepository> bleRepositoryProvider,
      Provider<LocationProvider> locationProvider) {
    return new SosViewModel_Factory(bleRepositoryProvider, locationProvider);
  }

  public static SosViewModel newInstance(BleRepository bleRepository,
      LocationProvider locationProvider) {
    return new SosViewModel(bleRepository, locationProvider);
  }
}
