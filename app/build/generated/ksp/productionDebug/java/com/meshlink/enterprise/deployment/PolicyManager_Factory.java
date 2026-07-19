package com.meshlink.enterprise.deployment;

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
public final class PolicyManager_Factory implements Factory<PolicyManager> {
  private final Provider<EnterpriseConfigurationManager> configurationManagerProvider;

  public PolicyManager_Factory(
      Provider<EnterpriseConfigurationManager> configurationManagerProvider) {
    this.configurationManagerProvider = configurationManagerProvider;
  }

  @Override
  public PolicyManager get() {
    return newInstance(configurationManagerProvider.get());
  }

  public static PolicyManager_Factory create(
      Provider<EnterpriseConfigurationManager> configurationManagerProvider) {
    return new PolicyManager_Factory(configurationManagerProvider);
  }

  public static PolicyManager newInstance(EnterpriseConfigurationManager configurationManager) {
    return new PolicyManager(configurationManager);
  }
}
