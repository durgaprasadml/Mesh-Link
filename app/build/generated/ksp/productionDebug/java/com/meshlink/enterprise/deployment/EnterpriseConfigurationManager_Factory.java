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
public final class EnterpriseConfigurationManager_Factory implements Factory<EnterpriseConfigurationManager> {
  private final Provider<Context> contextProvider;

  public EnterpriseConfigurationManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public EnterpriseConfigurationManager get() {
    return newInstance(contextProvider.get());
  }

  public static EnterpriseConfigurationManager_Factory create(Provider<Context> contextProvider) {
    return new EnterpriseConfigurationManager_Factory(contextProvider);
  }

  public static EnterpriseConfigurationManager newInstance(Context context) {
    return new EnterpriseConfigurationManager(context);
  }
}
