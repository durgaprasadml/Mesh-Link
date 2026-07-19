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
public final class BackupManager_Factory implements Factory<BackupManager> {
  private final Provider<Context> contextProvider;

  private final Provider<IntegrityManager> integrityManagerProvider;

  public BackupManager_Factory(Provider<Context> contextProvider,
      Provider<IntegrityManager> integrityManagerProvider) {
    this.contextProvider = contextProvider;
    this.integrityManagerProvider = integrityManagerProvider;
  }

  @Override
  public BackupManager get() {
    return newInstance(contextProvider.get(), integrityManagerProvider.get());
  }

  public static BackupManager_Factory create(Provider<Context> contextProvider,
      Provider<IntegrityManager> integrityManagerProvider) {
    return new BackupManager_Factory(contextProvider, integrityManagerProvider);
  }

  public static BackupManager newInstance(Context context, IntegrityManager integrityManager) {
    return new BackupManager(context, integrityManager);
  }
}
