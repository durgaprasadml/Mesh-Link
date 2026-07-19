package com.meshlink.service.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CleanupWorker_AssistedFactory_Impl implements CleanupWorker_AssistedFactory {
  private final CleanupWorker_Factory delegateFactory;

  CleanupWorker_AssistedFactory_Impl(CleanupWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public CleanupWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<CleanupWorker_AssistedFactory> create(
      CleanupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new CleanupWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<CleanupWorker_AssistedFactory> createFactoryProvider(
      CleanupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new CleanupWorker_AssistedFactory_Impl(delegateFactory));
  }
}
