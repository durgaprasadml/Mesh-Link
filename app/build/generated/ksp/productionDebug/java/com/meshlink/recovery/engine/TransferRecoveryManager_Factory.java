package com.meshlink.recovery.engine;

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
public final class TransferRecoveryManager_Factory implements Factory<TransferRecoveryManager> {
  @Override
  public TransferRecoveryManager get() {
    return newInstance();
  }

  public static TransferRecoveryManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TransferRecoveryManager newInstance() {
    return new TransferRecoveryManager();
  }

  private static final class InstanceHolder {
    private static final TransferRecoveryManager_Factory INSTANCE = new TransferRecoveryManager_Factory();
  }
}
