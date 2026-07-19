package com.meshlink.transfer;

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
public final class TransferCache_Factory implements Factory<TransferCache> {
  private final Provider<Context> contextProvider;

  public TransferCache_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TransferCache get() {
    return newInstance(contextProvider.get());
  }

  public static TransferCache_Factory create(Provider<Context> contextProvider) {
    return new TransferCache_Factory(contextProvider);
  }

  public static TransferCache newInstance(Context context) {
    return new TransferCache(context);
  }
}
