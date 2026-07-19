package com.meshlink.ui.broadcast;

import com.meshlink.domain.repository.MeshRepository;
import com.meshlink.domain.usecase.messaging.GetBroadcastMessagesUseCase;
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
public final class BroadcastViewModel_Factory implements Factory<BroadcastViewModel> {
  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<GetBroadcastMessagesUseCase> getBroadcastMessagesUseCaseProvider;

  public BroadcastViewModel_Factory(Provider<MeshRepository> meshRepositoryProvider,
      Provider<GetBroadcastMessagesUseCase> getBroadcastMessagesUseCaseProvider) {
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.getBroadcastMessagesUseCaseProvider = getBroadcastMessagesUseCaseProvider;
  }

  @Override
  public BroadcastViewModel get() {
    return newInstance(meshRepositoryProvider.get(), getBroadcastMessagesUseCaseProvider.get());
  }

  public static BroadcastViewModel_Factory create(Provider<MeshRepository> meshRepositoryProvider,
      Provider<GetBroadcastMessagesUseCase> getBroadcastMessagesUseCaseProvider) {
    return new BroadcastViewModel_Factory(meshRepositoryProvider, getBroadcastMessagesUseCaseProvider);
  }

  public static BroadcastViewModel newInstance(MeshRepository meshRepository,
      GetBroadcastMessagesUseCase getBroadcastMessagesUseCase) {
    return new BroadcastViewModel(meshRepository, getBroadcastMessagesUseCase);
  }
}
