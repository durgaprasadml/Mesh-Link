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
public final class PrivacyManager_Factory implements Factory<PrivacyManager> {
  @Override
  public PrivacyManager get() {
    return newInstance();
  }

  public static PrivacyManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PrivacyManager newInstance() {
    return new PrivacyManager();
  }

  private static final class InstanceHolder {
    private static final PrivacyManager_Factory INSTANCE = new PrivacyManager_Factory();
  }
}
