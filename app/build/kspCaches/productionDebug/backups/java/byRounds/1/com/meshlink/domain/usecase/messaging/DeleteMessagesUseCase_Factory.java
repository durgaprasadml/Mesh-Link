package com.meshlink.domain.usecase.messaging;

import com.meshlink.domain.repository.ChatRepository;
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
public final class DeleteMessagesUseCase_Factory implements Factory<DeleteMessagesUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public DeleteMessagesUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public DeleteMessagesUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static DeleteMessagesUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new DeleteMessagesUseCase_Factory(chatRepositoryProvider);
  }

  public static DeleteMessagesUseCase newInstance(ChatRepository chatRepository) {
    return new DeleteMessagesUseCase(chatRepository);
  }
}
