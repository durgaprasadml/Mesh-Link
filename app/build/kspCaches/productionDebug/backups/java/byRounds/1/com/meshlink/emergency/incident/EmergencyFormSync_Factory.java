package com.meshlink.emergency.incident;

import com.meshlink.routing.data.MeshRouter;
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
public final class EmergencyFormSync_Factory implements Factory<EmergencyFormSync> {
  private final Provider<MeshRouter> meshRouterProvider;

  public EmergencyFormSync_Factory(Provider<MeshRouter> meshRouterProvider) {
    this.meshRouterProvider = meshRouterProvider;
  }

  @Override
  public EmergencyFormSync get() {
    return newInstance(meshRouterProvider.get());
  }

  public static EmergencyFormSync_Factory create(Provider<MeshRouter> meshRouterProvider) {
    return new EmergencyFormSync_Factory(meshRouterProvider);
  }

  public static EmergencyFormSync newInstance(MeshRouter meshRouter) {
    return new EmergencyFormSync(meshRouter);
  }
}
