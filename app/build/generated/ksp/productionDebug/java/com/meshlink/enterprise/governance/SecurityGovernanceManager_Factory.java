package com.meshlink.enterprise.governance;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class SecurityGovernanceManager_Factory implements Factory<SecurityGovernanceManager> {
  @Override
  public SecurityGovernanceManager get() {
    return newInstance();
  }

  public static SecurityGovernanceManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SecurityGovernanceManager newInstance() {
    return new SecurityGovernanceManager();
  }

  private static final class InstanceHolder {
    private static final SecurityGovernanceManager_Factory INSTANCE = new SecurityGovernanceManager_Factory();
  }
}
