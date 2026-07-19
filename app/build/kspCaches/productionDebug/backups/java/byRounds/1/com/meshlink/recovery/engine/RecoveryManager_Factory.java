package com.meshlink.recovery.engine;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class RecoveryManager_Factory implements Factory<RecoveryManager> {
  private final Provider<Context> contextProvider;

  private final Provider<BackupManager> backupManagerProvider;

  private final Provider<IntegrityManager> integrityManagerProvider;

  public RecoveryManager_Factory(Provider<Context> contextProvider,
      Provider<BackupManager> backupManagerProvider,
      Provider<IntegrityManager> integrityManagerProvider) {
    this.contextProvider = contextProvider;
    this.backupManagerProvider = backupManagerProvider;
    this.integrityManagerProvider = integrityManagerProvider;
  }

  @Override
  public RecoveryManager get() {
    return newInstance(contextProvider.get(), backupManagerProvider.get(), integrityManagerProvider.get());
  }

  public static RecoveryManager_Factory create(Provider<Context> contextProvider,
      Provider<BackupManager> backupManagerProvider,
      Provider<IntegrityManager> integrityManagerProvider) {
    return new RecoveryManager_Factory(contextProvider, backupManagerProvider, integrityManagerProvider);
  }

  public static RecoveryManager newInstance(Context context, BackupManager backupManager,
      IntegrityManager integrityManager) {
    return new RecoveryManager(context, backupManager, integrityManager);
  }
}
