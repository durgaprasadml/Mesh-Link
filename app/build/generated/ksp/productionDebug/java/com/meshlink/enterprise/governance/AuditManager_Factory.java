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
public final class AuditManager_Factory implements Factory<AuditManager> {
  @Override
  public AuditManager get() {
    return newInstance();
  }

  public static AuditManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AuditManager newInstance() {
    return new AuditManager();
  }

  private static final class InstanceHolder {
    private static final AuditManager_Factory INSTANCE = new AuditManager_Factory();
  }
}
