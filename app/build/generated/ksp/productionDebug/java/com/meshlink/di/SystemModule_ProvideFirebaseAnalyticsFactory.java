package com.meshlink.di;

import android.content.Context;
import com.google.firebase.analytics.FirebaseAnalytics;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class SystemModule_ProvideFirebaseAnalyticsFactory implements Factory<FirebaseAnalytics> {
  private final Provider<Context> contextProvider;

  public SystemModule_ProvideFirebaseAnalyticsFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FirebaseAnalytics get() {
    return provideFirebaseAnalytics(contextProvider.get());
  }

  public static SystemModule_ProvideFirebaseAnalyticsFactory create(
      Provider<Context> contextProvider) {
    return new SystemModule_ProvideFirebaseAnalyticsFactory(contextProvider);
  }

  public static FirebaseAnalytics provideFirebaseAnalytics(Context context) {
    return Preconditions.checkNotNullFromProvides(SystemModule.INSTANCE.provideFirebaseAnalytics(context));
  }
}
