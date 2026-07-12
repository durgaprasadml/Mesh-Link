package com.meshlink.data.repository;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.meshlink.data.local.UserDao;
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
  private final Provider<UserDao> userDaoProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public UserRepositoryImpl_Factory(Provider<UserDao> userDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.userDaoProvider = userDaoProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public UserRepositoryImpl get() {
    return newInstance(userDaoProvider.get(), dataStoreProvider.get());
  }

  public static UserRepositoryImpl_Factory create(Provider<UserDao> userDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new UserRepositoryImpl_Factory(userDaoProvider, dataStoreProvider);
  }

  public static UserRepositoryImpl newInstance(UserDao userDao, DataStore<Preferences> dataStore) {
    return new UserRepositoryImpl(userDao, dataStore);
  }
}
