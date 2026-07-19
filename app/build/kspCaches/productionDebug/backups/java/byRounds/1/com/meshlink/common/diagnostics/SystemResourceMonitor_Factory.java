package com.meshlink.common.diagnostics;

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
public final class SystemResourceMonitor_Factory implements Factory<SystemResourceMonitor> {
  private final Provider<Context> contextProvider;

  public SystemResourceMonitor_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SystemResourceMonitor get() {
    return newInstance(contextProvider.get());
  }

  public static SystemResourceMonitor_Factory create(Provider<Context> contextProvider) {
    return new SystemResourceMonitor_Factory(contextProvider);
  }

  public static SystemResourceMonitor newInstance(Context context) {
    return new SystemResourceMonitor(context);
  }
}
