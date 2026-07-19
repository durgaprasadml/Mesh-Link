package com.meshlink.common.logger;

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
public final class EventTimeline_Factory implements Factory<EventTimeline> {
  @Override
  public EventTimeline get() {
    return newInstance();
  }

  public static EventTimeline_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EventTimeline newInstance() {
    return new EventTimeline();
  }

  private static final class InstanceHolder {
    private static final EventTimeline_Factory INSTANCE = new EventTimeline_Factory();
  }
}
