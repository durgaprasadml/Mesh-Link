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
public final class RetryWorker_AssistedFactory_Impl implements RetryWorker_AssistedFactory {
  private final RetryWorker_Factory delegateFactory;

  RetryWorker_AssistedFactory_Impl(RetryWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public RetryWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<RetryWorker_AssistedFactory> create(RetryWorker_Factory delegateFactory) {
    return InstanceFactory.create(new RetryWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<RetryWorker_AssistedFactory> createFactoryProvider(
      RetryWorker_Factory delegateFactory) {
    return InstanceFactory.create(new RetryWorker_AssistedFactory_Impl(delegateFactory));
  }
}
