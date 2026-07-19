package com.meshlink.service;

import com.meshlink.common.diagnostics.RuntimeWatchdog;
import com.meshlink.common.power.PowerStateManager;
import com.meshlink.domain.repository.MeshRepository;
import com.meshlink.wifi.data.WifiDirectManager;
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
  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<WifiDirectManager> wifiDirectManagerProvider;

  private final Provider<PowerStateManager> powerStateManagerProvider;

  private final Provider<RuntimeWatchdog> runtimeWatchdogProvider;

  public MeshRelayService_MembersInjector(Provider<MeshRepository> meshRepositoryProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<PowerStateManager> powerStateManagerProvider,
      Provider<RuntimeWatchdog> runtimeWatchdogProvider) {
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.wifiDirectManagerProvider = wifiDirectManagerProvider;
    this.powerStateManagerProvider = powerStateManagerProvider;
    this.runtimeWatchdogProvider = runtimeWatchdogProvider;
  }

  public static MembersInjector<MeshRelayService> create(
      Provider<MeshRepository> meshRepositoryProvider,
      Provider<WifiDirectManager> wifiDirectManagerProvider,
      Provider<PowerStateManager> powerStateManagerProvider,
      Provider<RuntimeWatchdog> runtimeWatchdogProvider) {
    return new MeshRelayService_MembersInjector(meshRepositoryProvider, wifiDirectManagerProvider, powerStateManagerProvider, runtimeWatchdogProvider);
  }

  @Override
  public void injectMembers(MeshRelayService instance) {
    injectMeshRepository(instance, meshRepositoryProvider.get());
    injectWifiDirectManager(instance, wifiDirectManagerProvider.get());
    injectPowerStateManager(instance, powerStateManagerProvider.get());
    injectRuntimeWatchdog(instance, runtimeWatchdogProvider.get());
  }

  @InjectedFieldSignature("com.meshlink.service.MeshRelayService.meshRepository")
  public static void injectMeshRepository(MeshRelayService instance,
      MeshRepository meshRepository) {
    instance.meshRepository = meshRepository;
  }

  @InjectedFieldSignature("com.meshlink.service.MeshRelayService.wifiDirectManager")
  public static void injectWifiDirectManager(MeshRelayService instance,
      WifiDirectManager wifiDirectManager) {
    instance.wifiDirectManager = wifiDirectManager;
  }

  @InjectedFieldSignature("com.meshlink.service.MeshRelayService.powerStateManager")
  public static void injectPowerStateManager(MeshRelayService instance,
      PowerStateManager powerStateManager) {
    instance.powerStateManager = powerStateManager;
  }

  @InjectedFieldSignature("com.meshlink.service.MeshRelayService.runtimeWatchdog")
  public static void injectRuntimeWatchdog(MeshRelayService instance,
      RuntimeWatchdog runtimeWatchdog) {
    instance.runtimeWatchdog = runtimeWatchdog;
  }
}
