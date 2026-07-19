package com.meshlink.core.data;

import com.meshlink.core.data.source.UserLocalDataSource;
import com.meshlink.security.data.source.CryptoDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class UserRepositoryImpl_Factory implements Factory<UserRepositoryImpl> {
  private final Provider<UserLocalDataSource> localDataSourceProvider;

  private final Provider<CryptoDataSource> cryptoDataSourceProvider;

  public UserRepositoryImpl_Factory(Provider<UserLocalDataSource> localDataSourceProvider,
      Provider<CryptoDataSource> cryptoDataSourceProvider) {
    this.localDataSourceProvider = localDataSourceProvider;
    this.cryptoDataSourceProvider = cryptoDataSourceProvider;
  }

  @Override
  public UserRepositoryImpl get() {
    return newInstance(localDataSourceProvider.get(), cryptoDataSourceProvider.get());
  }

  public static UserRepositoryImpl_Factory create(
      Provider<UserLocalDataSource> localDataSourceProvider,
      Provider<CryptoDataSource> cryptoDataSourceProvider) {
    return new UserRepositoryImpl_Factory(localDataSourceProvider, cryptoDataSourceProvider);
  }

  public static UserRepositoryImpl newInstance(UserLocalDataSource localDataSource,
      CryptoDataSource cryptoDataSource) {
    return new UserRepositoryImpl(localDataSource, cryptoDataSource);
  }
}
