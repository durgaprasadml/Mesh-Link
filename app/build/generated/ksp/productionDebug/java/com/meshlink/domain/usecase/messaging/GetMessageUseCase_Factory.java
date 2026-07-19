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
public final class GetMessageUseCase_Factory implements Factory<GetMessageUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public GetMessageUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public GetMessageUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static GetMessageUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new GetMessageUseCase_Factory(chatRepositoryProvider);
  }

  public static GetMessageUseCase newInstance(ChatRepository chatRepository) {
    return new GetMessageUseCase(chatRepository);
  }
}
