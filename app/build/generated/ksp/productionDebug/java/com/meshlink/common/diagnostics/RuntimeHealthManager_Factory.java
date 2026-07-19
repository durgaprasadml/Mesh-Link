package com.meshlink.common.diagnostics;

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
public final class RuntimeHealthManager_Factory implements Factory<RuntimeHealthManager> {
  private final Provider<DiagnosticsManager> diagnosticsManagerProvider;

  private final Provider<SystemResourceMonitor> resourceMonitorProvider;

  public RuntimeHealthManager_Factory(Provider<DiagnosticsManager> diagnosticsManagerProvider,
      Provider<SystemResourceMonitor> resourceMonitorProvider) {
    this.diagnosticsManagerProvider = diagnosticsManagerProvider;
    this.resourceMonitorProvider = resourceMonitorProvider;
  }

  @Override
  public RuntimeHealthManager get() {
    return newInstance(diagnosticsManagerProvider.get(), resourceMonitorProvider.get());
  }

  public static RuntimeHealthManager_Factory create(
      Provider<DiagnosticsManager> diagnosticsManagerProvider,
      Provider<SystemResourceMonitor> resourceMonitorProvider) {
    return new RuntimeHealthManager_Factory(diagnosticsManagerProvider, resourceMonitorProvider);
  }

  public static RuntimeHealthManager newInstance(DiagnosticsManager diagnosticsManager,
      SystemResourceMonitor resourceMonitor) {
    return new RuntimeHealthManager(diagnosticsManager, resourceMonitor);
  }
}
