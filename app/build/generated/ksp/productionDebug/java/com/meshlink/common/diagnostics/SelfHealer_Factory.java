package com.meshlink.common.diagnostics;

import android.content.Context;
import com.meshlink.ble.data.BleAdvertiserManager;
import com.meshlink.ble.data.BleScannerManager;
import com.meshlink.domain.repository.UserRepository;
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
public final class SelfHealer_Factory implements Factory<SelfHealer> {
  private final Provider<Context> contextProvider;

  private final Provider<BleScannerManager> scannerManagerProvider;

  private final Provider<BleAdvertiserManager> advertiserManagerProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public SelfHealer_Factory(Provider<Context> contextProvider,
      Provider<BleScannerManager> scannerManagerProvider,
      Provider<BleAdvertiserManager> advertiserManagerProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.scannerManagerProvider = scannerManagerProvider;
    this.advertiserManagerProvider = advertiserManagerProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public SelfHealer get() {
    return newInstance(contextProvider.get(), scannerManagerProvider.get(), advertiserManagerProvider.get(), userRepositoryProvider.get());
  }

  public static SelfHealer_Factory create(Provider<Context> contextProvider,
      Provider<BleScannerManager> scannerManagerProvider,
      Provider<BleAdvertiserManager> advertiserManagerProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new SelfHealer_Factory(contextProvider, scannerManagerProvider, advertiserManagerProvider, userRepositoryProvider);
  }

  public static SelfHealer newInstance(Context context, BleScannerManager scannerManager,
      BleAdvertiserManager advertiserManager, UserRepository userRepository) {
    return new SelfHealer(context, scannerManager, advertiserManager, userRepository);
  }
}
