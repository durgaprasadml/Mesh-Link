package com.meshlink.recovery.engine;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class BusinessContinuityManager_Factory implements Factory<BusinessContinuityManager> {
  private final Provider<Context> contextProvider;

  public BusinessContinuityManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BusinessContinuityManager get() {
    return newInstance(contextProvider.get());
  }

  public static BusinessContinuityManager_Factory create(Provider<Context> contextProvider) {
    return new BusinessContinuityManager_Factory(contextProvider);
  }

  public static BusinessContinuityManager newInstance(Context context) {
    return new BusinessContinuityManager(context);
  }
}
