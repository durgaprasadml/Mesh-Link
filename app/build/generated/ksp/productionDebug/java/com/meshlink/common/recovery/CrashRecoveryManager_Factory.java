package com.meshlink.common.recovery;

import android.content.Context;
import com.meshlink.domain.repository.MeshRepository;
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
public final class CrashRecoveryManager_Factory implements Factory<CrashRecoveryManager> {
  private final Provider<Context> contextProvider;

  private final Provider<StateRestorationManager> stateRestorationManagerProvider;

  private final Provider<MeshRepository> meshRepositoryProvider;

  public CrashRecoveryManager_Factory(Provider<Context> contextProvider,
      Provider<StateRestorationManager> stateRestorationManagerProvider,
      Provider<MeshRepository> meshRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.stateRestorationManagerProvider = stateRestorationManagerProvider;
    this.meshRepositoryProvider = meshRepositoryProvider;
  }

  @Override
  public CrashRecoveryManager get() {
    return newInstance(contextProvider.get(), stateRestorationManagerProvider.get(), meshRepositoryProvider.get());
  }

  public static CrashRecoveryManager_Factory create(Provider<Context> contextProvider,
      Provider<StateRestorationManager> stateRestorationManagerProvider,
      Provider<MeshRepository> meshRepositoryProvider) {
    return new CrashRecoveryManager_Factory(contextProvider, stateRestorationManagerProvider, meshRepositoryProvider);
  }

  public static CrashRecoveryManager newInstance(Context context,
      StateRestorationManager stateRestorationManager, MeshRepository meshRepository) {
    return new CrashRecoveryManager(context, stateRestorationManager, meshRepository);
  }
}
