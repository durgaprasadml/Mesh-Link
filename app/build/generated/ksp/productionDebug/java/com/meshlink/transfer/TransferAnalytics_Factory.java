package com.meshlink.transfer;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TransferAnalytics_Factory implements Factory<TransferAnalytics> {
  @Override
  public TransferAnalytics get() {
    return newInstance();
  }

  public static TransferAnalytics_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TransferAnalytics newInstance() {
    return new TransferAnalytics();
  }

  private static final class InstanceHolder {
    private static final TransferAnalytics_Factory INSTANCE = new TransferAnalytics_Factory();
  }
}
