package com.meshlink.di;

import android.content.Context;
import com.meshlink.data.location.LocationProvider;
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
public final class AppModule_ProvideLocationProviderFactory implements Factory<LocationProvider> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideLocationProviderFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LocationProvider get() {
    return provideLocationProvider(contextProvider.get());
  }

  public static AppModule_ProvideLocationProviderFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideLocationProviderFactory(contextProvider);
  }

  public static LocationProvider provideLocationProvider(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLocationProvider(context));
  }
}
