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
public final class RuntimeWatchdog_Factory implements Factory<RuntimeWatchdog> {
  private final Provider<SelfHealer> selfHealerProvider;

  public RuntimeWatchdog_Factory(Provider<SelfHealer> selfHealerProvider) {
    this.selfHealerProvider = selfHealerProvider;
  }

  @Override
  public RuntimeWatchdog get() {
    return newInstance(selfHealerProvider.get());
  }

  public static RuntimeWatchdog_Factory create(Provider<SelfHealer> selfHealerProvider) {
    return new RuntimeWatchdog_Factory(selfHealerProvider);
  }

  public static RuntimeWatchdog newInstance(SelfHealer selfHealer) {
    return new RuntimeWatchdog(selfHealer);
  }
}
