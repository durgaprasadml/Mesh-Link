package com.meshlink;

import com.meshlink.data.repository.BleRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<BleRepository> bleRepositoryProvider;

  public MainActivity_MembersInjector(Provider<BleRepository> bleRepositoryProvider) {
    this.bleRepositoryProvider = bleRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<BleRepository> bleRepositoryProvider) {
    return new MainActivity_MembersInjector(bleRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectBleRepository(instance, bleRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.meshlink.MainActivity.bleRepository")
  public static void injectBleRepository(MainActivity instance, BleRepository bleRepository) {
    instance.bleRepository = bleRepository;
  }
}
