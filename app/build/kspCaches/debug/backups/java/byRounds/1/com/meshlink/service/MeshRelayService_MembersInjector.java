package com.meshlink.service;

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
public final class MeshRelayService_MembersInjector implements MembersInjector<MeshRelayService> {
  private final Provider<BleRepository> bleRepositoryProvider;

  public MeshRelayService_MembersInjector(Provider<BleRepository> bleRepositoryProvider) {
    this.bleRepositoryProvider = bleRepositoryProvider;
  }

  public static MembersInjector<MeshRelayService> create(
      Provider<BleRepository> bleRepositoryProvider) {
    return new MeshRelayService_MembersInjector(bleRepositoryProvider);
  }

  @Override
  public void injectMembers(MeshRelayService instance) {
    injectBleRepository(instance, bleRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.meshlink.service.MeshRelayService.bleRepository")
  public static void injectBleRepository(MeshRelayService instance, BleRepository bleRepository) {
    instance.bleRepository = bleRepository;
  }
}
