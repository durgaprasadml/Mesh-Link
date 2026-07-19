package com.meshlink.ui.settings;

import com.meshlink.domain.repository.UserRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  public SettingsViewModel_Factory(Provider<UserRepository> userRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(userRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<UserRepository> userRepositoryProvider) {
    return new SettingsViewModel_Factory(userRepositoryProvider);
  }

  public static SettingsViewModel newInstance(UserRepository userRepository) {
    return new SettingsViewModel(userRepository);
  }
}
