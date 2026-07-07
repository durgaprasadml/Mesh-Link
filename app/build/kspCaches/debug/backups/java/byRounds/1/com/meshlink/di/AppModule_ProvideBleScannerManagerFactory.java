package com.meshlink.di;

import android.content.Context;
import com.meshlink.data.ble.BleScannerManager;
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
public final class AppModule_ProvideBleScannerManagerFactory implements Factory<BleScannerManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideBleScannerManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BleScannerManager get() {
    return provideBleScannerManager(contextProvider.get());
  }

  public static AppModule_ProvideBleScannerManagerFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideBleScannerManagerFactory(contextProvider);
  }

  public static BleScannerManager provideBleScannerManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBleScannerManager(context));
  }
}
