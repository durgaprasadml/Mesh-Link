package com.meshlink.security.data.source;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class CryptoDataSourceImpl_Factory implements Factory<CryptoDataSourceImpl> {
  @Override
  public CryptoDataSourceImpl get() {
    return newInstance();
  }

  public static CryptoDataSourceImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CryptoDataSourceImpl newInstance() {
    return new CryptoDataSourceImpl();
  }

  private static final class InstanceHolder {
    private static final CryptoDataSourceImpl_Factory INSTANCE = new CryptoDataSourceImpl_Factory();
  }
}
