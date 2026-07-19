package com.meshlink.domain.usecase.messaging;

import com.meshlink.domain.repository.ChatRepository;
import com.meshlink.domain.repository.MeshRepository;
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
public final class SendMessageUseCase_Factory implements Factory<SendMessageUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public SendMessageUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<MeshRepository> meshRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public SendMessageUseCase get() {
    return newInstance(chatRepositoryProvider.get(), meshRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static SendMessageUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<MeshRepository> meshRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new SendMessageUseCase_Factory(chatRepositoryProvider, meshRepositoryProvider, userRepositoryProvider);
  }

  public static SendMessageUseCase newInstance(ChatRepository chatRepository,
      MeshRepository meshRepository, UserRepository userRepository) {
    return new SendMessageUseCase(chatRepository, meshRepository, userRepository);
  }
}
