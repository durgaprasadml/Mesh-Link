package com.meshlink.di;

import com.meshlink.data.analytics.MeshAnalytics;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideMeshAnalyticsFactory implements Factory<MeshAnalytics> {
  @Override
  public MeshAnalytics get() {
    return provideMeshAnalytics();
  }

  public static AppModule_ProvideMeshAnalyticsFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MeshAnalytics provideMeshAnalytics() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMeshAnalytics());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideMeshAnalyticsFactory INSTANCE = new AppModule_ProvideMeshAnalyticsFactory();
  }
}
