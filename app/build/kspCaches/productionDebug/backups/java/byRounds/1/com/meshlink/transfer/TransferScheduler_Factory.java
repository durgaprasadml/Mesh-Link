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
public final class TransferScheduler_Factory implements Factory<TransferScheduler> {
  @Override
  public TransferScheduler get() {
    return newInstance();
  }

  public static TransferScheduler_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TransferScheduler newInstance() {
    return new TransferScheduler();
  }

  private static final class InstanceHolder {
    private static final TransferScheduler_Factory INSTANCE = new TransferScheduler_Factory();
  }
}
