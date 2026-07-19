package com.meshlink.emergency.rescue;

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
public final class TeamTracker_Factory implements Factory<TeamTracker> {
  @Override
  public TeamTracker get() {
    return newInstance();
  }

  public static TeamTracker_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TeamTracker newInstance() {
    return new TeamTracker();
  }

  private static final class InstanceHolder {
    private static final TeamTracker_Factory INSTANCE = new TeamTracker_Factory();
  }
}
