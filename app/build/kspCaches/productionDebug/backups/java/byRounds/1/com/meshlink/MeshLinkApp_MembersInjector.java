package com.meshlink;

import androidx.hilt.work.HiltWorkerFactory;
import com.meshlink.common.power.AdaptiveMeshPowerManager;
import com.meshlink.service.work.BackgroundTaskScheduler;
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
public final class MeshLinkApp_MembersInjector implements MembersInjector<MeshLinkApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  private final Provider<BackgroundTaskScheduler> backgroundTaskSchedulerProvider;

  private final Provider<AdaptiveMeshPowerManager> adaptivePowerManagerProvider;

  public MeshLinkApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<BackgroundTaskScheduler> backgroundTaskSchedulerProvider,
      Provider<AdaptiveMeshPowerManager> adaptivePowerManagerProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
    this.backgroundTaskSchedulerProvider = backgroundTaskSchedulerProvider;
    this.adaptivePowerManagerProvider = adaptivePowerManagerProvider;
  }

  public static MembersInjector<MeshLinkApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<BackgroundTaskScheduler> backgroundTaskSchedulerProvider,
      Provider<AdaptiveMeshPowerManager> adaptivePowerManagerProvider) {
    return new MeshLinkApp_MembersInjector(workerFactoryProvider, backgroundTaskSchedulerProvider, adaptivePowerManagerProvider);
  }

  @Override
  public void injectMembers(MeshLinkApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
    injectBackgroundTaskScheduler(instance, backgroundTaskSchedulerProvider.get());
    injectAdaptivePowerManager(instance, adaptivePowerManagerProvider.get());
  }

  @InjectedFieldSignature("com.meshlink.MeshLinkApp.workerFactory")
  public static void injectWorkerFactory(MeshLinkApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }

  @InjectedFieldSignature("com.meshlink.MeshLinkApp.backgroundTaskScheduler")
  public static void injectBackgroundTaskScheduler(MeshLinkApp instance,
      BackgroundTaskScheduler backgroundTaskScheduler) {
    instance.backgroundTaskScheduler = backgroundTaskScheduler;
  }

  @InjectedFieldSignature("com.meshlink.MeshLinkApp.adaptivePowerManager")
  public static void injectAdaptivePowerManager(MeshLinkApp instance,
      AdaptiveMeshPowerManager adaptivePowerManager) {
    instance.adaptivePowerManager = adaptivePowerManager;
  }
}
