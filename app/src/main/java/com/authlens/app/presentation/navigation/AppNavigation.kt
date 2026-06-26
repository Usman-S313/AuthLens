package com.authlens.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.authlens.app.presentation.result.ResultScreen
import com.authlens.app.presentation.result.ResultViewModel
import com.authlens.app.presentation.upload.UploadScreen
import com.authlens.app.presentation.upload.UploadViewModel

/**
 * Root navigation host for AuthLens.
 *
 * Two destinations:
 *  - [Routes.UPLOAD] — pick/capture a document, choose its type, run analysis
 *  - [Routes.RESULT] — render the fraud score + per-stage findings
 *
 * The result destination receives the analyzed [com.authlens.app.domain.model.FraudResult]
 * via the shared [ResultViewModel] (same Hilt instance scoped to the activity).
 */
@Composable
fun AuthLensApp() {
    val navController = rememberNavController()
    val uploadViewModel: UploadViewModel = hiltViewModel()
    val resultViewModel: ResultViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Routes.UPLOAD) {

        composable(Routes.UPLOAD) {
            UploadScreen(
                viewModel = uploadViewModel,
                resultViewModel = resultViewModel,
                onAnalysisComplete = { navController.navigate(Routes.RESULT) },
            )
        }

        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = resultViewModel,
                onBack = { navController.popBackStack() },
                onNewScan = {
                    navController.popBackStack(Routes.UPLOAD, inclusive = false)
                },
            )
        }
    }
}

object Routes {
    const val UPLOAD = "upload"
    const val RESULT = "result"
}
