package com.meshlink.di;

import android.content.Context;
import com.meshlink.data.media.MediaTransferManager;
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
public final class AppModule_ProvideMediaTransferManagerFactory implements Factory<MediaTransferManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideMediaTransferManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MediaTransferManager get() {
    return provideMediaTransferManager(contextProvider.get());
  }

  public static AppModule_ProvideMediaTransferManagerFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideMediaTransferManagerFactory(contextProvider);
  }

  public static MediaTransferManager provideMediaTransferManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMediaTransferManager(context));
  }
}
