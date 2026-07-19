package com.meshlink.routing.engine;

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
public final class QoSManager_Factory implements Factory<QoSManager> {
  @Override
  public QoSManager get() {
    return newInstance();
  }

  public static QoSManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static QoSManager newInstance() {
    return new QoSManager();
  }

  private static final class InstanceHolder {
    private static final QoSManager_Factory INSTANCE = new QoSManager_Factory();
  }
}
