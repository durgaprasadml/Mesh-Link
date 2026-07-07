package com.meshlink.data.crypto;

import android.content.Context;
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
public final class MeshCryptoManager_Factory implements Factory<MeshCryptoManager> {
  private final Provider<Context> contextProvider;

  public MeshCryptoManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MeshCryptoManager get() {
    return newInstance(contextProvider.get());
  }

  public static MeshCryptoManager_Factory create(Provider<Context> contextProvider) {
    return new MeshCryptoManager_Factory(contextProvider);
  }

  public static MeshCryptoManager newInstance(Context context) {
    return new MeshCryptoManager(context);
  }
}
