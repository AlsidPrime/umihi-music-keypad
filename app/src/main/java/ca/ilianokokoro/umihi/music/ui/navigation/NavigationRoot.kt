package ca.ilianokokoro.umihi.music.ui.navigation

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadNavigationFocus
import ca.ilianokokoro.umihi.music.ui.components.keypad.LocalKeypadNavigationFocus
import ca.ilianokokoro.umihi.music.ui.components.keypad.isKeypadScreen
import ca.ilianokokoro.umihi.music.core.helpers.LogHelper.printe
import ca.ilianokokoro.umihi.music.ui.components.miniplayer.MiniPlayerWrapper
import ca.ilianokokoro.umihi.music.ui.navigation.viewmodels.SharedViewModel
import ca.ilianokokoro.umihi.music.ui.screens.auth.AuthScreen
import ca.ilianokokoro.umihi.music.ui.screens.home.HomeScreen
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerScreen
import ca.ilianokokoro.umihi.music.ui.screens.playlist.PlaylistScreen
import ca.ilianokokoro.umihi.music.ui.screens.search.SearchScreen
import ca.ilianokokoro.umihi.music.ui.screens.settings.SettingsScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val sharedViewModel: SharedViewModel = viewModel()
    val backStack = rememberNavBackStack(HomeScreenKey)
    fun openTab(key: NavKey) {
        val existing = backStack.indexOfLast { it == key }
        if (existing < 0) backStack.add(key)
        else while (backStack.lastIndex > existing) backStack.removeAt(backStack.lastIndex)
    }
    val app = LocalContext.current.applicationContext as Application
    val currentScreen = backStack.last()
    val screenConfig = rememberScreenUiConfig(currentScreen)
    val keypad = isKeypadScreen()
    val keypadNavigationFocus = remember { KeypadNavigationFocus() }

    var showFullPlayer by remember { mutableStateOf(false) }
    var bottomBarHeightPixels by remember { mutableIntStateOf(0) }
    val bottomBarHeightDp = with(LocalDensity.current) { bottomBarHeightPixels.toDp() }
    val playerSheetState =
        rememberBottomSheetState(
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            initialValue = SheetValue.Hidden
        )


    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->

        val miniPlayerBottomPadding by animateDpAsState(
            targetValue = if (!screenConfig.showBottomBar) {
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            } else {
                bottomBarHeightDp
            },
            animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            CompositionLocalProvider(LocalKeypadNavigationFocus provides if (keypad) keypadNavigationFocus else null) {
                Column(Modifier.fillMaxSize().then(if (keypad) Modifier.navigationBarsPadding() else Modifier)) {
                    NavDisplay(
                        modifier = Modifier
                            .weight(1f).fillMaxWidth(),
                        backStack = backStack,
                        onBack = backStack::safePop,
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        transitionSpec = {
                            (scaleIn(
                                animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION),
                                initialScale = 0.85f
                            ) +
                                    fadeIn(animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION))) togetherWith
                                    (scaleOut(
                                        animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION),
                                        targetScale = 1.1f
                                    ) +
                                            fadeOut(animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION)))
                        },
                        popTransitionSpec = {
                            (scaleIn(
                                animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION),
                                initialScale = 1.1f
                            ) +
                                    fadeIn(animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION))) togetherWith
                                    (scaleOut(
                                        animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION),
                                        targetScale = 0.85f
                                    ) +
                                            fadeOut(animationSpec = tween(if (keypad) 0 else Constants.Animation.NAVIGATION_DURATION)))
                        },
                        predictivePopTransitionSpec = {
                            (scaleIn(
                                animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                                initialScale = 1.1f
                            ) +
                                    fadeIn(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION))) togetherWith
                                    (scaleOut(
                                        animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                                        targetScale = 0.85f
                                    ) +
                                            fadeOut(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)))
                        },
                        entryProvider = { key ->
                            when (key) {

                                is HomeScreenKey -> NavEntry(key) {
                                    HomeScreen(
                                        sharedViewModel = sharedViewModel,
                                        onPlaylistPressed = { playlist ->
                                            backStack.add(PlaylistScreenKey(playlistInfo = playlist))
                                        },
                                        onLoginPressed = { backStack.add(AuthScreenKey) },
                                        application = app
                                    )
                                }

                                is SettingsScreenKey -> NavEntry(key) {
                                    SettingsScreen(
                                        sharedViewModel = sharedViewModel,
                                        onBack = backStack::safePop,
                                        openAuthScreen = { backStack.add(AuthScreenKey) },
                                        application = app
                                    )
                                }

                                is PlaylistScreenKey -> NavEntry(key) {
                                    PlaylistScreen(
                                        sharedViewModel = sharedViewModel,
                                        playlistInfo = key.playlistInfo,
                                        onBack = backStack::safePop,
                                        onOpenPlayer = { showFullPlayer = true },
                                        application = app
                                    )
                                }

                                is AuthScreenKey -> NavEntry(key) {
                                    AuthScreen(
                                        onBack = backStack::safePop,
                                        sharedViewModel = sharedViewModel,
                                        application = app
                                    )
                                }

                                is SearchScreenKey -> NavEntry(key) {
                                    SearchScreen(
                                        application = app,
                                        onOpenPlayer = { showFullPlayer = true },
                                    )
                                }

                                else -> throw RuntimeException(
                                    app.getString(
                                        R.string.invalid_navkey,
                                        key
                                    )
                                )
                            }
                        }
                    )
                    if (keypad) {
                        KeypadNavigationBar(keypadNavigationFocus,
                            onHome = { openTab(HomeScreenKey) },
                            onSearch = { openTab(SearchScreenKey) },
                            onSettings = { openTab(SettingsScreenKey) },
                            onPlayer = {
                                if ((PlayerManager.currentController?.mediaItemCount ?: 0) > 0) showFullPlayer = true
                                else Toast.makeText(app, app.getString(R.string.keypad_start_song), Toast.LENGTH_SHORT).show()
                            })
                    }
                }
            }

            if (!keypad) {
                MiniPlayerWrapper(
                    showMiniPlayer = screenConfig.showMiniPlayer && !showFullPlayer,
                    onMiniPlayerPressed = { showFullPlayer = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = miniPlayerBottomPadding)
                )
            }


            AnimatedVisibility(
                visible = !keypad && screenConfig.showBottomBar,
                enter = slideInVertically(
                    animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                    targetOffsetY = { it }
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomNavigationBar(
                    currentTab = screenConfig.selectedTab,
                    onTabSelected = { key ->
                        if (backStack.last() != key) {
                            backStack.add(key)
                        }
                    },
                    modifier = Modifier.onSizeChanged { bottomBarHeightPixels = it.height }
                )
            }
        }

    }



    if (showFullPlayer) {
        ModalBottomSheet(
            // The default handle can take D-pad focus outside PlayerScreen.
            dragHandle = null,
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                showFullPlayer = false
            },
            sheetState = playerSheetState
        ) {
            PlayerScreen(onBack = { showFullPlayer = false }, application = app)
        }
    }

}


fun NavBackStack<NavKey>.safePop() {
    if (this.size > 1) {
        this.removeLastOrNull()
    } else {
        printe("Backstack Pop was called unsafely")
    }
}
