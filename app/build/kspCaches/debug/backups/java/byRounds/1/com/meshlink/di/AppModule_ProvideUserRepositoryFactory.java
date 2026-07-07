package com.meshlink.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.meshlink.data.local.UserDao;
import com.meshlink.domain.repository.UserRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideUserRepositoryFactory implements Factory<UserRepository> {
  private final Provider<UserDao> userDaoProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public AppModule_ProvideUserRepositoryFactory(Provider<UserDao> userDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.userDaoProvider = userDaoProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public UserRepository get() {
    return provideUserRepository(userDaoProvider.get(), dataStoreProvider.get());
  }

  public static AppModule_ProvideUserRepositoryFactory create(Provider<UserDao> userDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AppModule_ProvideUserRepositoryFactory(userDaoProvider, dataStoreProvider);
  }

  public static UserRepository provideUserRepository(UserDao userDao,
      DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUserRepository(userDao, dataStore));
  }
}
