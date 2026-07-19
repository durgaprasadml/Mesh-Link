package com.meshlink.common.logger;

import android.content.Context;
import com.meshlink.common.diagnostics.DiagnosticsManager;
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
public final class MeshCrashReporter_Factory implements Factory<MeshCrashReporter> {
  private final Provider<Context> contextProvider;

  private final Provider<DiagnosticsManager> diagnosticsManagerProvider;

  public MeshCrashReporter_Factory(Provider<Context> contextProvider,
      Provider<DiagnosticsManager> diagnosticsManagerProvider) {
    this.contextProvider = contextProvider;
    this.diagnosticsManagerProvider = diagnosticsManagerProvider;
  }

  @Override
  public MeshCrashReporter get() {
    return newInstance(contextProvider.get(), diagnosticsManagerProvider.get());
  }

  public static MeshCrashReporter_Factory create(Provider<Context> contextProvider,
      Provider<DiagnosticsManager> diagnosticsManagerProvider) {
    return new MeshCrashReporter_Factory(contextProvider, diagnosticsManagerProvider);
  }

  public static MeshCrashReporter newInstance(Context context,
      DiagnosticsManager diagnosticsManager) {
    return new MeshCrashReporter(context, diagnosticsManager);
  }
}
