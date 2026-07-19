package com.meshlink.recovery.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DatabaseContinuityManager_Factory implements Factory<DatabaseContinuityManager> {
  private final Provider<IntegrityManager> integrityManagerProvider;

  private final Provider<RecoveryManager> recoveryManagerProvider;

  private final Provider<BackupManager> backupManagerProvider;

  public DatabaseContinuityManager_Factory(Provider<IntegrityManager> integrityManagerProvider,
      Provider<RecoveryManager> recoveryManagerProvider,
      Provider<BackupManager> backupManagerProvider) {
    this.integrityManagerProvider = integrityManagerProvider;
    this.recoveryManagerProvider = recoveryManagerProvider;
    this.backupManagerProvider = backupManagerProvider;
  }

  @Override
  public DatabaseContinuityManager get() {
    return newInstance(integrityManagerProvider.get(), recoveryManagerProvider.get(), backupManagerProvider.get());
  }

  public static DatabaseContinuityManager_Factory create(
      Provider<IntegrityManager> integrityManagerProvider,
      Provider<RecoveryManager> recoveryManagerProvider,
      Provider<BackupManager> backupManagerProvider) {
    return new DatabaseContinuityManager_Factory(integrityManagerProvider, recoveryManagerProvider, backupManagerProvider);
  }

  public static DatabaseContinuityManager newInstance(IntegrityManager integrityManager,
      RecoveryManager recoveryManager, BackupManager backupManager) {
    return new DatabaseContinuityManager(integrityManager, recoveryManager, backupManager);
  }
}
