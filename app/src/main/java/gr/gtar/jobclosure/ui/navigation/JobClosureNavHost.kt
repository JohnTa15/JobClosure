package gr.gtar.jobclosure.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gr.gtar.jobclosure.JobClosureApp
import gr.gtar.jobclosure.data.AppSettings
import gr.gtar.jobclosure.ui.bookingdetail.BookingDetailScreen
import gr.gtar.jobclosure.ui.bookingdetail.BookingDetailViewModel
import gr.gtar.jobclosure.ui.bookingdetail.NewBookingDetailScreen
import gr.gtar.jobclosure.ui.bookingedit.BookingEditScreen
import gr.gtar.jobclosure.ui.bookingedit.BookingEditViewModel
import gr.gtar.jobclosure.ui.bookingedit.NewBookingEditScreen
import gr.gtar.jobclosure.ui.bookinglist.BookingListScreen
import gr.gtar.jobclosure.ui.bookinglist.BookingListViewModel
import gr.gtar.jobclosure.ui.bookinglist.NewBookingListScreen
import gr.gtar.jobclosure.ui.components.NewDesignEasing
import gr.gtar.jobclosure.ui.importcalendar.CalendarImportScreen
import gr.gtar.jobclosure.ui.importcalendar.CalendarImportViewModel
import gr.gtar.jobclosure.ui.settings.NewSettingsScreen
import gr.gtar.jobclosure.ui.settings.SettingsScreen
import gr.gtar.jobclosure.ui.settings.SettingsViewModel
import gr.gtar.jobclosure.ui.theme.AppTheme
import gr.gtar.jobclosure.ui.theme.AppThemePalettes

private const val ROUTE_LIST = "list"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_IMPORT_CALENDAR = "import-calendar"
private const val ARG_BOOKING_ID = "bookingId"
private const val ROUTE_EDIT = "edit/{$ARG_BOOKING_ID}"
private const val ROUTE_DETAIL = "detail/{$ARG_BOOKING_ID}"
private const val NEW_BOOKING_ID = -1L

/**
 * Picks the classic or restyled ("new design") composable for each destination, based on the
 * Settings > Νέα εμφάνιση switch (design_handoff_theme_switcher) - off by default, so nothing
 * changes for anyone until they opt in, and switching back is just the same toggle again.
 */
@Composable
fun JobClosureNavHost(app: JobClosureApp) {
    val navController = rememberNavController()
    val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())
    val useNewDesign = settings.useNewDesign

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST,
        enterTransition = {
            if (useNewDesign) {
                fadeIn(tween(400, easing = NewDesignEasing)) +
                    slideInHorizontally(tween(400, easing = NewDesignEasing)) { it / 12 } +
                    scaleIn(tween(400, easing = NewDesignEasing), initialScale = 0.99f)
            } else {
                slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn()
            }
        },
        exitTransition = {
            if (useNewDesign) {
                fadeOut(tween(200))
            } else {
                slideOutHorizontally(targetOffsetX = { -it / 5 }) + fadeOut()
            }
        },
        popEnterTransition = {
            if (useNewDesign) {
                fadeIn(tween(400, easing = NewDesignEasing)) +
                    slideInHorizontally(tween(400, easing = NewDesignEasing)) { -it / 12 } +
                    scaleIn(tween(400, easing = NewDesignEasing), initialScale = 0.99f)
            } else {
                slideInHorizontally(initialOffsetX = { -it / 5 }) + fadeIn()
            }
        },
        popExitTransition = {
            if (useNewDesign) {
                fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.99f)
            } else {
                slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut()
            }
        },
    ) {
        composable(ROUTE_LIST) {
            val viewModel: BookingListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { BookingListViewModel(app, app.bookingRepository, app.settingsRepository) }
                },
            )
            if (useNewDesign) {
                NewBookingListScreen(
                    viewModel = viewModel,
                    onAddBooking = { navController.navigate("edit/$NEW_BOOKING_ID") },
                    onOpenBooking = { id -> navController.navigate("detail/$id") },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                )
            } else {
                BookingListScreen(
                    viewModel = viewModel,
                    onAddBooking = { navController.navigate("edit/$NEW_BOOKING_ID") },
                    onOpenBooking = { id -> navController.navigate("detail/$id") },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                )
            }
        }

        composable(
            ROUTE_EDIT,
            arguments = listOf(navArgument(ARG_BOOKING_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong(ARG_BOOKING_ID) ?: NEW_BOOKING_ID
            val isNew = bookingId == NEW_BOOKING_ID
            val viewModel: BookingEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        BookingEditViewModel(
                            application = app,
                            repository = app.bookingRepository,
                            settingsRepository = app.settingsRepository,
                            placeSearchRepository = app.placeSearchRepository,
                            bookingId = if (isNew) null else bookingId,
                        )
                    }
                },
            )
            if (useNewDesign) {
                NewBookingEditScreen(viewModel = viewModel, isNew = isNew, onDone = { navController.popBackStack() })
            } else {
                BookingEditScreen(viewModel = viewModel, isNew = isNew, onDone = { navController.popBackStack() })
            }
        }

        composable(
            ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_BOOKING_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong(ARG_BOOKING_ID) ?: NEW_BOOKING_ID
            val viewModel: BookingDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        BookingDetailViewModel(
                            application = app,
                            bookingRepository = app.bookingRepository,
                            settingsRepository = app.settingsRepository,
                            travelTimeRepository = app.travelTimeRepository,
                            droneConditionsRepository = app.droneConditionsRepository,
                            placeSearchRepository = app.placeSearchRepository,
                            bookingId = bookingId,
                        )
                    }
                },
            )
            if (useNewDesign) {
                NewBookingDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("edit/$id") },
                )
            } else {
                BookingDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("edit/$id") },
                )
            }
        }

        composable(ROUTE_SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(
                            app,
                            app.settingsRepository,
                            app.updateRepository,
                            app.crashReportSender,
                            app.placeSearchRepository,
                        )
                    }
                },
            )
            if (useNewDesign) {
                NewSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onImportFromCalendar = { navController.navigate(ROUTE_IMPORT_CALENDAR) },
                )
            } else {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onImportFromCalendar = { navController.navigate(ROUTE_IMPORT_CALENDAR) },
                )
            }
        }

        composable(ROUTE_IMPORT_CALENDAR) {
            val viewModel: CalendarImportViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { CalendarImportViewModel(app, app.bookingRepository) }
                },
            )
            CalendarImportScreen(
                viewModel = viewModel,
                palette = AppThemePalettes.getValue(AppTheme.fromKey(settings.themeKey)),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
