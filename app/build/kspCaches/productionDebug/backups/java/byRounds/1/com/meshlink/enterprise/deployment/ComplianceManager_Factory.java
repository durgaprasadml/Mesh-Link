package com.meshlink.enterprise.deployment;

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
public final class ComplianceManager_Factory implements Factory<ComplianceManager> {
  private final Provider<Context> contextProvider;

  public ComplianceManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ComplianceManager get() {
    return newInstance(contextProvider.get());
  }

  public static ComplianceManager_Factory create(Provider<Context> contextProvider) {
    return new ComplianceManager_Factory(contextProvider);
  }

  public static ComplianceManager newInstance(Context context) {
    return new ComplianceManager(context);
  }
}
