package com.meshlink;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.meshlink.ai.data.LearningRepository;
import com.meshlink.ai.engine.BatteryPredictor;
import com.meshlink.ai.engine.CongestionPredictor;
import com.meshlink.ai.engine.FailurePredictor;
import com.meshlink.ai.engine.RoutePredictionEngine;
import com.meshlink.ai.engine.TransportPredictor;
import com.meshlink.analytics.data.MeshAnalytics;
import com.meshlink.ble.data.BleAdvertiserManager;
import com.meshlink.ble.data.BleConnectionManager;
import com.meshlink.ble.data.BleGattManager;
import com.meshlink.ble.data.BleRepositoryImpl;
import com.meshlink.ble.data.BleScannerManager;
import com.meshlink.ble.data.DiscoveryManager;
import com.meshlink.ble.data.MeshMessagingManager;
import com.meshlink.ble.data.RoutingCoordinator;
import com.meshlink.ble.data.source.BleMeshDataSource;
import com.meshlink.ble.data.source.BleMeshDataSourceImpl;
import com.meshlink.ble.discovery.BatteryAwareScanner;
import com.meshlink.ble.discovery.DiscoveryEngine;
import com.meshlink.common.diagnostics.RuntimeWatchdog;
import com.meshlink.common.diagnostics.SelfHealer;
import com.meshlink.common.power.AdaptiveMeshPowerManager;
import com.meshlink.common.power.PowerStateManager;
import com.meshlink.core.data.UserRepositoryImpl;
import com.meshlink.core.data.source.UserLocalDataSource;
import com.meshlink.core.data.source.UserLocalDataSourceImpl;
import com.meshlink.data.location.LocationProvider;
import com.meshlink.database.data.local.AuditLogDao;
import com.meshlink.database.data.local.ChatDao;
import com.meshlink.database.data.local.MeshDatabase;
import com.meshlink.database.data.local.RelayDao;
import com.meshlink.database.data.local.TrustDao;
import com.meshlink.database.data.local.UserDao;
import com.meshlink.database.data.source.ChatLocalDataSource;
import com.meshlink.database.data.source.ChatLocalDataSourceImpl;
import com.meshlink.di.CoroutineModule_ProvideDefaultDispatcherFactory;
import com.meshlink.di.CoroutineModule_ProvideIoDispatcherFactory;
import com.meshlink.di.CoroutineModule_ProvideMainDispatcherFactory;
import com.meshlink.di.DatabaseModule_ProvideAuditLogDaoFactory;
import com.meshlink.di.DatabaseModule_ProvideChatDaoFactory;
import com.meshlink.di.DatabaseModule_ProvideMeshDatabaseFactory;
import com.meshlink.di.DatabaseModule_ProvideRelayDaoFactory;
import com.meshlink.di.DatabaseModule_ProvideTrustDaoFactory;
import com.meshlink.di.DatabaseModule_ProvideUserDaoFactory;
import com.meshlink.di.SystemModule_ProvideDataStoreFactory;
import com.meshlink.di.SystemModule_ProvideFirebaseAnalyticsFactory;
import com.meshlink.domain.repository.ChatRepository;
import com.meshlink.domain.repository.UserRepository;
import com.meshlink.domain.usecase.messaging.DeleteChatUseCase;
import com.meshlink.domain.usecase.messaging.DeleteMessagesUseCase;
import com.meshlink.domain.usecase.messaging.GetAllChatsUseCase;
import com.meshlink.domain.usecase.messaging.GetBroadcastMessagesUseCase;
import com.meshlink.domain.usecase.messaging.GetChatMessagesUseCase;
import com.meshlink.domain.usecase.messaging.GetMessageUseCase;
import com.meshlink.domain.usecase.messaging.MarkChatAsReadUseCase;
import com.meshlink.domain.usecase.messaging.SendMessageUseCase;
import com.meshlink.emergency.EmergencyManager;
import com.meshlink.media.data.MediaTransferManager;
import com.meshlink.media.data.VoicePlayer;
import com.meshlink.media.data.VoiceRecorder;
import com.meshlink.messaging.data.MessagingRepositoryImpl;
import com.meshlink.messaging.presentation.ChatDetailViewModel;
import com.meshlink.messaging.presentation.ChatDetailViewModel_HiltModules;
import com.meshlink.messaging.presentation.ChatsListViewModel;
import com.meshlink.messaging.presentation.ChatsListViewModel_HiltModules;
import com.meshlink.routing.data.MeshRouter;
import com.meshlink.routing.engine.BatteryAwareNetworking;
import com.meshlink.routing.engine.CongestionMonitor;
import com.meshlink.routing.engine.IntelligentRetryEngine;
import com.meshlink.routing.engine.IntelligentTransportManager;
import com.meshlink.routing.engine.NetworkTopologyEngine;
import com.meshlink.routing.engine.QoSManager;
import com.meshlink.routing.engine.QueueOptimizer;
import com.meshlink.routing.engine.RouteCache;
import com.meshlink.routing.engine.RouteHealthMonitor;
import com.meshlink.routing.engine.RouteManager;
import com.meshlink.routing.engine.RouteOptimizer;
import com.meshlink.routing.engine.RouteScorer;
import com.meshlink.routing.engine.RoutingEngine;
import com.meshlink.security.data.DatabaseSecurityManager;
import com.meshlink.security.data.MeshCryptoManager;
import com.meshlink.security.data.MeshSecurityMonitor;
import com.meshlink.security.data.RekeyManager;
import com.meshlink.security.data.SessionManager;
import com.meshlink.security.data.TrustManager;
import com.meshlink.security.data.source.CryptoDataSource;
import com.meshlink.security.data.source.CryptoDataSourceImpl;
import com.meshlink.service.MeshRelayService;
import com.meshlink.service.MeshRelayService_MembersInjector;
import com.meshlink.service.work.BackgroundTaskScheduler;
import com.meshlink.service.work.CleanupWorker;
import com.meshlink.service.work.CleanupWorker_AssistedFactory;
import com.meshlink.service.work.RetryWorker;
import com.meshlink.service.work.RetryWorker_AssistedFactory;
import com.meshlink.storage.data.local.CacheManager;
import com.meshlink.transfer.ChunkManager;
import com.meshlink.transfer.FileMetadataManager;
import com.meshlink.transfer.IntegrityVerifier;
import com.meshlink.transfer.TransferAnalytics;
import com.meshlink.transfer.TransferCache;
import com.meshlink.transfer.TransferManager;
import com.meshlink.transfer.TransferScheduler;
import com.meshlink.ui.analytics.AnalyticsViewModel;
import com.meshlink.ui.analytics.AnalyticsViewModel_HiltModules;
import com.meshlink.ui.auth.AuthViewModel;
import com.meshlink.ui.auth.AuthViewModel_HiltModules;
import com.meshlink.ui.broadcast.BroadcastViewModel;
import com.meshlink.ui.broadcast.BroadcastViewModel_HiltModules;
import com.meshlink.ui.home.HomeViewModel;
import com.meshlink.ui.home.HomeViewModel_HiltModules;
import com.meshlink.ui.mesh.MeshDebugViewModel;
import com.meshlink.ui.mesh.MeshDebugViewModel_HiltModules;
import com.meshlink.ui.nearby.NearbyViewModel;
import com.meshlink.ui.nearby.NearbyViewModel_HiltModules;
import com.meshlink.ui.settings.SettingsViewModel;
import com.meshlink.ui.settings.SettingsViewModel_HiltModules;
import com.meshlink.ui.sos.SosViewModel;
import com.meshlink.ui.sos.SosViewModel_HiltModules;
import com.meshlink.video.transport.VideoTransport;
import com.meshlink.voice.transport.VoiceTransport;
import com.meshlink.wifi.data.WifiDirectManager;
import com.meshlink.wifi.data.WifiSocketTransport;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideApplicationFactory;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerMeshLinkApp_HiltComponents_SingletonC {
  private DaggerMeshLinkApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public MeshLinkApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements MeshLinkApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements MeshLinkApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements MeshLinkApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements MeshLinkApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements MeshLinkApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements MeshLinkApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements MeshLinkApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends MeshLinkApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends MeshLinkApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends MeshLinkApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends MeshLinkApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(10).put(LazyClassKeyProvider.com_meshlink_ui_analytics_AnalyticsViewModel, AnalyticsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_auth_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_broadcast_BroadcastViewModel, BroadcastViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_messaging_presentation_ChatDetailViewModel, ChatDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_messaging_presentation_ChatsListViewModel, ChatsListViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_home_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_mesh_MeshDebugViewModel, MeshDebugViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_nearby_NearbyViewModel, NearbyViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_settings_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_sos_SosViewModel, SosViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectMeshRepository(instance, singletonCImpl.bleRepositoryImplProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_meshlink_ui_auth_AuthViewModel = "com.meshlink.ui.auth.AuthViewModel";

      static String com_meshlink_ui_settings_SettingsViewModel = "com.meshlink.ui.settings.SettingsViewModel";

      static String com_meshlink_messaging_presentation_ChatsListViewModel = "com.meshlink.messaging.presentation.ChatsListViewModel";

      static String com_meshlink_ui_broadcast_BroadcastViewModel = "com.meshlink.ui.broadcast.BroadcastViewModel";

      static String com_meshlink_ui_nearby_NearbyViewModel = "com.meshlink.ui.nearby.NearbyViewModel";

      static String com_meshlink_ui_sos_SosViewModel = "com.meshlink.ui.sos.SosViewModel";

      static String com_meshlink_ui_home_HomeViewModel = "com.meshlink.ui.home.HomeViewModel";

      static String com_meshlink_ui_analytics_AnalyticsViewModel = "com.meshlink.ui.analytics.AnalyticsViewModel";

      static String com_meshlink_messaging_presentation_ChatDetailViewModel = "com.meshlink.messaging.presentation.ChatDetailViewModel";

      static String com_meshlink_ui_mesh_MeshDebugViewModel = "com.meshlink.ui.mesh.MeshDebugViewModel";

      @KeepFieldType
      AuthViewModel com_meshlink_ui_auth_AuthViewModel2;

      @KeepFieldType
      SettingsViewModel com_meshlink_ui_settings_SettingsViewModel2;

      @KeepFieldType
      ChatsListViewModel com_meshlink_messaging_presentation_ChatsListViewModel2;

      @KeepFieldType
      BroadcastViewModel com_meshlink_ui_broadcast_BroadcastViewModel2;

      @KeepFieldType
      NearbyViewModel com_meshlink_ui_nearby_NearbyViewModel2;

      @KeepFieldType
      SosViewModel com_meshlink_ui_sos_SosViewModel2;

      @KeepFieldType
      HomeViewModel com_meshlink_ui_home_HomeViewModel2;

      @KeepFieldType
      AnalyticsViewModel com_meshlink_ui_analytics_AnalyticsViewModel2;

      @KeepFieldType
      ChatDetailViewModel com_meshlink_messaging_presentation_ChatDetailViewModel2;

      @KeepFieldType
      MeshDebugViewModel com_meshlink_ui_mesh_MeshDebugViewModel2;
    }
  }

  private static final class ViewModelCImpl extends MeshLinkApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AnalyticsViewModel> analyticsViewModelProvider;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<BroadcastViewModel> broadcastViewModelProvider;

    private Provider<ChatDetailViewModel> chatDetailViewModelProvider;

    private Provider<ChatsListViewModel> chatsListViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<MeshDebugViewModel> meshDebugViewModelProvider;

    private Provider<NearbyViewModel> nearbyViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SosViewModel> sosViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private GetBroadcastMessagesUseCase getBroadcastMessagesUseCase() {
      return new GetBroadcastMessagesUseCase(singletonCImpl.bindChatRepositoryProvider.get());
    }

    private GetChatMessagesUseCase getChatMessagesUseCase() {
      return new GetChatMessagesUseCase(singletonCImpl.bindChatRepositoryProvider.get());
    }

    private DeleteMessagesUseCase deleteMessagesUseCase() {
      return new DeleteMessagesUseCase(singletonCImpl.bindChatRepositoryProvider.get());
    }

    private DeleteChatUseCase deleteChatUseCase() {
      return new DeleteChatUseCase(singletonCImpl.bindChatRepositoryProvider.get());
    }

    private MarkChatAsReadUseCase markChatAsReadUseCase() {
      return new MarkChatAsReadUseCase(singletonCImpl.bindChatRepositoryProvider.get());
    }

    private GetMessageUseCase getMessageUseCase() {
      return new GetMessageUseCase(singletonCImpl.bindChatRepositoryProvider.get());
    }

    private SendMessageUseCase sendMessageUseCase() {
      return new SendMessageUseCase(singletonCImpl.bindChatRepositoryProvider.get(), singletonCImpl.bleRepositoryImplProvider.get(), singletonCImpl.bindUserRepositoryProvider.get());
    }

    private GetAllChatsUseCase getAllChatsUseCase() {
      return new GetAllChatsUseCase(singletonCImpl.bindChatRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.analyticsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.broadcastViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.chatDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.chatsListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.meshDebugViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.nearbyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.sosViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(10).put(LazyClassKeyProvider.com_meshlink_ui_analytics_AnalyticsViewModel, ((Provider) analyticsViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_auth_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_broadcast_BroadcastViewModel, ((Provider) broadcastViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_messaging_presentation_ChatDetailViewModel, ((Provider) chatDetailViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_messaging_presentation_ChatsListViewModel, ((Provider) chatsListViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_home_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_mesh_MeshDebugViewModel, ((Provider) meshDebugViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_nearby_NearbyViewModel, ((Provider) nearbyViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_settings_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_sos_SosViewModel, ((Provider) sosViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_meshlink_messaging_presentation_ChatsListViewModel = "com.meshlink.messaging.presentation.ChatsListViewModel";

      static String com_meshlink_ui_home_HomeViewModel = "com.meshlink.ui.home.HomeViewModel";

      static String com_meshlink_ui_nearby_NearbyViewModel = "com.meshlink.ui.nearby.NearbyViewModel";

      static String com_meshlink_ui_analytics_AnalyticsViewModel = "com.meshlink.ui.analytics.AnalyticsViewModel";

      static String com_meshlink_ui_settings_SettingsViewModel = "com.meshlink.ui.settings.SettingsViewModel";

      static String com_meshlink_ui_broadcast_BroadcastViewModel = "com.meshlink.ui.broadcast.BroadcastViewModel";

      static String com_meshlink_ui_auth_AuthViewModel = "com.meshlink.ui.auth.AuthViewModel";

      static String com_meshlink_ui_sos_SosViewModel = "com.meshlink.ui.sos.SosViewModel";

      static String com_meshlink_messaging_presentation_ChatDetailViewModel = "com.meshlink.messaging.presentation.ChatDetailViewModel";

      static String com_meshlink_ui_mesh_MeshDebugViewModel = "com.meshlink.ui.mesh.MeshDebugViewModel";

      @KeepFieldType
      ChatsListViewModel com_meshlink_messaging_presentation_ChatsListViewModel2;

      @KeepFieldType
      HomeViewModel com_meshlink_ui_home_HomeViewModel2;

      @KeepFieldType
      NearbyViewModel com_meshlink_ui_nearby_NearbyViewModel2;

      @KeepFieldType
      AnalyticsViewModel com_meshlink_ui_analytics_AnalyticsViewModel2;

      @KeepFieldType
      SettingsViewModel com_meshlink_ui_settings_SettingsViewModel2;

      @KeepFieldType
      BroadcastViewModel com_meshlink_ui_broadcast_BroadcastViewModel2;

      @KeepFieldType
      AuthViewModel com_meshlink_ui_auth_AuthViewModel2;

      @KeepFieldType
      SosViewModel com_meshlink_ui_sos_SosViewModel2;

      @KeepFieldType
      ChatDetailViewModel com_meshlink_messaging_presentation_ChatDetailViewModel2;

      @KeepFieldType
      MeshDebugViewModel com_meshlink_ui_mesh_MeshDebugViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.meshlink.ui.analytics.AnalyticsViewModel 
          return (T) new AnalyticsViewModel(singletonCImpl.meshAnalyticsProvider.get(), singletonCImpl.bleRepositoryImplProvider.get());

          case 1: // com.meshlink.ui.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.bindUserRepositoryProvider.get());

          case 2: // com.meshlink.ui.broadcast.BroadcastViewModel 
          return (T) new BroadcastViewModel(singletonCImpl.bleRepositoryImplProvider.get(), viewModelCImpl.getBroadcastMessagesUseCase());

          case 3: // com.meshlink.messaging.presentation.ChatDetailViewModel 
          return (T) new ChatDetailViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.bleRepositoryImplProvider.get(), viewModelCImpl.getChatMessagesUseCase(), viewModelCImpl.deleteMessagesUseCase(), viewModelCImpl.deleteChatUseCase(), viewModelCImpl.markChatAsReadUseCase(), viewModelCImpl.getMessageUseCase(), singletonCImpl.voiceRecorderProvider.get(), singletonCImpl.voicePlayerProvider.get(), viewModelCImpl.sendMessageUseCase());

          case 4: // com.meshlink.messaging.presentation.ChatsListViewModel 
          return (T) new ChatsListViewModel(viewModelCImpl.getAllChatsUseCase());

          case 5: // com.meshlink.ui.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.bindUserRepositoryProvider.get(), singletonCImpl.bleRepositoryImplProvider.get(), singletonCImpl.provideChatDaoProvider.get());

          case 6: // com.meshlink.ui.mesh.MeshDebugViewModel 
          return (T) new MeshDebugViewModel(singletonCImpl.bleRepositoryImplProvider.get());

          case 7: // com.meshlink.ui.nearby.NearbyViewModel 
          return (T) new NearbyViewModel(singletonCImpl.bleRepositoryImplProvider.get(), singletonCImpl.bindUserRepositoryProvider.get(), singletonCImpl.wifiDirectManagerProvider.get());

          case 8: // com.meshlink.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.bindUserRepositoryProvider.get());

          case 9: // com.meshlink.ui.sos.SosViewModel 
          return (T) new SosViewModel(singletonCImpl.bleRepositoryImplProvider.get(), singletonCImpl.locationProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends MeshLinkApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends MeshLinkApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectMeshRelayService(MeshRelayService meshRelayService) {
      injectMeshRelayService2(meshRelayService);
    }

    @CanIgnoreReturnValue
    private MeshRelayService injectMeshRelayService2(MeshRelayService instance) {
      MeshRelayService_MembersInjector.injectMeshRepository(instance, singletonCImpl.bleRepositoryImplProvider.get());
      MeshRelayService_MembersInjector.injectWifiDirectManager(instance, singletonCImpl.wifiDirectManagerProvider.get());
      MeshRelayService_MembersInjector.injectPowerStateManager(instance, singletonCImpl.powerStateManagerProvider.get());
      MeshRelayService_MembersInjector.injectRuntimeWatchdog(instance, singletonCImpl.runtimeWatchdogProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends MeshLinkApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<CacheManager> cacheManagerProvider;

    private Provider<DatabaseSecurityManager> databaseSecurityManagerProvider;

    private Provider<MeshDatabase> provideMeshDatabaseProvider;

    private Provider<RelayDao> provideRelayDaoProvider;

    private Provider<AuditLogDao> provideAuditLogDaoProvider;

    private Provider<CleanupWorker_AssistedFactory> cleanupWorker_AssistedFactoryProvider;

    private Provider<RetryWorker_AssistedFactory> retryWorker_AssistedFactoryProvider;

    private Provider<BackgroundTaskScheduler> backgroundTaskSchedulerProvider;

    private Provider<PowerStateManager> powerStateManagerProvider;

    private Provider<BatteryAwareScanner> batteryAwareScannerProvider;

    private Provider<DiscoveryEngine> discoveryEngineProvider;

    private Provider<BleScannerManager> bleScannerManagerProvider;

    private Provider<BleAdvertiserManager> bleAdvertiserManagerProvider;

    private Provider<BleGattManager> bleGattManagerProvider;

    private Provider<BleMeshDataSourceImpl> bleMeshDataSourceImplProvider;

    private Provider<BleMeshDataSource> bindBleMeshDataSourceProvider;

    private Provider<MeshAnalytics> meshAnalyticsProvider;

    private Provider<TrustDao> provideTrustDaoProvider;

    private Provider<MeshSecurityMonitor> meshSecurityMonitorProvider;

    private Provider<TrustManager> trustManagerProvider;

    private Provider<RouteCache> routeCacheProvider;

    private Provider<RouteScorer> routeScorerProvider;

    private Provider<LearningRepository> learningRepositoryProvider;

    private Provider<FailurePredictor> failurePredictorProvider;

    private Provider<RoutePredictionEngine> routePredictionEngineProvider;

    private Provider<RouteOptimizer> routeOptimizerProvider;

    private Provider<RouteManager> routeManagerProvider;

    private Provider<QoSManager> qoSManagerProvider;

    private Provider<CongestionPredictor> congestionPredictorProvider;

    private Provider<CongestionMonitor> congestionMonitorProvider;

    private Provider<RouteHealthMonitor> routeHealthMonitorProvider;

    private Provider<NetworkTopologyEngine> networkTopologyEngineProvider;

    private Provider<BatteryPredictor> batteryPredictorProvider;

    private Provider<EmergencyManager> emergencyManagerProvider;

    private Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider;

    private Provider<TransportPredictor> transportPredictorProvider;

    private Provider<IntelligentTransportManager> intelligentTransportManagerProvider;

    private Provider<IntelligentRetryEngine> intelligentRetryEngineProvider;

    private Provider<QueueOptimizer> queueOptimizerProvider;

    private Provider<RoutingEngine> routingEngineProvider;

    private Provider<MeshRouter> meshRouterProvider;

    private Provider<ChatDao> provideChatDaoProvider;

    private Provider<UserDao> provideUserDaoProvider;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<UserLocalDataSourceImpl> userLocalDataSourceImplProvider;

    private Provider<UserLocalDataSource> bindUserLocalDataSourceProvider;

    private Provider<CryptoDataSourceImpl> cryptoDataSourceImplProvider;

    private Provider<CryptoDataSource> bindCryptoDataSourceProvider;

    private Provider<UserRepositoryImpl> userRepositoryImplProvider;

    private Provider<UserRepository> bindUserRepositoryProvider;

    private Provider<TransferScheduler> transferSchedulerProvider;

    private Provider<TransferCache> transferCacheProvider;

    private Provider<ChunkManager> chunkManagerProvider;

    private Provider<FileMetadataManager> fileMetadataManagerProvider;

    private Provider<IntegrityVerifier> integrityVerifierProvider;

    private Provider<TransferAnalytics> transferAnalyticsProvider;

    private Provider<TransferManager> transferManagerProvider;

    private Provider<MediaTransferManager> mediaTransferManagerProvider;

    private Provider<LocationProvider> locationProvider;

    private Provider<MeshCryptoManager> meshCryptoManagerProvider;

    private Provider<FirebaseAnalytics> provideFirebaseAnalyticsProvider;

    private Provider<WifiDirectManager> wifiDirectManagerProvider;

    private Provider<WifiSocketTransport> wifiSocketTransportProvider;

    private Provider<SessionManager> sessionManagerProvider;

    private Provider<RekeyManager> rekeyManagerProvider;

    private Provider<DiscoveryManager> discoveryManagerProvider;

    private Provider<BleConnectionManager> bleConnectionManagerProvider;

    private Provider<RoutingCoordinator> routingCoordinatorProvider;

    private Provider<VoiceTransport> voiceTransportProvider;

    private Provider<VideoTransport> videoTransportProvider;

    private Provider<MeshMessagingManager> meshMessagingManagerProvider;

    private Provider<BleRepositoryImpl> bleRepositoryImplProvider;

    private Provider<AdaptiveMeshPowerManager> adaptiveMeshPowerManagerProvider;

    private Provider<ChatLocalDataSourceImpl> chatLocalDataSourceImplProvider;

    private Provider<ChatLocalDataSource> bindChatLocalDataSourceProvider;

    private Provider<MessagingRepositoryImpl> messagingRepositoryImplProvider;

    private Provider<ChatRepository> bindChatRepositoryProvider;

    private Provider<VoiceRecorder> voiceRecorderProvider;

    private Provider<VoicePlayer> voicePlayerProvider;

    private Provider<SelfHealer> selfHealerProvider;

    private Provider<RuntimeWatchdog> runtimeWatchdogProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return ImmutableMap.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>of("com.meshlink.service.work.CleanupWorker", ((Provider) cleanupWorker_AssistedFactoryProvider), "com.meshlink.service.work.RetryWorker", ((Provider) retryWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.cacheManagerProvider = DoubleCheck.provider(new SwitchingProvider<CacheManager>(singletonCImpl, 1));
      this.databaseSecurityManagerProvider = DoubleCheck.provider(new SwitchingProvider<DatabaseSecurityManager>(singletonCImpl, 4));
      this.provideMeshDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<MeshDatabase>(singletonCImpl, 3));
      this.provideRelayDaoProvider = DoubleCheck.provider(new SwitchingProvider<RelayDao>(singletonCImpl, 2));
      this.provideAuditLogDaoProvider = DoubleCheck.provider(new SwitchingProvider<AuditLogDao>(singletonCImpl, 5));
      this.cleanupWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<CleanupWorker_AssistedFactory>(singletonCImpl, 0));
      this.retryWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<RetryWorker_AssistedFactory>(singletonCImpl, 6));
      this.backgroundTaskSchedulerProvider = DoubleCheck.provider(new SwitchingProvider<BackgroundTaskScheduler>(singletonCImpl, 7));
      this.powerStateManagerProvider = DoubleCheck.provider(new SwitchingProvider<PowerStateManager>(singletonCImpl, 9));
      this.batteryAwareScannerProvider = DoubleCheck.provider(new SwitchingProvider<BatteryAwareScanner>(singletonCImpl, 14));
      this.discoveryEngineProvider = DoubleCheck.provider(new SwitchingProvider<DiscoveryEngine>(singletonCImpl, 13));
      this.bleScannerManagerProvider = DoubleCheck.provider(new SwitchingProvider<BleScannerManager>(singletonCImpl, 12));
      this.bleAdvertiserManagerProvider = DoubleCheck.provider(new SwitchingProvider<BleAdvertiserManager>(singletonCImpl, 15));
      this.bleGattManagerProvider = DoubleCheck.provider(new SwitchingProvider<BleGattManager>(singletonCImpl, 16));
      this.bleMeshDataSourceImplProvider = new SwitchingProvider<>(singletonCImpl, 11);
      this.bindBleMeshDataSourceProvider = DoubleCheck.provider((Provider) bleMeshDataSourceImplProvider);
      this.meshAnalyticsProvider = DoubleCheck.provider(new SwitchingProvider<MeshAnalytics>(singletonCImpl, 18));
      this.provideTrustDaoProvider = DoubleCheck.provider(new SwitchingProvider<TrustDao>(singletonCImpl, 20));
      this.meshSecurityMonitorProvider = DoubleCheck.provider(new SwitchingProvider<MeshSecurityMonitor>(singletonCImpl, 21));
      this.trustManagerProvider = DoubleCheck.provider(new SwitchingProvider<TrustManager>(singletonCImpl, 19));
      this.routeCacheProvider = DoubleCheck.provider(new SwitchingProvider<RouteCache>(singletonCImpl, 24));
      this.routeScorerProvider = DoubleCheck.provider(new SwitchingProvider<RouteScorer>(singletonCImpl, 25));
      this.learningRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LearningRepository>(singletonCImpl, 28));
      this.failurePredictorProvider = DoubleCheck.provider(new SwitchingProvider<FailurePredictor>(singletonCImpl, 27));
      this.routePredictionEngineProvider = DoubleCheck.provider(new SwitchingProvider<RoutePredictionEngine>(singletonCImpl, 29));
      this.routeOptimizerProvider = DoubleCheck.provider(new SwitchingProvider<RouteOptimizer>(singletonCImpl, 26));
      this.routeManagerProvider = DoubleCheck.provider(new SwitchingProvider<RouteManager>(singletonCImpl, 23));
      this.qoSManagerProvider = DoubleCheck.provider(new SwitchingProvider<QoSManager>(singletonCImpl, 30));
      this.congestionPredictorProvider = DoubleCheck.provider(new SwitchingProvider<CongestionPredictor>(singletonCImpl, 32));
      this.congestionMonitorProvider = DoubleCheck.provider(new SwitchingProvider<CongestionMonitor>(singletonCImpl, 31));
      this.routeHealthMonitorProvider = DoubleCheck.provider(new SwitchingProvider<RouteHealthMonitor>(singletonCImpl, 33));
      this.networkTopologyEngineProvider = DoubleCheck.provider(new SwitchingProvider<NetworkTopologyEngine>(singletonCImpl, 34));
      this.batteryPredictorProvider = DoubleCheck.provider(new SwitchingProvider<BatteryPredictor>(singletonCImpl, 36));
      this.emergencyManagerProvider = DoubleCheck.provider(new SwitchingProvider<EmergencyManager>(singletonCImpl, 37));
      this.batteryAwareNetworkingProvider = DoubleCheck.provider(new SwitchingProvider<BatteryAwareNetworking>(singletonCImpl, 35));
      this.transportPredictorProvider = DoubleCheck.provider(new SwitchingProvider<TransportPredictor>(singletonCImpl, 39));
      this.intelligentTransportManagerProvider = DoubleCheck.provider(new SwitchingProvider<IntelligentTransportManager>(singletonCImpl, 38));
      this.intelligentRetryEngineProvider = DoubleCheck.provider(new SwitchingProvider<IntelligentRetryEngine>(singletonCImpl, 40));
      this.queueOptimizerProvider = DoubleCheck.provider(new SwitchingProvider<QueueOptimizer>(singletonCImpl, 41));
      this.routingEngineProvider = DoubleCheck.provider(new SwitchingProvider<RoutingEngine>(singletonCImpl, 22));
      this.meshRouterProvider = DoubleCheck.provider(new SwitchingProvider<MeshRouter>(singletonCImpl, 17));
      this.provideChatDaoProvider = DoubleCheck.provider(new SwitchingProvider<ChatDao>(singletonCImpl, 42));
      this.provideUserDaoProvider = DoubleCheck.provider(new SwitchingProvider<UserDao>(singletonCImpl, 45));
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 46));
      this.userLocalDataSourceImplProvider = new SwitchingProvider<>(singletonCImpl, 44);
      this.bindUserLocalDataSourceProvider = DoubleCheck.provider((Provider) userLocalDataSourceImplProvider);
      this.cryptoDataSourceImplProvider = new SwitchingProvider<>(singletonCImpl, 47);
      this.bindCryptoDataSourceProvider = DoubleCheck.provider((Provider) cryptoDataSourceImplProvider);
      this.userRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 43);
      this.bindUserRepositoryProvider = DoubleCheck.provider((Provider) userRepositoryImplProvider);
      this.transferSchedulerProvider = DoubleCheck.provider(new SwitchingProvider<TransferScheduler>(singletonCImpl, 49));
      this.transferCacheProvider = DoubleCheck.provider(new SwitchingProvider<TransferCache>(singletonCImpl, 50));
      this.chunkManagerProvider = DoubleCheck.provider(new SwitchingProvider<ChunkManager>(singletonCImpl, 51));
      this.fileMetadataManagerProvider = DoubleCheck.provider(new SwitchingProvider<FileMetadataManager>(singletonCImpl, 52));
      this.integrityVerifierProvider = DoubleCheck.provider(new SwitchingProvider<IntegrityVerifier>(singletonCImpl, 53));
      this.transferAnalyticsProvider = DoubleCheck.provider(new SwitchingProvider<TransferAnalytics>(singletonCImpl, 54));
      this.transferManagerProvider = DoubleCheck.provider(new SwitchingProvider<TransferManager>(singletonCImpl, 48));
      this.mediaTransferManagerProvider = DoubleCheck.provider(new SwitchingProvider<MediaTransferManager>(singletonCImpl, 55));
      this.locationProvider = DoubleCheck.provider(new SwitchingProvider<LocationProvider>(singletonCImpl, 56));
      this.meshCryptoManagerProvider = DoubleCheck.provider(new SwitchingProvider<MeshCryptoManager>(singletonCImpl, 57));
      this.provideFirebaseAnalyticsProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAnalytics>(singletonCImpl, 59));
      this.wifiDirectManagerProvider = DoubleCheck.provider(new SwitchingProvider<WifiDirectManager>(singletonCImpl, 58));
      this.wifiSocketTransportProvider = DoubleCheck.provider(new SwitchingProvider<WifiSocketTransport>(singletonCImpl, 60));
      this.sessionManagerProvider = DoubleCheck.provider(new SwitchingProvider<SessionManager>(singletonCImpl, 61));
      this.rekeyManagerProvider = DoubleCheck.provider(new SwitchingProvider<RekeyManager>(singletonCImpl, 62));
      this.discoveryManagerProvider = DoubleCheck.provider(new SwitchingProvider<DiscoveryManager>(singletonCImpl, 63));
      this.bleConnectionManagerProvider = DoubleCheck.provider(new SwitchingProvider<BleConnectionManager>(singletonCImpl, 64));
      this.routingCoordinatorProvider = DoubleCheck.provider(new SwitchingProvider<RoutingCoordinator>(singletonCImpl, 65));
      this.voiceTransportProvider = DoubleCheck.provider(new SwitchingProvider<VoiceTransport>(singletonCImpl, 67));
      this.videoTransportProvider = DoubleCheck.provider(new SwitchingProvider<VideoTransport>(singletonCImpl, 68));
      this.meshMessagingManagerProvider = DoubleCheck.provider(new SwitchingProvider<MeshMessagingManager>(singletonCImpl, 66));
      this.bleRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<BleRepositoryImpl>(singletonCImpl, 10));
      this.adaptiveMeshPowerManagerProvider = DoubleCheck.provider(new SwitchingProvider<AdaptiveMeshPowerManager>(singletonCImpl, 8));
      this.chatLocalDataSourceImplProvider = new SwitchingProvider<>(singletonCImpl, 70);
      this.bindChatLocalDataSourceProvider = DoubleCheck.provider((Provider) chatLocalDataSourceImplProvider);
      this.messagingRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 69);
      this.bindChatRepositoryProvider = DoubleCheck.provider((Provider) messagingRepositoryImplProvider);
      this.voiceRecorderProvider = DoubleCheck.provider(new SwitchingProvider<VoiceRecorder>(singletonCImpl, 71));
      this.voicePlayerProvider = DoubleCheck.provider(new SwitchingProvider<VoicePlayer>(singletonCImpl, 72));
      this.selfHealerProvider = DoubleCheck.provider(new SwitchingProvider<SelfHealer>(singletonCImpl, 74));
      this.runtimeWatchdogProvider = DoubleCheck.provider(new SwitchingProvider<RuntimeWatchdog>(singletonCImpl, 73));
    }

    @Override
    public void injectMeshLinkApp(MeshLinkApp meshLinkApp) {
      injectMeshLinkApp2(meshLinkApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private MeshLinkApp injectMeshLinkApp2(MeshLinkApp instance) {
      MeshLinkApp_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      MeshLinkApp_MembersInjector.injectBackgroundTaskScheduler(instance, backgroundTaskSchedulerProvider.get());
      MeshLinkApp_MembersInjector.injectAdaptivePowerManager(instance, adaptiveMeshPowerManagerProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.meshlink.service.work.CleanupWorker_AssistedFactory 
          return (T) new CleanupWorker_AssistedFactory() {
            @Override
            public CleanupWorker create(Context context, WorkerParameters workerParams) {
              return new CleanupWorker(context, workerParams, singletonCImpl.cacheManagerProvider.get(), singletonCImpl.provideRelayDaoProvider.get(), singletonCImpl.provideAuditLogDaoProvider.get());
            }
          };

          case 1: // com.meshlink.storage.data.local.CacheManager 
          return (T) new CacheManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.meshlink.database.data.local.RelayDao 
          return (T) DatabaseModule_ProvideRelayDaoFactory.provideRelayDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 3: // com.meshlink.database.data.local.MeshDatabase 
          return (T) DatabaseModule_ProvideMeshDatabaseFactory.provideMeshDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.databaseSecurityManagerProvider.get());

          case 4: // com.meshlink.security.data.DatabaseSecurityManager 
          return (T) new DatabaseSecurityManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.meshlink.database.data.local.AuditLogDao 
          return (T) DatabaseModule_ProvideAuditLogDaoFactory.provideAuditLogDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 6: // com.meshlink.service.work.RetryWorker_AssistedFactory 
          return (T) new RetryWorker_AssistedFactory() {
            @Override
            public RetryWorker create(Context context2, WorkerParameters workerParams2) {
              return new RetryWorker(context2, workerParams2);
            }
          };

          case 7: // com.meshlink.service.work.BackgroundTaskScheduler 
          return (T) new BackgroundTaskScheduler(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.meshlink.common.power.AdaptiveMeshPowerManager 
          return (T) new AdaptiveMeshPowerManager(singletonCImpl.powerStateManagerProvider.get(), singletonCImpl.bleRepositoryImplProvider.get());

          case 9: // com.meshlink.common.power.PowerStateManager 
          return (T) new PowerStateManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 10: // com.meshlink.ble.data.BleRepositoryImpl 
          return (T) new BleRepositoryImpl(ApplicationContextModule_ProvideApplicationFactory.provideApplication(singletonCImpl.applicationContextModule), singletonCImpl.bindBleMeshDataSourceProvider.get(), singletonCImpl.meshRouterProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.bindUserRepositoryProvider.get(), singletonCImpl.transferManagerProvider.get(), singletonCImpl.mediaTransferManagerProvider.get(), singletonCImpl.locationProvider.get(), singletonCImpl.meshCryptoManagerProvider.get(), singletonCImpl.wifiDirectManagerProvider.get(), singletonCImpl.wifiSocketTransportProvider.get(), singletonCImpl.sessionManagerProvider.get(), singletonCImpl.rekeyManagerProvider.get(), singletonCImpl.trustManagerProvider.get(), singletonCImpl.meshSecurityMonitorProvider.get(), singletonCImpl.discoveryManagerProvider.get(), singletonCImpl.bleConnectionManagerProvider.get(), singletonCImpl.routingCoordinatorProvider.get(), singletonCImpl.meshMessagingManagerProvider.get(), singletonCImpl.voiceTransportProvider.get(), singletonCImpl.videoTransportProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // com.meshlink.ble.data.source.BleMeshDataSourceImpl 
          return (T) new BleMeshDataSourceImpl(singletonCImpl.bleScannerManagerProvider.get(), singletonCImpl.bleAdvertiserManagerProvider.get(), singletonCImpl.bleGattManagerProvider.get());

          case 12: // com.meshlink.ble.data.BleScannerManager 
          return (T) new BleScannerManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.discoveryEngineProvider.get());

          case 13: // com.meshlink.ble.discovery.DiscoveryEngine 
          return (T) new DiscoveryEngine(singletonCImpl.batteryAwareScannerProvider.get());

          case 14: // com.meshlink.ble.discovery.BatteryAwareScanner 
          return (T) new BatteryAwareScanner(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 15: // com.meshlink.ble.data.BleAdvertiserManager 
          return (T) new BleAdvertiserManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 16: // com.meshlink.ble.data.BleGattManager 
          return (T) new BleGattManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 17: // com.meshlink.routing.data.MeshRouter 
          return (T) new MeshRouter(singletonCImpl.bleGattManagerProvider.get(), singletonCImpl.meshAnalyticsProvider.get(), singletonCImpl.provideRelayDaoProvider.get(), singletonCImpl.trustManagerProvider.get(), singletonCImpl.routingEngineProvider.get(), CoroutineModule_ProvideIoDispatcherFactory.provideIoDispatcher());

          case 18: // com.meshlink.analytics.data.MeshAnalytics 
          return (T) new MeshAnalytics();

          case 19: // com.meshlink.security.data.TrustManager 
          return (T) new TrustManager(singletonCImpl.provideTrustDaoProvider.get(), singletonCImpl.meshSecurityMonitorProvider.get(), CoroutineModule_ProvideIoDispatcherFactory.provideIoDispatcher());

          case 20: // com.meshlink.database.data.local.TrustDao 
          return (T) DatabaseModule_ProvideTrustDaoFactory.provideTrustDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 21: // com.meshlink.security.data.MeshSecurityMonitor 
          return (T) new MeshSecurityMonitor(singletonCImpl.provideAuditLogDaoProvider.get(), CoroutineModule_ProvideIoDispatcherFactory.provideIoDispatcher());

          case 22: // com.meshlink.routing.engine.RoutingEngine 
          return (T) new RoutingEngine(singletonCImpl.routeManagerProvider.get(), singletonCImpl.qoSManagerProvider.get(), singletonCImpl.congestionMonitorProvider.get(), singletonCImpl.routeHealthMonitorProvider.get(), singletonCImpl.networkTopologyEngineProvider.get(), singletonCImpl.batteryAwareNetworkingProvider.get(), singletonCImpl.intelligentTransportManagerProvider.get(), singletonCImpl.intelligentRetryEngineProvider.get(), singletonCImpl.queueOptimizerProvider.get(), singletonCImpl.routeOptimizerProvider.get());

          case 23: // com.meshlink.routing.engine.RouteManager 
          return (T) new RouteManager(singletonCImpl.routeCacheProvider.get(), singletonCImpl.routeScorerProvider.get(), singletonCImpl.routeOptimizerProvider.get());

          case 24: // com.meshlink.routing.engine.RouteCache 
          return (T) new RouteCache();

          case 25: // com.meshlink.routing.engine.RouteScorer 
          return (T) new RouteScorer();

          case 26: // com.meshlink.routing.engine.RouteOptimizer 
          return (T) new RouteOptimizer(singletonCImpl.routeCacheProvider.get(), singletonCImpl.failurePredictorProvider.get(), singletonCImpl.routePredictionEngineProvider.get());

          case 27: // com.meshlink.ai.engine.FailurePredictor 
          return (T) new FailurePredictor(singletonCImpl.learningRepositoryProvider.get());

          case 28: // com.meshlink.ai.data.LearningRepository 
          return (T) new LearningRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 29: // com.meshlink.ai.engine.RoutePredictionEngine 
          return (T) new RoutePredictionEngine(singletonCImpl.learningRepositoryProvider.get());

          case 30: // com.meshlink.routing.engine.QoSManager 
          return (T) new QoSManager();

          case 31: // com.meshlink.routing.engine.CongestionMonitor 
          return (T) new CongestionMonitor(singletonCImpl.congestionPredictorProvider.get());

          case 32: // com.meshlink.ai.engine.CongestionPredictor 
          return (T) new CongestionPredictor(singletonCImpl.learningRepositoryProvider.get());

          case 33: // com.meshlink.routing.engine.RouteHealthMonitor 
          return (T) new RouteHealthMonitor(singletonCImpl.routeCacheProvider.get(), singletonCImpl.routeScorerProvider.get());

          case 34: // com.meshlink.routing.engine.NetworkTopologyEngine 
          return (T) new NetworkTopologyEngine(singletonCImpl.routeCacheProvider.get());

          case 35: // com.meshlink.routing.engine.BatteryAwareNetworking 
          return (T) new BatteryAwareNetworking(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.batteryPredictorProvider.get(), singletonCImpl.emergencyManagerProvider.get());

          case 36: // com.meshlink.ai.engine.BatteryPredictor 
          return (T) new BatteryPredictor(singletonCImpl.learningRepositoryProvider.get());

          case 37: // com.meshlink.emergency.EmergencyManager 
          return (T) new EmergencyManager();

          case 38: // com.meshlink.routing.engine.IntelligentTransportManager 
          return (T) new IntelligentTransportManager(singletonCImpl.routeOptimizerProvider.get(), singletonCImpl.transportPredictorProvider.get());

          case 39: // com.meshlink.ai.engine.TransportPredictor 
          return (T) new TransportPredictor(singletonCImpl.learningRepositoryProvider.get(), singletonCImpl.batteryPredictorProvider.get());

          case 40: // com.meshlink.routing.engine.IntelligentRetryEngine 
          return (T) new IntelligentRetryEngine(singletonCImpl.congestionMonitorProvider.get(), singletonCImpl.batteryAwareNetworkingProvider.get(), singletonCImpl.learningRepositoryProvider.get());

          case 41: // com.meshlink.routing.engine.QueueOptimizer 
          return (T) new QueueOptimizer();

          case 42: // com.meshlink.database.data.local.ChatDao 
          return (T) DatabaseModule_ProvideChatDaoFactory.provideChatDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 43: // com.meshlink.core.data.UserRepositoryImpl 
          return (T) new UserRepositoryImpl(singletonCImpl.bindUserLocalDataSourceProvider.get(), singletonCImpl.bindCryptoDataSourceProvider.get());

          case 44: // com.meshlink.core.data.source.UserLocalDataSourceImpl 
          return (T) new UserLocalDataSourceImpl(singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideDataStoreProvider.get());

          case 45: // com.meshlink.database.data.local.UserDao 
          return (T) DatabaseModule_ProvideUserDaoFactory.provideUserDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 46: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) SystemModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 47: // com.meshlink.security.data.source.CryptoDataSourceImpl 
          return (T) new CryptoDataSourceImpl();

          case 48: // com.meshlink.transfer.TransferManager 
          return (T) new TransferManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.transferSchedulerProvider.get(), singletonCImpl.transferCacheProvider.get(), singletonCImpl.chunkManagerProvider.get(), singletonCImpl.fileMetadataManagerProvider.get(), singletonCImpl.integrityVerifierProvider.get(), singletonCImpl.transferAnalyticsProvider.get(), CoroutineModule_ProvideIoDispatcherFactory.provideIoDispatcher());

          case 49: // com.meshlink.transfer.TransferScheduler 
          return (T) new TransferScheduler();

          case 50: // com.meshlink.transfer.TransferCache 
          return (T) new TransferCache(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 51: // com.meshlink.transfer.ChunkManager 
          return (T) new ChunkManager();

          case 52: // com.meshlink.transfer.FileMetadataManager 
          return (T) new FileMetadataManager();

          case 53: // com.meshlink.transfer.IntegrityVerifier 
          return (T) new IntegrityVerifier();

          case 54: // com.meshlink.transfer.TransferAnalytics 
          return (T) new TransferAnalytics();

          case 55: // com.meshlink.media.data.MediaTransferManager 
          return (T) new MediaTransferManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), CoroutineModule_ProvideIoDispatcherFactory.provideIoDispatcher());

          case 56: // com.meshlink.data.location.LocationProvider 
          return (T) new LocationProvider(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 57: // com.meshlink.security.data.MeshCryptoManager 
          return (T) new MeshCryptoManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 58: // com.meshlink.wifi.data.WifiDirectManager 
          return (T) new WifiDirectManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideFirebaseAnalyticsProvider.get());

          case 59: // com.google.firebase.analytics.FirebaseAnalytics 
          return (T) SystemModule_ProvideFirebaseAnalyticsFactory.provideFirebaseAnalytics(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 60: // com.meshlink.wifi.data.WifiSocketTransport 
          return (T) new WifiSocketTransport();

          case 61: // com.meshlink.security.data.SessionManager 
          return (T) new SessionManager(singletonCImpl.meshCryptoManagerProvider.get(), singletonCImpl.trustManagerProvider.get(), singletonCImpl.meshSecurityMonitorProvider.get(), CoroutineModule_ProvideDefaultDispatcherFactory.provideDefaultDispatcher());

          case 62: // com.meshlink.security.data.RekeyManager 
          return (T) new RekeyManager(singletonCImpl.meshCryptoManagerProvider.get(), singletonCImpl.sessionManagerProvider.get(), singletonCImpl.bindUserRepositoryProvider.get(), CoroutineModule_ProvideDefaultDispatcherFactory.provideDefaultDispatcher());

          case 63: // com.meshlink.ble.data.DiscoveryManager 
          return (T) new DiscoveryManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.bindBleMeshDataSourceProvider.get(), singletonCImpl.discoveryEngineProvider.get());

          case 64: // com.meshlink.ble.data.BleConnectionManager 
          return (T) new BleConnectionManager(singletonCImpl.bindBleMeshDataSourceProvider.get(), singletonCImpl.discoveryEngineProvider.get());

          case 65: // com.meshlink.ble.data.RoutingCoordinator 
          return (T) new RoutingCoordinator(singletonCImpl.bindUserRepositoryProvider.get(), singletonCImpl.meshCryptoManagerProvider.get(), singletonCImpl.trustManagerProvider.get(), singletonCImpl.sessionManagerProvider.get(), singletonCImpl.rekeyManagerProvider.get(), singletonCImpl.meshRouterProvider.get(), singletonCImpl.bleConnectionManagerProvider.get(), singletonCImpl.discoveryManagerProvider.get());

          case 66: // com.meshlink.ble.data.MeshMessagingManager 
          return (T) new MeshMessagingManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.bindUserRepositoryProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.meshCryptoManagerProvider.get(), singletonCImpl.meshRouterProvider.get(), singletonCImpl.transferManagerProvider.get(), singletonCImpl.mediaTransferManagerProvider.get(), singletonCImpl.wifiDirectManagerProvider.get(), singletonCImpl.meshSecurityMonitorProvider.get(), singletonCImpl.locationProvider.get(), singletonCImpl.routingCoordinatorProvider.get(), singletonCImpl.sessionManagerProvider.get(), singletonCImpl.trustManagerProvider.get(), singletonCImpl.rekeyManagerProvider.get(), singletonCImpl.voiceTransportProvider.get(), singletonCImpl.videoTransportProvider.get(), singletonCImpl.bleConnectionManagerProvider.get(), singletonCImpl.discoveryManagerProvider.get());

          case 67: // com.meshlink.voice.transport.VoiceTransport 
          return (T) new VoiceTransport(singletonCImpl.meshCryptoManagerProvider.get(), CoroutineModule_ProvideIoDispatcherFactory.provideIoDispatcher());

          case 68: // com.meshlink.video.transport.VideoTransport 
          return (T) new VideoTransport(singletonCImpl.meshCryptoManagerProvider.get(), CoroutineModule_ProvideIoDispatcherFactory.provideIoDispatcher());

          case 69: // com.meshlink.messaging.data.MessagingRepositoryImpl 
          return (T) new MessagingRepositoryImpl(singletonCImpl.bindChatLocalDataSourceProvider.get());

          case 70: // com.meshlink.database.data.source.ChatLocalDataSourceImpl 
          return (T) new ChatLocalDataSourceImpl(singletonCImpl.provideChatDaoProvider.get());

          case 71: // com.meshlink.media.data.VoiceRecorder 
          return (T) new VoiceRecorder(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), CoroutineModule_ProvideDefaultDispatcherFactory.provideDefaultDispatcher(), CoroutineModule_ProvideMainDispatcherFactory.provideMainDispatcher());

          case 72: // com.meshlink.media.data.VoicePlayer 
          return (T) new VoicePlayer(CoroutineModule_ProvideDefaultDispatcherFactory.provideDefaultDispatcher());

          case 73: // com.meshlink.common.diagnostics.RuntimeWatchdog 
          return (T) new RuntimeWatchdog(singletonCImpl.selfHealerProvider.get());

          case 74: // com.meshlink.common.diagnostics.SelfHealer 
          return (T) new SelfHealer(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.bleScannerManagerProvider.get(), singletonCImpl.bleAdvertiserManagerProvider.get(), singletonCImpl.bindUserRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
