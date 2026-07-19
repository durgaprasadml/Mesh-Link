package com.meshlink.core.data.source;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.meshlink.database.data.local.UserDao;
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
public final class UserLocalDataSourceImpl_Factory implements Factory<UserLocalDataSourceImpl> {
  private final Provider<UserDao> userDaoProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public UserLocalDataSourceImpl_Factory(Provider<UserDao> userDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.userDaoProvider = userDaoProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public UserLocalDataSourceImpl get() {
    return newInstance(userDaoProvider.get(), dataStoreProvider.get());
  }

  public static UserLocalDataSourceImpl_Factory create(Provider<UserDao> userDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new UserLocalDataSourceImpl_Factory(userDaoProvider, dataStoreProvider);
  }

  public static UserLocalDataSourceImpl newInstance(UserDao userDao,
      DataStore<Preferences> dataStore) {
    return new UserLocalDataSourceImpl(userDao, dataStore);
  }
}
