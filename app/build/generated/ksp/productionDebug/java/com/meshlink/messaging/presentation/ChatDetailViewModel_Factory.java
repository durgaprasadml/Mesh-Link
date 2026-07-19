package com.meshlink.messaging.presentation;

import androidx.lifecycle.SavedStateHandle;
import com.meshlink.domain.repository.MeshRepository;
import com.meshlink.domain.usecase.messaging.DeleteChatUseCase;
import com.meshlink.domain.usecase.messaging.DeleteMessagesUseCase;
import com.meshlink.domain.usecase.messaging.GetChatMessagesUseCase;
import com.meshlink.domain.usecase.messaging.GetMessageUseCase;
import com.meshlink.domain.usecase.messaging.MarkChatAsReadUseCase;
import com.meshlink.domain.usecase.messaging.SendMessageUseCase;
import com.meshlink.media.data.VoicePlayer;
import com.meshlink.media.data.VoiceRecorder;
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
public final class ChatDetailViewModel_Factory implements Factory<ChatDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<GetChatMessagesUseCase> getChatMessagesUseCaseProvider;

  private final Provider<DeleteMessagesUseCase> deleteMessagesUseCaseProvider;

  private final Provider<DeleteChatUseCase> deleteChatUseCaseProvider;

  private final Provider<MarkChatAsReadUseCase> markChatAsReadUseCaseProvider;

  private final Provider<GetMessageUseCase> getMessageUseCaseProvider;

  private final Provider<VoiceRecorder> voiceRecorderProvider;

  private final Provider<VoicePlayer> voicePlayerProvider;

  private final Provider<SendMessageUseCase> sendMessageUseCaseProvider;

  public ChatDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MeshRepository> meshRepositoryProvider,
      Provider<GetChatMessagesUseCase> getChatMessagesUseCaseProvider,
      Provider<DeleteMessagesUseCase> deleteMessagesUseCaseProvider,
      Provider<DeleteChatUseCase> deleteChatUseCaseProvider,
      Provider<MarkChatAsReadUseCase> markChatAsReadUseCaseProvider,
      Provider<GetMessageUseCase> getMessageUseCaseProvider,
      Provider<VoiceRecorder> voiceRecorderProvider, Provider<VoicePlayer> voicePlayerProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.getChatMessagesUseCaseProvider = getChatMessagesUseCaseProvider;
    this.deleteMessagesUseCaseProvider = deleteMessagesUseCaseProvider;
    this.deleteChatUseCaseProvider = deleteChatUseCaseProvider;
    this.markChatAsReadUseCaseProvider = markChatAsReadUseCaseProvider;
    this.getMessageUseCaseProvider = getMessageUseCaseProvider;
    this.voiceRecorderProvider = voiceRecorderProvider;
    this.voicePlayerProvider = voicePlayerProvider;
    this.sendMessageUseCaseProvider = sendMessageUseCaseProvider;
  }

  @Override
  public ChatDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), meshRepositoryProvider.get(), getChatMessagesUseCaseProvider.get(), deleteMessagesUseCaseProvider.get(), deleteChatUseCaseProvider.get(), markChatAsReadUseCaseProvider.get(), getMessageUseCaseProvider.get(), voiceRecorderProvider.get(), voicePlayerProvider.get(), sendMessageUseCaseProvider.get());
  }

  public static ChatDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MeshRepository> meshRepositoryProvider,
      Provider<GetChatMessagesUseCase> getChatMessagesUseCaseProvider,
      Provider<DeleteMessagesUseCase> deleteMessagesUseCaseProvider,
      Provider<DeleteChatUseCase> deleteChatUseCaseProvider,
      Provider<MarkChatAsReadUseCase> markChatAsReadUseCaseProvider,
      Provider<GetMessageUseCase> getMessageUseCaseProvider,
      Provider<VoiceRecorder> voiceRecorderProvider, Provider<VoicePlayer> voicePlayerProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider) {
    return new ChatDetailViewModel_Factory(savedStateHandleProvider, meshRepositoryProvider, getChatMessagesUseCaseProvider, deleteMessagesUseCaseProvider, deleteChatUseCaseProvider, markChatAsReadUseCaseProvider, getMessageUseCaseProvider, voiceRecorderProvider, voicePlayerProvider, sendMessageUseCaseProvider);
  }

  public static ChatDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      MeshRepository meshRepository, GetChatMessagesUseCase getChatMessagesUseCase,
      DeleteMessagesUseCase deleteMessagesUseCase, DeleteChatUseCase deleteChatUseCase,
      MarkChatAsReadUseCase markChatAsReadUseCase, GetMessageUseCase getMessageUseCase,
      VoiceRecorder voiceRecorder, VoicePlayer voicePlayer, SendMessageUseCase sendMessageUseCase) {
    return new ChatDetailViewModel(savedStateHandle, meshRepository, getChatMessagesUseCase, deleteMessagesUseCase, deleteChatUseCase, markChatAsReadUseCase, getMessageUseCase, voiceRecorder, voicePlayer, sendMessageUseCase);
  }
}
