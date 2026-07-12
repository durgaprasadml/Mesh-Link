package com.meshlink.di;

import android.content.Context;
import com.meshlink.data.crypto.MeshCryptoManager;
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
public final class AppModule_ProvideMeshCryptoManagerFactory implements Factory<MeshCryptoManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideMeshCryptoManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MeshCryptoManager get() {
    return provideMeshCryptoManager(contextProvider.get());
  }

  public static AppModule_ProvideMeshCryptoManagerFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideMeshCryptoManagerFactory(contextProvider);
  }

  public static MeshCryptoManager provideMeshCryptoManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMeshCryptoManager(context));
  }
}
