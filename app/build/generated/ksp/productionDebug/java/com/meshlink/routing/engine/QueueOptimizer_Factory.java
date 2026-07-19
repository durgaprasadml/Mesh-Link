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
public final class QueueOptimizer_Factory implements Factory<QueueOptimizer> {
  @Override
  public QueueOptimizer get() {
    return newInstance();
  }

  public static QueueOptimizer_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static QueueOptimizer newInstance() {
    return new QueueOptimizer();
  }

  private static final class InstanceHolder {
    private static final QueueOptimizer_Factory INSTANCE = new QueueOptimizer_Factory();
  }
}
