package com.streak.examprepai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.streak.examprepai.data.ExamPrepRepository
import com.streak.examprepai.ui.ExamPrepApp
import com.streak.examprepai.ui.ExamPrepViewModel
import com.streak.examprepai.ui.ExamPrepViewModelFactory
import com.streak.examprepai.ui.theme.ExamPrepAITheme

class MainActivity : ComponentActivity() {

    private val viewModel: ExamPrepViewModel by viewModels {
        ExamPrepViewModelFactory(
            repository = ExamPrepRepository(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExamPrepAITheme {
                ExamPrepApp(viewModel = viewModel)
            }
        }
    }
}
